/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package skw.auto.test;

import static android.Manifest.permission.RECORD_AUDIO;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.ParkedOnlyOnClickListener;
import androidx.car.app.model.Template;

import java.util.ArrayList;
import java.util.List;

import skw.auto.test.database.Word;
import skw.auto.test.screen.FlashCardsScreen;

/**
 * Screen for asking the user to grant location permission.
 */
public class RequestPermissionScreen extends Screen {

    public List<Word> dictionary;

    public RequestPermissionScreen(
            @NonNull CarContext carContext, List<Word> mainDictionary) {
        super(carContext);

        dictionary = mainDictionary;
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        List<String> permissions = new ArrayList<>();
        permissions.add(RECORD_AUDIO);

        String message = "This app needs access to record audio";

        OnClickListener listener = ParkedOnlyOnClickListener.create(() ->
                getCarContext().requestPermissions(
                        permissions,
                        (approved, rejected) -> {
                            CarToast.makeText(
                                    getCarContext(),
                                    String.format("Approved: %s Rejected: %s", approved, rejected),
                                    CarToast.LENGTH_LONG).show();
                            if (!approved.isEmpty()) {
                                getScreenManager().push(new FlashCardsScreen(getCarContext(), dictionary, 0));
                                finish();
                            }
                        }));

        Action action = new Action.Builder()
                .setTitle("Grant Access")
                .setBackgroundColor(CarColor.GREEN)
                .setOnClickListener(listener)
                .build();

        return new MessageTemplate.Builder(message)
                .addAction(action)
                .setHeaderAction(Action.APP_ICON)
                .build();
    }
}
