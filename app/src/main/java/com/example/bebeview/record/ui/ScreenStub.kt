package com.example.bebeview.record.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** 아직 포팅 전인 화면 자리 */
@Composable
fun ScreenStub(name: String) {
    Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) { Text(name) }
}
