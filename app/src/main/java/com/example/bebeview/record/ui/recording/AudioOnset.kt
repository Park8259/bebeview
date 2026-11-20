package com.example.bebeview.record.ui.recording

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteOrder
import kotlin.math.sqrt

// 데모용: 촬영 대신 영상 파일을 골라 분석할 때 쓰는 도구 (growthLens recording/fileDemo.ts)

/**
 * 영상 속 소리에서 "이름을 부른 순간"(처음으로 소리가 크게 커지는 시점)을 찾아요. 단위는 ms.
 * 앱 촬영에서는 신호음 시각을 쓰지만, 파일에는 신호음이 없어서 소리로 대신해요.
 * 소리가 없거나 찾지 못하면 null.
 *
 * 웹은 OfflineAudioContext(44.1kHz)로 디코딩하지만, 여기서는 MediaCodec으로 첫 채널을 PCM으로 풀어
 * 0.05초 단위 RMS 를 같은 규칙(중앙값 × 5, 최소 0.05, 앞 0.5초 무시)으로 봐요.
 */
fun detectCallTime(file: File): Long? {
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    try {
        extractor.setDataSource(file.absolutePath)
        val track = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: return null
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        codec = MediaCodec.createDecoderByType(mime).apply {
            configure(format, null, null, 0)
            start()
        }

        // 첫 채널만 0.05초 단위로 제곱합을 모음
        val rms = ArrayList<Double>()
        var win = (sampleRate * 0.05).toInt().coerceAtLeast(1)
        var sum = 0.0
        var count = 0

        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        while (!outputDone) {
            if (!inputDone) {
                val inIdx = codec.dequeueInputBuffer(10_000)
                if (inIdx >= 0) {
                    val buf = codec.getInputBuffer(inIdx)!!
                    val size = extractor.readSampleData(buf, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIdx = codec.dequeueOutputBuffer(info, 10_000)
            when {
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val of = codec.outputFormat
                    sampleRate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    win = (sampleRate * 0.05).toInt().coerceAtLeast(1)
                }
                outIdx >= 0 -> {
                    val out = codec.getOutputBuffer(outIdx)!!
                    out.position(info.offset)
                    out.limit(info.offset + info.size)
                    val shorts = out.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    var i = 0
                    while (i < shorts.remaining()) {
                        val v = shorts.get(i) / 32768.0
                        sum += v * v
                        if (++count == win) {
                            rms.add(sqrt(sum / win))
                            sum = 0.0
                            count = 0
                        }
                        i += channels
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        }

        if (rms.isEmpty()) return null
        val median = rms.sorted()[rms.size / 2]
        // 평소 소리보다 5배 이상, 그리고 아주 작은 잡음은 무시
        val threshold = maxOf(0.05, median * 5)
        // 맨 앞 0.5초는 영상 시작 잡음일 수 있어서 건너뜀
        val start = kotlin.math.ceil(0.5 / 0.05).toInt()
        for (i in start until rms.size) {
            if (rms[i] >= threshold) return i * 50L
        }
        return null
    } catch (_: Exception) {
        return null
    } finally {
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        extractor.release()
    }
}
