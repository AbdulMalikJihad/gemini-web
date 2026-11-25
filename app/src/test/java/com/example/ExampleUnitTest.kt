package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun sanitizeUserAgent_stripsWebViewToken() {
    val raw =
      "Mozilla/5.0 (Linux; Android 15; Pixel 9 Build/AP3A; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/131.0.0.0 Mobile Safari/537.36"
    val sanitized = sanitizeUserAgent(raw)
    assertFalse(sanitized.contains("; wv"))
    assertFalse(sanitized.contains(";wv"))
    assertTrue(sanitized.contains("Chrome/131.0.0.0 Mobile Safari/537.36"))
  }

  @Test
  fun isInAppHost_allowsOnlyGeminiAndAccountsGoogle() {
    assertTrue(isInAppHost("gemini.google.com"))
    assertTrue(isInAppHost("accounts.google.com"))
    assertFalse(isInAppHost("example.com"))
    assertFalse(isInAppHost("evil-gemini.google.com.attacker.org"))
    assertFalse(isInAppHost(null))
  }

  @Test
  fun hidePromosScript_targetsRequiredElementsAndSafetyGuards() {
    assertTrue(HIDE_GEMINI_PROMOS_SCRIPT.contains("MutationObserver"))
    assertTrue(HIDE_GEMINI_PROMOS_SCRIPT.contains("document.body"))
    assertTrue(
      HIDE_GEMINI_PROMOS_SCRIPT.contains(
        "play.google.com/store/apps/details?id=com.google.android.apps.gemini"
      )
    )
    assertTrue(HIDE_GEMINI_PROMOS_SCRIPT.contains("chat with gemini in an app"))
    assertTrue(HIDE_GEMINI_PROMOS_SCRIPT.contains("now available on google play"))
    assertTrue(HIDE_GEMINI_PROMOS_SCRIPT.contains("try app"))
    assertTrue(HIDE_GEMINI_PROMOS_SCRIPT.contains("sign in"))
    assertTrue(HIDE_GEMINI_PROMOS_SCRIPT.contains("settings"))
    assertTrue(HIDE_GEMINI_PROMOS_SCRIPT.contains("subscriptions"))
    assertTrue(HIDE_GEMINI_PROMOS_SCRIPT.contains("for business"))
    assertTrue(HIDE_GEMINI_PROMOS_SCRIPT.contains("about gemini"))
  }
}
