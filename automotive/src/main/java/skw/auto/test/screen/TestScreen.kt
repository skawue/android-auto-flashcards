package skw.auto.test.screen

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.*
import skw.auto.test.database.Utils
import skw.auto.test.database.Word
import skw.auto.test.recognition.MyVoiceRecognitionListener
import skw.auto.test.recognition.RecognitionCallback
import skw.auto.test.recognition.RecognitionStatus

class TestScreen(carContext: CarContext) : Screen(carContext), RecognitionCallback {
    var nextWord: Int = 0
    var dictionary: List<Word> = listOf(Word("asd", "dupa", 0))

    private val recognitionManager: MyVoiceRecognitionListener by lazy {
        MyVoiceRecognitionListener(carContext, activationKeyword = "hello", callback = this)
    }

    constructor(
        carContext: CarContext,
        mainDictionary: List<Word>,
        next: Int
    ) : this(carContext) {
        nextWord = next
        dictionary = mainDictionary
    }

    override fun onGetTemplate(): Template {
        recognitionManager.createRecognizer()
        recognitionManager.startRecognition()

        val actionStrip = prepareActionStrip()

        return PaneTemplate.Builder(
            Pane.Builder().addRow(Row.Builder().setTitle("ads").build()).build()
        )
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(actionStrip)
            .build()
    }

    override fun onPrepared(status: RecognitionStatus) {
        Log.d("DUPA", "onPrepared: $status")
    }

    override fun onBeginningOfSpeech() {
        Log.d("DUPA", "onBeginningOfSpeech")
    }

    override fun onKeywordDetected() {
        Log.d("DUPA", "onKeywordDetected")
    }

    override fun onReadyForSpeech(params: Bundle) {
        Log.d("DUPA", "onReadyForSpeech")
    }

    override fun onBufferReceived(buffer: ByteArray) {
        Log.d("DUPA", "onBufferReceived")
    }

    override fun onRmsChanged(rmsdB: Float) {
        Log.d("DUPA", "onRmsChanged: $rmsdB")
    }

    override fun onPartialResults(results: List<String>) {
        Log.d(
            "DUPA",
            "onPartialResult: " + results.joinToString(", ")
        )
    }

    override fun onResults(results: List<String>, scores: FloatArray?) {
        Log.d("DUPA", "onResults: " + results.joinToString(", "))

        if (dictionary[nextWord].english
                .split(",")
                .filter { results.contains(it) }
                .isNotEmpty()
        ) {
            sayProperWordWithToast(true) { isValid ->
                loadNextWord(isValid)
            }
        }
    }

    override fun onError(errorCode: Int) {
        Log.d("DUPA", "onError: $errorCode")
    }

    override fun onEvent(eventType: Int, params: Bundle) {
        Log.d("DUPA", "onEvent: $eventType")
    }

    override fun onEndOfSpeech() {
        Log.d("DUPA", "onEndOfSpeech")
    }

    private fun sayProperWordWithToast(isValidAnswer: Boolean, onDone: (Boolean) -> Unit) {

    }

    private fun loadNextWord(isValidAnswer: Boolean) {
        Handler(carContext.mainLooper).post {
            recognitionManager.stopRecognition()

            dictionary[nextWord].number += if (isValidAnswer) 1 else -1
            nextWord += 1
            nextWord = nextWord.mod(dictionary.size)
            Utils.saveDictionaryToDb(carContext, dictionary)
            invalidate()
        }
    }

    private fun prepareActionStrip(): ActionStrip {
        return ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Settings")
                    .setOnClickListener {
                        CarToast.makeText(
                            carContext,
                            "W przygotowaniu...",
                            CarToast.LENGTH_LONG
                        ).show()
                    }
                    .build()
            ).build()
    }
}