package co.jp.kpbr.gomiman.ui.screens.garbageadd

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
import co.jp.kpbr.gomiman.data.model.GarbageCollectionModel
import co.jp.kpbr.gomiman.data.model.GarbageType
import co.jp.kpbr.gomiman.ui.theme.DefaultThemeColor
import co.jp.kpbr.gomiman.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarbageAddScreen(
    onNavigateBack: () -> Unit,
    onSaveSuccess: (GarbageCollectionModel) -> Unit
) {
    val context = LocalContext.current
    var weekStatus by remember { mutableIntStateOf(GarbageCollectionModel.WEEK_STATUS_EVERY_WEEK) }
    val selectedWeeks = remember { mutableStateListOf<Int>() }
    val selectedDays = remember { mutableStateListOf<Int>() }
    val selectedTypes = remember { mutableStateListOf<Int>() }

    var isSaving by remember { mutableStateOf(false) }

    val daysMeta = remember {
        listOf(
            1 to ("月" to "Mon"),
            2 to ("火" to "Tue"),
            3 to ("水" to "Wed"),
            4 to ("木" to "Thu"),
            5 to ("金" to "Fri"),
            6 to ("土" to "Sat"),
            7 to ("日" to "Sun")
        )
    }

    fun validateAndSave() {
        if (isSaving) return
        if (weekStatus == GarbageCollectionModel.WEEK_STATUS_BIWEEKLY && selectedWeeks.isEmpty()) {
            Toast.makeText(context, "週を選択してください", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedDays.isEmpty()) {
            Toast.makeText(context, "曜日を選択してください", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedTypes.isEmpty()) {
            Toast.makeText(context, "ゴミの種類を選択してください", Toast.LENGTH_SHORT).show()
            return
        }

        isSaving = true
        val model = GarbageCollectionModel(
            weekStatus = weekStatus,
            weeks = selectedWeeks.toMutableList(),
            days = selectedDays.toMutableList(),
            garbageTypes = selectedTypes.toMutableList()
        )
        onSaveSuccess(model)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "収集日の追加",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DefaultThemeColor
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onNavigateBack,
                        enabled = !isSaving
                    ) {
                        Text(
                            text = "キャンセル",
                            color = if (isSaving) DefaultThemeColor.copy(alpha = 0.5f) else DefaultThemeColor,
                            fontSize = 15.sp
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { validateAndSave() },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = DefaultThemeColor
                            )
                        } else {
                            Text(
                                text = "保存",
                                color = DefaultThemeColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
            // Section 1: 収集日
            Text(
                text = "収集日",
                color = DefaultThemeColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Frequency: 毎週 vs 隔週
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ToggleButton(
                    text = "毎週",
                    selected = weekStatus == GarbageCollectionModel.WEEK_STATUS_EVERY_WEEK,
                    width = 70,
                    height = 35,
                    onClick = { weekStatus = GarbageCollectionModel.WEEK_STATUS_EVERY_WEEK }
                )
                ToggleButton(
                    text = "隔週",
                    selected = weekStatus == GarbageCollectionModel.WEEK_STATUS_BIWEEKLY,
                    width = 70,
                    height = 35,
                    onClick = { weekStatus = GarbageCollectionModel.WEEK_STATUS_BIWEEKLY }
                )
            }

            AnimatedVisibility(visible = weekStatus == GarbageCollectionModel.WEEK_STATUS_BIWEEKLY) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (1..5).forEach { weekNum ->
                            val isSelected = selectedWeeks.contains(weekNum)
                            ToggleButton(
                                text = "第${weekNum}週",
                                selected = isSelected,
                                width = 64,
                                height = 30,
                                onClick = {
                                    if (isSelected) selectedWeeks.remove(weekNum)
                                    else selectedWeeks.add(weekNum)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Days of the week
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysMeta.take(4).forEach { (dayIndex, labels) ->
                    DayToggleButton(
                        kanji = labels.first,
                        en = labels.second,
                        selected = selectedDays.contains(dayIndex),
                        onClick = {
                            if (selectedDays.contains(dayIndex)) selectedDays.remove(dayIndex)
                            else selectedDays.add(dayIndex)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                daysMeta.drop(4).forEach { (dayIndex, labels) ->
                    DayToggleButton(
                        kanji = labels.first,
                        en = labels.second,
                        selected = selectedDays.contains(dayIndex),
                        onClick = {
                            if (selectedDays.contains(dayIndex)) selectedDays.remove(dayIndex)
                            else selectedDays.add(dayIndex)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Section 2: ゴミの種類
            Text(
                text = "ゴミの種類",
                color = DefaultThemeColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Types grid
            val types = GarbageType.allTypes
            val rows = listOf(
                types.take(3),
                types.drop(3).take(3),
                types.drop(6)
            )

            rows.forEach { rowTypes ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowTypes.forEach { garbageType ->
                        val isSelected = selectedTypes.contains(garbageType.id)
                        ToggleButton(
                            text = garbageType.shortName,
                            selected = isSelected,
                            width = 90,
                            height = 36,
                            fontSize = 15,
                            onClick = {
                                if (isSelected) selectedTypes.remove(garbageType.id)
                                else selectedTypes.add(garbageType.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleButton(
    text: String,
    selected: Boolean,
    width: Int,
    height: Int,
    fontSize: Int = 14,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(height.dp)
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
            fontSize = fontSize.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun DayToggleButton(
    kanji: String,
    en: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(76.dp)
            .height(48.dp)
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = kanji,
                color = if (selected) Color.White else DefaultThemeColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = en,
                color = if (selected) Color.White.copy(alpha = 0.8f) else TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}
