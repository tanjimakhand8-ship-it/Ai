package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessageEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var inputText by remember { mutableStateOf("") }
    var showTipsSheet by remember { mutableStateOf(false) }

    // Auto-scroll to bottom of list when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showTipsSheet) {
        AlertDialog(
            onDismissRequest = { showTipsSheet = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTipsSheet = false }) {
                    Text("Close")
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💡", fontSize = 24.sp)
                    Text(
                        text = "Marco v2.0 Tips",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Box(modifier = Modifier.fillMaxHeight(0.7f)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Category: Special Agent Power Abilities
                        item {
                            Text(
                                text = "🛡️ Special Capabilities (Self-Learning, Hack, Repair, Update, Budget, Talk, Analyse)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(specialCapabilitiesTips) { tip ->
                            TipItem(tip = tip) {
                                showTipsSheet = false
                                viewModel.sendMessage(tip.prompt)
                            }
                        }

                        // Category: Voice & Memory
                        item {
                            Text(
                                text = "🎙️ Voice & 🧠 Memory Systems",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(voiceMemoryTips) { tip ->
                            TipItem(tip = tip) {
                                showTipsSheet = false
                                viewModel.sendMessage(tip.prompt)
                            }
                        }

                        // Category: Asset Controls & Automation
                        item {
                            Text(
                                text = "💻 PC / 📱 Mobile Automation",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(automationTips) { tip ->
                            TipItem(tip = tip) {
                                showTipsSheet = false
                                viewModel.sendMessage(tip.prompt)
                            }
                        }

                        // Category: Business Research & Halal Trading
                        item {
                            Text(
                                text = "📈 Trading & 🛒 Dropshipping Research",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(financeTradingTips) { tip ->
                            TipItem(tip = tip) {
                                showTipsSheet = false
                                viewModel.sendMessage(tip.prompt)
                            }
                        }

                        // Category: Creators, Media & Coder Assistant
                        item {
                            Text(
                                text = "🎨 Creative & 👨‍💻 Coder Assistant",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(creatorTips) { tip ->
                            TipItem(tip = tip) {
                                showTipsSheet = false
                                viewModel.sendMessage(tip.prompt)
                            }
                        }

                        // Category: Master AI Directory
                        item {
                            Text(
                                text = "🛠️ Master AI Tools Directory",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(aiToolsTips) { tip ->
                            TipItem(tip = tip) {
                                showTipsSheet = false
                                viewModel.sendMessage(tip.prompt)
                            }
                        }

                        // Category: Logo & Custom Audio
                        item {
                            Text(
                                text = "🎨 Logo Style & 🎙️ Creative Audio",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(logoAudioTips) { tip ->
                            TipItem(tip = tip) {
                                showTipsSheet = false
                                viewModel.sendMessage(tip.prompt)
                            }
                        }

                        // Category: Advanced Marketing & Scholarly Research
                        item {
                            Text(
                                text = "📈 AI Marketing & 📚 Scholarly Research",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(marketingResearchTips) { tip ->
                            TipItem(tip = tip) {
                                showTipsSheet = false
                                viewModel.sendMessage(tip.prompt)
                            }
                        }

                        // Category: Copywriting & Intelligent SEO
                        item {
                            Text(
                                text = "✍️ Content Copywriting & 🔍 SEO Optimizations",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(copywritingSeoTips) { tip ->
                            TipItem(tip = tip) {
                                showTipsSheet = false
                                viewModel.sendMessage(tip.prompt)
                            }
                        }

                        // Category: Conversational Chatbots & Slide Decks
                        item {
                            Text(
                                text = "💬 Interactive Chatbots & 📊 Slide Presentations",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(chatbotPresentationTips) { tip ->
                            TipItem(tip = tip) {
                                showTipsSheet = false
                                viewModel.sendMessage(tip.prompt)
                            }
                        }

                        // Category: Business Productivity & High-Fidelity UI
                        item {
                            Text(
                                text = "💼 Enterprise Systems & 🌐 Beautiful Web UI",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(businessCategoryTips) { tip ->
                            TipItem(tip = tip) {
                                showTipsSheet = false
                                viewModel.sendMessage(tip.prompt)
                            }
                        }

                        // Category: Cybersecurity & Ethical Hacking
                        item {
                            Text(
                                text = "🔒 Cybersecurity & Ethical Hacking",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(hackingCyberTips) { tip ->
                            TipItem(tip = tip) {
                                showTipsSheet = false
                                viewModel.sendMessage(tip.prompt)
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = "Marco AI Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "Marco",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Always active AI",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.secondary)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showTipsSheet = true },
                        modifier = Modifier
                            .testTag("tips_button")
                            .minimumInteractiveComponentSize(),
                    ) {
                        Text(
                            text = "💡",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(4.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier
                            .testTag("clear_button")
                            .minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Chat History",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        // Premium gradient atmosphere
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        )
                    )
                )
        ) {
            // Conversational list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages) { message ->
                        ChatBubbleRow(
                            message = message,
                            onDelete = { viewModel.deleteMessage(message.id) },
                            onSpeak = { onSpeak(message.text) }
                        )
                    }
                }
            }

            // Quick suggestions list (only shown if keyboard is not active or as helper chips)
            if (messages.size <= 1) {
                QuickSuggestionsPanel(
                    onSuggestionClick = { suggestionText ->
                        viewModel.sendMessage(suggestionText)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Message Composer Section with custom elevation and background
            Surface(
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding() // Keyboard safe safe-areas
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(), // Navigation gesture bar padding
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask Marco anything...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("message_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.trim().isNotEmpty()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            }
                        )
                    )

                    FloatingActionButton(
                        onClick = {
                            if (inputText.trim().isNotEmpty()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        },
                        shape = CircleShape,
                        containerColor = if (inputText.trim().isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (inputText.trim().isNotEmpty()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Message",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleRow(
    message: ChatMessageEntity,
    onDelete: () -> Unit,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == "user"

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    var showTimestamp by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = { showTimestamp = !showTimestamp }),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (!isUser) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Top)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Marco Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                Surface(
                    color = bubbleColor,
                    shape = bubbleShape,
                    modifier = Modifier.testTag(if (isUser) "user_message" else "assistant_message")
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        if (message.isPending) {
                            TypingIndicator()
                        } else {
                            Text(
                                text = parseMarkdown(message.text),
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 22.sp
                                )
                            )
                        }
                    }
                }

                // Voice assist indicator for narration playback
                if (!isUser && !message.isPending) {
                    Row(
                        modifier = Modifier
                            .padding(top = 4.dp, start = 4.dp)
                            .clickable { onSpeak() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🔊 Read Aloud",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Smoothly animated timestamp visibility
                AnimatedVisibility(
                    visible = showTimestamp,
                    enter = fadeIn() + scaleIn(initialScale = 0.9f)
                ) {
                    val formattedTime = remember(message.timestamp) {
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        sdf.format(Date(message.timestamp))
                    }
                    Text(
                        text = formattedTime,
                        modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (isUser) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Top)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

data class PromptTip(val label: String, val prompt: String)

val specialCapabilitiesTips = listOf(
    PromptTip("🧠 Self-Learning & Adaptability", "Teach yourself from my instructions. How do you implement internal self-correction, debugging, and adaptive feedback loops to avoid repeating past mistakes?"),
    PromptTip("🔀 Concurrent Multitasking Core", "আমি এখন তোমাকে ৩টি কাজ একসাথে করতে দিব: ১) একটি পাইথোন স্ক্রিপ্ট রিফ্যাক্টর করো, ২) একটি ১ মাসের ফ্যামিলি বাজেট বানাও এবং ৩) একটি ডিফেন্সিভ সিকিউরিটি টিপস দাও। এই ৩টি কাজ এক সাথে প্যারালালি এবং আলাদা সেকশনে লিখে দাও।"),
    PromptTip("🔒 Ethical Hacking & Security", "Perform a comprehensive defensive cybersecurity analysis on our application and provide instructions for establishing safe penetration testing protocols."),
    PromptTip("🔧 Repair & Diagnostics", "Help me repair a broken software module or configure proper stack trace reviews to resolve runtime and compile-time system errors cleanly."),
    PromptTip("⚡ App System Update & Optimization", "Produce an automated class modification and platform update script to refactor obsolete dependencies, clean memory footprints, and streamline layouts."),
    PromptTip("💰 Halal Capital Budgeting", "Build a high-spec capital allocation model and Shariah-compliant budgeting template to manage cash flows with 2% risk limits."),
    PromptTip("🗣️ talk: Voice Output & Speech", "Demonstrate how you configure multi-language Voice Synthesis/TTS markers so you can speak aloud flawlessly in English, Bengali, and global human accents."),
    PromptTip("📊 Deep Multi-Dimensional Analysis", "Provide a detailed, structured, architectural analysis of stock records, local relational database operations, and system performance logs.")
)

val voiceMemoryTips = listOf(
    PromptTip("🎙️ বাংলায় কথা বলো (TTS Voice)", "আমার সাথে বাংলায় কথা বলো। অ্যান্ড্রয়েডে তোমার ভয়েস এবং মাল্টিলিঙ্গুয়াল রিডিং/রাইটিং ক্ষমতা কীভাবে কাজ করে আমাকে বিস্তারিত জানাও!"),
    PromptTip("🎙️ TTS Voice Output", "Speak back to me in English. Explain how your Voice Output & Narration capabilities work in any language!"),
    PromptTip("🧠 তথ্য মনে রাখো (Remember)", "মনে রেখো যে আমি জেটপ্যাক কম্পোজ দিয়ে একটি হালাল ড্রপশিপিং ও স্টক মার্কেট প্রেডিকশন প্রজেক্ট তৈরি করছি।"),
    PromptTip("🧠 সংরক্ষিত তথ্য রিট্রিভ করো", "আমার তানজিম সম্পর্কে কী কী তথ্য বা প্রেফারেন্স তোমার রিমেম্বারড মেমরিতে সেট করা আছে?")
)

val automationTips = listOf(
    PromptTip("💻 PC Control Scripts", "Provide a secure Python listener script to control my laptop volume/apps remotely via safe terminal commands."),
    PromptTip("📱 Mobile task automations", "How to automate Android flows using Tasker connected to safe API controllers or Marco?"),
    PromptTip("📱 Safe Device Authorization", "What security protocols do you recommend to ensure nobody else controls my authorized devices?")
)

val financeTradingTips = listOf(
    PromptTip("📈 Halal Trading Analysis", "Explain the core financial standards for evaluating if a stock or digital asset is Shariah-compliant (Halal) to invest."),
    PromptTip("🛒 Dropshipping Research Plan", "Create a research roadmap to identify winning dropshipping niches using Google Trends and social sentiment analysis."),
    PromptTip("💹 Market Strategy Risk Map", "Design a conservative capital allocation strategy for stock/crypto trading with maximum 2% risk limits."),
    PromptTip("📊 Kronos K-Line Foundation Model", "Explain how the Kronos foundation model applies generative transformer decoders to K-line (candlestick) sequences for financial forecasting."),
    PromptTip("📊 Chinese Stock Data Scraping", "Show me how to build a robust Python wrapper seeking high-availability stock data with transparent retry logic across AkShare, BaoStock, and Eastmoney APIs."),
    PromptTip("📊 Consumer Electronics Report", "Provide a comprehensive market analysis profile for stock 000021 (Shenzhen Kaifa/Consumer Electronics) incorporating macro indicators and sector resonance factor adjustments."),
    PromptTip("🖥️ Kronos Custom Web Interface", "Show me how to configure the Kronos Web UI (app.py Flask + Plotly.js charts) with advanced quality controls (T=1.2, top_p=0.95, sample_count=2) on CUDA or MPS."),
    PromptTip("🧪 Kronos Model Unit Testing", "Explain how to set up the regression test pipeline (test_kronos_predictor_regression) to verify lookback values [512, 256] and validate expected MSE with 0.000001 tolerance.")
)

val creatorTips = listOf(
    PromptTip("🎨 AI Flat Vectorprompts", "Suggest 3 highly descriptive flat modern vector logo prompts for Midjourney/Stable Diffusion."),
    PromptTip("🎬 Video Script masterclass", "Formulate a cohesive 30-second video script explaining how autonomous AI agent layers run."),
    PromptTip("👨‍💻 Kotlin flow memory leaks", "Explain how to safely collect cold flows in Jetpack Compose without leaking lifecycles."),
    PromptTip("🔧 Self-Diagnosis checklist", "Run a deep self-diagnostic simulation on your system prompt instructions and suggest three performance optimizations.")
)

val aiToolsTips = listOf(
    PromptTip("🛠️ AI Directory (Image/Video)", "Show me the top recommended AI tools for Image Generation, Video Generation, and 3D video styling like Flux, Midjourney, and Luma AI."),
    PromptTip("🛠️ Master AI Models List", "Provide a comprehensive list of world-leading AI models (ChatGPT, Claude, Grok 3, Gemini, Llama, Qwen, Deepseek) and their specialties."),
    PromptTip("🛠️ Professional Web & Logo AI", "Show me the best AI tools, components, and templates for designing gorgeous Websites and Logos (Looka, Tailor Brands, Aceternity UI, Magic UI)."),
    PromptTip("🛠️ Free vs Paid AI tools", "Give me a structured list comparing popular free AI services with premium paid AI subscriptions like Copilot and Adobe Studio."),
    PromptTip("🛠️ AI Data Analysis & Sheets", "Which AI tools are best suited for deep spreadsheet data analysis, modeling, and automated formulas (Gigasheet, Equals, Julius, SheetAi, NumerousAi)?"),
    PromptTip("🎬 Video Generation Platforms", "Show me the top-performing Text-to-Video generators and cinematic renderers (Runway, Sora, Kling, Vidu, PixVerse, Hailuo, HeyGen)."),
    PromptTip("💻 Coded Dev & Engineering", "List the leading AI systems for autonomous coding, repository refactoring, or UI designs (Devin, Cursor, Replit, Lovable, Blackbox AI, Tabnine, Windsurf).")
)

val logoAudioTips = listOf(
    PromptTip("🎨 AI Logo Design", "List the best AI toolkits for logo creation and automated branding (Looka, Tailor Brands, Logo.com, Brandmark)."),
    PromptTip("🎙️ Speech & Background Tracks", "Recommend premier AI voice cloning, speech-to-text, and sound generator platforms (ElevenLabs, Speechify, Soundraw, Murf).")
)

val marketingResearchTips = listOf(
    PromptTip("📈 Multi-Channel Marketing", "Show me advanced AI systems for social media tracking, customer data platforms, and automated marketing campaigns (Sprout Social, Optimove)."),
    PromptTip("📚 Smart Scholarly Research", "Provide the top-rated AI assistants for analyzing research journals, PDFs, and citations (Julius, Scholarcy, ChatPDF, Typeset.io, Google Scholar).")
)

val copywritingSeoTips = listOf(
    PromptTip("✍️ Conversion Copywriting", "List high-converting AI copywriting models for drafting newsletters, product copies, and ad headlines (Copy.ai, Writetone, Describly, Jasper)."),
    PromptTip("🔍 Semantic SEO Auditing", "Suggest AI tools for search intent optimization, landing page performance, and organic growth (Surfer SEO, MarketMuse, HubSpot's tools, Semrush).")
)

val chatbotPresentationTips = listOf(
    PromptTip("💬 Interactive Assistant Suite", "How do the leading conversational models compare on logical reasoning, coding speed, and custom instructions (ChatGPT, Claude, Grok 3, Gemini, Deepseek)?"),
    PromptTip("📊 Dynamic Presentation Decks", "Which AI slide builders offer the most customizable templates, transition controls, and document-to-presentation conversions (Gamma, Decktopus, Tome, SlidesAI, Prezi)?")
)

val businessCategoryTips = listOf(
    PromptTip("💼 Enterprise Business Tools", "Give me a structured summary comparing paid systems like Salesforce and Photoshop with cost-effective open-source CRMs and design tools like GIMP, WordPress, and Trello."),
    PromptTip("🌐 High-Fidelity UI & Web design", "Which frontend components, animation engines, and design assets create gorgeous visual websites (Aceternity UI, Magic UI, Jitter Video, Reactbits.dev, Lottie Files)?")
)

val hackingCyberTips = listOf(
    PromptTip("🔒 Hacking & Cybersecurity Tools", "Generate a directory of the most famous cybersecurity, penetration testing, and ethical hacking tools like Wireshark, Metasploit, Burp Suite, and Nmap."),
    PromptTip("🔒 Crypto, Hashes & Passwords", "Explain the most popular password cracking and hashing algorithms (John the Ripper, Hashcat, MD5, SHA-256, Bcrypt, Argon2id)."),
    PromptTip("🔒 Cybersecurity Training Platforms", "List the best platforms for learning ethical hacking and capture-the-flag (CTF) challenges (TryHackMe, Hack The Box, PortSwigger, OverTheWire).")
)

val quickSuggestions = listOf(
    PromptTip("🧠 Self-Learning & Adapt", "Teach yourself from my instructions. How do you implement internal self-correction and adaptive feedback loops to avoid repeating past mistakes?"),
    PromptTip("🔀 Multitask Parallel Work", "আমি এখন তোমাকে ৩টি কাজ একসাথে করতে দিব: ১) একটি পাইথোন স্ক্রিপ্ট রিফ্যাক্টর করো, ২) একটি ১ মাসের ফ্যামিলি বাজেট বানাও এবং ৩) একটি ডিফেন্সিভ সিকিউরিটি টিপস দাও। এই ৩টি কাজ এক সাথে প্যারালালি এবং আলাদা সেকশনে লিখে দাও।"),
    PromptTip("🔒 Defensive Hacking", "Perform a comprehensive defensive cybersecurity analysis on our application and provide instructions for establishing safe penetration testing protocols."),
    PromptTip("🔧 Software Repair", "Help me repair a broken software module or configure proper stack trace reviews to resolve runtime and compile-time system errors cleanly."),
    PromptTip("⚡ Code Updates", "Produce an automated class modification and platform update script to refactor obsolete dependencies, clean memory footprints, and streamline layouts."),
    PromptTip("💰 Halal Budgeting", "Build a high-spec capital allocation model and Shariah-compliant budgeting template to manage cash flows with 2% risk limits."),
    PromptTip("🗣️ Talk Voice Synthesis", "Demonstrate how you configure multi-language Voice Synthesis/TTS markers so you can speak aloud flawlessly in English, Bengali, and global human accents."),
    PromptTip("📊 Deep Data Analysis", "Provide a detailed, structured, architectural analysis of stock records, local relational database operations, and system performance logs.")
)

data class TipCategory(val name: String, val icon: String, val tips: List<PromptTip>)

@Composable
fun QuickSuggestionsPanel(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    
    val categories = remember {
        listOf(
            TipCategory("Quick", "⚡", quickSuggestions),
            TipCategory("Special", "🛡️", specialCapabilitiesTips),
            TipCategory("Memory", "🧠", voiceMemoryTips),
            TipCategory("Auto", "💻", automationTips),
            TipCategory("Finance", "📈", financeTradingTips),
            TipCategory("Creative", "🎨", creatorTips),
            TipCategory("AI Tools", "🛠️", aiToolsTips),
            TipCategory("Logos", "🎵", logoAudioTips),
            TipCategory("Marketing", "📊", marketingResearchTips),
            TipCategory("Copywriting", "✍️", copywritingSeoTips),
            TipCategory("Chatbots", "💬", chatbotPresentationTips),
            TipCategory("Business", "💼", businessCategoryTips),
            TipCategory("Cyber", "🔒", hackingCyberTips)
        )
    }
    
    val selectedCategory = categories.getOrNull(selectedCategoryIndex) ?: categories[0]

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Category selector header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ Browse Marco's Lightbulb Tips:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )
            Text(
                text = "${selectedCategoryIndex + 1}/${categories.size}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.secondary
            )
        }
        
        // Category filters row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories.size) { index ->
                val cat = categories[index]
                val isSelected = index == selectedCategoryIndex
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .height(32.dp)
                        .clickable { selectedCategoryIndex = index }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${cat.icon} ${cat.name}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Selected category tips horizontal scroll
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(selectedCategory.tips) { item ->
                SuggestionChip(
                    onClick = { onSuggestionClick(item.prompt) },
                    label = { Text(item.label) },
                    modifier = Modifier.testTag("suggestion_${item.hashCode()}")
                )
            }
        }
    }
}

@Composable
fun TipItem(
    tip: PromptTip,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "💡",
                fontSize = 20.sp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = tip.label.replace("💡 ", ""),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = tip.prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "dots")
    val dotAnimation = @Composable { delayMillis: Int ->
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(delayMillis)
            ),
            label = "dot"
        )
    }

    val dot1Alpha by dotAnimation(0)
    val dot2Alpha by dotAnimation(200)
    val dot3Alpha by dotAnimation(400)

    Row(
        modifier = modifier.padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dotModifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            
        Box(dotModifier.graphicsLayer { alpha = dot1Alpha })
        Box(dotModifier.graphicsLayer { alpha = dot2Alpha })
        Box(dotModifier.graphicsLayer { alpha = dot3Alpha })
    }
}

@Composable
fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val pattern = "\\*\\*(.*?)\\*\\*".toRegex()
        val matches = pattern.findAll(text)
        
        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1
            val innerText = match.groupValues[1]
            
            if (start > cursor) {
                append(text.substring(cursor, start))
            }
            
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(innerText)
            }
            cursor = end
        }
        
        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}
