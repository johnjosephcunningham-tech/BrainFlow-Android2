package ca.nymma.kiosk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.Toast

class MainActivity : Activity() {
 private lateinit var web: WebView
 private val home="https://book.nymma.ca/kiosk"
 private val handler=Handler(Looper.getMainLooper())
 private val idleMs=5*60*1000L
 private val reset=Runnable { web.loadUrl(home) }

 @SuppressLint("SetJavaScriptEnabled")
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState); window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); hideSystemUi(); enableDedicatedModeIfOwner()
  web=WebView(this); setContentView(web)
  CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(web,true)
  web.settings.apply { javaScriptEnabled=true; domStorageEnabled=true; databaseEnabled=true; allowFileAccess=false; allowContentAccess=true; mediaPlaybackRequiresUserGesture=false; userAgentString += " SilverbackKiosk/1.1" }
  web.webChromeClient=object:WebChromeClient(){ override fun onPermissionRequest(request:PermissionRequest)=runOnUiThread{request.grant(request.resources)}}
  web.webViewClient=object:WebViewClient(){
   override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean { val host=request.url.host?.lowercase()?:return true; val allowed=host=="book.nymma.ca"||host.endsWith(".nymma.ca"); if(!allowed) Toast.makeText(this@MainActivity,"This tablet is locked to Silverback.",Toast.LENGTH_SHORT).show(); return !allowed }
   override fun onReceivedSslError(view:WebView?,h:SslErrorHandler?,error:android.net.http.SslError?){h?.cancel()}
  }
  web.setDownloadListener{_,_,_,_,_->Toast.makeText(this,"Downloads are disabled in kiosk mode.",Toast.LENGTH_SHORT).show()}
  if(savedInstanceState==null) web.loadUrl(home) else web.restoreState(savedInstanceState); touchReset()
 }
 private fun enableDedicatedModeIfOwner(){ val dpm=getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager; val admin=ComponentName(this,KioskDeviceAdminReceiver::class.java); if(dpm.isDeviceOwnerApp(packageName)){ dpm.setLockTaskPackages(admin,arrayOf(packageName)); try{startLockTask()}catch(_:Exception){} } }
 private fun touchReset(){handler.removeCallbacks(reset);handler.postDelayed(reset,idleMs)}
 override fun dispatchTouchEvent(ev:android.view.MotionEvent?):Boolean{touchReset();return super.dispatchTouchEvent(ev)}
 override fun onUserInteraction(){super.onUserInteraction();touchReset();hideSystemUi()}
 private fun hideSystemUi(){window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE}
 override fun onWindowFocusChanged(hasFocus:Boolean){super.onWindowFocusChanged(hasFocus);if(hasFocus)hideSystemUi()}
 @Deprecated("Deprecated in Java") override fun onBackPressed(){if(web.canGoBack())web.goBack()else web.loadUrl(home)}
 override fun onSaveInstanceState(outState:Bundle){web.saveState(outState);super.onSaveInstanceState(outState)}
 override fun onDestroy(){handler.removeCallbacks(reset);web.destroy();super.onDestroy()}
}
