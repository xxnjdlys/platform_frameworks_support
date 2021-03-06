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

package androidx.media2;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Result of the asynchrnous APIs
 *
 * @hide
 */
// TODO(jaewan): Unhide
// TODO(jaewan): double check final vs non-final
// TODO(jaewna): versioned parcelable or not? -- Exception cannot be parcelable.
// Equivalent to 'status_t' in C/C++
@TargetApi(Build.VERSION_CODES.P)
@RestrictTo(LIBRARY)
public final class CommandResult2 {
    // No error code here!
    private final int mResultCode;
    private final long mCompletionTime;
    private final MediaItem2 mItem;

    public CommandResult2(int resultCode, MediaItem2 item) {
        this(resultCode, item, SystemClock.elapsedRealtime());
    }

    public CommandResult2(int resultCode, MediaItem2 item, long completionTime) {
        mResultCode = resultCode;
        mCompletionTime = completionTime;
        mItem = item;
    }

    /**
     * Gets the result code.
     * <p>
     * Check the class documentation that has returned this {@link CommandResult2} to understand the
     * meaning of the code value.
     *
     * @return result code
     */
    public final int getResultCode() {
        return mResultCode;
    }

    /**
     * Gets the completion time of the command.
     *
     * @return completion time
     */
    public final long getCompletionTime() {
        return mCompletionTime;
    }

    /**
     * Gets the {@link MediaItem2} for which the command was executed.
     *
     * @return media item
     */
    public @Nullable MediaItem2 getMediaItem() {
        return mItem;
    }
}
