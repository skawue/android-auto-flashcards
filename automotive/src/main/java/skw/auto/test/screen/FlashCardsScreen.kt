package skw.auto.test.screen

import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import java.util.*

class FlashCardsScreen(carContext: CarContext) : Screen(carContext), RecognitionCallback {
    var textToSpeechPl: TextToSpeech? = null
    var textToSpeechEn: TextToSpeech? = null

    var nextWord: Int = 0
    var dictionary: List<Word> = listOf(Word("english", "angielski", 0))

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

        textToSpeechEn = TextToSpeech(
            carContext
        ) { status ->
            textToSpeechEn?.language = Locale.forLanguageTag("en")
        }
        textToSpeechPl = TextToSpeech(
            carContext
        ) { status ->
            textToSpeechPl?.language = Locale.forLanguageTag("pl")
        }

        recognitionManager.createRecognizer()
    }

    override fun onGetTemplate(): Template {
        recognitionManager.startRecognition()

        Handler().postDelayed({
            sayInPolish(dictionary[nextWord].polish)
        }, 1000L)

        val itemList = prepareItemList()
        val actionStrip = prepareActionStrip()

        return SearchTemplate.Builder(object : SearchTemplate.SearchCallback {
            override fun onSearchTextChanged(searchText: String) {
            }

            override fun onSearchSubmitted(searchText: String) {
            }
        })
            .setHeaderAction(Action.APP_ICON)
            .setSearchHint("Fiszki, wyszukiwanie tylko dla picu...")
            .setShowKeyboardByDefault(false)
            .setItemList(itemList)
            .setActionStrip(actionStrip)
            .build()
    }

    override fun onPrepared(status: RecognitionStatus) {
        Log.d("Flash", "onPrepared: $status")
    }

    override fun onBeginningOfSpeech() {
        Log.d("Flash", "onBeginningOfSpeech")
    }

    override fun onKeywordDetected() {
        Log.d("Flash", "onKeywordDetected")
    }

    override fun onReadyForSpeech(params: Bundle) {
        Log.d("Flash", "onReadyForSpeech")
    }

    override fun onBufferReceived(buffer: ByteArray) {
        Log.d("Flash", "onBufferReceived")
    }

    override fun onRmsChanged(rmsdB: Float) {
//        Log.d("Flash", "onRmsChanged: $rmsdB")
    }

    override fun onPartialResults(results: List<String>) {
        Log.d(
            "Flash",
            "onPartialResult: " + results.joinToString(", ")
        )
    }

    override fun onResults(results: List<String>, scores: FloatArray?) {
        Log.d("Flash", "onResults: " + results.joinToString(", "))

        if (dictionary[nextWord].english
                .split(",")
                .filter { results.map { it.lowercase() }.contains(it) }
                .isNotEmpty()
        ) {
            sayProperWordWithToast(true) { isValid ->
                loadNextWord(isValid)
            }
        }
    }

    override fun onError(errorCode: Int) {
        Log.d("Flash", "onError: $errorCode")
    }

    override fun onEvent(eventType: Int, params: Bundle) {
        Log.d("Flash", "onEvent: $eventType")
    }

    override fun onEndOfSpeech() {
        Log.d("Flash", "onEndOfSpeech")
    }

    private fun sayInPolish(text: String) {
        val a = textToSpeechPl?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "" + Calendar.getInstance().timeInMillis
        )
        Log.d("Flash", "sayInPolish: $a")
    }

    private fun sayInEnglish(text: String) {
        val a = textToSpeechEn?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "" + Calendar.getInstance().timeInMillis
        )
        Log.d("Flash", "sayInEnglish: $a")
    }

    private fun sayProperWordWithToast(isValidAnswer: Boolean, onDone: (Boolean) -> Unit) {
        textToSpeechEn?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(text: String?) {
                CarToast.makeText(
                    carContext,
                    dictionary[nextWord].english,
                    CarToast.LENGTH_LONG
                ).show()
            }

            override fun onDone(text: String?) {
                onDone(isValidAnswer)
            }

            override fun onError(text: String?) {
            }
        })

        sayInEnglish(dictionary[nextWord].english)
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

    private fun prepareItemList(): ItemList {
        val polishText = dictionary[nextWord].polish

        return ItemList.Builder()
            .addItem(
                Row.Builder().setTitle("Przetłumacz:")
                    .addText(Utils.colorize(polishText, CarColor.BLUE, 0, polishText.length))
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Powtórz")
                    .setImage(CarIcon.Builder(CarIcon.ALERT).setTint(CarColor.YELLOW).build())
                    .setOnClickListener {
                        sayInPolish(polishText)
                    }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Pomiń")
                    .setImage(CarIcon.Builder(CarIcon.ERROR).setTint(CarColor.RED).build())
                    .setOnClickListener {
                        sayProperWordWithToast(false) { isValid ->
                            loadNextWord(isValid)
                        }
                    }
                    .build()
            ).build()
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