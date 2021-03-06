/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work.test.workers;

import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Worker;

/**
 * A test {@link Worker} that prints a log and returns a successful result.
 */
public class TestWorker extends Worker {
    private static final String TAG = "TestWorker";

    @Override
    public @NonNull Result doWork() {
        Log.i(TAG, "Doing work.");
        return Result.SUCCESS;
    }
}
