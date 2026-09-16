package co.jp.kpbr.gomiman.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.jp.kpbr.gomiman.R
import co.jp.kpbr.gomiman.data.repository.CalendarRepository
import co.jp.kpbr.gomiman.ui.theme.DefaultThemeColor
import co.jp.kpbr.gomiman.ui.theme.DividerColor
import co.jp.kpbr.gomiman.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    calendarRepository: CalendarRepository,
    onResetGarbageCollections: () -> Unit,
    onNavigateToPushSettings: () -> Unit,
    onNavigateToCalendarAppend: () -> Unit,
    onNavigateToFeedback: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showEventResetDialog by remember { mutableStateOf(false) }
    var showGarbageResetDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            showEventResetDialog = true
        } else {
            Toast.makeText(context, "カレンダーへのアクセス権限が必要です", Toast.LENGTH_SHORT).show()
        }
    }

    if (showEventResetDialog) {
        AlertDialog(
            onDismissRequest = { showEventResetDialog = false },
            title = { Text("カレンダー予定の削除", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "端末のカレンダーから、ゴミマンが登録した収集予定をすべて削除します。\nよろしいですか？",
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val count = calendarRepository.resetEvents()
                            if (count > 0) {
                                Toast.makeText(context, "カレンダーの予定を${count}件削除しました", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "削除対象のカレンダー予定がありませんでした", Toast.LENGTH_SHORT).show()
                            }
                            showEventResetDialog = false
                        }
                    }
                ) {
                    Text("削除", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEventResetDialog = false }) {
                    Text("キャンセル", color = DefaultThemeColor)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(8.dp)
        )
    }

    if (showGarbageResetDialog) {
        AlertDialog(
            onDismissRequest = { showGarbageResetDialog = false },
            title = { Text("ごみ収集日のリセット", fontWeight = FontWeight.Bold) },
            text = { Text("登録されているすべての収集日設定が削除されます。\nリセットしてもよろしいですか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetGarbageCollections()
                        Toast.makeText(context, "ごみ収集日をリセットしました", Toast.LENGTH_SHORT).show()
                        showGarbageResetDialog = false
                    }
                ) {
                    Text("リセット", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGarbageResetDialog = false }) {
                    Text("キャンセル", color = DefaultThemeColor)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(8.dp)
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "設定",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DefaultThemeColor
                    )
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.icon_1024),
                contentDescription = "Gomiman Logo",
                modifier = Modifier.size(130.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ゴミマン",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = DefaultThemeColor
            )

            Text(
                text = "Ver 1.4.3",
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Settings options card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DefaultThemeColor),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    SettingsRow(title = "カレンダーの予定を削除") {
                        if (calendarRepository.hasCalendarPermission()) {
                            showEventResetDialog = true
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR
                                )
                            )
                        }
                    }
                    HorizontalDivider(color = DividerColor, thickness = 1.dp, modifier = Modifier.padding(start = 12.dp))

                    SettingsRow(title = "ごみ収集日のリセット") { showGarbageResetDialog = true }
                    HorizontalDivider(color = DividerColor, thickness = 1.dp, modifier = Modifier.padding(start = 12.dp))

                    SettingsRow(title = "通知設定") { onNavigateToPushSettings() }
                    HorizontalDivider(color = DividerColor, thickness = 1.dp, modifier = Modifier.padding(start = 12.dp))

                    SettingsRow(title = "カレンダーに登録") { onNavigateToCalendarAppend() }
                    HorizontalDivider(color = DividerColor, thickness = 1.dp, modifier = Modifier.padding(start = 12.dp))

                    SettingsRow(title = "アプリを評価する") {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=co.jp.kpbr.gomiman"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=co.jp.kpbr.gomiman"))
                            context.startActivity(webIntent)
                        }
                    }
                    HorizontalDivider(color = DividerColor, thickness = 1.dp, modifier = Modifier.padding(start = 12.dp))

                    SettingsRow(title = "ご意見・ご要望") { onNavigateToFeedback() }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = DefaultThemeColor
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
