package co.jp.kpbr.gomiman.ui.screens.garbagelist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.jp.kpbr.gomiman.R
import co.jp.kpbr.gomiman.data.model.GarbageCollectionModel
import co.jp.kpbr.gomiman.data.model.GarbageType
import co.jp.kpbr.gomiman.ui.theme.DefaultThemeColor
import co.jp.kpbr.gomiman.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarbageListScreen(
    garbageModels: List<GarbageCollectionModel>,
    onNavigateToAdd: () -> Unit,
    onNavigateToPushSettings: () -> Unit,
    onDeleteSchedule: (Long) -> Unit
) {
    var scheduleToDelete by remember { mutableStateOf<GarbageCollectionModel?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    if (scheduleToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) scheduleToDelete = null
            },
            title = { Text("収集日の削除", fontWeight = FontWeight.Bold) },
            text = { Text("この収集設定を削除しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isDeleting) return@TextButton
                        isDeleting = true
                        val id = scheduleToDelete?.id
                        scheduleToDelete = null
                        if (id != null) {
                            onDeleteSchedule(id)
                        }
                        isDeleting = false
                    },
                    enabled = !isDeleting
                ) {
                    Text("削除する", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { scheduleToDelete = null },
                    enabled = !isDeleting
                ) {
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
                        text = "ゴミマン",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DefaultThemeColor
                    )
                },
                navigationIcon = {
                    if (garbageModels.isNotEmpty()) {
                        TextButton(onClick = onNavigateToPushSettings) {
                            Text(
                                text = "通知設定",
                                color = DefaultThemeColor,
                                fontSize = 15.sp
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToAdd) {
                        Text(
                            text = "追加",
                            color = DefaultThemeColor,
                            fontSize = 15.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (garbageModels.isEmpty()) {
                    EmptyGarbageView()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(garbageModels, key = { it.id ?: 0L }) { model ->
                            GarbageScheduleCard(
                                model = model,
                                onDelete = { scheduleToDelete = model }
                            )
                        }
                    }
                }
            }

            // Bottom Banner Ad
            co.jp.kpbr.gomiman.ui.components.BannerAdView(
                adUnitId = co.jp.kpbr.gomiman.ui.components.AdConstants.getCollectionBannerUnitId(),
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun EmptyGarbageView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.garbage_date_empty),
            contentDescription = "No Data",
            modifier = Modifier.size(160.dp)
        )
        Spacer(modifier = Modifier.height(25.dp))
        Text(
            text = "登録されている収集日はありません",
            fontSize = 18.sp,
            color = DefaultThemeColor,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun GarbageScheduleCard(
    model: GarbageCollectionModel,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DefaultThemeColor),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateUtils.formatScheduleSummary(model),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = DefaultThemeColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "削除",
                    color = DefaultThemeColor,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { onDelete() }
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                model.garbageTypes.forEach { typeId ->
                    val type = GarbageType.fromId(typeId)
                    if (type != null) {
                        Box(
                            modifier = Modifier
                                .size(45.dp)
                                .border(1.dp, DefaultThemeColor, CircleShape)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.shortName,
                                fontSize = 13.sp,
                                color = DefaultThemeColor
                            )
                        }
                    }
                }
            }
        }
    }
}
