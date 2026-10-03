package ca.nymma.kiosk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.*
import android.graphics.Color
import android.graphics.Rect
import android.graphics.BitmapFactory
import java.io.File
import android.os.*
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*

class MainActivity : Activity() {
 private lateinit var root: FrameLayout
 private lateinit var web: WebView
 private lateinit var idle: FrameLayout
 private lateinit var black: View
 private val home="https://book.nymma.ca/kiosk"
 private val h=Handler(Looper.getMainLooper())
 private val showIdle=Runnable{hideKeyboard();idle.visibility=View.VISIBLE;black.visibility=View.GONE}
 private val showBlack=Runnable{hideKeyboard();idle.visibility=View.GONE;black.visibility=View.VISIBLE}
 private val screenReceiver=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){if(i?.action==Intent.ACTION_SCREEN_ON)runOnUiThread{returnToFrontPage()}}}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

 @SuppressLint("SetJavaScriptEnabled")
 override fun onCreate(s:Bundle?){
  super.onCreate(s)
  window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
  hideUi(); dedicated()
  root=FrameLayout(this); web=WebView(this)
  root.addView(web,FrameLayout.LayoutParams(-1,-1))
  idle=buildIdleScreen()
  black=View(this).apply{setBackgroundColor(Color.BLACK);visibility=View.GONE;setOnClickListener{wake()}}
  root.addView(idle,FrameLayout.LayoutParams(-1,-1));root.addView(black,FrameLayout.LayoutParams(-1,-1));setContentView(root)
  CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,true)
  web.settings.apply{javaScriptEnabled=true;domStorageEnabled=true;databaseEnabled=true;allowFileAccess=false;allowContentAccess=true;mediaPlaybackRequiresUserGesture=false;userAgentString+=" SilverbackKiosk/1.14"}
  web.webChromeClient=object:WebChromeClient(){override fun onPermissionRequest(r:PermissionRequest)=runOnUiThread{r.grant(r.resources)}}
  web.webViewClient=object:WebViewClient(){
   override fun shouldOverrideUrlLoading(v:WebView,r:WebResourceRequest):Boolean{val x=r.url.scheme?.lowercase();if(x=="https"||x=="http")return false;Toast.makeText(this@MainActivity,"This tablet is locked to Silverback.",Toast.LENGTH_SHORT).show();return true}
   override fun onReceivedSslError(v:WebView?,x:SslErrorHandler?,e:android.net.http.SslError?){x?.cancel()}
  }
  if(s==null)web.loadUrl(home)else web.restoreState(s)
  val f=IntentFilter(Intent.ACTION_SCREEN_ON);if(Build.VERSION.SDK_INT>=33)registerReceiver(screenReceiver,f,Context.RECEIVER_NOT_EXPORTED)else registerReceiver(screenReceiver,f)
  installKeyboardResizeFix();reset()
 }

 private fun buildIdleScreen():FrameLayout{
  val frame=FrameLayout(this).apply{setBackgroundColor(Color.BLACK);visibility=View.GONE;setOnClickListener{wake()}}
  val poster=ImageView(this).apply{
   val full=File(getExternalFilesDir(null),"gym_rules_full.jpg")
   if(full.exists())setImageBitmap(BitmapFactory.decodeFile(full.absolutePath)) else setImageResource(R.drawable.gym_rules_poster)
   scaleType=ImageView.ScaleType.FIT_CENTER;setBackgroundColor(Color.BLACK)
  }
  frame.addView(poster,FrameLayout.LayoutParams(-1,-1).apply{bottomMargin=dp(68)})
  val bar=TextView(this).apply{text="MEMBER & VISITOR CHECK-IN  →";setTextColor(Color.WHITE);textSize=20f;gravity=Gravity.CENTER;typeface=android.graphics.Typeface.DEFAULT_BOLD;setBackgroundColor(Color.rgb(215,20,24));setOnClickListener{wake()}}
  frame.addView(bar,FrameLayout.LayoutParams(-1,dp(68),Gravity.BOTTOM));return frame
 }

 private fun installKeyboardResizeFix(){
  val decor=window.decorView
  decor.viewTreeObserver.addOnGlobalLayoutListener{
   if(!::web.isInitialized)return@addOnGlobalLayoutListener
   val r=Rect();decor.getWindowVisibleDisplayFrame(r);val total=decor.rootView.height;val open=total-r.bottom>total*.15
   val lp=web.layoutParams as FrameLayout.LayoutParams;val wanted=if(open)r.height()else FrameLayout.LayoutParams.MATCH_PARENT
   if(lp.height!=wanted){lp.height=wanted;web.layoutParams=lp;if(open)web.postDelayed({web.evaluateJavascript("(function(){var e=document.activeElement;if(e&&e.scrollIntoView)e.scrollIntoView({block:'center',behavior:'smooth'});})();",null)},180)}
  }
 }
 private fun hideKeyboard(){currentFocus?.clearFocus();(getSystemService(Context.INPUT_METHOD_SERVICE)as InputMethodManager).hideSoftInputFromWindow(web.windowToken,0)}
 private fun returnToFrontPage(){hideKeyboard();idle.visibility=View.GONE;black.visibility=View.GONE;web.clearHistory();web.loadUrl(home);reset();hideUi()}
 private fun wake(){returnToFrontPage()}
 private fun reset(){h.removeCallbacks(showIdle);h.removeCallbacks(showBlack);h.postDelayed(showIdle,60000);h.postDelayed(showBlack,1800000)}
 override fun dispatchTouchEvent(e:MotionEvent?):Boolean{if(e?.action==MotionEvent.ACTION_DOWN&&(idle.visibility==View.VISIBLE||black.visibility==View.VISIBLE)){wake();return true};reset();return super.dispatchTouchEvent(e)}
 private fun dedicated(){val d=getSystemService(Context.DEVICE_POLICY_SERVICE)as DevicePolicyManager;val a=ComponentName(this,KioskDeviceAdminReceiver::class.java);if(d.isDeviceOwnerApp(packageName)){d.setLockTaskPackages(a,arrayOf(packageName));try{startLockTask()}catch(_:Exception){}}}
 override fun onUserInteraction(){super.onUserInteraction();reset();hideUi()}
 private fun hideUi(){window.decorView.systemUiVisibility=5894}
 override fun onWindowFocusChanged(f:Boolean){super.onWindowFocusChanged(f);if(f)hideUi()}
 @Deprecated("Deprecated in Java")override fun onBackPressed(){if(web.canGoBack())web.goBack()else web.loadUrl(home)}
 override fun onSaveInstanceState(o:Bundle){web.saveState(o);super.onSaveInstanceState(o)}
 override fun onDestroy(){h.removeCallbacks(showIdle);h.removeCallbacks(showBlack);try{unregisterReceiver(screenReceiver)}catch(_:Exception){};web.destroy();super.onDestroy()}
}