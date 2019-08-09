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

package com.babylon.certificatetransparency.sampleapp.item

import com.babylon.certificatetransparency.sampleapp.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder

object BabylonLogoItem : Item() {

    override fun getLayout() = R.layout.babylon_item

    override fun bind(viewHolder: ViewHolder, position: Int) = Unit
}
