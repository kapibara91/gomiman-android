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
import co.jp.kpbr.gomiman.data.repository.CalendarAccount
import co.jp.kpbr.gomiman.data.repository.CalendarRegisterResult
import co.jp.kpbr.gomiman.data.repository.CalendarRepository
import co.jp.kpbr.gomiman.ui.theme.DefaultThemeColor
import co.jp.kpbr.gomiman.ui.theme.DividerColor
import co.jp.kpbr.gomiman.ui.theme.TextSecondary
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
    var writableCalendars by remember { mutableStateOf<List<CalendarAccount>>(emptyList()) }
    var selectedCalendar by remember { mutableStateOf<CalendarAccount?>(null) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (calendarRepository.hasCalendarPermission()) {
            val cals = calendarRepository.getWritableCalendars()
            writableCalendars = cals
            selectedCalendar = calendarRepository.getDefaultCalendar()
        }
    }

    fun doRegister() {
        if (garbageModels.isEmpty()) {
            Toast.makeText(context, "登録するゴミ収集日がありません。「ゴミの日」で先に設定してください", Toast.LENGTH_LONG).show()
            return
        }
        if (isRegistering) return
        isRegistering = true

        scope.launch {
            try {
                val targetCalId = selectedCalendar?.id ?: calendarRepository.getDefaultCalendar()?.id
                when (val result = calendarRepository.registerEvents(
                    models = garbageModels,
                    periodMonths = eventPeriod,
                    targetCalendarId = targetCalId
                )) {
                    is CalendarRegisterResult.Success -> {
                        if (result.count > 0) {
                            val accountName = selectedCalendar?.accountName ?: ""
                            val accountMsg = if (accountName.isNotEmpty()) "（$accountName）" else ""
                            Toast.makeText(
                                context,
                                "${result.count}件の予定をカレンダーに登録しました$accountMsg",
                                Toast.LENGTH_LONG
                            ).show()
                            onNavigateBack()
                        } else {
                            Toast.makeText(
                                context,
                                "選択した期間内に該当するゴミ収集日がありませんでした",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    is CalendarRegisterResult.NoSchedules -> {
                        Toast.makeText(
                            context,
                            "登録するゴミ収集日がありません。「ゴミの日」で先に設定してください",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is CalendarRegisterResult.NoCalendarFound -> {
                        Toast.makeText(
                            context,
                            "端末に書き込み可能なカレンダーが見つかりませんでした",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is CalendarRegisterResult.PermissionDenied -> {
                        Toast.makeText(
                            context,
                            "カレンダーへのアクセス権限が必要です",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } finally {
                isRegistering = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            val cals = calendarRepository.getWritableCalendars()
            writableCalendars = cals
            selectedCalendar = calendarRepository.getDefaultCalendar()
            doRegister()
        } else {
            Toast.makeText(context, "カレンダーへのアクセス権限が必要です", Toast.LENGTH_SHORT).show()
        }
    }

    if (showCalendarDialog && writableCalendars.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showCalendarDialog = false },
            title = {
                Text(
                    text = "登録先カレンダーの選択",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = DefaultThemeColor
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    writableCalendars.forEach { cal ->
                        val isSelected = cal.id == selectedCalendar?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCalendar = cal
                                    showCalendarDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedCalendar = cal
                                    showCalendarDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = DefaultThemeColor
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (cal.displayName.isNotEmpty()) cal.displayName else cal.accountName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = Color(0xFF2C3E50)
                                )
                                Text(
                                    text = "${cal.accountName} (${if (cal.accountType == "com.google") "Google" else cal.accountType})",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCalendarDialog = false }) {
                    Text("キャンセル", color = DefaultThemeColor)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(12.dp)
        )
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
            // Informative banner explaining destination
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DefaultThemeColor.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "※ 端末のカレンダーアプリ（Googleカレンダー等）に、設定したゴミ収集日（終日予定）を登録します。",
                    fontSize = 13.sp,
                    color = DefaultThemeColor,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

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

            // Confirmation / details section
            Text(
                text = "登録内容の確認",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DefaultThemeColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FBF9)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DividerColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "登録対象日", fontSize = 14.sp, color = TextSecondary)
                        Text(
                            text = "収集日の当日（終日予定）",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2C3E50)
                        )
                    }
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "登録期間", fontSize = 14.sp, color = TextSecondary)
                        val periodText = when (eventPeriod) {
                            0 -> "今日から 1週間"
                            1 -> "今日から 1ヶ月"
                            2 -> "今日から 2ヶ月"
                            else -> "今日から 1ヶ月"
                        }
                        Text(
                            text = periodText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2C3E50)
                        )
                    }
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = writableCalendars.size > 1) {
                                showCalendarDialog = true
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(text = "登録先カレンダー", fontSize = 14.sp, color = TextSecondary)
                            if (writableCalendars.size > 1) {
                                Text(
                                    text = "タップして変更",
                                    fontSize = 11.sp,
                                    color = DefaultThemeColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val calName = selectedCalendar?.let { cal ->
                                if (cal.displayName.isNotEmpty() && cal.displayName != cal.accountName) {
                                    "${cal.displayName} (${cal.accountName})"
                                } else {
                                    cal.accountName.ifEmpty { cal.displayName.ifEmpty { "標準カレンダー" } }
                                }
                            } ?: "端末の標準カレンダー"

                            Text(
                                text = calName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2C3E50),
                                maxLines = 1
                            )
                            if (writableCalendars.size > 1) {
                                Text(text = "▼", fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

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
                enabled = !isRegistering,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DefaultThemeColor,
                    disabledContainerColor = DefaultThemeColor.copy(alpha = 0.5f)
                )
            ) {
                if (isRegistering) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "登録中...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
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
