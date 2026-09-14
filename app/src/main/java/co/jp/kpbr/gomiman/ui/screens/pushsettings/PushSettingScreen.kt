package co.jp.kpbr.gomiman.ui.screens.pushsettings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import co.jp.kpbr.gomiman.data.local.PreferencesManager
import co.jp.kpbr.gomiman.data.model.PushSettingModel
import co.jp.kpbr.gomiman.data.repository.SyncRepository
import co.jp.kpbr.gomiman.ui.theme.DefaultThemeColor
import co.jp.kpbr.gomiman.ui.theme.DividerColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushSettingScreen(
    preferencesManager: PreferencesManager,
    syncRepository: SyncRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val initialSetting = remember { preferencesManager.getPushSetting() }
    var collectionDayBefore by remember { mutableStateOf(initialSetting.collectionDayBefore) }
    var selectedTimeDayBefore by remember { mutableIntStateOf(initialSetting.selectedTimeDayBefore) }
    var collectionDayAfter by remember { mutableStateOf(initialSetting.collectionDayAfter) }
    var selectedTimeDayAfter by remember { mutableIntStateOf(initialSetting.selectedTimeDayAfter) }

    val timesBefore = listOf("19:00", "20:00", "21:00", "22:00", "23:00")
    val timesAfter = listOf("05:00", "06:00", "07:00", "08:00", "09:00")

    fun saveAndExit() {
        val updated = PushSettingModel(
            collectionDayBefore = collectionDayBefore,
            selectedTimeDayBefore = selectedTimeDayBefore,
            collectionDayAfter = collectionDayAfter,
            selectedTimeDayAfter = selectedTimeDayAfter
        )
        preferencesManager.savePushSetting(updated)
        scope.launch {
            syncRepository.syncPushSetting(updated)
        }
        Toast.makeText(context, "設定保存済み", Toast.LENGTH_SHORT).show()
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "通知設定",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DefaultThemeColor
                    )
                },
                navigationIcon = {
                    TextButton(onClick = { saveAndExit() }) {
                        Text(text = "戻る", color = DefaultThemeColor, fontSize = 15.sp)
                    }
                },
                actions = {
                    TextButton(onClick = { saveAndExit() }) {
                        Text(text = "保存", color = DefaultThemeColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
            // Day before notification toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "収集日の前日通知",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DefaultThemeColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = collectionDayBefore,
                    onCheckedChange = { collectionDayBefore = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DefaultThemeColor,
                        uncheckedThumbColor = DefaultThemeColor,
                        uncheckedTrackColor = DividerColor
                    )
                )
            }

            AnimatedVisibility(visible = collectionDayBefore) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        timesBefore.forEachIndexed { index, timeText ->
                            Box(
                                modifier = Modifier
                                    .width(62.dp)
                                    .height(34.dp)
                                    .border(
                                        border = if (selectedTimeDayBefore == index) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, DefaultThemeColor),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .background(
                                        color = if (selectedTimeDayBefore == index) DefaultThemeColor else Color.White,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { selectedTimeDayBefore = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = timeText,
                                    color = if (selectedTimeDayBefore == index) Color.White else DefaultThemeColor,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider(color = DividerColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))

            // Day after / Day of notification toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "収集日の当日通知",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DefaultThemeColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = collectionDayAfter,
                    onCheckedChange = { collectionDayAfter = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DefaultThemeColor,
                        uncheckedThumbColor = DefaultThemeColor,
                        uncheckedTrackColor = DividerColor
                    )
                )
            }

            AnimatedVisibility(visible = collectionDayAfter) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        timesAfter.forEachIndexed { index, timeText ->
                            Box(
                                modifier = Modifier
                                    .width(62.dp)
                                    .height(34.dp)
                                    .border(
                                        border = if (selectedTimeDayAfter == index) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, DefaultThemeColor),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .background(
                                        color = if (selectedTimeDayAfter == index) DefaultThemeColor else Color.White,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { selectedTimeDayAfter = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = timeText,
                                    color = if (selectedTimeDayAfter == index) Color.White else DefaultThemeColor,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
