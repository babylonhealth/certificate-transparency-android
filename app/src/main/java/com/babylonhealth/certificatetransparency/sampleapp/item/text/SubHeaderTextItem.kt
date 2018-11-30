/*
 * Copyright 2018 Babylon Healthcare Services Limited
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

package com.babylonhealth.certificatetransparency.sampleapp.item.text

import androidx.annotation.StringRes
import com.babylonhealth.certificatetransparency.sampleapp.R
import com.babylonhealth.certificatetransparency.sampleapp.item.getString
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.header_text_item.view.*

class SubHeaderTextItem(
    @StringRes private val titleResId: Int? = null,
    private val title: String? = null
) : Item() {

    init {
        if ((titleResId != null && title != null) || (titleResId == null && title == null))
            throw IllegalStateException("Provide either titleResId or title")
    }

    override fun getLayout() = R.layout.sub_header_text_item

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.containerView.title.text = if (titleResId != null) viewHolder.getString(titleResId) else title
    }
}
