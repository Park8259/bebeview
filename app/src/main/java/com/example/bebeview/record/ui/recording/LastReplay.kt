package com.example.bebeview.record.ui.recording

import com.example.bebeview.record.data.FaceTrace
import com.example.bebeview.record.data.TaskType
import java.io.File

// 방금 분석한 영상과 좌표 (growthLens store/lastReplay.ts).
// 결과 화면의 "AI가 본 장면"에만 쓰고, 저장하지 않아요. 앱을 다시 켜거나 다음 과제를 분석하면 사라져요.
// 영상은 기기 밖으로 나가지 않아요.

data class LastReplay(val task: TaskType, val video: File, val trace: FaceTrace)

object LastReplayHolder {
    private var last: LastReplay? = null

    fun set(r: LastReplay?) {
        last = r
    }

    fun get(task: TaskType): LastReplay? = last?.takeIf { it.task == task && it.video.exists() }
}
