package com.example.bebeview.record.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bebeview.ui.theme.Bebe
import com.example.bebeview.ui.theme.bebeText
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// growthLens src/components/WheelPicker.tsx 포팅

private val ROW = 44.dp  // 한 줄 높이 (Figma 휠 간격)
private const val VISIBLE = 5 // 보이는 줄 수

/** 휠 안의 스크롤이 바텀시트를 끌어내리지 않게 남은 스크롤을 모두 먹음 */
private val swallowNestedScroll = object : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) = available
    override suspend fun onPostFling(consumed: Velocity, available: Velocity) = available
}

/**
 * 스크롤해서 고르는 휠 선택기 한 열.
 * 가운데 줄이 선택값이에요. 가운데는 굵고 진하게, 멀어질수록 작고 흐리게 보여요.
 */
@Composable
fun <T> RowScope.WheelPicker(
    items: List<T>,
    value: T,
    onChange: (T) -> Unit,
    format: (T) -> String = { it.toString() },
) {
    val selected = items.indexOf(value).coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selected)
    val scope = rememberCoroutineScope()
    val rowPx = with(LocalDensity.current) { ROW.toPx() }
    val latestItems by rememberUpdatedState(items)
    val latestValue by rememberUpdatedState(value)
    val latestOnChange by rememberUpdatedState(onChange)

    // 가운데 줄 (스크롤 중에도 바로 바뀜)
    val active by remember {
        derivedStateOf {
            (state.firstVisibleItemIndex + (state.firstVisibleItemScrollOffset / rowPx).roundToInt())
                .coerceIn(0, (latestItems.size - 1).coerceAtLeast(0))
        }
    }

    // 스크롤이 멈추면 값 반영
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                val item = latestItems.getOrNull(active) ?: return@collect
                if (item != latestValue) latestOnChange(item)
            }
    }

    // 바깥에서 값이 바뀌면(예: 월이 바뀌어 일 수가 줄어듦) 그 위치로 옮김
    LaunchedEffect(selected, items.size) {
        if (!state.isScrollInProgress && active != selected) state.scrollToItem(selected)
    }

    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state, SnapPosition.Center),
        contentPadding = PaddingValues(vertical = ROW * (VISIBLE / 2)),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .height(ROW * VISIBLE)
            .nestedScroll(swallowNestedScroll),
    ) {
        itemsIndexed(items) { i, item ->
            val d = abs(i - active)
            val style = when (d) {
                0 -> bebeText(20.sp, Bebe.Ink, bold = true)
                1 -> bebeText(17.sp, Bebe.InkHint)
                else -> bebeText(15.sp, Bebe.InkFaint)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(ROW)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        scope.launch { state.animateScrollToItem(i) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(format(item), style = style, maxLines = 1)
            }
        }
    }
}

/** 휠 여러 열을 나란히 놓는 틀. highlight 가 켜지면 가운데 줄에 회색 띠를 깔아요. */
@Composable
fun WheelGroup(highlight: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().height(ROW * VISIBLE)) {
        if (highlight) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .offset(y = ROW * 2)
                    .height(ROW)
                    .background(Bebe.FillWheel, RoundedCornerShape(10.dp)),
            )
        }
        Row(Modifier.fillMaxWidth().height(ROW * VISIBLE), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
