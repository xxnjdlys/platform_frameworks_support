/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.emoji.widget;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import android.text.InputFilter;
import android.text.method.TransformationMethod;
import android.widget.TextView;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(maxSdkVersion = 18)
public class EmojiTextViewHelperPre19Test {
    EmojiTextViewHelper mTextViewHelper;
    TextView mTextView;

    @Before
    public void setup() {
        mTextView = new TextView(InstrumentationRegistry.getTargetContext());
        mTextViewHelper = new EmojiTextViewHelper(mTextView);
    }

    @Test
    public void testUpdateTransformationMethod_doesNotUpdateTransformationMethod() {
        final TransformationMethod tm = mock(TransformationMethod.class);
        mTextView.setTransformationMethod(tm);

        mTextViewHelper.updateTransformationMethod();

        assertSame(tm, mTextView.getTransformationMethod());
    }

    @Test
    public void testGetFilters_returnsSameFilters() {
        final InputFilter existingFilter = mock(InputFilter.class);
        final InputFilter[] filters = new InputFilter[]{existingFilter};

        final InputFilter[] newFilters = mTextViewHelper.getFilters(filters);

        assertSame(filters, newFilters);
    }

    @Test
    public void testGetTransformationMethod_returnSameTransformationMethod() {
        assertNull(mTextViewHelper.wrapTransformationMethod(null));

        final TransformationMethod tm = mock(TransformationMethod.class);
        assertSame(tm, mTextViewHelper.wrapTransformationMethod(tm));
    }

    @Test
    public void testSetAllCaps_doesNotUpdateTransformationMethod() {
        final TransformationMethod tm = mock(TransformationMethod.class);
        mTextView.setTransformationMethod(tm);
        mTextViewHelper.setAllCaps(true);
        assertSame(tm, mTextView.getTransformationMethod());

        mTextViewHelper.setAllCaps(false);
        assertSame(tm, mTextView.getTransformationMethod());
    }
}
