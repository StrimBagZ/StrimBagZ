package net.lubot.strimbagzrewrite.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.N
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.design.widget.Snackbar
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.NotificationCompat
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.View.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.*
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.PopupMenu.OnMenuItemClickListener
import android.widget.RelativeLayout
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.cocosw.bottomsheet.BottomSheet
import com.google.android.exoplayer.ExoPlaybackException
import com.google.android.exoplayer.ExoPlayer
import com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException
import com.google.android.exoplayer.MediaCodecUtil.DecoderQueryException
import com.google.android.exoplayer.MediaFormat
import com.google.android.exoplayer.audio.AudioCapabilities
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver
import com.google.android.exoplayer.drm.UnsupportedDrmException
import com.google.android.exoplayer.metadata.id3.Id3Frame
import com.google.android.exoplayer.text.Cue
import com.google.android.exoplayer.util.MimeTypes
import com.google.android.exoplayer.util.Util
import com.google.android.exoplayer.util.VerboseLogUtil
import com.neovisionaries.ws.client.*
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.drawer_channel_description_new.*
import kotlinx.android.synthetic.main.drawer_viewer_list.*
import kotlinx.android.synthetic.main.newmedia_controller.*
import kotlinx.android.synthetic.main.player_activity.*
import net.lubot.strimbagzrewrite.R
import net.lubot.strimbagzrewrite.BuildConfig
import net.lubot.strimbagzrewrite.Constants
import net.lubot.strimbagzrewrite.data.model.FrankerFaceZ.SRLRaceEntrant
import net.lubot.strimbagzrewrite.data.TwitchAPI
import net.lubot.strimbagzrewrite.data.TwitchKraken
import net.lubot.strimbagzrewrite.StrimBagZApplication
import net.lubot.strimbagzrewrite.data.model.Twitch.*
import net.lubot.strimbagzrewrite.util.*
import org.apache.commons.lang3.StringUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.io.InputStream
import java.net.CookieHandler
import java.net.CookieManager
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.concurrent.*

class PlayerActivity : AppCompatActivity(), SurfaceHolder.Callback, Player.Listener,
        Player.CaptionListener, Player.Id3MetadataListener, AudioCapabilitiesReceiver.Listener {

    private var contentFrame: FrameLayout? = null
    private var contentRelative: RelativeLayout? = null
    private var player: Player? = null
    private var controller: VideoController? = null
    private var playerNeedsPrepare: Boolean = false

    private var chat: WebView? = null

    private var playerPosition: Long = 0
    private var enableBackgroundAudio: Boolean = false
    private var VoD: Boolean = false
    private var isTablet: Boolean = false
    private var chatOnly: Boolean = false
    private var loggedIn: Boolean = false
    private var blockPlayer: Boolean = false
    private var debug: Boolean = false
    private var scriptLoaded: Boolean = false
    private var streamFirstLoaded = true

    private var debugAddress: String? = null
    private var displayNameUser: String = Constants.NO_USER
    private var login: String = Constants.NO_USER
    private var twitchID: String = Constants.NO_USER
    private var currentlyHosting: String? = null
    private var currentlyHostingDisplayName: String? = null
    private var darkMode: Boolean = false
    private var oauth: String = Constants.NO_TOKEN
    private var quality: Int = 0

    private var channel: Channel = Channel.createEmpty()
    private var productExists: Boolean = false

    private var ws: WebSocket? = null
    private var wsAttempt: Int = 1
    private var counterFFZ = 1
    private var awaitAnswers = HashMap<Int, String>()
    private var receivedAnswers = HashMap<String, String>()
    private var followButtonsMap = HashMap<String, String>()
    private var followButtons = ArrayList<String>()
    private var followChannel: String? = ""

    private var raceEntrants: ArrayList<SRLRaceEntrant>? = ArrayList()

    var contentUri: Uri? = null

    //Overlay
    private var overlayWebViewContainer: FrameLayout? = null
    private var overlayWebView: WebView? = null
    private var show: Animation? = null
    private var hide: Animation? = null

    private var audioCapabilitiesReceiver: AudioCapabilitiesReceiver? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils.onActivityCreateSetTheme(this@PlayerActivity)

        isTablet = resources.getBoolean(R.bool.isTablet)
        setContentView(R.layout.player_activity)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)


        if (savedInstanceState != null) {
            Log.d("savedInstanceState", "restore from previous state")
            channel = savedInstanceState.getParcelable("channel")
            chatOnly = savedInstanceState.getBoolean("chatOnly")
            VoD = savedInstanceState.getBoolean("VoD")
            streamFirstLoaded = false
            getStreamURI(channel.name(), true)
        } else {
            contentUri = intent.data
            Log.d("onCreate", "Current contentUri: " + contentUri)
            channel = intent.getParcelableExtra("channel")
            chatOnly = intent.getBooleanExtra("chatOnly", false)
            VoD = intent.getBooleanExtra("VoD", false)
        }

        StrimBagZApplication.currentChat = channel.name()

        if (!chatOnly) {
            aspectRatio.setAspectRatio(1.78f) // 16:9
            determineDefaultQuality(intent.getStringExtra("quality"))
            videoSurface.holder.addCallback(this)
        }

        // Tablets and phones for the PlayerActivity are using different layouts
        if (isTablet) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            contentFrame = content as FrameLayout
        } else {
            contentRelative = content as RelativeLayout
            contentRelative
                    ?.viewTreeObserver
                    ?.addOnGlobalFocusChangeListener(ViewTreeObserver.OnGlobalFocusChangeListener {
                        view, view1 ->
                        // navigation bar height
                        var navigationBarHeight = 0
                        var resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
                        if (resourceId > 0) {
                            navigationBarHeight = resources.getDimensionPixelSize(resourceId)
                        }

                        // status bar height
                        var statusBarHeight = 0
                        resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
                        if (resourceId > 0) {
                            statusBarHeight = resources.getDimensionPixelSize(resourceId)
                        }

                        // display window size for the app layout
                        val rect = Rect()
                        window.decorView.getWindowVisibleDisplayFrame(rect)

                        // screen height - (user app height + status + nav) ..... if non-zero, then there is a soft keyboard
                        val keyboardHeight = contentRelative!!.rootView.height.minus((statusBarHeight + navigationBarHeight + rect.height()))

                        if (keyboardHeight <= 0) {
                            Log.d("Keyboard detection", "closed")
                        } else {
                            Log.d("Keyboard detection", "opend")
                        }
                    })
        }

        val currentHandler = CookieHandler.getDefault()
        if (currentHandler !== defaultCookieManager) {
            CookieHandler.setDefault(defaultCookieManager)
        }

        if (channel.partner()) {
            // Partnered channels can have no Subscribe button, let's check it
            checkProduct()
        }

        initPreferences()
        getHostStatus()
        initPlayerUI()
        initDescriptionUI()
        initViewerListUI()
        initOverlayUI()
        initChatUI()
        setupTopController()

        setFollowButtons(null)
        connectWebSocket()
        setChannelDescription()

        //handelSRL(srlJson)
    }

    fun setupTopController() {
        if (!chatOnly) {
            btn_quality.setOnClickListener({
                //view -> showVideoPopup(view)
                if (hasVideoTracks()) {
                    if (productExists) {
                        val btm : BottomSheet.Builder
                        if (darkMode) {
                            btm = BottomSheet.Builder(this, R.style.BottomSheet_CustomDarkTheme)
                                    .title("Options")
                                    .sheet(R.menu.sheet_player)
                        } else {
                            btm = BottomSheet.Builder(this)
                                    .title("Options")
                                    .sheet(R.menu.sheet_player)
                        }
                        btm.listener({ p0, p1 ->
                            when (p1) {
                                R.id.player_source -> changeQuality(true, 1)
                                R.id.player_high -> changeQuality(true, 2)
                                R.id.player_medium -> changeQuality(true, 3)
                                R.id.player_low -> changeQuality(true, 4)
                                R.id.player_mobile -> changeQuality(true, 5)
                                R.id.player_subscribe -> openSubscribeSite()
                                R.id.player_share -> shareChannel()
                                R.id.chatRooms -> requestChatRooms()
                                R.id.hostChannel -> hostChannel()
                                R.id.unhostChannel -> unhostChannel()
                            }
                        })
                        if (currentlyHosting != null && currentlyHosting.equals(channel.name())) {
                            btm.sheet(R.id.unhostChannel, formatHostString())
                        } else {
                            btm.sheet(R.id.hostChannel, formatHostString())
                        }
                        btm.show()
                    } else {
                        val btm : BottomSheet.Builder
                        if (darkMode) {
                            btm = BottomSheet.Builder(this, R.style.BottomSheet_CustomDarkTheme)
                                    .title("Options")
                                    .sheet(R.menu.sheet_player)
                        } else {
                            btm = BottomSheet.Builder(this)
                                    .title("Options")
                                    .sheet(R.menu.sheet_player)
                        }
                        btm.listener({ p0, p1 ->
                            when (p1) {
                                R.id.player_source -> changeQuality(true, 1)
                                R.id.player_high -> changeQuality(true, 2)
                                R.id.player_medium -> changeQuality(true, 3)
                                R.id.player_low -> changeQuality(true, 4)
                                R.id.player_mobile -> changeQuality(true, 5)
                                R.id.player_share -> shareChannel()
                                R.id.chatRooms -> requestChatRooms()
                                R.id.hostChannel -> hostChannel()
                                R.id.unhostChannel -> unhostChannel()
                            }
                        })
                        if (currentlyHosting != null && currentlyHosting.equals(channel.name())) {
                            btm.sheet(R.id.unhostChannel, formatHostString())
                        } else {
                            btm.sheet(R.id.hostChannel, formatHostString())
                        }
                        btm.show()
                    }
                } else {
                    val btm : BottomSheet.Builder
                    if (darkMode) {
                        btm = BottomSheet.Builder(this, R.style.BottomSheet_CustomDarkTheme)
                                .title("Options")
                                .sheet(R.menu.sheet_player)
                    } else {
                        btm = BottomSheet.Builder(this)
                                .title("Options")
                                .sheet(R.menu.sheet_player)
                    }
                    btm.listener({ p0, p1 ->
                        when (p1) {
                            R.id.player_share -> shareChannel()
                            R.id.chatRooms -> requestChatRooms()
                            R.id.hostChannel -> hostChannel()
                            R.id.unhostChannel -> unhostChannel()
                        }
                    })
                    if (currentlyHosting != null && currentlyHosting.equals(channel.name())) {
                        btm.sheet(R.id.unhostChannel, formatHostString())
                    } else {
                        btm.sheet(R.id.hostChannel, formatHostString())
                    }
                    btm.show()
                }
            })

            btn_viewer_list.setOnClickListener {
                drawerLayout.openDrawer(Gravity.LEFT)
                controller?.hide()
            }

            btn_info.setOnClickListener {
                drawerLayout.openDrawer(Gravity.RIGHT)
                controller?.hide()
            }
        }
    }

    //TODO Finish Tab completion
    /*
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed()
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB))
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    */

    fun hostChannel() {
        if (login.equals(Constants.NO_USER)) {
            return
        }
        chat?.loadUrl("javascript:(function (){host('${login}', '${channel.name()}')}());")
    }

    fun unhostChannel() {
        if (login.equals(Constants.NO_USER)) {
            return
        }
        chat?.loadUrl("javascript:(function (){unhostChannel('${login}')}());")
        currentlyHosting = null
    }

    fun requestChatRooms() {
        chat?.loadUrl("javascript:(function (){strimBagInterface.getRooms(getRoomArray(),getRoomIDs())}());")
    }

    fun changeChatRoom(room: String, joinRoom: Boolean) {
        if (joinRoom) {
            chat?.loadUrl("javascript:(function (){" +
                    "ffz._join_room('$room')" +
                    "}());")
        }
        chat?.loadUrl("javascript:(function (){" +
                "var controller = App.__container__.lookup('controller:chat');" +
                " controller.blurRoom();" +
                " controller.focusRoom(ffz.rooms['$room'].room);}());")
    }

    fun changeChatRoomSheet(array: Array<String>, ids: Array<String>) {
        val bottomSheet = BottomSheet.Builder((chat?.context as MutableContextWrapper).baseContext as Activity)
                .title("Choose Chat room to switch:")
        var id = 0
        array.forEach {
            Log.d("Chatrooms", it + ", id: " + id)
            bottomSheet.sheet(id, it)
            id++
        }
        bottomSheet.listener({ p0, p1 ->
            val room = ids[p1]
            Log.d("changeRoom", room)
            changeChatRoom(room, false)
        }).show()
    }

    fun initPreferences() {
        val prefs = getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE)
        debug = prefs.getBoolean(Constants.SETTING_DEBUG, false)
        debugAddress = prefs.getString(Constants.SETTING_DEBUG_ADDRESS, "")
        darkMode = prefs.getBoolean(Constants.SETTING_DARK_THEME, false)
        displayNameUser = prefs.getString(Constants.DISPLAY_NAME, Constants.NO_USER)
        login = prefs.getString(Constants.LOGIN, Constants.NO_USER)
        twitchID = prefs.getString(Constants.TWITCH_ID, Constants.NO_USER)
        oauth = prefs.getString(Constants.OAUTH, Constants.NO_TOKEN)
        loggedIn = true
    }

    fun initPlayerUI() {
        if (!chatOnly) {
            videoSurface.setOnTouchListener { view, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    if (controller?.isShowing as Boolean) {
                        controller?.hide()
                    } else {
                        controller?.show()
                    }
                } else if (motionEvent.action == MotionEvent.ACTION_UP) {
                    view.performClick()
                }
                true
            }
            audioCapabilitiesReceiver = AudioCapabilitiesReceiver(this, this)
            audioCapabilitiesReceiver?.register()

            videoSurface.holder.addCallback(this)
            videoIsLoading(true)
        } else {
            Log.d("chatOnly", "Removing Video surface")
            if (isTablet) {
                contentFrame?.removeView(aspectRatio)
            } else {
                contentRelative?.removeView(aspectRatio)
            }
        }

        if (VoD) {
            if (isTablet) {
                contentFrame?.removeView(chatContainer)
            } else {
                contentRelative?.removeView(chatContainer)
            }
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            window.decorView.systemUiVisibility.and(SYSTEM_UI_FLAG_LAYOUT_STABLE
                    and  SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    and  SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    and  SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    and  SYSTEM_UI_FLAG_FULLSCREEN
                    and  SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            if (SDK_INT >= LOLLIPOP) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            }

            btn_viewer_list.visibility = GONE
            btn_info.visibility = GONE
            btn_favorite.visibility = GONE
            btn_quality.visibility = GONE
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

            controller = VideoController(this, this@PlayerActivity, surface_controller, isTablet)

            if (isTablet) {

                //controller = VideoControllerView(this, this@PlayerActivity,
                //        surface_top, false, true, true, true)
            } else {
                //controller = VideoControllerView(this, this@PlayerActivity,
                //        surface_top, false, true, true, false)
            }
        } else {
            controller = VideoController(this, this@PlayerActivity, surface_controller, isTablet)

            if (isTablet) {
                //controller = VideoControllerView(this, this@PlayerActivity,
                //        surface_top, true, false, false, true)
            } else {
                //controller = VideoControllerView(this, this@PlayerActivity,
                //        surface_top, true, false, false, false)
            }
        }
        //controller?.setAnchorView(aspectRatio)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun initDescriptionUI() {

        drawerLayout.setScrimColor(Color.TRANSPARENT)

        drawerLayout.setDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {

            }

            override fun onDrawerOpened(drawerView: View) {
                if (drawerView.id == R.id.viewerListDrawer) {
                    loadViewerList()
                }
            }

            override fun onDrawerClosed(drawerView: View) {

            }

            override fun onDrawerStateChanged(newState: Int) {

            }
        })

        //channelDescriptionTitle.text = channel

        webviewDescription.settings.javaScriptEnabled = true
        webviewDescription.settings.domStorageEnabled = true
        webviewDescription.settings.databaseEnabled = true
        webviewDescription.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webviewDescription.settings.useWideViewPort = true
        webviewDescription.isHorizontalScrollBarEnabled = false
        webviewDescription.setWebViewClient(object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {

                if (!url.equals("https://www.twitch.tv/${login}/chat")) {
                    if (url.startsWith("http:") || url.startsWith("https:")) {
                        Log.d("urlOverride", "url: " + url)
                        Log.d("urlOverride", "is YouTube: " + StringUtils.contains(url, "youtube.com"))
                        if (StringUtils.contains(url, "youtube.com") && player != null) {
                            player?.setMute(true)
                        }
                        showOverlay(url)
                        drawerLayout?.closeDrawers()
                        Log.d("Changes", "changes")
                        return true
                    }
                }
                return false
            }


            override fun onPageFinished(view: WebView, url: String) {
                view.loadUrl("javascript:(function (){" +
                        "document.getElementById('top').style.display = 'none';" +
                        " document.getElementsByClassName('container')[2].style.display = 'none';" +
                        " document.getElementById('foot').style.display = 'none';" +
                        "}());")
            }
        }
        )

        if (channel.displayName() != null) {
            descriptionToolbar.title = channel.displayName()
        } else {
            descriptionToolbar.title = channel.name()
        }

        if (channel.profileBanner() != null) {
            Glide.with(this)
                    .load(channel.profileBanner())
                    .fitCenter()
                    .into(backdrop)
        }

        if (channel.logo() != null) {
            Glide.with(this)
                    .load(channel.logo())
                    .asBitmap()
                    .centerCrop()
                    .into(object : BitmapImageViewTarget(avatar) {
                        override fun setResource(resource: Bitmap) {
                            val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(this@PlayerActivity.resources, resource)
                            circularBitmapDrawable.isCircular = true
                            avatar.setImageDrawable(circularBitmapDrawable)
                        }
                    });
        }
    }

    fun initViewerListUI() {
        viewerListReload.setOnClickListener { loadViewerList() }
        var html = ""
        html += "<html>" +
                "<head>" +
                "<style>html{color: #8c8c9c; background: #25252A;}  " +
                "img{ max-width:100%; height:auto; } " +
                "a { color: #AB81F9; font-size: 100%; text-decoration: none; } .list-header { font-size: 18px; margin-top: 20px; } .item { color: #a68ed2; }" +
                "</style>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0'>" +
                "</head>" +
                "<body>"
        html += "</body></html>"
        webViewViewerList.settings.domStorageEnabled = true
        webViewViewerList.settings.javaScriptEnabled = true
        webViewViewerList.loadData(html, "text/html; charset=UTF-8", null)
    }

    fun loadViewerList() {
        //todo renew this
        //TDTaskManager.executeTask(ViewerListCallback(this, channel))
        viewerListReload.visibility = GONE
        viewerListLoading.visibility = VISIBLE
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun initOverlayUI() {
        show = AnimationUtils.loadAnimation(this, R.anim.view_show)
        hide = AnimationUtils.loadAnimation(this, R.anim.view_hide)

        overlayWebViewContainer = findViewById(R.id.overlayWebView) as FrameLayout

        overlayWebView = WebView(this)
        overlayWebView?.settings?.javaScriptEnabled = true
        overlayWebView?.settings?.useWideViewPort = true
        overlayWebView?.settings?.domStorageEnabled = true
        overlayWebView?.settings?.databaseEnabled = true
        overlayWebView?.settings?.cacheMode = WebSettings.LOAD_NO_CACHE
        overlayWebView?.settings?.builtInZoomControls = true

        overlayWebView?.setWebChromeClient(object : WebChromeClient() {
            /*
            override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                if (BuildConfig.DEBUG) {
                    Log.d("TAG", cm.message() + " at " + cm.sourceId() + ":" + cm.lineNumber());
                }
                return true
            }
            */
        })
        overlayWebView?.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                Log.d("urlOverride", "url: " + url)
                Log.d("urlOverride", "is YouTube: " + StringUtils.containsIgnoreCase(url, "https://m.youtube.com/"))
                if (StringUtils.containsIgnoreCase(url, "https://m.youtube.com/") && player != null) {
                    player?.setMute(true)
                }
                if (StringUtils.containsIgnoreCase(url, "https://secure.xsolla.com/")) {

                }
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (overlayToolbar != null) {
                    overlayTitle.text = "Loading..."
                    overlayLoading.visibility = VISIBLE
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (overlayToolbar != null) {
                    if (StringUtils.containsIgnoreCase(url, "https://secure.xsolla.com/")) {
                        if (channel.displayName() != null) {
                            overlayTitle.text = "Subscribe to " + channel.displayName()
                        } else {
                            overlayTitle.text = "Subscribe to " + channel.name()
                        }
                        overlayLoading.visibility = GONE
                        return
                    }
                    overlayTitle.text = view.title
                    overlayLoading.visibility = GONE
                }
            }
        })

        if (overlayToolbar != null) {
            overlayClose.setOnClickListener { hideOverlay() }
        }
        Log.d("OverlayWebView", "adding webview to container")
        overlayWebViewContainer?.addView(overlayWebView)
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected fun initChatUI() {
        if (chat == null && !VoD && StrimBagZApplication.chat == null) {
            val contextWrapper = MutableContextWrapper(this)

            chat = WebView(contextWrapper)
            chat?.resumeTimers()
            chat?.settings?.javaScriptEnabled = true
            chat?.settings?.domStorageEnabled = true
            chat?.settings?.databaseEnabled = true
            chat?.settings?.useWideViewPort = true
            chat?.settings?.javaScriptCanOpenWindowsAutomatically = true
            chat?.settings?.cacheMode = WebSettings.LOAD_NO_CACHE

            chat?.addJavascriptInterface(strimBagInterface(), "strimBagInterface")
            chat?.setWebChromeClient(object : WebChromeClient() {
                override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                    if (BuildConfig.DEBUG) {
                        //Log.d("TAG", cm.message() + " at " + cm.sourceId() + ":" + cm.lineNumber());
                    }
                    return true
                }
            })
            chat?.setWebViewClient(webViewClient)
            if (loggedIn) {
                chat?.loadUrl("https://www.twitch.tv/${channel.name()}/chat")
                //chat?.loadUrl("file:///android_asset/chat/index.html")
            } else {
                chat?.loadUrl("https://twitch.tv/login")
            }
        }

        /*
        //TODO New Chat implementation
        chatInput.setOnEditorActionListener { textView, i, keyEvent ->
            var handled = false
            if (i == EditorInfo.IME_ACTION_SEND) {
                val msg = textView.text.toString()
                chat?.loadUrl("javascript:(function (){client.say(client.channels[0], '$msg')}());")
                chatInput.setText("")
                handled = true
            }
            handled
        }
        */

        if (StrimBagZApplication.chat == null) {
            StrimBagZApplication.chat = chat
        } else {
            chat = StrimBagZApplication.chat
            if (chat?.context is MutableContextWrapper) {
                (chat?.context as MutableContextWrapper).baseContext = this
            }
            chat?.setWebViewClient(webViewClient)
            // Change chat room if we're in a different chat
            if (StrimBagZApplication.previousChat != StrimBagZApplication.currentChat) {
                Log.d("ChatSwitch", "new chat detected")
                StrimBagZApplication.chat?.post { chat?.loadUrl("javascript:(function (){strimBagInterface.changeChannel(strimBagInterface.oldChannel(),strimBagInterface.channel())}());") }
            }
        }

        if (chat != null && chatListView != null) {
            try {
                chatListView.addView(chat)
            } catch (e: IllegalStateException) {
                //child already has a parent
                (chat?.parent as ViewGroup).removeAllViews()
                chatListView.addView(chat)
            }
        }
    }

    fun destroyWebView() {
        if (overlayWebView != null) {
            overlayWebView?.loadUrl("about:blank")
            overlayWebView?.clearCache(true)
            overlayWebView?.destroy()
            overlayWebView = null
        }

        if (chatListView != null) {
            chatListView.removeAllViews()
        }
        if (overlayStream != null) {
            overlayStream.removeAllViews()
        }
    }

    fun destroyChatView() {
        if (chat != null) {
            chat?.clearHistory();
            chat?.clearCache(true);
            chat?.loadUrl("about:blank");
            chat?.pauseTimers();
            chat = null;

            StrimBagZApplication.chat?.clearHistory();
            StrimBagZApplication.chat?.clearCache(true);
            StrimBagZApplication.chat?.loadUrl("about:blank");
            StrimBagZApplication.chat?.pauseTimers();
            StrimBagZApplication.chat = null;

            StrimBagZApplication.currentChat = ""
            StrimBagZApplication.previousChat = ""
        }
    }

    fun destroyInfoView() {
        if (webviewDescription != null) {
            webviewDescription.clearCache(true)
            webviewDescription.destroy()
        }
    }


    public override fun onNewIntent(intent: Intent) {
        releasePlayer()
        playerPosition = 0
        setIntent(intent)
    }

    override fun onBackPressed() {
        Log.d("onBackPressed", "pressed")
        if (overlayStream.visibility == VISIBLE) {
            hideOverlay()
            return
        }
        super.onBackPressed()
    }

    public override fun onResume() {
        super.onResume()
        if (SDK_INT >= N && isInMultiWindowMode && player?.playbackState == Player.STATE_READY) {
            // Support for Android N's Multi-Window mode
            return
        }

        if (!chatOnly) {
            Log.d("onResume", "Stream first time loaded: " + streamFirstLoaded)
            if (!streamFirstLoaded) {
                // Renew stream url
                if (!blockPlayer) {
                    getStreamURI(channel.name(), true)
                } else {
                    blockPlayer = false
                }
            } else {
                // Stream was loaded for the first time
                streamFirstLoaded = false
                if (player == null) {
                    videoIsLoading(true)
                    preparePlayer(true)
                }
            }

            if (enableBackgroundAudio) {
                player?.backgrounded = false
            }
        }
    }

    fun videoIsLoading(isLoading: Boolean) {
        if (isLoading) {
            surface_error.visibility = VISIBLE
            surface_progress.visibility = VISIBLE
        } else {
            surface_progress.visibility = GONE
            surface_error.visibility = INVISIBLE
        }
    }

    public override fun onPause() {
        super.onPause()
        if (SDK_INT >= N && isInMultiWindowMode && player?.playbackState == Player.STATE_READY) {
            // Support for Android N's Multi-Window mode
            return
        }
        if (!chatOnly) {
            if (!enableBackgroundAudio) {
                releasePlayer()
            } else {
                player?.backgrounded = true
            }
            if (surface_error.visibility == VISIBLE) {
                surface_text.text = ""
                surface_text.visibility = GONE
                videoIsLoading(false)
            }
        }
        //setupBackgroundNotification()
    }

    fun setupBackgroundNotification() {
        if (SDK_INT >= LOLLIPOP) {
            val notification = NotificationCompat.Builder(this)
            if (chatOnly) {
                if (channel.displayName() != null) {
                    notification.setContentTitle(String.format(resources.getString
                    (R.string.notification_chat_backgrounded_title), channel.displayName()))
                    notification.setContentText(String.format(resources.getString
                    (R.string.notification_chat_backgrounded), channel.displayName()))
                } else {
                    notification.setContentTitle(String.format(resources.getString
                    (R.string.notification_chat_backgrounded_title), channel.name()))
                    notification.setContentText(String.format(resources.getString
                    (R.string.notification_chat_backgrounded), channel.name()))
                }
            } else {
                if (channel.displayName() != null) {
                    notification.setContentTitle(String.format(resources.getString
                    (R.string.notification_stream_backgrounded_title), channel.displayName()))
                    notification.setContentText(String.format(resources.getString
                    (R.string.notification_stream_backgrounded), channel.displayName()))
                } else {
                    notification.setContentTitle(String.format(resources.getString
                    (R.string.notification_stream_backgrounded_title), channel.name()))
                    notification.setContentText(String.format(resources.getString
                    (R.string.notification_stream_backgrounded), channel.name()))
                }
            }
            notification.setSmallIcon(R.drawable.ic_notification)
            notification.setOngoing(true)
            notification.setAutoCancel(true)
            val nIntent = Intent(this, this@PlayerActivity.javaClass)
            val pending = PendingIntent.getActivity(this, 0, nIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            val rIntent = Intent()
            rIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            rIntent.action = "net.lubot.strimbagz.PlayerActivity"
            val pendingRemove = PendingIntent.getBroadcast(this, 1, rIntent, PendingIntent.FLAG_CANCEL_CURRENT)
            notification.setContentIntent(pending)
            notification.addAction(R.drawable.ic_close, "Close", pendingRemove)

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(42069, notification.build())

            val filter = IntentFilter()
            filter.addAction("net.lubot.strimbagz.PlayerActivity")
            registerReceiver(receiver, filter)
        }
    }

    val receiver: BroadcastReceiver
        get() = object :  BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                try {
                    unregisterReceiver(receiver)
                } catch (e : IllegalArgumentException) {
                    // Nothing, Receiver was just not registerd
                }
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(42069)
                destroyChatView()
                finish()
            }
        }

    public override fun onDestroy() {
        super.onDestroy()
        StrimBagZApplication.previousChat = channel.name()
        if (!chatOnly) {
            audioCapabilitiesReceiver?.unregister()
            releasePlayer()
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(42069)
        chat?.removeAllViews()
        chat?.removeAllViewsInLayout()
        destroyWebView()
        destroyInfoView()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // AudioCapabilitiesReceiver.Listener methods

    override fun onAudioCapabilitiesChanged(audioCapabilities: AudioCapabilities) {
        if (player == null) {
            return
        }
        val backgrounded = player?.backgrounded
        val playWhenReady = player?.playWhenReady
        releasePlayer()
        preparePlayer(playWhenReady as Boolean)
        player?.backgrounded = backgrounded as Boolean
    }



    // Internal methods

    private val rendererBuilder: Player.RendererBuilder
        get() {
            //val userAgent = Util.getUserAgent(this, "StrimBagZ")
            val userAgent = "TwitchExoPlayer/4.13.2" + " (Linux;Android " + Build.VERSION.RELEASE +") " + "ExoPlayerLib/1.3.3"
            return HlsRendererBuilder(this, userAgent, contentUri.toString())
        }

    private fun preparePlayer(playWhenReady: Boolean) {
        if (player == null) {
            player = Player(rendererBuilder)
            player?.addListener(this)
            player?.setCaptionListener(this)
            player?.setMetadataListener(this)
            player?.seekTo(playerPosition)
            playerNeedsPrepare = true
            //controller?.setMediaPlayer(player?.playerControl)
            //controller?.isEnabled = true
        }
        if (playerNeedsPrepare) {
            player?.prepare()
            playerNeedsPrepare = false
        }

        player?.surface = videoSurface.holder.surface
        player?.playWhenReady = playWhenReady

    }

    private fun releasePlayer() {
        if (player != null) {
            playerPosition = player!!.currentPosition
            player?.release()
            player = null
        }
    }

    private fun determineDefaultQuality(quality: String) {
        when (quality) {
            "auto" -> this.quality = 0
            "source" -> this.quality = 1
            "high" -> this.quality = 2
            "medium" -> this.quality = 3
            "low" -> this.quality = 4
            "mobile" -> this.quality = 5
            else -> this.quality = 0
        }
    }

    fun changeQuality(setNewDefault: Boolean) {
        changeQuality(setNewDefault, null)
    }

    fun changeQuality(setNewDefault: Boolean, newQuality: Int?) {
        if (setNewDefault && newQuality != null) {
            this.quality = newQuality
        }
        when (quality) {
            0 -> // Auto
                player?.setSelectedTrack(Player.TYPE_VIDEO, 0)
            1 -> // Source
                player?.setSelectedTrack(Player.TYPE_VIDEO, 1)
            2 -> // High
                player?.setSelectedTrack(Player.TYPE_VIDEO, 2)
            3 -> // Medium
                player?.setSelectedTrack(Player.TYPE_VIDEO, 3)
            4 -> // Low
                player?.setSelectedTrack(Player.TYPE_VIDEO, 4)
            5 -> // Mobile
                player?.setSelectedTrack(Player.TYPE_VIDEO, 5)
        }
    }

    override fun onStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_BUFFERING -> {
                Log.d("Player onStateChanged", "Stream buffering")
                Log.d("trackCount Buffering", player?.getTrackCount(Player.TYPE_VIDEO).toString())
                Log.d("track audio", player?.getTrackCount(Player.TYPE_AUDIO).toString())
                // Show the user that we're buffering
                videoIsLoading(true)
                val trackCount = player?.getTrackCount(Player.TYPE_VIDEO)
                // Check if quality options are available
                if (trackCount == 1) {
                    return
                }
                Log.d("loadPreferredQuality", "Quality: " + quality)
                changeQuality(false)
            }
            ExoPlayer.STATE_ENDED -> {
                Log.d("Player onStateChanged", "Stream ended")
                surface_error.visibility = VISIBLE
                surface_text.visibility = VISIBLE
                surface_progress.visibility = GONE
                surface_text.setText(R.string.stream_went_offline)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            ExoPlayer.STATE_IDLE -> Log.d("Player onStateChanged", "Stream idle")
            ExoPlayer.STATE_PREPARING -> Log.d("Player onStateChanged", "Stream preparing")
            ExoPlayer.STATE_READY -> {
                Log.d("Player onStateChanged", "Stream ready")
                if (surface_error.visibility == VISIBLE) {
                    videoIsLoading(false)
                }
            }
            else -> Log.d("Player onStateChanged", "Stream unknown")
        }
    }

    override fun onError(e: Exception) {
        Log.d("ExoPlayer onError", e.message)
        var errorString: String? = null
        if (e is UnsupportedDrmException) {
            // Special case DRM failures.
            errorString = getString(if (Util.SDK_INT < 18)
                R.string.error_drm_not_supported
            else if (e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME)
                R.string.error_drm_unsupported_scheme
            else
                R.string.error_drm_unknown)
        } else if (e is ExoPlaybackException && e.cause is DecoderInitializationException) {
            // Special case for decoder initialization failures.
            val decoderInitializationException = e.cause as DecoderInitializationException
            if (decoderInitializationException.decoderName == null) {
                if (decoderInitializationException.cause is DecoderQueryException) {
                    errorString = getString(R.string.error_querying_decoders)
                } else if (decoderInitializationException.secureDecoderRequired) {
                    errorString = getString(R.string.error_no_secure_decoder,
                            decoderInitializationException.mimeType)
                } else {
                    errorString = getString(R.string.error_no_decoder,
                            decoderInitializationException.mimeType)
                }
            } else {
                errorString = getString(R.string.error_instantiating_decoder,
                        decoderInitializationException.decoderName)
            }
        }
        if (errorString != null) {
            Toast.makeText(applicationContext, errorString, Toast.LENGTH_LONG).show()
        }

        // Stream ended
        if (StringUtils.equals(e.message, Constants.STREAM_ENDED) || StringUtils.equals(e.message, Constants.STREAM_STOPPED)) {
            surface_error.visibility = VISIBLE
            surface_text.visibility = VISIBLE
            surface_progress.visibility = GONE
            surface_text.setText(R.string.stream_went_offline)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // Connection lost
        if (StringUtils.startsWith(e.message, Constants.STREAM_CONNECTION_LOST)) {
            Log.d("ExoPlayer onError", "Connection lost")
            surface_error.visibility = VISIBLE
            surface_text.visibility = VISIBLE
            surface_text.setText(R.string.stream_connection_lost)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        playerNeedsPrepare = true
    }

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int,
                                    pixelWidthAspectRatio: Float) {
        Log.d("onVideoSizeChanged", "width: $width height: $height unappliedRotationDegrees: " +
                "$unappliedRotationDegrees pixelWidthAspectRatio: $pixelWidthAspectRatio")
        if (!VoD) {
            val tmp = width.toDouble() / height.toDouble()
            val aspect = round(tmp, 2).toFloat()
            Log.d("Video size", width.toString() + " x " + height.toString())
            Log.d("Video size", pixelWidthAspectRatio.toString())
            Log.d("Video size", aspect.toString())
            videoSurface.setVideoWidthHeightRatio(aspect)
            aspectRatio.setAspectRatio(1.78f)
        } else {
            val tmp = width.toDouble() / height.toDouble()
            val aspect = round(tmp, 2).toFloat()
            Log.d("Video size", width.toString() + " x " + height.toString())
            Log.d("Video size", pixelWidthAspectRatio.toString())
            Log.d("Video size", aspect.toString())
            videoSurface.setVideoWidthHeightRatio(aspect)
        }
    }

    // User controls

    private fun haveTracks(type: Int): Boolean {
        return player != null && player!!.getTrackCount(type) > 0
    }

    private fun hasVideoTracks(): Boolean {
        return player != null && player!!.getTrackCount(Player.TYPE_VIDEO) > 1
    }

    fun showVideoPopup(v: View) {
        val popup: PopupMenu = object : PopupMenu(this, v) {
            override fun show() {
                window.decorView.systemUiVisibility = SYSTEM_UI_FLAG_LAYOUT_STABLE and
                        SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION and
                        SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN and
                        SYSTEM_UI_FLAG_HIDE_NAVIGATION and
                        SYSTEM_UI_FLAG_FULLSCREEN and
                        SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                super.show()
            }
        }
        configurePopupWithTracks(popup, null, Player.TYPE_VIDEO)
        popup.show()
    }

    fun showAudioPopup(v: View) {
        val popup = PopupMenu(this, v)
        val menu = popup.menu
        menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.enable_background_audio)
        val backgroundAudioItem = menu.findItem(0)
        backgroundAudioItem.isCheckable = true
        backgroundAudioItem.isChecked = enableBackgroundAudio
        val clickListener = OnMenuItemClickListener { item ->
            if (item === backgroundAudioItem) {
                enableBackgroundAudio = !item.isChecked
                return@OnMenuItemClickListener true
            }
            false
        }
        configurePopupWithTracks(popup, clickListener, Player.TYPE_AUDIO)
        popup.show()
    }

    fun showTextPopup(v: View) {
        val popup = PopupMenu(this, v)
        configurePopupWithTracks(popup, null, Player.TYPE_TEXT)
        popup.show()
    }

    fun showVerboseLogPopup(v: View) {
        val popup = PopupMenu(this, v)
        val menu = popup.menu
        menu.add(Menu.NONE, 0, Menu.NONE, R.string.logging_normal)
        menu.add(Menu.NONE, 1, Menu.NONE, R.string.logging_verbose)
        menu.setGroupCheckable(Menu.NONE, true, true)
        menu.findItem(if (VerboseLogUtil.areAllTagsEnabled()) 1 else 0).isChecked = true
        popup.setOnMenuItemClickListener { item ->
            if (item.itemId == 0) {
                VerboseLogUtil.setEnableAllTags(false)
            } else {
                VerboseLogUtil.setEnableAllTags(true)
            }
            true
        }
        popup.show()
    }

    private fun configurePopupWithTracks(popup: PopupMenu,
                                         customActionClickListener: OnMenuItemClickListener?,
                                         trackType: Int) {
        if (player == null) {
            return
        }
        val trackCount = player!!.getTrackCount(trackType)
        Log.d("trackCount Popup", trackCount.toString())
        if (trackCount == 0) {
            return
        }
        popup.setOnMenuItemClickListener { item -> customActionClickListener != null
                && customActionClickListener.onMenuItemClick(item)
                || onTrackItemClick(item, trackType) }
        val menu = popup.menu
        // ID_OFFSET ensures we avoid clashing with Menu.NONE (which equals 0).
        //menu.add(MENU_GROUP_TRACKS, NewPlayer.TRACK_DISABLED + ID_OFFSET, Menu.NONE, R.string.off);
        for (i in 0..trackCount - 1) {
            menu.add(MENU_GROUP_TRACKS, i + ID_OFFSET, Menu.NONE,
                    buildTrackName(player?.getTrackFormat(trackType, i) as MediaFormat))
        }
        menu.setGroupCheckable(MENU_GROUP_TRACKS, true, true)
        menu.findItem(player!!.getSelectedTrack(trackType) + ID_OFFSET).isChecked = true
    }

    private fun onTrackItemClick(item: MenuItem, type: Int): Boolean {
        if (player == null || item.groupId != MENU_GROUP_TRACKS) {
            return false
        }
        changeQuality(true, item.itemId - ID_OFFSET)
        Log.d("onTrackItemClick", (item.itemId - ID_OFFSET).toString())
        return true
    }

    // NewPlayer.CaptionListener implementation

    override fun onCues(cues: List<Cue>) {
    }

    // NewPlayer.MetadataListener implementation

    override fun onId3Metadata(id3Frames: List<Id3Frame>) {

    }

    // SurfaceHolder.Callback implementation

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (player != null) {
            player?.surface = holder.surface
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Do nothing.
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (player != null) {
            player?.blockingClearSurface()
        }
    }

    // Chat methods

    fun showChat() {
        if (chatContainer.visibility == GONE) {
            chatContainer.visibility = VISIBLE
        }
    }

    fun hideChat() {
        if (chatContainer.visibility == VISIBLE) {
            chatContainer.visibility = GONE
        }
    }

    fun hideIcons(isFullscreen : Boolean) {
        if (!VoD) {
            if (isFullscreen) {
                btn_viewer_list.visibility = GONE
                btn_info.visibility = GONE
                btn_favorite.visibility = GONE
                btn_quality.visibility = GONE
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            } else {
                btn_viewer_list.visibility = VISIBLE
                btn_info.visibility = VISIBLE
                btn_favorite.visibility = VISIBLE
                btn_quality.visibility = VISIBLE
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }
        }
    }

    // Overlay

    fun showOverlay(url: String) {
        if (url.contains("https://secure.twitch.tv")) {
            overlayWebView?.settings?.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        overlayWebView?.loadUrl(url)
        overlayWebView?.requestFocus(FOCUS_DOWN)

        overlayStream.visibility = VISIBLE
        overlayStream.startAnimation(show)
    }

    fun hideOverlay() {
        if (player != null) {
            player?.setMute(false)
        }
        overlayWebView?.loadUrl("about:blank")
        overlayWebView?.clearFocus()
        overlayStream.startAnimation(hide)
        overlayStream.visibility = GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            hideOverlay()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Callbacks

    /*

    inner class ViewerListCallback
    constructor(caller: Any, private val channel: String?) : TDBasicCallback<ViewerList>(caller) {

        override fun startRequest(): ViewerList {
            return TDServiceImpl.getInstance().getViewerList(channel)
        }

        override fun onResponse(response: ViewerList) {
            Log.d("ViewerListCallback onResponse", response.chatterCount.toString())
            var count = response.chatterCount.toString();
            var html = ""
            html += "<html>" +
                    "<head>" +
                    "<style>html{color: #8c8c9c; background: #25252A;}  " +
                    "img{ max-width:100%; height:auto; } " +
                    "a { color: #AB81F9; font-size: 100%; text-decoration: none; } .list-header { font-size: 18px; margin-top: 20px; } .item { color: #a68ed2; }" +
                    "</style>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0'>" +
                    "</head>" +
                    "<body>"
            // Check if the broadcaster is in chat
            if (response.chatters.moderators.contains(channel?.toLowerCase())) {
                Log.d("Broadcaster", channel)
                html += "<div class='list-header'>BROADCASTER</div>"
                html += "<div class='item'>${StringUtils.capitalize(channel)}</div>"
                response.chatters.moderators.remove(channel?.toLowerCase())
            }

            var headerMod = false
            var headerViewer = false

            for (user in response.chatters.moderators) {
                if (!headerMod) {
                    html += "<div class='list-header'>MODERATORS</div>"
                    headerMod = true
                }
                html += "<div class='item'>${StringUtils.capitalize(user)}</div>"
            }

            for (user in response.chatters.viewers) {
                if (!headerViewer) {
                    html += "<div class='list-header'>VIEWERS</div>"
                    headerViewer = true
                }
                html += "<div class='item'>${StringUtils.capitalize(user)}</div>"
            }
            html += "</body></html>"
            Log.d("ViewerList HTML", html);
            Handler(Looper.getMainLooper()).post {
                viewerCounter.text = count
                viewerListLoading.visibility = INVISIBLE
                viewerListReload.visibility = VISIBLE
                webViewViewerList.loadDataWithBaseURL(null, html, null, "text/html; charset=UTF-8", null)
            }
        }

        override fun onError(title: String?, message: String?) {
            super.onError(title, message)
            Log.d("ViewerListCallback onError", message)
        }

        override fun isAdded(): Boolean {
            return true
        }
    }

*/
    fun setChannelDescription() {
        TwitchAPI.getService()
                .getChannelPanels(channel.name()).enqueue(object : Callback<List<Panel>> {
            override fun onResponse(call: Call<List<Panel>>, response: Response<List<Panel>>) {
                val panels = response.body()
                var html = ""
                html += "<html>" +
                        "<head>" +
                        "<style>html{color: #8c8c9c; background: #25252A;}  " +
                        "img{ max-width:100%; height:auto; } " +
                        "a { color: #AB81F9; font-size: 90%; text-decoration: none; }" +
                        "h1 { font-size: 160%; }" +
                        "h2 { font-size: 140%; }" +
                        "h3 { font-size: 120%; }" +
                        "</style>" +
                        "<meta name='viewport' content='width=device-width, initial-scale=1.0," +
                        " maximum-scale=1.0, user-scalable=0'>" +
                        "</head>" +
                        "<body>"
                if (panels != null && panels.isNotEmpty()) {
                    for (element in panels) {
                        if (element.kind() == "default") {
                            if (element.data().link() != null) {
                                if (element.data().link() != "") {
                                    html += "<a target='_blank' href='" + element.data().link() + "'>"
                                }
                            }
                            if (element.data().image() != null) {
                                if (element.data().image() != "") {
                                    html += "<p><img src='" + element.data().image() + "'>"
                                }
                                if (element.data().link() != null) {
                                    if (element.data().link() != "") {
                                        html += "</a>"
                                    }
                                }
                            }
                            if (element.data().title() != null) {
                                if (element.data().title() != "") {
                                    html += "<p><h1>" + element.data().title() + "</h1></p>"
                                }
                            }
                            if (element.html_description() != null) {
                                if (element.html_description() != "") {
                                    html += "<p>" + element.html_description() + "</p>"
                                }
                            }
                        }
                    }
                }
                html += "</body></html>"

                Handler(Looper.getMainLooper()).post {
                    webviewDescription?.loadData(html, "text/html; charset=UTF-8", null)
                }
            }

            override fun onFailure(call: Call<List<Panel>>, t: Throwable) {

            }
        })

    }

    fun getStreamURI(channel: String, play: Boolean) {
        TwitchAPI.getService().getChannelToken(channel).enqueue(object : Callback<AccessToken> {
            override fun onResponse(call: Call<AccessToken>, response: Response<AccessToken>) {
                if (response.isSuccessful) {
                    val token = response.body()
                    val url = "https://usher.ttvnw.net/api/channel/hls/$channel.m3u8"
                    val uri = Uri.parse(url)
                            .buildUpon()
                            .appendQueryParameter("allow_audio_only", "false")
                            .appendQueryParameter("token", token.token())
                            .appendQueryParameter("sig", token.sig())
                            .appendQueryParameter("allow_source", "true")
                            .appendQueryParameter("allow_spectre", "true")
                            .appendQueryParameter("p", Random().nextInt(999999).toString())
                            .build()
                    contentUri = uri
                    if (play) {
                        Handler(Looper.getMainLooper()).post { preparePlayer(true) }
                    }
                }
            }

            override fun onFailure(call: Call<AccessToken>, t: Throwable) {

            }
        })
    }

    fun getHostStatus() {
        TwitchAPI.getService().getHostingStatus(Constants.URL_HOST + twitchID)
                .enqueue(object : Callback<Hosts> {
            override fun onResponse(call: Call<Hosts>?, response: Response<Hosts>) {
                if (response.code() == 200) {
                    val hosts = response.body().hosts()
                    if (hosts.isNotEmpty()) {
                        val host = hosts[0]
                        if (host.target_login() == null) {
                            Log.d("Hosting", login + " is not hosting anyone.")
                        } else {
                            Log.d("Hosting", login + " is hosting " + host.target_login())
                            currentlyHosting = host.target_login()
                            currentlyHostingDisplayName = host.target_display_name()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<Hosts>?, t: Throwable) {
                Log.d("Hosting", t.message)
            }
        })
    }

    fun formatHostString(): String {
        //getHostStatus()
        if (currentlyHosting != null) {
            if (channel.displayName() != null) {
                if (currentlyHostingDisplayName.equals(channel.displayName())) {
                    return "Unhost " + channel.displayName()
                }
            }
            if (currentlyHosting.equals(channel.name())) {
                return "Unhost " + channel.name()
            }
        }
        if (channel.displayName() != null) {
            return "Host " + channel.displayName()
        } else {
            return  "Host " + channel.name()
        }
    }

    internal var webViewClient: WebViewClient = object : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (!url.equals("https://www.twitch.tv/${channel.name()}/chat")) {
                if (url.startsWith("http:") || url.startsWith("https:")) {
                    Log.d("urlOverride", "url: " + url)
                    Log.d("urlOverride", "is YouTube: " + StringUtils.contains(url, "youtube.com"))
                    if (StringUtils.contains(url, "youtube.com") && player != null) {
                        player?.setMute(true)
                    }
                    showOverlay(url)
                    return true
                }
            }
            return false
        }

        override fun onLoadResource(view: WebView, url: String) {
            view.clearHistory()
            view.clearCache(true)
            super.onLoadResource(view, url)
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (loggedIn) {
                contentRelative?.requestFocus()
                //ffz._inputv.chatTextArea.blur()
                if (!scriptLoaded) {
                    val firstSetup = readAsset("firstSetup.js")
                    val twitchSetup = readAsset("twitchSetup.js")
                    val roomJS = readAsset("room.js")
                    val strimBagExtension = readAsset("strimBagExtension.js")
                    chat?.loadUrl("javascript:(function (){document.getElementsByTagName('head')[0].appendChild(document.createElement('script')).innerHTML=\"$firstSetup\";}());")
                    chat?.loadUrl("javascript:(function (){document.getElementsByTagName('head')[0].appendChild(document.createElement('script')).innerHTML=\"$twitchSetup\";}());")
                    chat?.loadUrl("javascript:(function (){document.getElementsByTagName('head')[0].appendChild(document.createElement('script')).innerHTML=\"$roomJS\";}());")
                    chat?.loadUrl("javascript:(function (){document.getElementsByTagName('head')[0].appendChild(document.createElement('script')).innerHTML=\"$strimBagExtension\";}());")
                    chat?.loadUrl("javascript:{var metaTag=document.createElement('meta'); metaTag.name = 'viewport'; metaTag.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0'; document.getElementsByTagName('head')[0].appendChild(metaTag);}")
                    if (debug) {
                        val link = "javascript:(function (){document.getElementsByTagName('head')[0].appendChild(document.createElement('script')).src='http://$debugAddress/script/script.min.js';}());"
                        Log.d("debug", link)
                        chat?.loadUrl(link)
                    } else {
                        val date = Calendar.getInstance().timeInMillis
                        Log.d("date", "time in milli: " + date)
                        chat?.loadUrl("javascript:(function (){document.getElementsByTagName('head')[0].appendChild(document.createElement('script')).src='//cdn.frankerfacez.com/script/script.min.js?_=$date';}());")
                        chat?.loadUrl("javascript:(function (){document.getElementsByTagName('head')[0].appendChild(document.createElement('script')).src='//cdn.lordmau5.com/inject.js';}());")
                    }
                    scriptLoaded = true
                    view.clearHistory()
                    view.clearCache(true)


                }
                //chat?.loadUrl("javascript:(function (){setupChat('$username', '$oauth', '$channel')}());")
                //chat?.loadUrl("javascript:(function (){connectChat()}());")
            } else {
                println("WebView URL is: " + url)
                if (url.equals("http://www.twitch.tv/", ignoreCase = true) || url.equals("https://www.twitch.tv/", ignoreCase = true)) {
                    loggedIn = true
                    view.loadUrl("https://www.twitch.tv/${channel.name()}/chat")
                    //view.loadUrl("file:///android_asset/chat/index.html")
                }
            }
            super.onPageFinished(view, url)
        }
    }

    fun readAsset(file : String): String {
        val input: InputStream
        var text = ""
        try {
            input = assets.open(file)
            val size = input.available()
            val buffer = ByteArray(size)
            input.read(buffer)
            input.close()
            text = String(buffer)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return text
    }

    fun openSubscribeSite() {
        showOverlay("https://secure.twitch.tv/products/${channel.name()}/ticket/initiate/xsolla_v3")
    }

    fun shareChannel() {
        // Since returning from this intent triggers the onResume function, we need to block the
        // renewal process, else the player would try to initialize a second time and the user would
        // see an error message
        blockPlayer = true
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_TEXT, "https://www.twitch.tv/${channel.name()}")
        intent.type = "text/plain"
        startActivity(Intent.createChooser(intent, "Share Channel with..."));
    }

    internal var UserFollow: OnClickListener = OnClickListener { /*followUser()*/ }

    internal var UserUnfollow: OnClickListener = OnClickListener { /*unfollowUser()*/ }

    fun toast(text : String) {
        Toast.makeText(this@PlayerActivity, text, Toast.LENGTH_SHORT).show()
    }

    fun toast(id : Int) {
        Toast.makeText(this@PlayerActivity, id, Toast.LENGTH_SHORT).show()
    }

    fun toast(id : Int, text : String) {
        Toast.makeText(this@PlayerActivity, String.format(resources.getString(id), text),
                Toast.LENGTH_SHORT).show()
    }

    // JavaScript Interface

    inner class strimBagInterface {

        @JavascriptInterface
        fun channel(): String {
            return StrimBagZApplication.currentChat
        }

        @JavascriptInterface
        fun oldChannel(): String {
            return StrimBagZApplication.previousChat
        }

        @JavascriptInterface
        fun checkIfDisconnected() {
            chat?.post { chat?.loadUrl("javascript:(function (){}(strimBagInterface.checkConnection(TMI._sessions[0]._connections.main.isConnected)));") }
        }

        @JavascriptInterface
        fun checkConnection(connected: Boolean) {
            chat?.post {
                if (!connected) {
                    scriptLoaded = false
                    chat?.reload()
                }
            }
        }

        @JavascriptInterface
        fun getRooms(array: Array<String>, ids: Array<String>) {
            Handler(Looper.getMainLooper()).post {
                changeChatRoomSheet(array, ids)
            }
        }

        @JavascriptInterface
        fun followData(data: Array<String>) {
            for (name in data) {
                val n = name + ""
                chat?.post { chat?.loadUrl("javascript:console.log('[StrimBagInterface] Channel name: $n')") }
                Toast.makeText(StrimBagZApplication.getContext(), "[StrimBagInterface] Channel name: " + name, Toast.LENGTH_SHORT).show()
                Log.d("FollowData", name)
            }
        }

        @JavascriptInterface
        fun changeChannel(old: String, c: String) {
            chat?.post { chat?.loadUrl("javascript:(function (){" +
                    "var controller = App.__container__.lookup('controller:chat');" +
                    " var Room = App.__deprecatedInstance__.registry.resolve('model:room');" +
                    " controller.focusRoom(Room.findOne('$c'));" +
                    " r = ffz.rooms['$old'].room;" +
                    " r.destroy();" +
                    " delete r;" +
                    "}());") }
        }
    }

    companion object {

        private val TAG = "PlayerActivity"
        private val MENU_GROUP_TRACKS = 1
        private val ID_OFFSET = 2

        private val defaultCookieManager: CookieManager

        init {
            defaultCookieManager = CookieManager()
            defaultCookieManager.setCookiePolicy(java.net.CookiePolicy.ACCEPT_ORIGINAL_SERVER)
        }

        fun round(value: Double, places: Int): Double {
            var value = value
            if (places < 0) throw IllegalArgumentException()
            val factor = Math.pow(10.0, places.toDouble()).toLong()
            value *= factor
            val tmp = Math.round(value)
            return tmp.toDouble() / factor
        }

        private fun buildTrackName(format: MediaFormat): String {
            if (format.adaptive) {
                return "Auto"
            }

            Log.d("Track ID", format.trackId)

            var trackName = ""
            if (MimeTypes.isVideo(format.mimeType)) {
                when (format.trackId) {
                    "0" -> trackName = joinWithSeparator("Source", buildBitrateString(format))
                    "1" -> trackName = joinWithSeparator("High", buildBitrateString(format))
                    "2" -> trackName = joinWithSeparator("Medium", buildBitrateString(format))
                    "3" -> trackName = joinWithSeparator("Low", buildBitrateString(format))
                    "4" -> trackName = joinWithSeparator("Mobile", buildBitrateString(format))
                }
            }
            /*
        if (MimeTypes.isVideo(format.mimeType)) {
            trackName = joinWithSeparator(joinWithSeparator(buildResolutionString(format),
                    buildBitrateString(format)), buildTrackIdString(format));
        } else if (MimeTypes.isAudio(format.mimeType)) {
            trackName = joinWithSeparator(joinWithSeparator(joinWithSeparator(buildLanguageString(format),
                    buildAudioPropertyString(format)), buildBitrateString(format)),
                    buildTrackIdString(format));
        } else {
            trackName = joinWithSeparator(joinWithSeparator(buildLanguageString(format),
                    buildBitrateString(format)), buildTrackIdString(format));
        }
        */
            return if (trackName.length == 0) "unknown" else trackName
        }

        private fun buildResolutionString(format: MediaFormat): String {
            return if (format.width == MediaFormat.NO_VALUE || format.height == MediaFormat.NO_VALUE)
                ""
            else
                format.width.toString() + "x" + format.height.toString()
        }

        private fun buildAudioPropertyString(format: MediaFormat): String {
            return if (format.channelCount == MediaFormat.NO_VALUE || format.sampleRate == MediaFormat.NO_VALUE)
                ""
            else
                format.channelCount.toString() + "ch, " + format.sampleRate.toString() + "Hz"
        }

        private fun buildLanguageString(format: MediaFormat): String {
            return if (TextUtils.isEmpty(format.language) || "und" == format.language)
                ""
            else
                format.language
        }

        private fun buildBitrateString(format: MediaFormat): String {
            return if (format.bitrate == MediaFormat.NO_VALUE)
                ""
            else
                String.format(Locale.US, "%.2fMbit", format.bitrate / 1000000f)
        }

        private fun joinWithSeparator(first: String, second: String): String {
            return if (first.length == 0) second else if (second.length == 0) first else first + ", " + second
        }

        private fun buildTrackIdString(format: MediaFormat): String {
            return if (format.trackId == null) "" else " (" + format.trackId + ")"
        }

    }

    // New Follow stuff

    private fun getServers(): Array<String> {
        return arrayOf(
                "catbag.frankerfacez.com",
                "andknuckles.frankerfacez.com",
                "tuturu.frankerfacez.com"
        )
    }

    fun connectWebSocket() {
        val servers = getServers()
        val server = servers[ThreadLocalRandom.current().nextInt(servers.size)]
        Log.d("WebSocket", server)
        try {
            ws = WebSocketFactory().createSocket("wss://$server/", 5000)
                    .setMaxPayloadSize(2048)
                    .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (ws != null) {
            ws!!.addListener(object : WebSocketAdapter() {
                @Throws(Exception::class)
                override fun onConnected(websocket: WebSocket, headers: Map<String, List<String>>?) {
                    super.onConnected(websocket, headers)
                    Log.d("WebSocket", "Connected.")
                    wsAttempt = 1
                    sendHelloMessage(websocket)
                    sendWsMessage(websocket, "sub", "room.${channel.name()}")
                    sendWsMessage(websocket, "sub", "channel.${channel.name()}")
                    //sendWsMessage(websocket, "sub", "room.luigitus")
                    //sendWsMessage(websocket, "sub", "channel.luigitus")
                    //sendWsMessageArray(websocket, "update_follow_buttons", "[\"${username?.toLowerCase()}\",[\"unlink2\"]]")
                }

                @Throws(Exception::class)
                override fun onError(websocket: WebSocket?, cause: WebSocketException) {
                    super.onError(websocket, cause)
                    Log.d("WebSocekt", "onError")
                    Log.d("WebSocket", cause.message)
                }

                @Throws(Exception::class)
                override fun onUnexpectedError(websocket: WebSocket?, cause: WebSocketException?) {
                    super.onUnexpectedError(websocket, cause)
                    Log.d("WebSocket", "onUnexpectedError")
                    Log.d("WebSocket", cause!!.message)
                }

                @Throws(Exception::class)
                override fun onTextMessage(websocket: WebSocket, text: String) {
                    super.onTextMessage(websocket, text)
                    var text = text
                    Log.d("onText", text)
                    if (text.startsWith("-1")) {
                        if (text.contains("follow_sets")) {
                            //Log.d("FFZ", "follow_sets")
                        } else if (text.contains("follow_buttons")) {
                            Log.d("FFZ", "follow_buttons")
                            text = text.substring(text.indexOf("{"))
                            val obj = JSONObject(text)
                            val channels = obj.getJSONArray(channel.name())
                            //val channels = obj.getJSONArray("luigitus")
                            runOnUiThread {
                                Snackbar.make(chatContainer, "Follow Buttons updated", Snackbar.LENGTH_LONG).show()
                                getDisplayName(websocket, channels)
                            }
                        } else if (text.contains("srl_race")) {
                            Log.d("FFZ", "srl_race")
                            handelSRL(text)
                        } else if (text.contains("do_authorize")) {
                            Log.d("FFZ", "do_authorize")
                            Log.d("FFZ", text)
                            doAuthorize()
                        }
                        return
                    }
                    if (awaitAnswers.isEmpty()) {
                        return
                    }
                    for ((k,v) in awaitAnswers) {
                        if (text.startsWith(k.toString())) {
                            text = text.substring(text.indexOf("\""))
                            text = text.replace("\"", "")
                            receivedAnswers.put(text, v)
                            awaitAnswers.remove(k)
                        }
                    }

                    /*
                    for (item: Int in awaitAnswers) {
                        if (text.startsWith(item.toString())) {
                            text = text.substring(text.indexOf("\""))
                            text = text.replace("\"", "")
                            Log.d("get_display_name", text)
                            receivedAnswers.add(text)
                            awaitAnswers.remove(item)
                        }
                    }
                    */
                    if (receivedAnswers.isNotEmpty() && awaitAnswers.isEmpty()) {
                        runOnUiThread {
                            setFollowButtons(receivedAnswers)
                            receivedAnswers.clear()
                        }
                    }
                }

                @Throws(Exception::class)
                override fun onMessageError(websocket: WebSocket?, cause: WebSocketException?, frames: List<WebSocketFrame>?) {
                    super.onMessageError(websocket, cause, frames)
                    Log.d("WebSocket", "onMsgError")
                    Log.d("WebSocket", cause!!.message)
                }

                @Throws(Exception::class)
                override fun onConnectError(websocket: WebSocket?, exception: WebSocketException?) {
                    super.onConnectError(websocket, exception)
                    Log.d("WebSocket", "onConnectError")
                    counterFFZ = 1
                    reconnect()
                }

                @Throws(Exception::class)
                override fun onDisconnected(websocket: WebSocket?, serverCloseFrame: WebSocketFrame?, clientCloseFrame: WebSocketFrame?, closedByServer: Boolean) {
                    super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer)
                    Log.d("WebSocket", "Disconnected.")
                    counterFFZ = 1
                    reconnect()
                }
            })
        }
        if (ws != null) {
            ws!!.connectAsynchronously()
        }
    }

    private fun reconnect() {
        wsAttempt++
        if (wsAttempt > 5) {
            Log.d("WebSocket", "Exceeded connection attempts, abort.")
            return
        }
        val timer = Timer("schedule", true)
        val delay = ((wsAttempt * 8) * 1000).toLong()
        timer.schedule(delay) {
            connectWebSocket()
        }
        Log.d("WebSocket", "Exceeded connection attempts, abort.")
    }

    private fun getDisplayName(webSocket: WebSocket, channels: JSONArray?) {
        if (channels != null) {
            for (i in 0..channels.length() - 1) {
                val tmp = channels.get(i).toString()
                awaitAnswers.put(counterFFZ, tmp)
                webSocket.sendText(counterFFZ++.toString() + " get_display_name \"$tmp\"")
            }
        }
    }

    private fun sendHelloMessage(webSocket: WebSocket) {
        webSocket.sendText(counterFFZ++.toString() + " " + "hello [\"strimbagz_1.3.0\",false]")
        webSocket.sendText(counterFFZ++.toString() + " " + "setuser \"${login}\"")
        webSocket.sendText(counterFFZ++.toString() + " " + "ready 0")
    }

    private fun sendWsMessage(webSocket: WebSocket, command: String, data: String) {
        webSocket.sendText(counterFFZ++.toString() + " " + "$command \"$data\"")
    }

    private fun sendWsMessageArray(webSocket: WebSocket, command: String, data: String) {
        webSocket.sendText(counterFFZ++.toString() + " " + "$command $data")
    }

    private fun doAuthorize() {

    }

    private fun handelSRL(obj: String) {
        val arr = JSONArray(obj.substring(obj.indexOf("[")))
        val channels = arr.getJSONArray(0)
        val data = arr.getJSONObject(1)

        if (data == null) {
            btn_srl.visibility = GONE
            return
        }

        val entrants: JSONObject? = data.getJSONObject("entrants")
        val keys = entrants?.keys() ?: return
        val jsonAdapter = SRLRaceEntrant.jsonAdapter(Moshi.Builder().build())
        raceEntrants?.clear()
        while (keys.hasNext()) {
            val key = keys.next()
            val tmp: Any? = entrants?.get(key)
            if (tmp != null && tmp is JSONObject) {
                raceEntrants?.add(jsonAdapter.fromJson(tmp.toString()))
                //Log.d("srl entrant", "Channel: " + getStringFromJSONObject("channel", tmp) + " DisplayName: " + getStringFromJSONObject("display_name", tmp) + " State: " + getStringFromJSONObject("state", tmp))
            }
        }
        Log.d("srl entrants", raceEntrants.toString())

        btn_srl.visibility = VISIBLE
        btn_srl.setOnClickListener {

        }

        val state = data.get("state")
        when (state) {
            "open" -> Log.d("FFZ srl_race state", "Open")
            "progressing" -> Log.d("FFZ srl_race state", "Progressing")
            "done" -> Log.d("FFZ srl_race state", "Done")
            else -> Log.d("FFZ srl_race state", "Unknown")
        }
    }

    private fun getStringFromJSONObject(query: String, json: JSONObject): String? {
        val tmp: String?
        tmp = try {
            json.getString(query)
        } catch (e: JSONException) {
            null
        }
        //Calendar.getInstance().timeInMillis
        return tmp
    }

    private fun setFollowButtons(channels: HashMap<String, String>?) {
        followButtons.clear()
        followButtonsMap.clear()
        checkFollow(channel.name(), channel.displayName())
        if (channels != null) {
            for ((k,v) in channels) {
                followButtonsMap.put(k, v)
                checkFollow(v, k)
            }
        }
        btn_favorite.setOnClickListener {
            val pop = android.support.v7.widget.PopupMenu(this@PlayerActivity, btn_favorite)
            pop.menu.add(followChannel)
            for (i in followButtons.indices) {
                pop.menu.add(followButtons[i])
            }
            pop.setOnMenuItemClickListener { item ->
                val title = item.title.toString()
                if (title.startsWith("Follow")) {
                    val target = title.substring(title.indexOf(" ") + 1)
                    // Follow user action
                    followChannel(followButtonsMap[target], target)
                } else if (title.startsWith("Unfollow")) {
                    val target = title.substring(title.indexOf(" ") + 1)
                    // Unfollow user action
                    unfollowChannel(followButtonsMap[target], target)
                }
                false
            }

            pop.show()
        }
    }

    private fun followChannel(target: String?, displayName: String?) {
        if (target == null || login == Constants.NO_USER || oauth == Constants.NO_TOKEN) {
            return
        }
        TwitchKraken.getService().followChannel(login, target)
                .enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.code() == 200) {
                    Log.d("followChannel", "Now following target: " + target)
                } else {
                    Log.d("followChannel", "Error following target: " + target)
                    runOnUiThread {
                        toast("Couldn't follow " + (displayName ?: target) + ", try again in a few.")
                    }
                    return
                }
                runOnUiThread {
                    setFollowStatus(target, displayName, true, true)
                    toast("You're now following " + (displayName ?: target))
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                runOnUiThread {
                    toast("Couldn't follow " + (displayName ?: target) + ", try again in a few.")
                }
            }
        })
    }

    private fun unfollowChannel(target: String?, displayName: String?) {
        if (target == null || login == Constants.NO_USER || oauth == Constants.NO_TOKEN) {
            return
        }
        TwitchKraken.getService().unfollowChannel(login, target)
                .enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.code() == 204) {
                    Log.d("unfollowChannel", "Unfollowed target: " + target)
                } else {
                    Log.d("unfollowChannel", "Error Unfollowing target: " + target)
                    runOnUiThread {
                        toast("Couldn't unfollow " + (displayName ?: target) + ", try again in a few.")
                    }
                    return
                }
                runOnUiThread {
                    setFollowStatus(target, displayName, false, true)
                    toast("You're not following " + (displayName ?: target) + " anymore.")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                runOnUiThread {
                    toast("Couldn't unfollow " + (displayName ?: target) + ", try again in a few.")
                }
            }
        })
    }

    private fun checkFollow(target: String?, displayName: String?) {
        if (target == null || login == Constants.NO_USER) {
            return
        }
        TwitchKraken.getService().checkFollow(login, target)
                .enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                var follows = false
                if (response.code() == 200) {
                    Log.d("checkFollow", "Follows target: " + target)
                    follows = true
                }
                if (response.code() == 404) {
                    Log.d("checkFollow", "Not following the target: " + target)
                    follows = false
                }
                runOnUiThread { setFollowStatus(target, displayName, follows, false) }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {

            }
        })
    }

    private fun checkProduct() {
        if (channel.name() == "") {
            return
        }
        TwitchAPI.getService().checkProduct(channel.name()).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.code() == 200) {
                    productExists = true
                    Log.d("checkProduct", "product exists, subscribe button active")
                } else {
                    productExists = false
                    Log.d("checkProduct", "product doesn't exist, subscribe button inactive")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {

            }

        })
    }

    private fun setFollowStatus(ch: String, displayName: String?, follows: Boolean, replace: Boolean) {
        if (follows) {
            if (channel.name() == ch) {
                followChannel = "Unfollow " + (displayName ?: ch)
                return
            }
            if (replace) {
                followButtons.remove("Follow " + (displayName ?: ch))
            }
            followButtons.add("Unfollow " + (displayName ?: ch))
        } else {
            if (channel.name() == ch) {
                followChannel = "Follow " + (displayName ?: ch)
                return
            }
            if (replace) {
                followButtons.remove("Unfollow " + (displayName ?: ch))
            }
            followButtons.add("Follow " + (displayName ?: ch))
        }
    }

    val srlJson = "[[\"tob3000\",\"sva16162\",\"makko9143\",\"spamminn\",\"fig02\",\"alaris_villain\",\"psymarth\",\"moosecrap\",\"flanthis\",\"phoenixfeather1\",\"cma2819\",\"mikekatz45\",\"sniping117\",\"exodus122\",\"mrjabujabu\"],{\"entrants\":{\"alaris_villain\":{\"channel\":\"alaris_villain\",\"comment\":\"row3\",\"display_name\":\"alaris_villain\",\"place\":10,\"state\":\"done\",\"time\":5388},\"cma\":{\"channel\":\"cma2819\",\"comment\":\"row3\",\"display_name\":\"Cma\",\"place\":8,\"state\":\"done\",\"time\":5064},\"exodus\":{\"channel\":\"exodus122\",\"comment\":\"col 2\",\"display_name\":\"Exodus\",\"place\":2,\"state\":\"done\",\"time\":4611},\"fig02\":{\"channel\":\"fig02\",\"display_name\":\"fig02\",\"place\":12,\"state\":\"done\",\"time\":6805},\"flanthis\":{\"channel\":\"flanthis\",\"comment\":\"bltr omg this bingo was trash\",\"display_name\":\"flanthis\",\"place\":11,\"state\":\"done\",\"time\":5685},\"makko\":{\"channel\":\"makko9143\",\"comment\":\"col1 blame blank b\",\"display_name\":\"makko\",\"place\":7,\"state\":\"done\",\"time\":5037},\"marthur\":{\"comment\":\"row2 i dont know how to get to spirit so i just quit :P\",\"display_name\":\"marthur\",\"state\":\"forfeit\"},\"mikekatz45\":{\"channel\":\"mikekatz45\",\"comment\":\"col2\",\"display_name\":\"MikeKatz45\",\"place\":1,\"state\":\"done\",\"time\":4598},\"moosecrap\":{\"channel\":\"moosecrap\",\"comment\":\"c4 routing mistakes\",\"display_name\":\"moosecrap\",\"place\":13,\"state\":\"done\",\"time\":8626},\"mrjabujabu\":{\"channel\":\"mrjabujabu\",\"display_name\":\"Mrjabujabu\",\"place\":14,\"state\":\"done\",\"time\":8741},\"niamek\":{\"comment\":\"MRJABUJABU I will redeem myself eventually. Prepare yourself!\",\"display_name\":\"niamek\",\"state\":\"forfeit\"},\"phoenixfeather\":{\"channel\":\"phoenixfeather1\",\"comment\":\"row 4\",\"display_name\":\"PhoenixFeather\",\"place\":4,\"state\":\"done\",\"time\":4834},\"psymarth\":{\"channel\":\"psymarth\",\"comment\":\"row 4; mostly child\",\"display_name\":\"PsyMarth\",\"place\":5,\"state\":\"done\",\"time\":4959},\"sniping117\":{\"channel\":\"sniping117\",\"comment\":\"tl-br, so many hearts but mine is broken\",\"display_name\":\"SNIPING117\",\"place\":9,\"state\":\"done\",\"time\":5357},\"spamminn\":{\"channel\":\"spamminn\",\"display_name\":\"spamminn\",\"place\":15,\"state\":\"done\",\"time\":9263},\"sva\":{\"channel\":\"sva16162\",\"comment\":\"col 1 no hover boots for beat deku and water, that was scary\",\"display_name\":\"sva\",\"place\":3,\"state\":\"done\",\"time\":4743},\"tob3000\":{\"channel\":\"tob3000\",\"comment\":\"row3, bad\",\"display_name\":\"tob3000\",\"place\":6,\"state\":\"done\",\"time\":5034}},\"filename\":\"true\",\"game\":\"The Legend of Zelda: Ocarina of Time\",\"goal\":\"http://www.speedrunslive.com/tools/oot-bingo?mode=normal\\u0026amp;seed=455884\",\"id\":\"nid68\",\"state\":\"done\",\"time\":1.475977149e+09}]"

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("channel", channel)
        outState.putBoolean("chatOnly", chatOnly)
        outState.putBoolean("VoD", VoD)
    }
}
