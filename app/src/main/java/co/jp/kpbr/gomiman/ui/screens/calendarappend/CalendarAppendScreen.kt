package co.jp.kpbr.gomiman.ui.screens.calendarappend

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.jp.kpbr.gomiman.data.model.GarbageCollectionModel
import co.jp.kpbr.gomiman.data.repository.CalendarRepository
import co.jp.kpbr.gomiman.ui.theme.DefaultThemeColor
import co.jp.kpbr.gomiman.ui.theme.DividerColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarAppendScreen(
    garbageModels: List<GarbageCollectionModel>,
    calendarRepository: CalendarRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var eventPeriod by remember { mutableIntStateOf(1) } // 0: 1 week, 1: 1 month, 2: 2 months
    var collectionDateFlag by remember { mutableIntStateOf(0) } // 0: day before, 1: day of
    var selectedTimeDayBefore by remember { mutableIntStateOf(0) } // 0: 19:00, 1: 20:00, 2: 21:00, 3: 22:00, 4: 23:00
    var selectedTimeDayAfter by remember { mutableIntStateOf(0) } // 0: 05:00, 1: 06:00, 2: 07:00, 3: 08:00, 4: 09:00
    var isEventNotification by remember { mutableStateOf(true) }

    val timesBefore = listOf("19:00", "20:00", "21:00", "22:00", "23:00")
    val timesAfter = listOf("05:00", "06:00", "07:00", "08:00", "09:00")

    fun doRegister() {
        scope.launch {
            val hour = if (collectionDateFlag == 0) {
                19 + selectedTimeDayBefore
            } else {
                5 + selectedTimeDayAfter
            }
            val count = calendarRepository.registerEvents(
                models = garbageModels,
                periodMonths = eventPeriod,
                collectionDateFlag = collectionDateFlag,
                selectedHour = hour,
                hasNotification = isEventNotification
            )
            Toast.makeText(context, "${count}件の予定をカレンダーに登録しました", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            doRegister()
        } else {
            Toast.makeText(context, "カレンダーへのアクセス権限が必要です", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "登録設定",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DefaultThemeColor
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(text = "キャンセル", color = DefaultThemeColor, fontSize = 15.sp)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Period section
            Text(
                text = "イベント期間",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DefaultThemeColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("一週間", "一ヶ月", "二ヶ月").forEachIndexed { index, label ->
                    SelectablePill(
                        text = label,
                        selected = eventPeriod == index,
                        modifier = Modifier.weight(1f),
                        onClick = { eventPeriod = index }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Time section
            Text(
                text = "イベント時間",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DefaultThemeColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SelectablePill(
                    text = "収集日の前日",
                    selected = collectionDateFlag == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { collectionDateFlag = 0 }
                )
                SelectablePill(
                    text = "収集日の当日",
                    selected = collectionDateFlag == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { collectionDateFlag = 1 }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hour buttons
            val hours = if (collectionDateFlag == 0) timesBefore else timesAfter
            val selectedHourIndex = if (collectionDateFlag == 0) selectedTimeDayBefore else selectedTimeDayAfter

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                hours.forEachIndexed { index, timeText ->
                    Box(
                        modifier = Modifier
                            .width(62.dp)
                            .height(34.dp)
                            .border(
                                border = if (selectedHourIndex == index) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, DefaultThemeColor),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .background(
                                color = if (selectedHourIndex == index) DefaultThemeColor else Color.White,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                if (collectionDateFlag == 0) selectedTimeDayBefore = index
                                else selectedTimeDayAfter = index
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = timeText,
                            color = if (selectedHourIndex == index) Color.White else DefaultThemeColor,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Notification switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "通知を受け取る",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DefaultThemeColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = isEventNotification,
                    onCheckedChange = { isEventNotification = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DefaultThemeColor,
                        uncheckedThumbColor = DefaultThemeColor,
                        uncheckedTrackColor = DividerColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Action button
            Button(
                onClick = {
                    if (calendarRepository.hasCalendarPermission()) {
                        doRegister()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DefaultThemeColor)
            ) {
                Text(
                    text = "カレンダーに登録",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SelectablePill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .border(
                border = if (selected) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, DefaultThemeColor),
                shape = RoundedCornerShape(4.dp)
            )
            .background(
                color = if (selected) DefaultThemeColor else Color.White,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else DefaultThemeColor,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
