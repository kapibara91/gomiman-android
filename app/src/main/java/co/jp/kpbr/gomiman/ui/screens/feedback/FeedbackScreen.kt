package co.jp.kpbr.gomiman.ui.screens.feedback

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.jp.kpbr.gomiman.data.repository.SyncRepository
import co.jp.kpbr.gomiman.ui.theme.DefaultThemeColor
import co.jp.kpbr.gomiman.ui.theme.InactiveGray
import co.jp.kpbr.gomiman.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    syncRepository: SyncRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var feedbackText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    fun submitFeedback() {
        if (feedbackText.isBlank() || isSubmitting) return
        isSubmitting = true
        scope.launch {
            val result = syncRepository.submitFeedback(feedbackText)
            isSubmitting = false
            if (result.isSuccess) {
                Toast.makeText(context, "フィードバックを送信しました。ご協力ありがとうございます。", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            } else {
                Toast.makeText(context, "送信に失敗しました。通信環境をご確認の上、再度お試しください。", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "フィードバック",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DefaultThemeColor
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(text = "戻る", color = DefaultThemeColor, fontSize = 15.sp)
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
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ご意見・ご要望の送信",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DefaultThemeColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .border(1.dp, DefaultThemeColor, RoundedCornerShape(4.dp))
                    .padding(14.dp)
            ) {
                if (feedbackText.isEmpty()) {
                    Text(
                        text = "アプリへのご意見・ご要望やお気づきの点をお聞かせください。",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                }
                BasicTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = DefaultThemeColor
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { submitFeedback() },
                enabled = feedbackText.isNotBlank() && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DefaultThemeColor,
                    disabledContainerColor = InactiveGray
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "送信",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
