package ca.nymma.kiosk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
 private lateinit var root: FrameLayout
 private lateinit var web: WebView
 private lateinit var idleScreen: View
 private lateinit var blackScreen: View
 private val home = "https://book.nymma.ca/kiosk"
 private val handler = Handler(Looper.getMainLooper())
 private val idleDelay = 2 * 60 * 1000L
 private val blackDelay = 30 * 60 * 1000L
 private val showIdle = Runnable { hideKeyboard(); web.clearFocus(); idleScreen.visibility = View.VISIBLE; blackScreen.visibility = View.GONE }
 private val showBlack = Runnable { hideKeyboard(); web.clearFocus(); idleScreen.visibility = View.GONE; blackScreen.visibility = View.VISIBLE }

 @SuppressLint("SetJavaScriptEnabled")
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  hideSystemUi()
  enableDedicatedModeIfOwner()

  root = FrameLayout(this)
  web = WebView(this)
  root.addView(web, FrameLayout.LayoutParams(-1, -1))
  idleScreen = makeIdleScreen()
  blackScreen = View(this).apply { setBackgroundColor(Color.BLACK); visibility = View.GONE }
  root.addView(idleScreen, FrameLayout.LayoutParams(-1, -1))
  root.addView(blackScreen, FrameLayout.LayoutParams(-1, -1))
  setContentView(root)

  CookieManager.getInstance().setAcceptCookie(true)
  CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
  web.settings.apply {
   javaScriptEnabled = true; domStorageEnabled = true; databaseEnabled = true
   allowFileAccess = false; allowContentAccess = true; mediaPlaybackRequiresUserGesture = false
   userAgentString += " SilverbackKiosk/1.3"
  }
  web.webChromeClient = object : WebChromeClient() {
   override fun onPermissionRequest(request: PermissionRequest) = runOnUiThread { request.grant(request.resources) }
  }
  web.webViewClient = object : WebViewClient() {
   override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
    val host = request.url.host?.lowercase() ?: return true
    val allowed = host == "book.nymma.ca" || host.endsWith(".nymma.ca")
    if (!allowed) Toast.makeText(this@MainActivity, "This tablet is locked to Silverback.", Toast.LENGTH_SHORT).show()
    return !allowed
   }
   override fun onReceivedSslError(view: WebView?, h: SslErrorHandler?, error: android.net.http.SslError?) { h?.cancel() }
  }
  web.setDownloadListener { _,_,_,_,_ -> Toast.makeText(this, "Downloads are disabled in kiosk mode.", Toast.LENGTH_SHORT).show() }
  if (savedInstanceState == null) web.loadUrl(home) else web.restoreState(savedInstanceState)
  resetIdleTimers()
 }

 private fun makeIdleScreen(): View {
  return FrameLayout(this).apply {
   setBackgroundColor(Color.BLACK)
   visibility = View.GONE
   val icon = ImageView(this@MainActivity).apply {
    setImageResource(ca.nymma.kiosk.R.drawable.ic_launcher)
    scaleType = ImageView.ScaleType.CENTER_INSIDE
   }
   addView(icon, FrameLayout.LayoutParams(360, 360, Gravity.CENTER))
   val title = TextView(this@MainActivity).apply {
    text = "SILVERBACK\nNORTH YORK MMA"
    setTextColor(Color.WHITE); textSize = 34f; gravity = Gravity.CENTER
   }
   addView(title, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER).apply { topMargin = 420 })
  }
 }

 private fun hideKeyboard() {
  currentFocus?.clearFocus()
  val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  imm.hideSoftInputFromWindow(web.windowToken, 0)
 }

 private fun wakeFromIdle() {
  if (idleScreen.visibility == View.VISIBLE || blackScreen.visibility == View.VISIBLE) {
   idleScreen.visibility = View.GONE; blackScreen.visibility = View.GONE
   web.loadUrl(home)
  }
  resetIdleTimers()
 }

 private fun resetIdleTimers() {
  handler.removeCallbacks(showIdle); handler.removeCallbacks(showBlack)
  handler.postDelayed(showIdle, idleDelay)
  handler.postDelayed(showBlack, blackDelay)
 }

 override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
  if (ev?.action == MotionEvent.ACTION_DOWN && (idleScreen.visibility == View.VISIBLE || blackScreen.visibility == View.VISIBLE)) {
   wakeFromIdle(); return true
  }
  resetIdleTimers()
  return super.dispatchTouchEvent(ev)
 }

 private fun enableDedicatedModeIfOwner() {
  val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
  val admin = ComponentName(this, KioskDeviceAdminReceiver::class.java)
  if (dpm.isDeviceOwnerApp(packageName)) {
   dpm.setLockTaskPackages(admin, arrayOf(packageName))
   try { startLockTask() } catch (_: Exception) {}
  }
 }

 override fun onUserInteraction() { super.onUserInteraction(); resetIdleTimers(); hideSystemUi() }
 private fun hideSystemUi() { window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE }
 override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (hasFocus) hideSystemUi() }
 @Deprecated("Deprecated in Java") override fun onBackPressed() { if (web.canGoBack()) web.goBack() else web.loadUrl(home) }
 override fun onSaveInstanceState(outState: Bundle) { web.saveState(outState); super.onSaveInstanceState(outState) }
 override fun onDestroy() { handler.removeCallbacks(showIdle); handler.removeCallbacks(showBlack); web.destroy(); super.onDestroy() }
}
