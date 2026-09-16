package co.jp.kpbr.gomiman.ui.screens.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.jp.kpbr.gomiman.data.model.GarbageCollectionModel
import co.jp.kpbr.gomiman.data.model.GarbageType
import co.jp.kpbr.gomiman.ui.theme.DefaultThemeColor
import co.jp.kpbr.gomiman.ui.theme.DividerColor
import co.jp.kpbr.gomiman.ui.theme.TextSecondary
import co.jp.kpbr.gomiman.utils.DateUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class TimelineItem(
    val date: LocalDate,
    val daysAfter: Long,
    val types: List<GarbageType>
)

@Composable
fun CalendarTimelineScreen(
    garbageModels: List<GarbageCollectionModel>,
    onNavigateToAdd: () -> Unit,
    onNavigateToCalendarAppend: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val isCollected = remember { LocalTime.now().hour >= 9 }
    val todayTypes = remember(garbageModels) {
        DateUtils.getGarbageTypesForDate(today, garbageModels)
    }

    // Generate upcoming 60-day timeline items that have garbage collection
    val timelineItems = remember(garbageModels) {
        val list = mutableListOf<TimelineItem>()
        if (garbageModels.isNotEmpty()) {
            for (i in 1..60) {
                val futureDate = today.plusDays(i.toLong())
                val types = DateUtils.getGarbageTypesForDate(futureDate, garbageModels)
                if (types.isNotEmpty()) {
                    list.add(
                        TimelineItem(
                            date = futureDate,
                            daysAfter = ChronoUnit.DAYS.between(today, futureDate),
                            types = types
                        )
                    )
                }
            }
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Header: Big Day of Week + Date + Append button
        TimelineHeader(
            today = today,
            hasGarbageSchedules = garbageModels.isNotEmpty(),
            onNavigateToCalendarAppend = onNavigateToCalendarAppend
        )

        // Today's garbage collection status row
        TodayGarbageRow(
            todayTypes = todayTypes,
            isCollected = isCollected
        )

        HorizontalDivider(
            color = DividerColor,
            thickness = 1.dp
        )

        if (garbageModels.isEmpty()) {
            EmptyScheduleView(onNavigateToAdd = onNavigateToAdd)
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                itemsIndexed(timelineItems) { index, item ->
                    TimelineListItem(
                        item = item,
                        isFirst = index == 0,
                        isLast = index == timelineItems.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineHeader(
    today: LocalDate,
    hasGarbageSchedules: Boolean,
    onNavigateToCalendarAppend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val weekdayName = DateUtils.weekdayInHanji.getOrElse(today.dayOfWeek.value) { "" }
        Text(
            text = weekdayName,
            color = DefaultThemeColor,
            fontSize = 50.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = DateUtils.convertJPYear(today.year),
                fontSize = 14.sp,
                color = TextSecondary
            )
            Text(
                text = "${today.monthValue}月${today.dayOfMonth}日",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DefaultThemeColor
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (hasGarbageSchedules) {
            OutlinedButton(
                onClick = onNavigateToCalendarAppend,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DefaultThemeColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DefaultThemeColor),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "カレンダーに登録",
                    fontSize = 12.sp,
                    color = DefaultThemeColor
                )
            }
        }
    }
}

@Composable
private fun TodayGarbageRow(
    todayTypes: List<GarbageType>,
    isCollected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (todayTypes.isEmpty()) {
            GarbageTypeBadge(text = "なし")
        } else {
            todayTypes.forEach { type ->
                GarbageTypeBadge(text = type.shortName)
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (isCollected) {
                Text(
                    text = "【収集済み】",
                    color = DefaultThemeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun GarbageTypeBadge(text: String) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .border(1.dp, DefaultThemeColor, CircleShape)
            .background(Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = DefaultThemeColor,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun EmptyScheduleView(onNavigateToAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "収集日の登録がありません",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DefaultThemeColor
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(40.dp)
                .border(1.dp, DefaultThemeColor, RoundedCornerShape(4.dp))
                .clickable { onNavigateToAdd() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "設定する",
                fontSize = 16.sp,
                color = DefaultThemeColor
            )
        }
    }
}

@Composable
private fun TimelineListItem(
    item: TimelineItem,
    isFirst: Boolean,
    isLast: Boolean
) {
    val weekday = DateUtils.weekdayInHanji.getOrElse(item.date.dayOfWeek.value) { "" }
    val dateHeadline = when (item.daysAfter) {
        1L -> "明日（$weekday）"
        2L -> "明後日（$weekday）"
        else -> "${item.daysAfter}日後（$weekday）"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Vertical connecting line with circle marker
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(84.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(if (isLast) Color.Transparent else DividerColor)
            )
            // Node
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(14.dp)
                    .border(2.dp, DefaultThemeColor, CircleShape)
                    .background(Color.White, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = dateHeadline,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DefaultThemeColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item.types.forEach { type ->
                    GarbageTypeBadge(text = type.shortName)
                }
            }
        }
    }
}
