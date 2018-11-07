package org.certificatetransparency.ctlog.utils

import org.junit.Assert

inline fun <reified T> assertIsA(result: Any) {
    Assert.assertTrue(result is T)
}

inline fun <reified T> assertIsA(message: String, result: Any) {
    Assert.assertTrue(message, result is T)
}
