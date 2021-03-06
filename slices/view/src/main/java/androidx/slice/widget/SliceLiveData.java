/*
 * Copyright 2017 The Android Open Source Project
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
package androidx.slice.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.ArraySet;
import androidx.lifecycle.LiveData;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceSpec;
import androidx.slice.SliceSpecs;
import androidx.slice.SliceStructure;
import androidx.slice.SliceUtils;
import androidx.slice.SliceViewManager;
import androidx.slice.core.SliceQuery;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Class with factory methods for creating LiveData that observes slices.
 *
 * @see #fromUri(Context, Uri)
 * @see LiveData
 */
@RequiresApi(19)
public final class SliceLiveData {
    private static final String TAG = "SliceLiveData";

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final SliceSpec OLD_BASIC = new SliceSpec("androidx.app.slice.BASIC", 1);

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final SliceSpec OLD_LIST = new SliceSpec("androidx.app.slice.LIST", 1);

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final Set<SliceSpec> SUPPORTED_SPECS = new ArraySet<>(
            Arrays.asList(SliceSpecs.BASIC, SliceSpecs.LIST, OLD_BASIC, OLD_LIST));

    /**
     * Produces a {@link LiveData} that tracks a Slice for a given Uri. To use
     * this method your app must have the permission to the slice Uri.
     */
    public static @NonNull LiveData<Slice> fromUri(@NonNull Context context, @NonNull Uri uri) {
        return new SliceLiveDataImpl(context.getApplicationContext(), uri);
    }

    /**
     * Produces a {@link LiveData} that tracks a Slice for a given Intent. To use
     * this method your app must have the permission to the slice Uri.
     */
    public static @NonNull LiveData<Slice> fromIntent(@NonNull Context context,
            @NonNull Intent intent) {
        return new SliceLiveDataImpl(context.getApplicationContext(), intent);
    }

    /**
     * Produces a {@link LiveData} that tracks a Slice for a given InputStream. To use
     * this method your app must have the permission to the slice Uri.
     *
     * This will not ask the hosting app for a slice immediately, instead it will display
     * the slice passed in through the input. When the user interacts with the slice, then
     * the app will be started to obtain the current slice and trigger the user action.
     */
    public static @NonNull LiveData<Slice> fromStream(@NonNull Context context,
            @NonNull InputStream input, OnErrorListener listener) {
        return fromStream(context, SliceViewManager.getInstance(context), input, listener, false);
    }

    /**
     * Version of {@link #fromStream} that blocks until initial slice loading
     * is complete.
     */
    public static @NonNull LiveData<Slice> fromStreamBlocking(@NonNull Context context,
            @NonNull InputStream input, OnErrorListener listener) {
        return fromStream(context, SliceViewManager.getInstance(context), input, listener, true);
    }

    /**
     * Version for testing
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    public static LiveData<Slice> fromStream(@NonNull Context context,
            SliceViewManager manager, @NonNull InputStream input, OnErrorListener listener,
            boolean blocking) {
        return new CachedLiveDataImpl(context, manager, input, listener, blocking);
    }

    private static class CachedLiveDataImpl extends LiveData<Slice> {
        final SliceViewManager mSliceViewManager;
        private final OnErrorListener mListener;
        final Context mContext;
        Uri mUri;
        private boolean mActive;
        List<Uri> mPendingUri = new ArrayList<>();
        private boolean mLive;
        SliceStructure mStructure;
        List<Context> mPendingContext = new ArrayList<>();
        List<Intent> mPendingIntent = new ArrayList<>();
        private boolean mSliceCallbackRegistered;

        CachedLiveDataImpl(final Context context, final SliceViewManager manager,
                final InputStream input, final OnErrorListener listener, boolean blocking) {
            super();
            mContext = context;
            mSliceViewManager = manager;
            mUri = null;
            mListener = listener;
            if (blocking) {
                loadInitialSlice(input);
            } else {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        loadInitialSlice(input);
                    }
                });
            }
        }

        protected void loadInitialSlice(InputStream input) {
            try {
                Slice s = SliceUtils.parseSlice(mContext, input, "UTF-8",
                        new SliceUtils.SliceActionListener() {
                            @Override
                            public void onSliceAction(Uri actionUri, Context context,
                                    Intent intent) {
                                goLive(actionUri, context, intent);
                            }
                        });
                mStructure = new SliceStructure(s);
                mUri = s.getUri();
                if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                    setValue(s);
                } else {
                    postValue(s);
                }
            } catch (Exception e) {
                mListener.onSliceError(OnErrorListener.ERROR_INVALID_INPUT, e);
            }
        }

        void goLive(Uri actionUri, Context context, Intent intent) {
            mLive = true;
            mPendingUri.add(actionUri);
            mPendingContext.add(context);
            mPendingIntent.add(intent);
            if (mActive && !mSliceCallbackRegistered) {
                AsyncTask.execute(mUpdateSlice);
                mSliceViewManager.registerSliceCallback(mUri, mSliceCallback);
                mSliceCallbackRegistered = true;
            }
        }

        @Override
        protected void onActive() {
            mActive = true;
            if (mLive && !mSliceCallbackRegistered) {
                AsyncTask.execute(mUpdateSlice);
                mSliceViewManager.registerSliceCallback(mUri, mSliceCallback);
                mSliceCallbackRegistered = true;
            }
        }

        @Override
        protected void onInactive() {
            mActive = false;
            if (mLive && mSliceCallbackRegistered) {
                mSliceViewManager.unregisterSliceCallback(mUri, mSliceCallback);
                mSliceCallbackRegistered = false;
            }
        }

        void onSliceError(int error, Throwable t) {
            mListener.onSliceError(error, t);
            if (mLive) {
                if (mSliceCallbackRegistered) {
                    mSliceViewManager.unregisterSliceCallback(mUri, mSliceCallback);
                    mSliceCallbackRegistered = false;
                }
                mLive = false;
            }
        }

        protected void updateSlice() {
            try {
                Slice s = mSliceViewManager.bindSlice(mUri);
                mSliceCallback.onSliceUpdated(s);
            } catch (Exception e) {
                mListener.onSliceError(OnErrorListener.ERROR_UNKNOWN, e);
            }
        }


        private final Runnable mUpdateSlice = new Runnable() {
            @Override
            public void run() {
                updateSlice();
            }
        };

        final SliceViewManager.SliceCallback mSliceCallback =
                new SliceViewManager.SliceCallback() {
            @Override
            public void onSliceUpdated(@NonNull Slice s) {
                if (mPendingUri.size() > 0) {
                    if (s == null) {
                        onSliceError(OnErrorListener.ERROR_SLICE_NO_LONGER_PRESENT, null);
                        return;
                    }
                    SliceStructure structure = new SliceStructure(s);
                    if (!mStructure.equals(structure)) {
                        onSliceError(OnErrorListener.ERROR_STRUCTURE_CHANGED, null);
                        return;
                    }
                    SliceMetadata metaData = SliceMetadata.from(mContext, s);
                    if (metaData.getLoadingState() == SliceMetadata.LOADED_ALL) {
                        for (int i = 0; i < mPendingUri.size(); i++) {
                            SliceItem item = SliceQuery.findItem(s, mPendingUri.get(i));
                            if (item != null) {
                                try {
                                    item.fireAction(mPendingContext.get(i), mPendingIntent.get(i));
                                } catch (PendingIntent.CanceledException e) {
                                    onSliceError(OnErrorListener.ERROR_UNKNOWN, e);
                                    return;
                                }
                            } else {
                                onSliceError(
                                        OnErrorListener.ERROR_UNKNOWN, new NullPointerException());
                                return;
                            }
                        }
                        mPendingUri.clear();
                        mPendingContext.clear();
                        mPendingIntent.clear();
                    }
                }
                postValue(s);
            }
        };
    }

    private static class SliceLiveDataImpl extends LiveData<Slice> {
        final Intent mIntent;
        final SliceViewManager mSliceViewManager;
        Uri mUri;

        SliceLiveDataImpl(Context context, Uri uri) {
            super();
            mSliceViewManager = SliceViewManager.getInstance(context);
            mUri = uri;
            mIntent = null;
            // TODO: Check if uri points at a Slice?
        }

        SliceLiveDataImpl(Context context, Intent intent) {
            super();
            mSliceViewManager = SliceViewManager.getInstance(context);
            mUri = null;
            mIntent = intent;
        }

        @Override
        protected void onActive() {
            AsyncTask.execute(mUpdateSlice);
            if (mUri != null) {
                mSliceViewManager.registerSliceCallback(mUri, mSliceCallback);
            }
        }

        @Override
        protected void onInactive() {
            if (mUri != null) {
                mSliceViewManager.unregisterSliceCallback(mUri, mSliceCallback);
            }
        }

        private final Runnable mUpdateSlice = new Runnable() {
            @Override
            public void run() {
                try {
                    Slice s = mUri != null ? mSliceViewManager.bindSlice(mUri)
                            : mSliceViewManager.bindSlice(mIntent);
                    if (mUri == null && s != null) {
                        mUri = s.getUri();
                        mSliceViewManager.registerSliceCallback(mUri, mSliceCallback);
                    }
                    postValue(s);
                } catch (Exception e) {
                    Log.e(TAG, "Error binding slice", e);
                    postValue(null);
                }
            }
        };

        final SliceViewManager.SliceCallback mSliceCallback =
                new SliceViewManager.SliceCallback() {
            @Override
            public void onSliceUpdated(@NonNull Slice s) {
                postValue(s);
            }
        };
    }

    private SliceLiveData() {
    }

    /**
     * Listener for errors when using {@link #fromStream(Context, InputStream, OnErrorListener)}.
     */
    public interface OnErrorListener {
        int ERROR_UNKNOWN = 0;
        int ERROR_STRUCTURE_CHANGED = 1;
        int ERROR_SLICE_NO_LONGER_PRESENT = 2;
        int ERROR_INVALID_INPUT = 3;

        @IntDef({ERROR_UNKNOWN, ERROR_STRUCTURE_CHANGED, ERROR_SLICE_NO_LONGER_PRESENT,
                ERROR_INVALID_INPUT})
        @interface ErrorType {

        }

        /**
         * Called when an error occurs converting a serialized slice into a live slice.
         */
        void onSliceError(@ErrorType int type, @Nullable Throwable source);
    }
}
