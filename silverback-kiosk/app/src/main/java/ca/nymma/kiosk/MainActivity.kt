package ca.nymma.kiosk
import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast

class MainActivity:Activity(){
 private lateinit var root:FrameLayout; private lateinit var web:WebView; private lateinit var idle:ImageView; private lateinit var black:View
 private val home="https://book.nymma.ca/kiosk"; private val h=Handler(Looper.getMainLooper())
 private val showIdle=Runnable{hideKeyboard();idle.visibility=View.VISIBLE;black.visibility=View.GONE}
 private val showBlack=Runnable{hideKeyboard();idle.visibility=View.GONE;black.visibility=View.VISIBLE}
 @SuppressLint("SetJavaScriptEnabled") override fun onCreate(s:Bundle?){super.onCreate(s);window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);hideUi();dedicated()
  root=FrameLayout(this);web=WebView(this);root.addView(web,FrameLayout.LayoutParams(-1,-1))
  idle=ImageView(this).apply{setImageResource(R.drawable.kiosk_bg);setBackgroundColor(Color.BLACK);adjustViewBounds=false;scaleType=if(resources.configuration.orientation==2) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER;visibility=View.GONE;setOnClickListener{wake()}}
  black=View(this).apply{setBackgroundColor(Color.BLACK);visibility=View.GONE;setOnClickListener{wake()}}
  root.addView(idle,FrameLayout.LayoutParams(-1,-1));root.addView(black,FrameLayout.LayoutParams(-1,-1));setContentView(root)
  CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,true)
  web.settings.apply{javaScriptEnabled=true;domStorageEnabled=true;databaseEnabled=true;allowFileAccess=false;allowContentAccess=true;mediaPlaybackRequiresUserGesture=false;userAgentString+=" SilverbackKiosk/1.5"}
  web.webChromeClient=object:WebChromeClient(){override fun onPermissionRequest(r:PermissionRequest)=runOnUiThread{r.grant(r.resources)}}
  web.webViewClient=object:WebViewClient(){override fun shouldOverrideUrlLoading(v:WebView,r:WebResourceRequest):Boolean{val s=r.url.scheme?.lowercase();if(s=="https"||s=="http")return false;Toast.makeText(this@MainActivity,"This tablet is locked to Silverback.",Toast.LENGTH_SHORT).show();return true};override fun onReceivedSslError(v:WebView?,x:SslErrorHandler?,e:android.net.http.SslError?){x?.cancel()}}
  if(s==null)web.loadUrl(home)else web.restoreState(s);reset()
 }
 private fun hideKeyboard(){currentFocus?.clearFocus();(getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(web.windowToken,0)}
 private fun wake(){idle.visibility=View.GONE;black.visibility=View.GONE;web.loadUrl(home);reset()}
 private fun reset(){h.removeCallbacks(showIdle);h.removeCallbacks(showBlack);h.postDelayed(showIdle,120000L);h.postDelayed(showBlack,1800000L)}
 override fun dispatchTouchEvent(e:MotionEvent?):Boolean{if(e?.action==MotionEvent.ACTION_DOWN&&(idle.visibility==View.VISIBLE||black.visibility==View.VISIBLE)){wake();return true};reset();return super.dispatchTouchEvent(e)}
 private fun dedicated(){val d=getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager;val a=ComponentName(this,KioskDeviceAdminReceiver::class.java);if(d.isDeviceOwnerApp(packageName)){d.setLockTaskPackages(a,arrayOf(packageName));try{startLockTask()}catch(_:Exception){}}}
 override fun onUserInteraction(){super.onUserInteraction();reset();hideUi()}
 private fun hideUi(){window.decorView.systemUiVisibility=5894}
 override fun onWindowFocusChanged(f:Boolean){super.onWindowFocusChanged(f);if(f)hideUi()}
 @Deprecated("Deprecated in Java") override fun onBackPressed(){if(web.canGoBack())web.goBack()else web.loadUrl(home)}
 override fun onSaveInstanceState(o:Bundle){web.saveState(o);super.onSaveInstanceState(o)}
 override fun onDestroy(){h.removeCallbacks(showIdle);h.removeCallbacks(showBlack);web.destroy();super.onDestroy()}
}