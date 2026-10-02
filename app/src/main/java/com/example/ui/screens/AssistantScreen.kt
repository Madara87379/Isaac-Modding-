package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun AssistantScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.assistantMessages.size, uiState.isAssistantThinking) {
        if (uiState.assistantMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.assistantMessages.size - 1)
        }
    }

    val quickQuestions = if (uiState.isArabic) {
        listOf(
            "كيف أحول دالة EvaluateCache إلى items.xml؟",
            "لماذا يرفض بورت أندرويد تشغيل أكواد اللوا؟",
            "كيف أصل لمجلد media في أندرويد 13 و 14؟",
            "طريقة إضافة كوستيوم في costumes2.xml بدون لوا",
            "حل مشكلة المربعات السوداء عند استبدال السبرايت"
        )
    } else {
        listOf(
            "How to convert EvaluateCache to items.xml?",
            "Why doesn't Android port support Lua scripts?",
            "How to access Android/media on Android 13/14?",
            "How to add costumes in costumes2.xml without Lua?",
            "Fix black square sprites on Android port"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 80.dp)
    ) {
        // Chat Header with Clear button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = IsaacTearBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isArabic) "مساعد خبير بورت آيزك (Gemini AI)" else "Isaac Port Modding Assistant",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = IsaacBoneWhite
                )
            }

            IconButton(
                onClick = { viewModel.clearAssistantChat() },
                modifier = Modifier.testTag("clear_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Chat",
                    tint = IsaacTextMuted
                )
            }
        }

        // Quick Suggestion Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickQuestions.forEach { question ->
                SuggestionChip(
                    onClick = {
                        inputText = question
                        viewModel.sendAssistantPrompt(question)
                        inputText = ""
                    },
                    label = { Text(question, fontSize = 11.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = IsaacSurfaceVariant,
                        labelColor = IsaacBoneWhite
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = IsaacBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.assistantMessages) { msg ->
                val isUser = msg.role == "user"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isUser) 12.dp else 2.dp,
                                    bottomEnd = if (isUser) 2.dp else 12.dp
                                )
                            )
                            .background(if (isUser) IsaacTearBlue else IsaacSurfaceVariant)
                            .border(
                                1.dp,
                                if (isUser) IsaacTearBlue else IsaacBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = msg.content,
                            fontSize = 13.sp,
                            color = if (isUser) IsaacSurface else IsaacBoneWhite,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            if (uiState.isAssistantThinking) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            color = IsaacTearBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isArabic) "جيميناي يحلل كود المود ودليل البورت..." else "Gemini is analyzing Isaac rules...",
                            fontSize = 11.sp,
                            color = IsaacTextMuted
                        )
                    }
                }
            }
        }

        // Input bar
        Surface(
            color = IsaacSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (uiState.isArabic) "اسأل جيميناي أو الصق كود Lua/XML..." else "Ask Gemini or paste Lua/XML...",
                            fontSize = 12.sp,
                            color = IsaacTextMuted
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IsaacTearBlue,
                        unfocusedBorderColor = IsaacBorder,
                        focusedTextColor = IsaacBoneWhite,
                        unfocusedTextColor = IsaacBoneWhite
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val text = inputText.trim()
                        if (text.isNotEmpty() && !uiState.isAssistantThinking) {
                            viewModel.sendAssistantPrompt(text)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !uiState.isAssistantThinking,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (inputText.isNotBlank() && !uiState.isAssistantThinking) IsaacTearBlue else IsaacSurfaceVariant)
                        .testTag("send_prompt_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank() && !uiState.isAssistantThinking) IsaacSurface else IsaacTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
