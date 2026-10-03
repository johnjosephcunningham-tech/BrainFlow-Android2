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
import android.graphics.Rect
import android.view.ViewTreeObserver
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
  window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
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
   userAgentString += " SilverbackKiosk/1.11"
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
  installKeyboardResizeFix()
  reset()
 }

 private fun buildIdleScreen():FrameLayout {
  val frame=FrameLayout(this).apply { setBackgroundColor(Color.BLACK); visibility=View.GONE; setOnClickListener{wake()} }
  val scroll=ScrollView(this).apply { isFillViewport=true; setBackgroundColor(Color.BLACK) }
  val content=LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER_HORIZONTAL
   setPadding(dp(24),dp(18),dp(24),dp(92)); setBackgroundColor(Color.BLACK)
  }
  scroll.addView(content,FrameLayout.LayoutParams(-1,-1))
  frame.addView(scroll,FrameLayout.LayoutParams(-1,-1))

  val gorilla=ImageView(this).apply { setImageResource(R.drawable.gorilla_icon); scaleType=ImageView.ScaleType.CENTER_INSIDE }
  content.addView(gorilla,LinearLayout.LayoutParams(-1,dp(130)))

  fun t(value:String,size:Float,bold:Boolean=false)=TextView(this).apply {
   text=value; setTextColor(Color.WHITE); textSize=size; gravity=Gravity.CENTER
   typeface=Typeface.create("sans-serif-condensed",if(bold)Typeface.BOLD else Typeface.NORMAL)
   setShadowLayer(5f,0f,2f,Color.BLACK)
  }
  content.addView(t("SILVERBACK",38f,true))
  content.addView(t("NORTH YORK",21f,true).apply{setTextColor(Color.rgb(220,25,28))})
  content.addView(t("MIXED MARTIAL ARTS",15f,true),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)})
  content.addView(t("GYM RULES",44f,true).apply{
   setPadding(dp(8),dp(5),dp(8),dp(5)); background=GradientDrawable().apply{setColor(Color.rgb(125,12,16))}
  },LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})

  val rules=listOf(
   "👣  GOING BAREFOOT IS ONLY ALLOWED ON THE MATS.",
   "👟  YOU MUST WEAR SHOES/SANDALS AT ALL TIMES WHEN NOT ON THE MAT.",
   "🤼  DO NOT ROLL TOO HARD/AGGRESSIVELY AND/OR INJURE YOUR TRAINING PARTNERS.",
   "✋  NEVER CRANK SUBMISSIONS.",
   "✓  SUBMISSIONS MUST ALWAYS BE FINISHED SLOWLY & JUST LET GO IF THEY’RE NOT TAPPING.",
   "👏  TAP EARLY & OFTEN TO KEEP YOURSELF SAFE.",
   "✂  MAKE SURE YOUR FINGERNAILS AND TOENAILS ARE ALWAYS TRIMMED.",
   "✚  IF YOU HAVE A SKIN CONDITION LET A COACH KNOW AND/OR STAY HOME.",
   "●  IF YOU HAVE A COLD SORE, STAY HOME.",
   "⌂  IF SICK, PLEASE STAY HOME.",
   "♥  BE FRIENDLY AND SPREAD GOOD VIBES!"
  )
  rules.forEach { rule ->
   content.addView(t(rule,15f,true).apply{gravity=Gravity.START or Gravity.CENTER_VERTICAL;setPadding(dp(10),dp(7),dp(10),dp(7))},
    LinearLayout.LayoutParams(-1,-2))
   content.addView(View(this).apply{setBackgroundColor(Color.rgb(115,15,18))},LinearLayout.LayoutParams(-1,dp(1)))
  }
  content.addView(t("RESPECT THE GYM. RESPECT EACH OTHER. GET BETTER EVERY DAY.",17f,true).apply{setTextColor(Color.rgb(220,25,28))},
   LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})
  content.addView(t("THANK YOU FOR BEING PART OF THE SILVERBACK FAMILY.",11f,true))

  val bar=TextView(this).apply {
   text="MEMBER & VISITOR CHECK-IN  →"; setTextColor(Color.WHITE); textSize=20f; gravity=Gravity.CENTER
   typeface=Typeface.DEFAULT_BOLD; setBackgroundColor(Color.rgb(215,20,24)); setOnClickListener{wake()}
  }
  frame.addView(bar,FrameLayout.LayoutParams(-1,dp(68),Gravity.BOTTOM))
  return frame
 }

 private fun installKeyboardResizeFix(){
  val decor=window.decorView
  decor.viewTreeObserver.addOnGlobalLayoutListener(object:ViewTreeObserver.OnGlobalLayoutListener{
   override fun onGlobalLayout(){
    if(!::web.isInitialized) return
    val r=Rect(); decor.getWindowVisibleDisplayFrame(r)
    val total=decor.rootView.height
    val obscured=total-r.bottom
    val keyboardOpen=obscured>total*0.15
    val lp=web.layoutParams as FrameLayout.LayoutParams
    val wanted=if(keyboardOpen) r.height() else FrameLayout.LayoutParams.MATCH_PARENT
    if(lp.height!=wanted){
     lp.height=wanted; web.layoutParams=lp
     if(keyboardOpen) web.postDelayed({
      web.evaluateJavascript("(function(){var e=document.activeElement;if(e&&e.scrollIntoView){e.scrollIntoView({block:'center',behavior:'smooth'});}})();",null)
     },180)
    }
   }
  })
 }

 private fun hideKeyboard(){currentFocus?.clearFocus();(getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(web.windowToken,0)}
 private fun returnToFrontPage(){hideKeyboard();idle.visibility=View.GONE;black.visibility=View.GONE;web.clearHistory();web.loadUrl(home);reset();hideUi()}
 private fun wake(){returnToFrontPage()}
 private fun reset(){h.removeCallbacks(showIdle);h.removeCallbacks(showBlack);h.postDelayed(showIdle,60000L);h.postDelayed(showBlack,1800000L)}
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