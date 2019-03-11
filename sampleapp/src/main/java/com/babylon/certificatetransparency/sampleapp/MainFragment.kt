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

package com.babylon.certificatetransparency.sampleapp

import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.babylon.certificatetransparency.sampleapp.item.BabylonLogoItem
import com.babylon.certificatetransparency.sampleapp.item.ExampleCardItem
import com.babylon.certificatetransparency.sampleapp.item.text.HeaderTextItem
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    @Suppress("LongMethod")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        @Suppress("MagicNumber")
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics)
        recyclerView.addItemDecoration(SpaceItemDecoration(px.toInt()))

        recyclerView.adapter = GroupAdapter<ViewHolder>().apply {
            val navController = findNavController()

            add(HeaderTextItem(R.string.certificate_transparency, iconResId = R.drawable.ic_launcher_foreground))

            add(
                ExampleCardItem(
                    navController,
                    "OkHttp",
                    Uri.parse("https://square.github.io/okhttp/"),
                    R.id.okhttp_kotlin_example_fragment,
                    R.id.okhttp_java_example_fragment
                )
            )
            add(
                ExampleCardItem(
                    navController,
                    "HttpURLConnection",
                    Uri.parse("https://developer.android.com/reference/java/net/HttpURLConnection"),
                    R.id.httpurlconnection_kotlin_example_fragment,
                    R.id.httpurlconnection_java_example_fragment
                )
            )
            add(
                ExampleCardItem(
                    navController,
                    "Volley",
                    Uri.parse("https://developer.android.com/training/volley/index.html"),
                    R.id.volley_kotlin_example_fragment,
                    R.id.volley_java_example_fragment
                )
            )

            add(BabylonLogoItem)
        }
    }
}
