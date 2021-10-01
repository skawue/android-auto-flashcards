package skw.auto.test.database

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import androidx.car.app.CarAppService
import androidx.car.app.model.CarColor
import androidx.car.app.model.ForegroundCarColorSpan
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object Utils {
    val SHARED_NAME = "SHARED"
    val DICTIONARY_NAME = "dictionary"

    fun getDictionaryFromDb(context: Context): List<Word> {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_NAME, CarAppService.MODE_PRIVATE)
        val dictionaryString = sharedPreferences.getString(DICTIONARY_NAME, "[]")

        val dictionary: List<Word> =
            Gson().fromJson(dictionaryString, object : TypeToken<List<Word>>() {}.type)

        if (dictionary.isEmpty()) {
            sharedPreferences.edit().putString(DICTIONARY_NAME, Gson().toJson(Data.dictionary))
                .commit()

            return Data.dictionary
        }

        return dictionary
    }

    fun saveDictionaryToDb(context: Context, dictionary: List<Word>) {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_NAME, CarAppService.MODE_PRIVATE)

        sharedPreferences.edit().putString(DICTIONARY_NAME, Gson().toJson(dictionary)).commit()
    }

    fun colorize(text: String, color: CarColor, index: Int, length: Int): CharSequence {
        val spannableText = SpannableString(text)
        spannableText.setSpan(
            ForegroundCarColorSpan.create(color),
            index,
            index + length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannableText
    }
}