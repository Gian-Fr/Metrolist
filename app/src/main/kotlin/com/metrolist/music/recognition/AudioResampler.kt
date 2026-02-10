package com.metrolist.music.recognition

import android.media.AudioFormat
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Data class representing decoded audio data with its properties.
 */
data class DecodedAudio(
    val data: ByteArray,
    val channelCount: Int,
    val sampleRate: Int,
    val pcmEncoding: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DecodedAudio
        return data.contentEquals(other.data) &&
                channelCount == other.channelCount &&
                sampleRate == other.sampleRate &&
                pcmEncoding == other.pcmEncoding
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + channelCount
        result = 31 * result + sampleRate
        result = 31 * result + pcmEncoding
        return result
    }
}

/**
 * Audio resampler using Media3 SonicAudioProcessor.
 * Resamples audio to the required sample rate for fingerprinting.
 */
@OptIn(UnstableApi::class)
object AudioResampler {

    suspend fun resample(
        decodedAudio: DecodedAudio,
        outputSampleRate: Int
    ): Result<DecodedAudio> = withContext(Dispatchers.Default) {
        if (decodedAudio.sampleRate == outputSampleRate) {
            return@withContext Result.success(decodedAudio)
        }
        
        var sonicRef: AudioProcessor? = null
        try {
            val sonic: AudioProcessor = SonicAudioProcessor().apply {
                setOutputSampleRateHz(outputSampleRate)
            }
            sonicRef = sonic
            
            val inputFormat = AudioProcessor.AudioFormat(
                decodedAudio.sampleRate,
                decodedAudio.channelCount,
                decodedAudio.pcmEncoding
            )
            val outputFormat = sonic.configure(inputFormat)
            sonic.flush()

            val inputBuf = ByteBuffer.wrap(decodedAudio.data).order(ByteOrder.nativeOrder())
            sonic.queueInput(inputBuf)
            sonic.queueEndOfStream()

            val outputChunks = mutableListOf<ByteArray>()
            var outputChunksByteSize = 0

            while (!sonic.isEnded) {
                ensureActive()
                val outputBuffer = sonic.output
                if (!outputBuffer.hasRemaining()) continue
                val chunk = ByteArray(outputBuffer.remaining())
                outputBuffer.get(chunk)
                outputChunks.add(chunk)
                outputChunksByteSize += chunk.size
            }
            sonic.reset()

            val resampledData = if (outputChunks.size == 1) {
                outputChunks[0]
            } else {
                ByteArray(outputChunksByteSize).also {
                    var dest = 0
                    for (chunk in outputChunks) {
                        System.arraycopy(chunk, 0, it, dest, chunk.size)
                        dest += chunk.size
                    }
                }
            }
            
            Result.success(DecodedAudio(
                data = resampledData,
                channelCount = outputFormat.channelCount,
                sampleRate = outputFormat.sampleRate,
                pcmEncoding = outputFormat.encoding,
            ))
        } catch (e: Exception) {
            ensureActive()
            Result.failure(e)
        } finally {
            sonicRef?.reset()
        }
    }

    /**
     * Convert stereo to mono by averaging channels.
     */
    fun convertToMono(audio: DecodedAudio): DecodedAudio {
        if (audio.channelCount == 1) return audio
        
        val bytesPerSample = when (audio.pcmEncoding) {
            AudioFormat.ENCODING_PCM_16BIT -> 2
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_FLOAT -> 4
            else -> 2
        }
        
        val frameCount = audio.data.size / (bytesPerSample * audio.channelCount)
        val monoData = ByteArray(frameCount * bytesPerSample)
        
        val inputBuffer = ByteBuffer.wrap(audio.data).order(ByteOrder.LITTLE_ENDIAN)
        val outputBuffer = ByteBuffer.wrap(monoData).order(ByteOrder.LITTLE_ENDIAN)
        
        for (i in 0 until frameCount) {
            when (audio.pcmEncoding) {
                AudioFormat.ENCODING_PCM_16BIT -> {
                    var sum = 0
                    for (ch in 0 until audio.channelCount) {
                        sum += inputBuffer.getShort().toInt()
                    }
                    outputBuffer.putShort((sum / audio.channelCount).toShort())
                }
                else -> {
                    // For simplicity, just take the first channel for other formats
                    for (ch in 0 until audio.channelCount) {
                        if (ch == 0) {
                            for (b in 0 until bytesPerSample) {
                                outputBuffer.put(inputBuffer.get())
                            }
                        } else {
                            inputBuffer.position(inputBuffer.position() + bytesPerSample)
                        }
                    }
                }
            }
        }
        
        return DecodedAudio(
            data = monoData,
            channelCount = 1,
            sampleRate = audio.sampleRate,
            pcmEncoding = audio.pcmEncoding
        )
    }
}
