package ca.nymma.kiosk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
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
 private lateinit var idleScreen: View
 private lateinit var blackScreen: View
 private val home = "https://book.nymma.ca/kiosk"
 private val handler = Handler(Looper.getMainLooper())
 private val idleDelay = 2 * 60 * 1000L
 private val blackDelay = 30 * 60 * 1000L
 private val showIdle = Runnable { hideKeyboard(); web.clearFocus(); idleScreen.visibility = View.VISIBLE; blackScreen.visibility = View.GONE }
 private val showBlack = Runnable { hideKeyboard(); web.clearFocus(); idleScreen.visibility = View.GONE; blackScreen.visibility = View.VISIBLE }

 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

 @SuppressLint("SetJavaScriptEnabled")
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  hideSystemUi(); enableDedicatedModeIfOwner()
  root=FrameLayout(this); web=WebView(this); root.addView(web,FrameLayout.LayoutParams(-1,-1))
  idleScreen=makeIdleScreen()
  blackScreen=View(this).apply{setBackgroundColor(Color.BLACK);visibility=View.GONE}
  root.addView(idleScreen,FrameLayout.LayoutParams(-1,-1));root.addView(blackScreen,FrameLayout.LayoutParams(-1,-1));setContentView(root)

  CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,true)
  web.settings.apply{javaScriptEnabled=true;domStorageEnabled=true;databaseEnabled=true;allowFileAccess=false;allowContentAccess=true;mediaPlaybackRequiresUserGesture=false;userAgentString+=" SilverbackKiosk/1.4"}
  web.webChromeClient=object:WebChromeClient(){override fun onPermissionRequest(request:PermissionRequest)=runOnUiThread{request.grant(request.resources)}}
  web.webViewClient=object:WebViewClient(){
   override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean{
    val scheme=request.url.scheme?.lowercase()
    if(scheme=="https"||scheme=="http") return false
    Toast.makeText(this@MainActivity,"This tablet is locked to Silverback.",Toast.LENGTH_SHORT).show();return true
   }
   override fun onReceivedSslError(view:WebView?,h:SslErrorHandler?,error:android.net.http.SslError?){h?.cancel()}
  }
  web.setDownloadListener{_,_,_,_,_->Toast.makeText(this,"Downloads are disabled in kiosk mode.",Toast.LENGTH_SHORT).show()}
  if(savedInstanceState==null)web.loadUrl(home)else web.restoreState(savedInstanceState)
  resetIdleTimers()
 }

 private fun makeIdleScreen():View{
  val portrait=resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_PORTRAIT
  val bg=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(7,7,7),Color.rgb(28,28,28),Color.BLACK))
  val frame=FrameLayout(this).apply{background=bg;visibility=View.GONE}
  val content=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL
   setPadding(dp(if(portrait)24 else 48),dp(if(portrait)24 else 14),dp(if(portrait)24 else 48),dp(18))
  }
  frame.addView(content,FrameLayout.LayoutParams(-1,-1))
  val logo=ImageView(this).apply{setImageResource(R.drawable.ic_launcher);scaleType=ImageView.ScaleType.CENTER_INSIDE}
  content.addView(logo,LinearLayout.LayoutParams(-1,0,if(portrait)4.0f else 3.5f).apply{bottomMargin=dp(if(portrait)8 else 2)})
  val brand=TextView(this).apply{
   text="SILVERBACK";setTextColor(Color.WHITE);textSize=if(portrait)44f else 42f;gravity=Gravity.CENTER
   typeface=Typeface.create("sans-serif-condensed",Typeface.BOLD);letterSpacing=.04f
  }
  content.addView(brand,LinearLayout.LayoutParams(-1,-2))
  val martial=TextView(this).apply{
   text="M A R T I A L   A R T S";setTextColor(Color.LTGRAY);textSize=if(portrait)18f else 16f;gravity=Gravity.CENTER
   typeface=Typeface.create("sans-serif",Typeface.BOLD)
  }
  content.addView(martial,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(if(portrait)28 else 12)})
  val checkin=TextView(this).apply{
   text="MEMBER & VISITOR CHECK-IN";setTextColor(Color.WHITE);textSize=if(portrait)24f else 23f;gravity=Gravity.CENTER
   typeface=Typeface.DEFAULT_BOLD
  }
  content.addView(checkin,LinearLayout.LayoutParams(-1,-2))
  val welcome=TextView(this).apply{
   text="Welcome! Please complete the steps below.";setTextColor(Color.LTGRAY);textSize=if(portrait)16f else 14f;gravity=Gravity.CENTER
  }
  content.addView(welcome,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6);bottomMargin=dp(if(portrait)24 else 12)})
  val button=TextView(this).apply{
   text="GET STARTED  →";setTextColor(Color.WHITE);textSize=if(portrait)20f else 18f;gravity=Gravity.CENTER
   typeface=Typeface.DEFAULT_BOLD
   background=GradientDrawable().apply{cornerRadius=dp(5).toFloat();setColor(Color.rgb(225,25,28))}
   setOnClickListener{wakeFromIdle()}
  }
  val maxWidth=if(portrait)520 else 620
  content.addView(button,LinearLayout.LayoutParams(-1,dp(if(portrait)64 else 56)).apply{
   marginStart=dp(if(portrait)10 else 90);marginEnd=dp(if(portrait)10 else 90);bottomMargin=dp(10)
  })
  return frame
 }

 private fun hideKeyboard(){currentFocus?.clearFocus();(getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(web.windowToken,0)}
 private fun wakeFromIdle(){idleScreen.visibility=View.GONE;blackScreen.visibility=View.GONE;web.loadUrl(home);resetIdleTimers()}
 private fun resetIdleTimers(){handler.removeCallbacks(showIdle);handler.removeCallbacks(showBlack);handler.postDelayed(showIdle,idleDelay);handler.postDelayed(showBlack,blackDelay)}
 override fun dispatchTouchEvent(ev:MotionEvent?):Boolean{
  if(ev?.action==MotionEvent.ACTION_DOWN&&(idleScreen.visibility==View.VISIBLE||blackScreen.visibility==View.VISIBLE)){wakeFromIdle();return true}
  resetIdleTimers();return super.dispatchTouchEvent(ev)
 }
 private fun enableDedicatedModeIfOwner(){
  val dpm=getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
  val admin=ComponentName(this,KioskDeviceAdminReceiver::class.java)
  if(dpm.isDeviceOwnerApp(packageName)){dpm.setLockTaskPackages(admin,arrayOf(packageName));try{startLockTask()}catch(_:Exception){}}
 }
 override fun onUserInteraction(){super.onUserInteraction();resetIdleTimers();hideSystemUi()}
 private fun hideSystemUi(){window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE}
 override fun onWindowFocusChanged(hasFocus:Boolean){super.onWindowFocusChanged(hasFocus);if(hasFocus)hideSystemUi()}
 @Deprecated("Deprecated in Java") override fun onBackPressed(){if(web.canGoBack())web.goBack()else web.loadUrl(home)}
 override fun onSaveInstanceState(outState:Bundle){web.saveState(outState);super.onSaveInstanceState(outState)}
 override fun onDestroy(){handler.removeCallbacks(showIdle);handler.removeCallbacks(showBlack);web.destroy();super.onDestroy()}
}
