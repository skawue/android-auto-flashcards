package skw.auto.test.service

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import skw.auto.test.RequestPermissionScreen
import skw.auto.test.database.Utils
import skw.auto.test.database.Word
import skw.auto.test.screen.FlashCardsScreen

class MyCarAppService : CarAppService() {

    var dictionary = listOf<Word>()

    override fun onCreate() {
        super.onCreate()

        dictionary = Utils.getDictionaryFromDb(this).sortedBy { it.number }
        Log.d("Flash", "onCreate");
    }

    override fun createHostValidator(): HostValidator {
        return if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
        } else {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build();
        }
    }

    override fun onCreateSession(): Session {
        return object : Session() {
            override fun onCreateScreen(intent: Intent): Screen {
                Log.d("Flash", "onCreateScreen");
                return if (carContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    FlashCardsScreen(carContext, dictionary, 0)
                } else {
                    RequestPermissionScreen(carContext, dictionary)
                }
            }
        }
    }
}