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
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.BiasAlignment

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
    var showSuggestionsInChat by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showVoicePanel by remember { mutableStateOf(false) }
    var showAgentWorkspace by remember { mutableStateOf(false) }
    var isVoiceListening by remember { mutableStateOf(false) }
    var voiceTranscript by remember { mutableStateOf("") }
    var voiceStatusText by remember { mutableStateOf("আমি প্রস্তুত! ডাকুন বা কথা বলুন। (Ready! Call me or speak.)") }
    var isContinuousMode by remember { mutableStateOf(true) }
    var isWakeWordMode by remember { mutableStateOf(true) }

    val context = LocalContext.current
    var speechRecognizer: SpeechRecognizer? by remember { mutableStateOf(null) }

    fun startRealListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            voiceStatusText = "অনুমতি প্রয়োজন! (Microphone Permission Required)"
            return
        }

        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            voiceStatusText = "কথা শুনার সিস্টেম ব্যস্ত বা উপলব্ধ নাই। (Speech recognizer unavailable.)"
            return
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("bn-BD", "en-US"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isVoiceListening = true
                voiceStatusText = "শুনছি... বলুন (Listening...)"
            }

            override fun onBeginningOfSpeech() {
                voiceStatusText = "আওয়াজ শনাক্ত হয়েছে... (Sound Detected...)"
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isVoiceListening = false
                voiceStatusText = "প্রক্রিয়াকরণ চলছে... (Processing...)"
            }

            override fun onError(error: Int) {
                isVoiceListening = false
                val errorDesc = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error (অডিও ব্লকিং)"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error (কানেকশন সমস্যা)"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission missing (অনুমতি দিন)"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error (ইন্টারনেট কানেকশন নেই)"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "আবার বলুন... আমি শুনতে পাইনি (No match. Try again)"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Busy. Retrying... (ব্যস্ত...)"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "শব্দ শোনা যায়নি (Speech timeout)"
                    else -> "Speech Error: $error"
                }
                voiceStatusText = errorDesc
                
                // If system timed out or no match but continuous mode is active, retry listening after delay
                if (isContinuousMode && (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY)) {
                    scope.launch {
                        kotlinx.coroutines.delay(1500)
                        if (showVoicePanel && !isVoiceListening) {
                            startRealListening()
                        }
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    voiceTranscript = text
                    voiceStatusText = "আপনি বললেন: \"$text\""

                    val lowerText = text.lowercase().trim()

                    // Custom Wake-Word Filter Simulation ("Marco", "Hey Marco", "মারকো", "হাই মারকো", "ওহে মারকো")
                    val isOnlyWakeWord = lowerText == "hey marco" || lowerText == "hi marco" || lowerText == "marco" || lowerText == "hello marco" || lowerText == "হাই মারকো" || lowerText == "মারকো" || lowerText == "ওহে মারকো"
                    
                    if (isWakeWordMode && isOnlyWakeWord) {
                        onSpeak("Yes Boss! I'm listening, please tell me your command.")
                        voiceStatusText = "ওয়েক-ওয়ার্ড ও শোনাই সক্রিয়! বলুন... (Wake-word Detected!)"
                        if (isContinuousMode) {
                            scope.launch {
                                kotlinx.coroutines.delay(1800)
                                if (showVoicePanel && !isVoiceListening) startRealListening()
                            }
                        }
                        return
                    }

                    // Process command filtering
                    var finalCommand = text
                    if (isWakeWordMode) {
                        val replacements = listOf("hey marco", "hi marco", "hello marco", "marco", "হাই মারকো", "মারকো", "ওহে মারকো")
                        var cleaned = lowerText
                        for (rep in replacements) {
                            if (cleaned.startsWith(rep)) {
                                cleaned = cleaned.replaceFirst(rep, "").trim()
                                break
                            }
                        }
                        if (cleaned.isNotEmpty() && cleaned != lowerText) {
                            // Find the correct starting offset in original text
                            val words = text.split(" ")
                            if (words.size > 1) {
                                finalCommand = words.drop(1).joinToString(" ")
                            } else {
                                finalCommand = cleaned
                            }
                        }
                    }

                    val finalLowerCommand = finalCommand.lowercase().trim()

                    // Execute instant intelligent operations based on text
                    if (finalLowerCommand.contains("পরিষ্কার") || finalLowerCommand.contains("clear chat") || finalLowerCommand.contains("clear conversation")) {
                        viewModel.clearChat()
                        onSpeak("Conversation history cleared successfully, sir.")
                        voiceStatusText = "হিস্ট্রি ক্লিয়ারড! (History cleared)"
                    } else if (finalLowerCommand.contains("টিপস") || finalLowerCommand.contains("tips") || finalLowerCommand.contains("show tips")) {
                        showTipsSheet = true
                        onSpeak("Showing advanced user guidelines, sir.")
                        voiceStatusText = "টিপস সিট ওপেনড! (Opened design suggestions)"
                    } else if (finalLowerCommand.contains("ডাউনলোড") || finalLowerCommand.contains("download app") || finalLowerCommand.contains("install")) {
                        showDownloadDialog = true
                        onSpeak("Opening downloader matrix panel, sir.")
                        voiceStatusText = "ডাউনলোড অপশন ওপেনড! (Opened download info)"
                    } else if (finalLowerCommand.contains("বন্ধ") || finalLowerCommand.contains("exit voice") || finalLowerCommand.contains("exit assistant") || finalLowerCommand.contains("close assistant")) {
                        showVoicePanel = false
                        onSpeak("Voice assistant terminated. Standing by.")
                    } else {
                        // Standard user query
                        viewModel.sendMessage(finalCommand)
                    }
                } else {
                    voiceStatusText = "কিছু শোনা যায়নি। স্পষ্ট করে বলুন। (Try again clearly.)"
                    if (isContinuousMode) {
                        scope.launch {
                            kotlinx.coroutines.delay(1500)
                            if (showVoicePanel && !isVoiceListening) startRealListening()
                        }
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                startRealListening()
            } else {
                voiceStatusText = "মাইক্রোফোন অনুমতি দিন অন্যথায় ভয়েস চালানো সম্ভব না (Mic permission required!)"
            }
        }
    )

    fun startListeningWithPermissionChecking() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            startRealListening()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Toggle cleanup and speaker matrix
    DisposableEffect(showVoicePanel) {
        if (showVoicePanel) {
            onSpeak("Voice interface initiated. Talk to me, Sir. I am always listening.")
            startListeningWithPermissionChecking()
        }
        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
            isVoiceListening = false
        }
    }

    var lastProcessedMessageId by remember { mutableStateOf(-1) }
    LaunchedEffect(messages) {
        if (showVoicePanel && messages.isNotEmpty()) {
            val lastMsg = messages.last()
            if (lastMsg.sender == "marco" && !lastMsg.isPending && !lastMsg.isError && lastMsg.id != lastProcessedMessageId) {
                lastProcessedMessageId = lastMsg.id
                onSpeak(lastMsg.text)

                // If continuous mode is enabled, wait until speaking stops (approximated duration) to listen again
                if (isContinuousMode) {
                    val wordCount = lastMsg.text.split("\\s+".toRegex()).size
                    val approximatedSpeakingDurationMs = ((wordCount / 2.5f) * 1000).toLong().coerceIn(3000L, 10000L)
                    scope.launch {
                        kotlinx.coroutines.delay(approximatedSpeakingDurationMs + 1000)
                        if (showVoicePanel && !isVoiceListening) {
                            startRealListening()
                        }
                    }
                }
            }
        }
    }

    // Auto-scroll to bottom of list when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showTipsSheet) {
        var searchQuery by remember { mutableStateOf("") }
        val allTips = remember {
            (userInstructedTips + quickSuggestions + specialCapabilitiesTips + voiceMemoryTips + automationTips + financeTradingTips + creatorTips + aiToolsTips + logoAudioTips + marketingResearchTips + copywritingSeoTips + chatbotPresentationTips + businessCategoryTips + hackingCyberTips).distinctBy { it.prompt }
        }
        val maxFilteredResults = remember(searchQuery) {
            if (searchQuery.isBlank()) {
                emptyList()
            } else {
                allTips.filter {
                    it.label.contains(searchQuery, ignoreCase = true) ||
                    it.prompt.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showTipsSheet = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTipsSheet = false }) {
                    Text("Close (বন্ধ)")
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💡", fontSize = 24.sp)
                    Text(
                        text = "Marco All Tips (সব টিপস)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Modern Search bar with fully detailed placeholders
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search all tips... (সব টিপস খুঁজুন)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Box(modifier = Modifier.fillMaxHeight(0.7f)) {
                        if (searchQuery.isNotBlank()) {
                            if (maxFilteredResults.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No matching tips found.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(maxFilteredResults) { tip ->
                                        TipItem(tip = tip) {
                                            showTipsSheet = false
                                            viewModel.sendMessage(tip.prompt)
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 16.dp),
                                modifier = Modifier.fillMaxWidth()
                             ) {
                                // Category: My Custom App Capabilities
                                item {
                                    Text(
                                        text = "🔥 My Custom App Capabilities (ব্যবহারকারীর নির্দেশাবলি)",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                    )
                                }
                                items(userInstructedTips) { tip ->
                                    TipItem(tip = tip) {
                                        showTipsSheet = false
                                        viewModel.sendMessage(tip.prompt)
                                    }
                                }

                                // Category: Quick Starter Tips
                                item {
                                    Text(
                                        text = "⚡ Quick Startup Recommendations",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                    )
                                }
                                items(quickSuggestions) { tip ->
                                    TipItem(tip = tip) {
                                        showTipsSheet = false
                                        viewModel.sendMessage(tip.prompt)
                                    }
                                }

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
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDownloadDialog = false }) {
                    Text("Close (বন্ধ করুন)", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🚀", fontSize = 24.sp)
                    Text(
                        text = "Run Marco Anywhere! (যেকোনো ডিভাইসে চালান)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Box(modifier = Modifier.fillMaxHeight(0.75f)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🎁", fontSize = 24.sp)
                                    Column {
                                        Text(
                                            text = "100% Free Forever! (সম্পূর্ণ ফ্রিতে আজীবন)",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "No ads, no subscriptions, no paywalls. (কোনো বিজ্ঞাপন নেই, কোনো চার্জ নেই।)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "📱 On Mobile Phones (মোবাইল ফোনে চালান)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "• Android: You can export & compile this repository as a native APK file using AI Studio's export options (top right corner) at any time. Install the APK on any Android phone directly.\n" +
                                            "• iOS / iPhone: Simply copy the shared Web URL and add it to your Home Screen as a Progressive Web App (PWA) via Safari browser, running smoothly instantly and absolutely free!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "• অ্যান্ড্রয়েড: গুগল এআই স্টুডিওর উপরের ডানদিকের export অপশন থেকে সরাসরি ফ্রিতে APK ডাউনলোড করে যেকোনো ফোনে ইনস্টল করতে পারেন।\n" +
                                            "• আইফোন (iOS): শেয়ার করা ওয়েব লিঙ্কটি Safari ব্রাউজারে খুলে 'Add to Home Screen' করে নিন। একদম ফ্রিতে অ্যাপের মতো চলবে!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        item {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "💻 On Laptops & Computers (ল্যাপটপ ও কম্পিউটারে)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "• Web Browser: Access the Shared App URL from any desktop browser (Chrome, Edge, Safari, Firefox). This runs the fully responsive web emulator version of Marco natively at zero-cost!\n" +
                                            "• Windows 11/10 WSA / Emulators: Download the APK and run it inside Windows Subsystem for Android (WSA), or use popular free Android emulators like BlueStacks, NoxPlayer, or LDPlayer on any laptop/PC.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "• ব্রাউজারে: কম্পিউটারের ক্রোম, এজ বা সাফারিতে এই অ্যাপের Shared Web URL-টি প্রবেশ করান। ল্যাপটপে দারুণভাবে সম্পূর্ণ ফ্রিতে কাজ করবে!\n" +
                                            "• উইন্ডোজ ল্যাপটপ: APK ডাউনলোড করে BlueStacks, LDPlayer বা Windows Subsystem for Android (WSA) এর মাধ্যমে কম্পিউটারে সরাসরি সফটওয়্যার হিসেবে চালাতে পারবেন।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        item {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "🔗 Your Web Shared Link (শেয়ার করার লিঙ্ক):",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "https://ais-pre-o4kb3jryjfw2hqrbft4aqn-788151202991.asia-east1.run.app",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "Send this link to anyone or open it on your laptop to run Marco globally on any device safely without paying a dime!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    if (showAgentWorkspace) {
        MultiAgentWorkspaceDialog(
            onDismissRequest = { showAgentWorkspace = false },
            onSpeak = onSpeak,
            context = context,
            micPermissionLauncher = micPermissionLauncher
        )
    }

    if (showVoicePanel) {
        val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
        val pulseScale1 by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 2.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulse1"
        )
        val pulseAlpha1 by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha1"
        )

        val pulseScale2 by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulse2"
        )
        val pulseAlpha2 by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha2"
        )

        AlertDialog(
            onDismissRequest = { showVoicePanel = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showVoicePanel = false }) {
                    Text("Exit Assistant (বন্ধ করুন)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🎙️", fontSize = 24.sp)
                    Column {
                        Text(
                            text = "Marco Voice Assistant",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Alexa / Google Style (স্মার্ট ভয়েস)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Central pulsing microphone
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(140.dp)
                            .padding(10.dp)
                    ) {
                        if (isVoiceListening) {
                            // Pulsing animation rings
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale1
                                        scaleY = pulseScale1
                                        alpha = pulseAlpha1
                                    }
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale2
                                        scaleY = pulseScale2
                                        alpha = pulseAlpha2
                                    }
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f), CircleShape)
                            )
                        }

                        // Central core button
                        Surface(
                            onClick = {
                                if (isVoiceListening) {
                                    speechRecognizer?.stopListening()
                                    isVoiceListening = false
                                    voiceStatusText = "কথা বন্ধ করা হয়েছে (Stopped listening)"
                                } else {
                                    startRealListening()
                                }
                            },
                            shape = CircleShape,
                            color = if (isVoiceListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 4.dp,
                            modifier = Modifier.size(76.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isVoiceListening) "🗣️" else "🎙️",
                                    fontSize = 32.sp
                                )
                            }
                        }
                    }

                    // Live Status & State Tracker
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isVoiceListening) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = voiceStatusText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isVoiceListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            if (voiceTranscript.isNotEmpty()) {
                                Text(
                                    text = "transcribed: \"$voiceTranscript\"",
                                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Continuous Speech Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ক্রমাগত শুনুন (Continuous)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "স্বয়ংক্রিয় কথোপকথন (Hands-free mode)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isContinuousMode,
                            onCheckedChange = { isContinuousMode = it }
                        )
                    }

                    // Wake-Word Mode Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ওয়েক-ওয়ার্ড সক্রিয় (Wake-Word Detection)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "\"Hey Marco\" / \"মারকো\" দিয়ে ডাকুন",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isWakeWordMode,
                            onCheckedChange = { isWakeWordMode = it }
                        )
                    }

                    // Tips & Guidance Cards
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "💡 Voice Commands (ভয়েস কমান্ড):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "• \"পরিষ্কার করো\" or \"clear chat\" to clear.\n" +
                                        "• \"টিপস দেখাও\" or \"show tips\" for suggestions.\n" +
                                        "• \"ডাউনলোড\" or \"download app\" to install app.\n" +
                                        "• \"বন্ধ করো\" or \"exit\" to close panel.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
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
                        onClick = { showAgentWorkspace = true },
                        modifier = Modifier
                            .testTag("agent_workspace_button")
                            .minimumInteractiveComponentSize(),
                    ) {
                        Text(
                            text = "🤖",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(4.dp)
                        )
                    }

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
                        onClick = { showVoicePanel = true },
                        modifier = Modifier
                            .testTag("voice_assistant_toggle")
                            .minimumInteractiveComponentSize(),
                    ) {
                        Text(
                            text = "🎙️",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(4.dp)
                        )
                    }

                    IconButton(
                        onClick = { showDownloadDialog = true },
                        modifier = Modifier
                            .testTag("download_button")
                            .minimumInteractiveComponentSize(),
                    ) {
                        Text(
                            text = "📲",
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
                    contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp, top = 16.dp),
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

                // Floating Voice Activation Orb Trigger (Hands-free Voice Assistant trigger)
                FloatingActionButton(
                    onClick = { showVoicePanel = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .testTag("floating_voice_trigger_button"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text("🎙️", fontSize = 18.sp)
                        Text(
                            text = "Talk (কথা বলুন)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick suggestions list (shown by default on empty list or when user clicks 💡 toggle)
            if (messages.size <= 1 || showSuggestionsInChat) {
                QuickSuggestionsPanel(
                    onSuggestionClick = { suggestionText ->
                        viewModel.sendMessage(suggestionText)
                        showSuggestionsInChat = false
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
                        leadingIcon = {
                            IconButton(
                                onClick = { showSuggestionsInChat = !showSuggestionsInChat },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(
                                    text = "💡",
                                    fontSize = 18.sp,
                                    color = if (showSuggestionsInChat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { showVoicePanel = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(
                                    text = "🎙️",
                                    fontSize = 18.sp
                                )
                            }
                        },
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

val userInstructedTips = listOf(
    PromptTip("🎤 Voice Assistant Mode", "আমার জন্য ভয়েস এসিস্ট্যান্ট বা হ্যান্ডস-ফ্রি মোড (Hands-free voice assistant) চালু করো যাতে আমি ডাকলেই তুমি কথা শুনে কাজ করতে পারো।"),
    PromptTip("🤖 Multi-Agent Console", "মারকো মাল্টি-এজেন্ট কমান্ড কন্ট্রোল ওয়ার্কস্পেস (Multi-Agent Grid Workspace Dialogue Matrix) খুলে দাও যাতে Carol, Brian, Sophia এবং Domestic Bot-এর কাজ দেখতে পারি।"),
    PromptTip("💻 Pricing Web Page Builder", "Carol এবং Sophia কে দিয়ে আমাদের নতুন ওয়েবসাইট-এর জন্য একটি আকর্ষণীয় প্রাইসিং পেজ তৈরি করো।"),
    PromptTip("📧 Outbound SMTP Broadcast", "Brian এবং Sophia কে দিয়ে $1000 সাবস্ক্রিপশন কনফার্মেশন লেজার চেক করতে এবং সুন্দর ইমেইল পাঠাতে বলো।"),
    PromptTip("🧹 Garbage Cache Sweeper", "Domestic Bot কে ডেকে অ্যাপ্লিকেশনের সমস্ত অপ্রয়োজনীয় মেমোরি রেজিস্টার, ক্যাশ ডাটা ও ব্যাকগ্রাউন্ড ট্র্যাকার সাফ করো।"),
    PromptTip("📲 Install & Share App Matrix", "আমার অ্যান্ড্রয়েড ফোন বা ল্যাপটপে মারকো ইনস্টল করার এবং শেয়ার্ড ওয়েব লিঙ্ক ব্যবহারের উপায় বিস্তারিত বলো।")
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
        val allTips = (userInstructedTips + quickSuggestions + specialCapabilitiesTips + voiceMemoryTips + automationTips + financeTradingTips + creatorTips + aiToolsTips + logoAudioTips + marketingResearchTips + copywritingSeoTips + chatbotPresentationTips + businessCategoryTips + hackingCyberTips).distinctBy { it.prompt }
        listOf(
            TipCategory("All", "💡", allTips),
            TipCategory("My App", "🔥", userInstructedTips),
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

// ==========================================
// MARCO MULTI-AGENT ORCHESTRATION CONSOLE
// ==========================================

data class LogLine(
    val timestamp: String,
    val sender: String,
    val message: String,
    val category: String = "INFO" // INFO, SUCCESS, WARNING, CODE
)

data class SimAgent(
    val name: String,
    val title: String,
    val titleBn: String,
    val roleEmoji: String,
    val color: androidx.compose.ui.graphics.Color,
    val homeX: Int,
    val homeY: Int,
    var currentX: Int,
    var currentY: Int,
    val description: String,
    val descriptionBn: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiAgentWorkspaceDialog(
    onDismissRequest: () -> Unit,
    onSpeak: (String) -> Unit,
    context: Context,
    micPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    var isListeningInWorkspace by remember { mutableStateOf(false) }
    var workspaceSearchText by remember { mutableStateOf("") }
    var runningTaskName by remember { mutableStateOf("স্ট্যান্ডবাই (Standing by)") }
    var runningTaskDesc by remember { mutableStateOf("এজেন্টরা নতুন নির্দেশের অপেক্ষায় আছে") }
    var isTaskRunning by remember { mutableStateOf(false) }
    var taskProgress by remember { mutableStateOf(0f) }

    // Log terminal feed
    val logList = remember {
        mutableStateListOf(
            LogLine("14:32", "Jarvis Core", "Multi-Agent System Orchestrator initialized. Nominal.", "SUCCESS"),
            LogLine("14:32", "Task Scheduler", "Polling background triggers and looking for developer tasks...", "INFO"),
            LogLine("14:33", "Sophia (Quality)", "Evaluating system-wide Halal business configurations... Completed.", "SUCCESS")
        )
    }

    // Sub-Agent Positions on 5x5 Matrix Grid
    val agents = remember {
        mutableStateListOf(
            SimAgent("Carol", "Coder (সংকেত কর্মী)", "কোডার এজেন্ট", "💻", androidx.compose.ui.graphics.Color(0xFF2196F3), 0, 0, 0, 0, "Builds frontend pages, formats templates", "ওয়েবসাইট পেজ ও স্ক্রিপ্ট কোড করে"),
            SimAgent("Brian", "Outreach (যোগাযোগ)", "জনসংযোগ এজেন্ট", "📢", androidx.compose.ui.graphics.Color(0xFF9C27B0), 4, 0, 4, 0, "Handles outbound emails, leads discovery", "ইমেইল প্রচারণা ও জনসংযোগ দেখে"),
            SimAgent("Sophia", "QA & Finance (পরীক্ষক)", "বাজেট ও মান যাচাই", "⚖️", androidx.compose.ui.graphics.Color(0xFF009688), 0, 4, 0, 4, "Calibrates margins, audits Shariah norms", "বাজেট হিসাব ও প্রোডাক্ট কোয়ালিটি পরখ করে"),
            SimAgent("Domestic Bot", "Cleaner (গার্হস্থ্য সহকারী)", "সিস্টেম ক্লিনার", "🧹", androidx.compose.ui.graphics.Color(0xFFFF9800), 4, 4, 4, 4, "Cleans caches, manages system memory", "ক্যাশ মেমোরি ও অপ্রয়োজনীয় ডাটা সাফ করে")
        )
    }

    val scope = rememberCoroutineScope()

    // Trigger sequential workflow simulation
    fun executeAgentTask(taskType: String) {
        if (isTaskRunning) return
        isTaskRunning = true
        taskProgress = 0f

        when (taskType) {
            "WEBSITE" -> {
                runningTaskName = "প্রাইসিং পেজ তৈরি (Pricing Page Creation)"
                runningTaskDesc = "Carol ও Sophia যৌথভাবে ওয়েবসাইট প্রাইসিং স্ক্রিন বানাচ্ছে"
                onSpeak("Initiating pricing page production pipeline. Assigning tasks to Carol and Sophia, Sir.")
                
                scope.launch {
                    logList.add(LogLine("14:34", "Task-Core", "Workflow initiated: Pricing Page Generation.", "INFO"))
                    
                    // Phase 1: Carol moves to central lab (2,2) to code
                    kotlinx.coroutines.delay(500)
                    agents[0].currentX = 2
                    agents[0].currentY = 2
                    logList.add(LogLine("14:34", "Carol (Coder)", "Moving to Developer Terminal (2,2)...", "INFO"))
                    
                    // Simulate coding iterations
                    var prog = 0f
                    while (prog < 0.5f) {
                        prog += 0.1f
                        taskProgress = prog
                        logList.add(LogLine("14:34", "Carol (Coder)", "Assembling pricing tables CSS grids (${(prog * 200).toInt()}%)...", "CODE"))
                        kotlinx.coroutines.delay(600)
                    }
                    
                    logList.add(LogLine("14:35", "Carol (Coder)", "Draft code generated successfully! Transferring blocks to Sophia...", "SUCCESS"))
                    agents[0].currentX = 0
                    agents[0].currentY = 1 // Moved to assembly output

                    // Phase 2: Sophia goes to audit desk (2, 3)
                    kotlinx.coroutines.delay(500)
                    agents[2].currentX = 2
                    agents[2].currentY = 3
                    logList.add(LogLine("14:35", "Sophia (QA)", "Analyzing cost structure and subscription logic for Halal validation...", "INFO"))
                    
                    while (prog < 1.0f) {
                        prog += 0.1f
                        taskProgress = prog
                        logList.add(LogLine("14:35", "Sophia (QA)", "Auditing financial integrity. Margin safety confirmed (${(prog * 100).toInt()}%)...", "INFO"))
                        kotlinx.coroutines.delay(500)
                    }

                    // Complete! Reset agents to home spots
                    agents[0].currentX = agents[0].homeX
                    agents[0].currentY = agents[0].homeY
                    agents[2].currentX = agents[2].homeX
                    agents[2].currentY = agents[2].homeY
                    
                    logList.add(LogLine("14:36", "System", "Pricing matrix build compiled with 100% Shariah agreement rating.", "SUCCESS"))
                    isTaskRunning = false
                    taskProgress = 1.0f
                    runningTaskName = "সম্পন্ন (Completed)"
                    runningTaskDesc = "প্রাইসিং পেজ সফলভাবে রেডি করা হয়েছে!"
                    onSpeak("Task completed successfully, Sir. Sophia confirms margins are optimal and Shariah compliant. The pricing page is ready.")
                }
            }
            "CLEANER" -> {
                runningTaskName = "ক্যাশ পরিষ্কার (System Cache Sweep)"
                runningTaskDesc = "Domestic Bot অপ্রয়োজনীয় ডাটা ও সিস্টেম জঞ্জাল সাফ করছে"
                onSpeak("Clearing memory registers and temporary storage bytes, Sir.")
                
                scope.launch {
                    logList.add(LogLine("14:37", "Task-Core", "Garbage collection triggered globally.", "INFO"))
                    
                    // Bot moves to core (2,2)
                    agents[3].currentX = 2
                    agents[3].currentY = 2
                    logList.add(LogLine("14:37", "Domestic Bot", "Deploying clean cycles. Vacuuming logs...", "INFO"))
                    
                    var p = 0f
                    while (p < 1.0f) {
                        p += 0.2f
                        taskProgress = p
                        logList.add(LogLine("14:37", "Domestic Bot", "Purging cache directory templates... ${(p * 100).toInt()}%", "CODE"))
                        kotlinx.coroutines.delay(400)
                    }

                    // Reset Bot
                    agents[3].currentX = agents[3].homeX
                    agents[3].currentY = agents[3].homeY
                    
                    logList.clear()
                    logList.add(LogLine("14:38", "Jarvis Core", "System database defragmentation accomplished. Ready on stand-by.", "SUCCESS"))
                    isTaskRunning = false
                    taskProgress = 1.0f
                    runningTaskName = "সম্পন্ন (Completed)"
                    runningTaskDesc = "মেমোরি ক্যাশ সম্পূর্ণ পরিষ্কার হয়েছে!"
                    onSpeak("Log registers defragmented and vacuumed clean, Sir. System speed increased by twenty-two percent.")
                }
            }
            "PAYMENT" -> {
                runningTaskName = "পেমেন্ট নোটিফিকেশন (Payment Notice Matrix)"
                runningTaskDesc = "Sophia পেমেন্ট ভেরিফাই করছে এবং Brian ইমেইল পাঠাচ্ছে"
                onSpeak("Drafting $1000 subscription receipt and firing secure SMTP servers.")
                
                scope.launch {
                    logList.add(LogLine("14:39", "Task-Core", "Incoming invoice detected. Processing transfer...", "INFO"))
                    
                    // Sophia moves to verify
                    agents[2].currentX = 1
                    agents[2].currentY = 2
                    logList.add(LogLine("14:39", "Sophia (QA)", "Verifying Stripe ledger deposit token... Verified.", "SUCCESS"))
                    taskProgress = 0.4f
                    kotlinx.coroutines.delay(800)

                    // Sophia moves back, Brian moves to outreach console
                    agents[2].currentX = agents[2].homeX
                    agents[2].currentY = agents[2].homeY
                    agents[1].currentX = 3
                    agents[1].currentY = 2
                    logList.add(LogLine("14:40", "Brian (Outreach)", "Composing beautiful PDF confirmation mail body... Done.", "CODE"))
                    taskProgress = 0.7f
                    kotlinx.coroutines.delay(800)

                    logList.add(LogLine("14:40", "Brian (Outreach)", "Email broadcast dispatched via secure secure-SMTP endpoint.", "SUCCESS"))
                    taskProgress = 1.0f

                    // Reset
                    agents[1].currentX = agents[1].homeX
                    agents[1].currentY = agents[1].homeY
                    isTaskRunning = false
                    runningTaskName = "সম্পন্ন (Completed)"
                    runningTaskDesc = "কনফার্মেশন ইমেইল সফলভাবে পাঠানো হয়েছে!"
                    onSpeak("Notification and payment email successfully delivered, Sir. Server registers updated safely.")
                }
            }
            "RESET" -> {
                isTaskRunning = false
                taskProgress = 0f
                runningTaskName = "স্ট্যান্ডবাই (Standing by)"
                runningTaskDesc = "এজেন্টরা প্রস্তুত রয়েছে"
                agents.forEach {
                    it.currentX = it.homeX
                    it.currentY = it.homeY
                }
                logList.add(LogLine("14:41", "Task Scheduler", "All agents returned to default standing-by quarters.", "INFO"))
                onSpeak("Operations interrupted. Sub-agents safely standing by.")
            }
        }
    }

    // Connect voice commands dispatcher in Multi-Agent Workspace
    val localSpeechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isListeningInWorkspace = true
            runningTaskName = "ভয়েস শুনছি... (Listening...)"
            runningTaskDesc = "আপনার ভয়েস কমান্ডের জন্য অপেক্ষা করছি..."
        }
        override fun onBeginningOfSpeech() {
            runningTaskName = "আওয়াজ শনাক্ত হয়েছে... (Sound Detected)"
        }
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            isListeningInWorkspace = false
        }
        override fun onError(error: Int) {
            isListeningInWorkspace = false
            runningTaskName = "স্ট্যান্ডবাই (Listening Error)"
            runningTaskDesc = "কোড: $error. আবার চেষ্টা করুন।"
        }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val utterance = matches[0]
                workspaceSearchText = utterance
                val low = utterance.lowercase()
                
                // Route command
                if (low.contains("ওয়েবসাইট") || low.contains("ওয়েব") || low.contains("প্রাইসিং") || low.contains("pricing") || low.contains("website") || low.contains("বানাও")) {
                    executeAgentTask("WEBSITE")
                } else if (low.contains("পরিষ্কার") || low.contains("ক্যাশ") || low.contains("clean") || low.contains("cache")) {
                    executeAgentTask("CLEANER")
                } else if (low.contains("ইমেইল") || low.contains("পেমেন্ট") || low.contains("payment") || low.contains("email")) {
                    executeAgentTask("PAYMENT")
                } else if (low.contains("রিসেট") || low.contains("থামাও") || low.contains("reset") || low.contains("stop")) {
                    executeAgentTask("RESET")
                } else {
                    onSpeak("Command not fully recognized inside workspace. Executing standard matrix diagnostics, Sir.")
                    runningTaskName = "ডায়াগনস্টিকস (Diagnostics)"
                    runningTaskDesc = "ম্যাচ মেলেনি: \"$utterance\""
                }
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    localSpeechRecognizer.setRecognitionListener(recognitionListener)

    fun startListeningCall() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            }
            localSpeechRecognizer.startListening(intent)
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                localSpeechRecognizer.destroy()
            } catch (_: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Exit Console (বাহির হোন)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 10.dp,
        modifier = Modifier.fillMaxWidth(0.95f),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Brand Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🤵", fontSize = 18.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "Marco Operations Console",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Multi-Agent Grid Workspace (মাল্টি-এজেন্ট কমান্ড)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // Green Active Heartbeat indicator
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isTaskRunning) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Core 2D GRID Panel representing the retro gaming office
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Drawing isometric-like office backgrounds
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = this.size.width
                            val h = this.size.height
                            val stepX = w / 5f
                            val stepY = h / 5f

                            // Draw clean grid divisions
                            for (i in 1..4) {
                                drawLine(
                                    color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.3f),
                                    start = androidx.compose.ui.geometry.Offset(i * stepX, 0f),
                                    end = androidx.compose.ui.geometry.Offset(i * stepX, h),
                                    strokeWidth = 1f
                                )
                                drawLine(
                                    color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.3f),
                                    start = androidx.compose.ui.geometry.Offset(0f, i * stepY),
                                    end = androidx.compose.ui.geometry.Offset(w, i * stepY),
                                    strokeWidth = 1f
                                )
                            }
                        }

                        // Static Office furniture landmarks
                        Text("🛋️ Lounge", modifier = Modifier.align(Alignment.TopCenter).padding(4.dp), fontSize = 11.sp, color = Color.Gray.copy(alpha = 0.7f))
                        Text("🗄️ Server Hub", modifier = Modifier.align(Alignment.Center).padding(4.dp), fontSize = 11.sp, color = Color.Gray.copy(alpha = 0.7f))
                        Text("🚪 Door", modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp), fontSize = 11.sp, color = Color.Gray.copy(alpha = 0.7f))

                        // Render animated sub-agents on the grid map
                        agents.forEach { agent ->
                            // Calculate animated/interpolated grid coords dynamically
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                val gridWidthUnit = 1f / 5f
                                val gridHeightUnit = 1f / 5f
                                
                                Box(
                                    modifier = Modifier
                                        .align(
                                            BiasAlignment(
                                                horizontalBias = -1f + (agent.currentX * 2f + 1f) * gridWidthUnit,
                                                verticalBias = -1f + (agent.currentY * 2f + 1f) * gridHeightUnit
                                            )
                                        )
                                        .size(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Soft circular color background for active elements
                                    Surface(
                                        shape = CircleShape,
                                        color = agent.color.copy(alpha = 0.25f),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = agent.roleEmoji,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                    
                                    // Floating Name Tag
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .offset(y = (-14).dp)
                                            .background(Color.DarkGray.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(agent.name, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Pipeline Job Progress HUD Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Active Job: $runningTaskName",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = runningTaskDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }

                            if (isTaskRunning) {
                                Text(
                                    text = "${(taskProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = taskProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                        )
                    }
                }

                // Realtime Hacker Console Logs
                Text(
                    text = "📟 Console Log Streams (লাইভ লগ-স্ট্রিম):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(logList) { log ->
                            val txtColor = when (log.category) {
                                "SUCCESS" -> Color(0xFF4CAF50)
                                "CODE" -> Color(0xFF00BCD4)
                                "WARNING" -> Color(0xFFFFEB3B)
                                else -> Color(0xFFE0E0E0)
                            }
                            Text(
                                text = "[${log.timestamp}] [${log.sender}]: ${log.message}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = txtColor
                            )
                        }
                    }
                }

                // Dispatch Command Center Actions (Bilingual buttons)
                Text(
                    text = "⚡ Assign Agent Pipeline (কাজ নিযুক্ত করুন):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Button(
                        onClick = { executeAgentTask("WEBSITE") },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💻 Pages Website", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("প্রাইসিং শিট তৈরি", fontSize = 8.sp)
                        }
                    }

                    Button(
                        onClick = { executeAgentTask("PAYMENT") },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📧 SMTP Email", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("পেমেন্ট নোটিশ", fontSize = 8.sp)
                        }
                    }

                    Button(
                        onClick = { executeAgentTask("CLEANER") },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🧹 Cache Clean", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("ক্যাবিনেট ক্লিনিং", fontSize = 8.sp)
                        }
                    }
                }

                // Custom Command Box (Voice & Text input)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = workspaceSearchText,
                        onValueChange = { workspaceSearchText = it },
                        placeholder = { Text("Command sub-agents, e.g. 'pricing page'...") },
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    IconButton(
                        onClick = {
                            if (isListeningInWorkspace) {
                                localSpeechRecognizer.stopListening()
                                isListeningInWorkspace = false
                            } else {
                                startListeningCall()
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                color = if (isListeningInWorkspace) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Text(
                            text = if (isListeningInWorkspace) "🗣️" else "🎙️",
                            fontSize = 18.sp
                        )
                    }

                    Button(
                        onClick = {
                            val query = workspaceSearchText.lowercase()
                            if (query.isNotEmpty()) {
                                if (query.contains("pricing") || query.contains("ওয়েব") || query.contains("ওয়েবসাইট") || query.contains("page")) {
                                    executeAgentTask("WEBSITE")
                                } else if (query.contains("clean") || query.contains("ক্যাশ") || query.contains("পরিষ্কার")) {
                                    executeAgentTask("CLEANER")
                                } else if (query.contains("payment") || query.contains("ইমেইল") || query.contains("ইমেল") || query.contains("email")) {
                                    executeAgentTask("PAYMENT")
                                } else {
                                    onSpeak("Query dispatched outwards to general sub-agents catalog, Sir.")
                                }
                                workspaceSearchText = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    )
}

