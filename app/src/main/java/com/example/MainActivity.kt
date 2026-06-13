package com.example

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

  private var tts: TextToSpeech? = null
  private var isTtsInitialized = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize standard TextToSpeech engine
    try {
        tts = TextToSpeech(this, this)
    } catch (e: Exception) {
        Log.e("MainActivity", "Failed to construct TextToSpeech instance", e)
    }

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          val chatViewModel: ChatViewModel = viewModel()
          ChatScreen(
              viewModel = chatViewModel,
              onSpeak = { text -> speakOut(text) },
              modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      val result = tts?.setLanguage(Locale.US)
      if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
        Log.e("MainActivity", "Language is currently missing or not supported.")
      } else {
        isTtsInitialized = true
      }
    } else {
      Log.e("MainActivity", "TextToSpeech init status failed.")
    }
  }

  private fun speakOut(text: String) {
    if (isTtsInitialized && tts != null) {
      // Remove basic markdown signs for clear narration
      val cleanText = text.replace("**", "").replace("*", "")
      
      // Determine if there is Bengali text in the message to switch TTS language
      val containsBengali = cleanText.any { it.code in 0x0980..0x09FF }
      if (containsBengali) {
          tts?.language = Locale("bn", "BD")
      } else {
          tts?.language = Locale.US
      }
      
      tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "MarcoSpeech")
    }
  }

  override fun onDestroy() {
    tts?.let {
        it.stop()
        it.shutdown()
    }
    super.onDestroy()
  }
}

