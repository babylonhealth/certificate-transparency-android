package com.babylon.certificatetransparency.internal.loglist.parser

import com.babylon.certificatetransparency.loglist.*

internal interface LogListJsonParser {
    fun parseJson(logListJson: String): LogListResult
}