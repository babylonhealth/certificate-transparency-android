/*
 * Copyright 2019 Babylon Partners Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.babylon.certificatetransparency.sampleapp.item.text

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.babylon.certificatetransparency.sampleapp.R
import com.babylon.certificatetransparency.sampleapp.item.getString
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.header_text_item.view.*

class HeaderTextItem(
    @StringRes private val titleResId: Int? = null,
    private val title: String? = null,
    @DrawableRes private val iconResId: Int? = null
) : Item() {

    init {
        if (!((titleResId != null) xor (title != null))) {
            throw IllegalStateException("Provide either titleResId or title")
        }
    }

    override fun getLayout() = R.layout.header_text_item

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.containerView.title.text = if (titleResId != null) viewHolder.getString(titleResId) else title

        if (iconResId != null) {
            val drawable = ContextCompat.getDrawable(viewHolder.containerView.context, iconResId)
            viewHolder.containerView.title.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
        } else {
            viewHolder.containerView.title.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
        }
    }
}
