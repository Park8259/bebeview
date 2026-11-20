package com.example.bebeview

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.bebeview.record.ui.RecordAppRoot
import com.example.bebeview.ui.theme.BebeviewTheme

// growthLens(웹 버전)를 네이티브 Compose(RecordAppRoot)로 옮긴 앱.
// 카메라·마이크 권한은 웹처럼 촬영 화면에 들어갈 때 요청해요 (RecordingScreen).
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 웹 화면처럼 흰 바탕이라 다크 모드에서도 상태바 아이콘을 어둡게 유지
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            BebeviewTheme(dynamicColor = false) {
                RecordAppRoot()
            }
        }
    }
}
