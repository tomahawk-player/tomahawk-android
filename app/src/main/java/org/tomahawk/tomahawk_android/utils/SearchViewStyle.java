/*
 * Copyright (C) 2015 Jared Rummler <jared.rummler@gmail.com>
 * Ported to support lib by Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.utils;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import java.lang.reflect.Field;

/**
 * Helper class to style a {@link SearchView}.</p>
 *
 * Example usage:</p>
 *
 * <pre>
 * <code>
 * SearchViewStyle.on(searchView)
 *    .setCursorColor(Color.WHITE)
 *    .setTextColor(Color.WHITE)
 *    .setHintTextColor(Color.WHITE)
 *    .setSearchHintDrawable(R.drawable.ic_search_api_material)
 *    .setSearchButtonImageResource(R.drawable.ic_search_api_material)
 *    .setCloseBtnImageResource(R.drawable.ic_clear_material)
 *    .setVoiceBtnImageResource(R.drawable.ic_voice_search_api_material)
 *    .setGoBtnImageResource(R.drawable.ic_go_search_api_material)
 *    .setCommitIcon(R.drawable.ic_commit_search_api_material)
 *    .setSubmitAreaDrawableId(R.drawable.abc_textfield_search_activated_mtrl_alpha)
 *    .setSearchPlateDrawableId(R.drawable.abc_textfield_search_activated_mtrl_alpha)
 *    .setSearchPlateTint(Color.WHITE)
 *    .setSubmitAreaTint(Color.WHITE);
 * </pre>
 *
 * </code>
 *
 * @author Jared Rummler <jared.rummler@gmail.com>
 * @since Oct 24, 2014
 */
public class SearchViewStyle {

    // ===========================================================
    // STATIC METHODS
    // ===========================================================
    public static SearchViewStyle on(final SearchView searchView) {
        return new SearchViewStyle(searchView);
    }

    // ===========================================================
    // FIELDS
    // ===========================================================
    private final SearchView mSearchView;

    // ===========================================================
    // CONSTRUCTORS
    // ===========================================================
    private SearchViewStyle(final SearchView searchView) {
        mSearchView = searchView;
    }

    // ===========================================================
    // METHODS
    // ===========================================================

    @SuppressWarnings("unchecked")
    public <T extends View> T getView(final int id) {
        if (id == 0) {
            return null;
        }
        View view = mSearchView.findViewById(id);
        return (T) view;
    }

    public SearchViewStyle setSearchPlateDrawableId(final int id) {
        final View view = getView(android.support.v7.appcompat.R.id.search_plate);
        if (view != null) {
            view.setBackgroundResource(id);
        }
        return this;
    }

    public SearchViewStyle setCursorColor(final int color) {
        final AutoCompleteTextView editText = getView(
                android.support.v7.appcompat.R.id.search_src_text);
        if (editText != null) {
            try {
                final Field fCursorDrawableRes = TextView.class
                        .getDeclaredField("mCursorDrawableRes");
                fCursorDrawableRes.setAccessible(true);
                final int mCursorDrawableRes = fCursorDrawableRes.getInt(editText);
                final Field fEditor = TextView.class.getDeclaredField("mEditor");
                fEditor.setAccessible(true);
                final Object editor = fEditor.get(editText);
                final Class<?> clazz = editor.getClass();
                final Field fCursorDrawable = clazz.getDeclaredField("mCursorDrawable");
                fCursorDrawable.setAccessible(true);
                final Drawable[] drawables = new Drawable[2];
                drawables[0] = editText.getContext().getResources().getDrawable(mCursorDrawableRes);
                drawables[1] = editText.getContext().getResources().getDrawable(mCursorDrawableRes);
                drawables[0].setColorFilter(color, PorterDuff.Mode.SRC_IN);
                drawables[1].setColorFilter(color, PorterDuff.Mode.SRC_IN);
                fCursorDrawable.set(editor, drawables);
            } catch (final Throwable ignored) {
            }
        }
        return this;
    }

}