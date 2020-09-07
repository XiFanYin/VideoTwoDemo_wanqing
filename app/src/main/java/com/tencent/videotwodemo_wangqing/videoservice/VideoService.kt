package com.tencent.videotwodemo_wangqing.videoservice

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.gson.Gson
import com.tencent.videotwodemo_wangqing.R
import com.tencent.videotwodemo_wangqing.app.App
import com.tencent.videotwodemo_wangqing.bean.ServicePullData
import com.tencent.videotwodemo_wangqing.dialog.AlertDialog
import com.tencent.videotwodemo_wangqing.bean.SocketUser
import com.tencent.videotwodemo_wangqing.utils.FloatingWindowHelper
import com.tencent.videotwodemo_wangqing.utils.MediaHelper
import com.tencent.videotwodemo_wangqing.utils.dp2px
import com.tencent.videotwodemo_wangqing.videoactivity.VideoActivity
import com.tencent.videotwodemo_wangqing.videoutil.IVideo
import com.tencent.videotwodemo_wangqing.videoutil.VideoError
import com.tencent.videotwodemo_wangqing.videoutil.VideoManager
import com.tencent.videotwodemo_wangqing.websocket.WsManager
import com.tencent.videotwodemo_wangqing.websocket.listener.WsStatusListener
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.ByteString
import java.util.*
import java.util.concurrent.TimeUnit

class VideoService : Service(), IVideo {

    companion object {
        //标记服务是否开启
        var isStart = false

        //静音按钮
        var audioState = false

        //websocket管理者
        var wsManager: WsManager? = null
    }

    //视频管理者
    lateinit var videoManager: VideoManager

    //打气筒对象
    lateinit var layoutInflater: LayoutInflater

    //本地视频回调
    lateinit var mLocalSurfaceView: (SurfaceView?) -> Unit

    //远程视频回调
    lateinit var mRemoteSurfaceView: (LinkedList<Pair<Int, SurfaceView>>) -> Unit


    //创建Handler，去发送心跳消息，底层处理的心跳消息没法用
    private val mHandler  = object :Handler(){
        override fun handleMessage(msg: Message) {
            if (msg.what==100){
                wsManager?.sendMessage("心跳内容")
                sendEmptyMessageDelayed(100,1000)
            }
        }
    }



    private lateinit var user: SocketUser
    private lateinit var gson: Gson

    private var call: AlertDialog? = null

    override fun onCreate() {
        super.onCreate()
        isStart = true
        //创建视频管理者
        videoManager = VideoManager(this)
        //获取打气筒对象
        layoutInflater = LayoutInflater.from(this@VideoService)
        //创建socket连接对象
        user = SocketUser("333333", "郑焕奇")
        gson = Gson()

    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //连接后台服务
        connectService()
        return START_REDELIVER_INTENT
    }


    /**
     * 连接后台服务器，建立长连接
     */
    private fun connectService() {
        //创建websocket管理者
        wsManager = WsManager.Builder(App.ApplicationINSTANCE)
            .client(
                OkHttpClient().newBuilder()
//                    .pingInterval(10, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build()
            )
            .needReconnect(true)
            .wsUrl("ws://192.168.1.196:9326?user=" + gson.toJson(user))
            .build()
        //设置监听
        wsManager?.setWsStatusListener(wsStatusListener)
        //开始连接
        wsManager?.startConnect()
    }


    //websocket监听
    private val wsStatusListener: WsStatusListener = object : WsStatusListener() {
        override fun onOpen(response: Response) {
            Log.e("rrrrrrr","onOpen")
            //连接后台成功
            mHandler.sendEmptyMessageDelayed(100,10000)
        }

        override fun onMessage(text: String) {
            Log.e("rrrrrrr",text)
         val data=  gson.fromJson<ServicePullData>(text, ServicePullData::class.java)
            //如果正在通话，就告诉服务器，当前人正在通话
            if (videoManager.isCalling) {
            } else {
                //播放音乐
                MediaHelper.playSound(assets.openFd("lingsheng.aac"))
                showCallDialog(data)
            }


        }

        override fun onMessage(bytes: ByteString) {}
        override fun onReconnect() {
            Log.e("rrrrrrr", "WsManager-----onReconnect")

        }
        override fun onClosing(code: Int, reason: String) {
            Log.e("rrrrrrr", "WsManager-----onClosing")
        }

        override fun onClosed(code: Int, reason: String) {
            Log.e("rrrrrrr", "WsManager-----onClosed")
        }

        override fun onFailure(t: Throwable, response: Response?) {
            Log.e("rrrrrrr","onFailure")
            //正在连接后台
            mHandler.removeCallbacksAndMessages(null)
        }
    }


    //显示被呼叫的dialog
    private fun showCallDialog(data: ServicePullData) {
        call?.dismiss()
        call = AlertDialog.Builder(this)
            .setContentView(R.layout.dialog_call)
            .setWidthAndHeight(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            .setText(R.id.userName,data.username)
            .setOnClickListener(R.id.endcall,{
                //停止播放音乐
                MediaHelper.stop()
                call?.dismiss()
            })
            .setOnClickListener(R.id.call,{
                //停止播放音乐
                MediaHelper.stop()
                call?.dismiss()
                //跳转到通话页面
                val intent = Intent(this,VideoActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)

            })
            .setCancelable(false)
            .fromBottom(true)
            .setBackgroundTransparence(1f)
            .setback(false)
            .showSystem()
    }


    //本地视频渲染成功回调
    override fun LocalSurfaceView(mlocalSurfaceView: SurfaceView?) {
        mLocalSurfaceView.invoke(mlocalSurfaceView)
    }

    //远端视频渲染
    override fun onRemoteChanged(mRemoteSurfaceView: LinkedList<Pair<Int, SurfaceView>>) {
        this@VideoService.mRemoteSurfaceView.invoke(mRemoteSurfaceView)
    }





    //视频发生错误
    override fun onError(error: VideoError) {

    }


    //创建binder对象
    override fun onBind(intent: Intent?): IBinder? {
        return MyBinder()
    }


    inner class MyBinder : Binder() {

        lateinit var mFloatingWindowHelper: FloatingWindowHelper

        lateinit var layout_float: FrameLayout

        //初始化
        fun initVideo(
            uid: Int,
            localSurfaceView: (SurfaceView?) -> Unit,
            mRemoteSurfaceView: (LinkedList<Pair<Int, SurfaceView>>) -> Unit
        ) {
            this@VideoService.mLocalSurfaceView = localSurfaceView
            this@VideoService.mRemoteSurfaceView = mRemoteSurfaceView
            videoManager.initVideo(uid)
        }

        //加入房间
        fun joinChannel(
            channelName: String,
            token: String? = null,
            optionalInfo: String = "",
            optionalUid: Int = 0
        ) {
            videoManager.joinChannel(channelName, token, optionalInfo, optionalUid)
        }

        //静音
        fun setAudioState(state: Boolean) {
            videoManager.setAudioState(state)
        }

        //切换摄像头
        fun switchCamera() {
            videoManager.switchCamera()
        }

        //切换大屏幕和小屏幕显示
        fun switchBigContainerShow(position: Int) {
            videoManager.switchBigContainerShow(position)
        }

        //开启悬浮窗
        fun showFloatWindow() {
            //创建悬浮窗帮助类
            mFloatingWindowHelper = FloatingWindowHelper(this@VideoService)
            //获取悬浮窗的父类
            val patient = (videoManager.localSurfaceView!!.second.parent as ViewGroup)
            //打入布局
            layout_float =
                layoutInflater.inflate(R.layout.float_layout, patient, false) as FrameLayout
            //移除原有的挂载
            patient.removeAllViews()
            //添加新的挂载
            val lp = FrameLayout.LayoutParams(dp2px(192F), dp2px(108F))
            lp.setMargins(10, 10, 10, 10)
            layout_float.addView(videoManager.localSurfaceView?.second, 0, lp)
            //展示悬浮窗
            mFloatingWindowHelper.addView(layout_float, true)

            //悬浮窗点击，去打开Activity
            layout_float.setOnClickListener {
                layout_float.removeAllViews()
                val eee = Intent(this@VideoService, VideoActivity::class.java)
                eee.putExtra("from", "Service")
                startActivity(eee)
            }


        }
        //关闭悬浮窗

        fun dismassFloatWindow(
            localSurfaceView: (SurfaceView?) -> Unit,
            mRemoteSurfaceView: (LinkedList<Pair<Int, SurfaceView>>) -> Unit
        ) {
            this@VideoService.mLocalSurfaceView = localSurfaceView
            this@VideoService.mRemoteSurfaceView = mRemoteSurfaceView
            mFloatingWindowHelper.clear()
            mFloatingWindowHelper.destroy()
            videoManager.localSurfaceView!!.second.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            this@VideoService.mLocalSurfaceView.invoke(videoManager.localSurfaceView!!.second)
            this@VideoService.mRemoteSurfaceView.invoke(videoManager.mSurfaceView)
        }

        //离开频道
        fun leaveChannel(){
            videoManager.leaveChannel()
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        isStart = false
    }


}