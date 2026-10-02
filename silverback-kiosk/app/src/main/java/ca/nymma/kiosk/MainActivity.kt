package ca.nymma.kiosk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*

class MainActivity : Activity() {
 private lateinit var root: FrameLayout
 private lateinit var web: WebView
 private lateinit var idle: FrameLayout
 private lateinit var black: View
 private val home = "https://book.nymma.ca/kiosk"
 private val h = Handler(Looper.getMainLooper())
 private val showIdle = Runnable { hideKeyboard(); idle.visibility = View.VISIBLE; black.visibility = View.GONE }
 private val showBlack = Runnable { hideKeyboard(); idle.visibility = View.GONE; black.visibility = View.VISIBLE }
 private val screenReceiver = object: BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) {
   if(intent?.action == Intent.ACTION_SCREEN_ON) runOnUiThread { returnToFrontPage() }
  }
 }
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

 @SuppressLint("SetJavaScriptEnabled")
 override fun onCreate(s: Bundle?) {
  super.onCreate(s)
  window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  hideUi(); dedicated()
  root=FrameLayout(this)
  web=WebView(this)
  root.addView(web,FrameLayout.LayoutParams(-1,-1))
  idle=buildIdleScreen()
  black=View(this).apply { setBackgroundColor(Color.BLACK); visibility=View.GONE; setOnClickListener{wake()} }
  root.addView(idle,FrameLayout.LayoutParams(-1,-1))
  root.addView(black,FrameLayout.LayoutParams(-1,-1))
  setContentView(root)

  CookieManager.getInstance().setAcceptCookie(true)
  CookieManager.getInstance().setAcceptThirdPartyCookies(web,true)
  web.settings.apply {
   javaScriptEnabled=true; domStorageEnabled=true; databaseEnabled=true
   allowFileAccess=false; allowContentAccess=true; mediaPlaybackRequiresUserGesture=false
   userAgentString += " SilverbackKiosk/1.8"
  }
  web.webChromeClient=object:WebChromeClient(){override fun onPermissionRequest(r:PermissionRequest)=runOnUiThread{r.grant(r.resources)}}
  web.webViewClient=object:WebViewClient(){
   override fun shouldOverrideUrlLoading(v:WebView,r:WebResourceRequest):Boolean {
    val scheme=r.url.scheme?.lowercase()
    if(scheme=="https"||scheme=="http") return false
    Toast.makeText(this@MainActivity,"This tablet is locked to Silverback.",Toast.LENGTH_SHORT).show(); return true
   }
   override fun onReceivedSslError(v:WebView?,x:SslErrorHandler?,e:android.net.http.SslError?){x?.cancel()}
  }
  if(s==null) web.loadUrl(home) else web.restoreState(s)
  val filter=IntentFilter(Intent.ACTION_SCREEN_ON)
  if(Build.VERSION.SDK_INT>=33) registerReceiver(screenReceiver,filter,Context.RECEIVER_NOT_EXPORTED) else registerReceiver(screenReceiver,filter)
  reset()
 }

 private fun buildIdleScreen():FrameLayout {
  val portrait=resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_PORTRAIT
  val frame=FrameLayout(this).apply { setBackgroundColor(Color.BLACK); visibility=View.GONE; setOnClickListener{wake()} }

  val bg=ImageView(this).apply {
   setImageResource(R.drawable.kiosk_bg)
   scaleType=ImageView.ScaleType.FIT_CENTER
   adjustViewBounds=false
  }
  frame.addView(bg,FrameLayout.LayoutParams(-1,-1))

  val shade=View(this).apply { setBackgroundColor(Color.argb(65,0,0,0)) }
  frame.addView(shade,FrameLayout.LayoutParams(-1,-1))

  val content=LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL
   gravity=Gravity.CENTER_HORIZONTAL
   setPadding(dp(if(portrait)24 else 48),dp(12),dp(if(portrait)24 else 48),dp(18))
  }
  val contentParams=FrameLayout.LayoutParams(
   if(portrait) -1 else dp(600), -2, Gravity.CENTER
  )
  frame.addView(content,contentParams)

  val gorilla=ImageView(this).apply {
   setImageResource(R.drawable.gorilla_icon)
   scaleType=ImageView.ScaleType.CENTER_INSIDE
  }
  content.addView(gorilla,LinearLayout.LayoutParams(-1,dp(if(portrait)170 else 125)))

  fun text(value:String,size:Float,bold:Boolean=false,spacing:Float=0f)=TextView(this).apply {
   this.text=value; setTextColor(Color.WHITE); textSize=size; gravity=Gravity.CENTER
   typeface=Typeface.create(if(bold)"sans-serif-condensed" else "sans-serif",if(bold)Typeface.BOLD else Typeface.NORMAL)
   letterSpacing=spacing
   setShadowLayer(7f,0f,2f,Color.BLACK)
  }

  content.addView(text("SILVERBACK",if(portrait)40f else 36f,true,.05f),LinearLayout.LayoutParams(-1,-2))
  content.addView(text("MARTIAL ARTS",if(portrait)18f else 16f,true,.18f),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(if(portrait)22 else 12)})
  content.addView(text("MEMBER & VISITOR CHECK-IN",if(portrait)23f else 22f,true),LinearLayout.LayoutParams(-1,-2))
  content.addView(text("Welcome! Please complete the steps below.",if(portrait)15f else 14f),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5);bottomMargin=dp(if(portrait)20 else 12)})

  val button=TextView(this).apply {
   text="GET STARTED  →"; setTextColor(Color.WHITE); textSize=if(portrait)19f else 17f
   gravity=Gravity.CENTER; typeface=Typeface.DEFAULT_BOLD
   background=GradientDrawable().apply { setColor(Color.rgb(225,25,28)); cornerRadius=dp(5).toFloat() }
   setOnClickListener{wake()}
  }
  content.addView(button,LinearLayout.LayoutParams(-1,dp(if(portrait)60 else 52)).apply{marginStart=dp(if(portrait)16 else 70);marginEnd=dp(if(portrait)16 else 70)})
  return frame
 }

 private fun hideKeyboard(){currentFocus?.clearFocus();(getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(web.windowToken,0)}
 private fun returnToFrontPage(){hideKeyboard();idle.visibility=View.GONE;black.visibility=View.GONE;web.clearHistory();web.loadUrl(home);reset();hideUi()}
 private fun wake(){returnToFrontPage()}
 private fun reset(){h.removeCallbacks(showIdle);h.removeCallbacks(showBlack);h.postDelayed(showIdle,120000L);h.postDelayed(showBlack,1800000L)}
 override fun dispatchTouchEvent(e:MotionEvent?):Boolean {
  if(e?.action==MotionEvent.ACTION_DOWN&&(idle.visibility==View.VISIBLE||black.visibility==View.VISIBLE)){wake();return true}
  reset();return super.dispatchTouchEvent(e)
 }
 private fun dedicated(){val d=getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager;val a=ComponentName(this,KioskDeviceAdminReceiver::class.java);if(d.isDeviceOwnerApp(packageName)){d.setLockTaskPackages(a,arrayOf(packageName));try{startLockTask()}catch(_:Exception){}}}
 override fun onUserInteraction(){super.onUserInteraction();reset();hideUi()}
 private fun hideUi(){window.decorView.systemUiVisibility=5894}
 override fun onWindowFocusChanged(f:Boolean){super.onWindowFocusChanged(f);if(f)hideUi()}
 @Deprecated("Deprecated in Java") override fun onBackPressed(){if(web.canGoBack())web.goBack()else web.loadUrl(home)}
 override fun onSaveInstanceState(o:Bundle){web.saveState(o);super.onSaveInstanceState(o)}
 override fun onDestroy(){h.removeCallbacks(showIdle);h.removeCallbacks(showBlack);try{unregisterReceiver(screenReceiver)}catch(_:Exception){};web.destroy();super.onDestroy()}
}