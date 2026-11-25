package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.ui.theme.MyApplicationTheme

private const val GEMINI_URL = "https://gemini.google.com"

internal const val HIDE_GEMINI_PROMOS_SCRIPT = """
(function() {
  var PLAY_STORE_URL = 'play.google.com/store/apps/details?id=com.google.android.apps.gemini';
  var PROTECTED_LABELS = ['sign in', 'settings', 'subscriptions', 'for business', 'about gemini'];

  function normalizeText(el) {
    return ((el && (el.innerText || el.textContent)) || '').replace(/\s+/g, ' ').trim().toLowerCase();
  }

  function isInsideChatOrPrompt(el) {
    if (!el || !el.closest) return false;
    return !!el.closest(
      'textarea, input, [contenteditable="true"], [role="textbox"], [role="log"], ' +
      'message-content, model-response, user-query, .conversation-container, ' +
      '.chat-history, [data-message-id]'
    );
  }

  function containsProtectedOrEditor(el) {
    if (!el) return true;
    if (el.querySelector && el.querySelector(
      'textarea, input, [contenteditable="true"], [role="textbox"], [role="log"], ' +
      'message-content, model-response, user-query, [data-message-id]'
    )) {
      return true;
    }
    var txt = normalizeText(el);
    for (var i = 0; i < PROTECTED_LABELS.length; i++) {
      if (txt.indexOf(PROTECTED_LABELS[i]) !== -1) {
        return true;
      }
    }
    return false;
  }

  function hideElement(el) {
    if (!el || !el.style) return;
    if (el.style.getPropertyValue('display') !== 'none' ||
        el.style.getPropertyPriority('display') !== 'important') {
      el.style.setProperty('display', 'none', 'important');
    }
  }

  function isReasonableSize(el, maxHeight, maxTextLen) {
    var txt = normalizeText(el);
    if (txt.length > maxTextLen) return false;
    var rect = el.getBoundingClientRect ? el.getBoundingClientRect() : null;
    if (rect && rect.height > maxHeight) return false;
    return true;
  }

  function findBannerContainer(startEl) {
    var current = startEl;
    var best = startEl;
    for (var depth = 0; depth < 6 && current && current.parentElement; depth++) {
      var parent = current.parentElement;
      var tag = parent.tagName ? parent.tagName.toUpperCase() : '';
      if (tag === 'BODY' || tag === 'HTML' || tag === 'MAIN' || tag === 'NAV' || tag === 'ASIDE') {
        break;
      }
      if (containsProtectedOrEditor(parent)) {
        break;
      }
      if (!isReasonableSize(parent, 180, 220)) {
        break;
      }
      best = parent;
      current = parent;
    }
    return best;
  }

  function findDrawerItemContainer(startEl) {
    var candidate = startEl.closest
      ? startEl.closest('li, [role="menuitem"], [role="listitem"], mat-list-item, a, button')
      : startEl;
    if (candidate && !containsProtectedOrEditor(candidate) && isReasonableSize(candidate, 120, 100)) {
      return candidate;
    }
    return !containsProtectedOrEditor(startEl) && isReasonableSize(startEl, 120, 100) ? startEl : null;
  }

  function hidePromos() {
    var root = document.body;
    if (!root) return;

    var playLinks = root.querySelectorAll('a[href], button[data-href], [role="button"][data-href]');
    for (var i = 0; i < playLinks.length; i++) {
      var link = playLinks[i];
      if (isInsideChatOrPrompt(link)) continue;
      var href = (link.getAttribute('href') || link.getAttribute('data-href') || '').toLowerCase();
      if (href.indexOf(PLAY_STORE_URL) !== -1) {
        var inDrawer = !!link.closest('nav, aside, [role="navigation"], [role="dialog"], mat-sidenav, bard-sidenav');
        var target = inDrawer ? findDrawerItemContainer(link) : findBannerContainer(link);
        if (target && !containsProtectedOrEditor(target)) {
          hideElement(target);
        } else if (!containsProtectedOrEditor(link)) {
          hideElement(link);
        }
      }
    }

    var candidates = root.querySelectorAll('a, button, [role="button"], [role="menuitem"], [role="listitem"], li, div, span, p');
    for (var j = 0; j < candidates.length; j++) {
      var el = candidates[j];
      if (el.style && el.style.getPropertyValue('display') === 'none') continue;
      if (isInsideChatOrPrompt(el)) continue;

      var txt = normalizeText(el);
      if (!txt || txt.length > 220) continue;

      if (txt === 'try app' ||
          txt.indexOf('chat with gemini in an app') !== -1 ||
          txt.indexOf('now available on google play') !== -1) {
        var banner = findBannerContainer(el);
        if (banner && !containsProtectedOrEditor(banner) && isReasonableSize(banner, 180, 220)) {
          hideElement(banner);
        }
        continue;
      }

      if (/^get\s+app(\s+new)?$/.test(txt)) {
        var drawerItem = findDrawerItemContainer(el);
        if (drawerItem) {
          hideElement(drawerItem);
        }
      }
    }
  }

  hidePromos();

  if (document.body && !window.__geminiPromoObserverInstalled) {
    window.__geminiPromoObserverInstalled = true;
    var scheduled = false;
    var observer = new MutationObserver(function() {
      if (scheduled) return;
      scheduled = true;
      var run = function() {
        scheduled = false;
        hidePromos();
      };
      if (window.requestAnimationFrame) {
        window.requestAnimationFrame(run);
      } else {
        setTimeout(run, 16);
      }
    });
    observer.observe(document.body, { childList: true, subtree: true });
  }
})();
"""

internal fun isGeminiHost(url: String?): Boolean {
  if (url.isNullOrBlank()) return false
  val host = Uri.parse(url).host?.lowercase() ?: return false
  return host == "gemini.google.com" || host.endsWith(".gemini.google.com")
}

internal fun sanitizeUserAgent(userAgent: String): String {
  return userAgent.replace("; wv", "").replace(";wv", "")
}

internal fun isInAppHost(host: String?): Boolean {
  if (host.isNullOrBlank()) return false
  val normalized = host.lowercase()
  return normalized == "gemini.google.com" ||
    normalized.endsWith(".gemini.google.com") ||
    normalized == "accounts.google.com" ||
    normalized.endsWith(".accounts.google.com")
}

internal fun isInAppUri(uri: Uri?): Boolean {
  if (uri == null) return false
  val scheme = uri.scheme?.lowercase()
  if (uri.toString() == "about:blank") return true
  if (scheme != "http" && scheme != "https") return false
  return isInAppHost(uri.host)
}

class MainActivity : ComponentActivity() {

  private var webView: WebView? = null
  private var filePathCallback: ValueCallback<Array<Uri>>? = null
  private var pendingPermissionRequest: PermissionRequest? = null
  private var hasMainFrameError by mutableStateOf(false)

  private val fileChooserLauncher =
    registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
      val callback = filePathCallback
      filePathCallback = null
      callback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
    }

  private val audioPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
      val request = pendingPermissionRequest
      pendingPermissionRequest = null
      if (request != null) {
        if (isGranted) {
          request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
        } else {
          request.deny()
        }
      }
    }

  private val backCallback =
    object : OnBackPressedCallback(false) {
      override fun handleOnBackPressed() {
        val currentWebView = webView
        if (currentWebView != null && currentWebView.canGoBack()) {
          currentWebView.goBack()
        } else {
          isEnabled = false
          onBackPressedDispatcher.onBackPressed()
        }
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    onBackPressedDispatcher.addCallback(this, backCallback)

    val configuredWebView = createConfiguredWebView(savedInstanceState)
    webView = configuredWebView

    setContent {
      MyApplicationTheme {
        val isDark = isSystemInDarkTheme()
        SideEffect {
          val controller = WindowCompat.getInsetsController(window, window.decorView)
          controller.isAppearanceLightStatusBars = !isDark
          controller.isAppearanceLightNavigationBars = !isDark
        }

        GeminiWebScreen(
          webView = configuredWebView,
          hasError = hasMainFrameError,
          onRetry = {
            hasMainFrameError = false
            if (configuredWebView.url.isNullOrBlank()) {
              configuredWebView.loadUrl(GEMINI_URL)
            } else {
              configuredWebView.reload()
            }
          },
        )
      }
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  private fun createConfiguredWebView(savedInstanceState: Bundle?): WebView {
    return WebView(this).apply {
      layoutParams =
        ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        )
      setBackgroundColor(AndroidColor.TRANSPARENT)

      settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        cacheMode = WebSettings.LOAD_DEFAULT
        allowFileAccess = true
        allowContentAccess = true
        mediaPlaybackRequiresUserGesture = false
        loadsImagesAutomatically = true
        useWideViewPort = true
        loadWithOverviewMode = true
        setSupportMultipleWindows(false)
        userAgentString = sanitizeUserAgent(userAgentString)
      }

      val targetWebView = this
      CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(targetWebView, true)
      }

      webViewClient =
        object : WebViewClient() {
          override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
          ): Boolean {
            val uri = request?.url ?: return false
            if (isInAppUri(uri)) {
              return false
            }
            openInExternalBrowser(uri)
            return true
          }

          override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
            backCallback.isEnabled = view?.canGoBack() == true
          }

          override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            hasMainFrameError = false
          }

          override fun onPageCommitVisible(view: WebView?, url: String?) {
            super.onPageCommitVisible(view, url)
            if (isGeminiHost(url)) {
              view?.evaluateJavascript(HIDE_GEMINI_PROMOS_SCRIPT, null)
            }
          }

          override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            backCallback.isEnabled = view?.canGoBack() == true
            if (isGeminiHost(url)) {
              view?.evaluateJavascript(HIDE_GEMINI_PROMOS_SCRIPT, null)
            }
            CookieManager.getInstance().flush()
          }

          override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?,
          ) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
              hasMainFrameError = true
            }
          }
        }

      webChromeClient =
        object : WebChromeClient() {
          override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?,
          ): Boolean {
            this@MainActivity.filePathCallback?.onReceiveValue(null)
            this@MainActivity.filePathCallback = filePathCallback

            val acceptedTypes =
              fileChooserParams?.acceptTypes
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() && it.contains("/") }
                .orEmpty()
            val mimeType = if (acceptedTypes.size == 1) acceptedTypes.first() else "*/*"

            return try {
              fileChooserLauncher.launch(mimeType)
              true
            } catch (_: ActivityNotFoundException) {
              this@MainActivity.filePathCallback?.onReceiveValue(null)
              this@MainActivity.filePathCallback = null
              false
            }
          }

          override fun onPermissionRequest(request: PermissionRequest?) {
            if (request == null) return
            val originHost = request.origin?.host
            if (originHost != null && !isInAppHost(originHost)) {
              request.deny()
              return
            }

            if (!request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
              request.deny()
              return
            }

            runOnUiThread {
              val isGranted =
                ContextCompat.checkSelfPermission(
                  this@MainActivity,
                  Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
              if (isGranted) {
                request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
              } else {
                pendingPermissionRequest?.deny()
                pendingPermissionRequest = request
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
              }
            }
          }

          override fun onPermissionRequestCanceled(request: PermissionRequest?) {
            if (pendingPermissionRequest == request) {
              pendingPermissionRequest = null
            }
          }
        }

      if (savedInstanceState != null) {
        restoreState(savedInstanceState)
      } else {
        loadUrl(GEMINI_URL)
      }
    }
  }

  private fun openInExternalBrowser(uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
      addCategory(Intent.CATEGORY_BROWSABLE)
    }
    try {
      startActivity(intent)
    } catch (_: ActivityNotFoundException) {
      // No browser or handler installed for this URI scheme.
    }
  }

  override fun onResume() {
    super.onResume()
    webView?.onResume()
  }

  override fun onPause() {
    webView?.onPause()
    CookieManager.getInstance().flush()
    super.onPause()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    webView?.saveState(outState)
  }

  override fun onDestroy() {
    filePathCallback?.onReceiveValue(null)
    filePathCallback = null
    pendingPermissionRequest?.deny()
    pendingPermissionRequest = null
    webView?.apply {
      stopLoading()
      removeAllViews()
      destroy()
    }
    webView = null
    super.onDestroy()
  }
}

@Composable
private fun GeminiWebScreen(
  webView: WebView,
  hasError: Boolean,
  onRetry: () -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    AndroidView(
      factory = { webView },
      modifier = Modifier.fillMaxSize().testTag("gemini_webview"),
    )

    if (hasError) {
      Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
      ) {
        Column(
          modifier = Modifier.fillMaxSize().padding(24.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = stringResource(R.string.offline_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = stringResource(R.string.offline_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(20.dp))
          Button(
            onClick = onRetry,
            modifier = Modifier.testTag("retry_button"),
          ) {
            Text(text = stringResource(R.string.action_retry))
          }
        }
      }
    }
  }
}
