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

package com.babylon.certificatetransparency.sampleapp.item

import com.babylon.certificatetransparency.sampleapp.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.checkbox_item.view.*

typealias CheckboxCallback = ((Boolean) -> Unit)

class CheckboxItem(private val title: String, private val isChecked: Boolean, private val callback: CheckboxCallback? = null) : Item() {

    override fun getLayout() = R.layout.checkbox_item

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.containerView.checkbox.text = title
        viewHolder.containerView.checkbox.isChecked = isChecked

        viewHolder.containerView.checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            callback?.invoke(isChecked)
        }
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return (other is CheckboxItem && title == other.title && isChecked == other.isChecked)
    }
}
