package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Gemini Web", appName)
  }

  @Test
  fun `sanitizeUserAgent strips wv token`() {
    val raw =
      "Mozilla/5.0 (Linux; Android 15; Pixel 9 Build/AP3A; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/131.0.0.0 Mobile Safari/537.36"
    val sanitized = sanitizeUserAgent(raw)
    assertFalse(sanitized.contains("; wv"))
    assertFalse(sanitized.contains(";wv"))
    assertTrue(sanitized.contains("Chrome/131.0.0.0 Mobile Safari/537.36"))
  }

  @Test
  fun `isInAppUri allows gemini and accounts google domains only`() {
    assertTrue(isInAppUri(Uri.parse("https://gemini.google.com/app")))
    assertTrue(isInAppUri(Uri.parse("https://accounts.google.com/ServiceLogin")))
    assertFalse(isInAppUri(Uri.parse("https://example.com/docs")))
    assertFalse(isInAppUri(Uri.parse("https://evil-gemini.google.com.attacker.org")))
    assertFalse(isInAppUri(Uri.parse("mailto:user@example.com")))
  }
}
