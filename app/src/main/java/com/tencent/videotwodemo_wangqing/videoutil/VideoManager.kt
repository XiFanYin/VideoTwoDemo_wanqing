package com.tencent.videotwodemo_wangqing.videoutil

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.SurfaceView
import android.view.ViewGroup
import com.tencent.videotwodemo_wangqing.R
import com.tencent.videotwodemo_wangqing.app.App
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.CameraCapturerConfiguration
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import java.util.*

/**
 * 操作视频封装的工具类
 */
class VideoManager(val Ivideo: IVideo) {

    private var mRtcEngine: RtcEngine? = null
    private var handler: Handler
    val mSurfaceView = LinkedList<Pair<Int, SurfaceView>>()


    var localSurfaceView: Pair<Int, SurfaceView>? = null

    //是否正在通话中
     var isCalling = false


    init {
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                val code = msg.what
                val uid = msg.obj as Int
                when (code) {
                    VideoEventCode.REMOTEENTER -> {//进入频道
                        //如果当前用户已经存在，就不添加
                        if (mSurfaceView.any { it.first == uid }) {
                            mSurfaceView.removeIf { t ->  t.first==uid }
                        }
                        val mRemoteSurfaceView =
                            RtcEngine.CreateRendererView(App.ApplicationINSTANCE)
                        mRemoteSurfaceView.setZOrderMediaOverlay(true)
                        mSurfaceView.add(Pair(uid, mRemoteSurfaceView))
                        //设置远程用户加入
                        Ivideo.onRemoteChanged(mSurfaceView)
                        mRtcEngine!!.setupRemoteVideo(
                            VideoCanvas(
                                mRemoteSurfaceView,
                                VideoCanvas.RENDER_MODE_FIT,
                                uid
                            )
                        )
                    }
                    VideoEventCode.REMOTELEAVE -> { //挂断视频
                        if (mSurfaceView.any { it.first == uid }) {
                            mSurfaceView.remove(mSurfaceView.find { it.first == uid })
                            Ivideo.onRemoteChanged(mSurfaceView)
                        } else {
                            //如果大屏幕显示的是退出人
                            if (localSurfaceView?.first == uid) {
                                localSurfaceView = null
                                switchBigContainerShow(mSurfaceView.indexOf(mSurfaceView.find { it.first == 0 }))
                            }

                        }
                    }


                }

            }


        }
    }


    /**
     * 初始化视频
     */
    fun initVideo(uid: Int) {
        isCalling = true
        try {
            //初始化视频设置全局回调
            mRtcEngine = RtcEngine.create(
                App.ApplicationINSTANCE,
                App.ApplicationINSTANCE.getString(R.string.agora_app_id),
                VideoEvent(handler)
            )
            //配置视频参数
            mRtcEngine?.enableVideo()
            mRtcEngine?.setVideoEncoderConfiguration(
                VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_1280x720,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_LANDSCAPE
                )
            )
            //设置前置摄像头和后置摄像头
            mRtcEngine?.setCameraCapturerConfiguration(
                CameraCapturerConfiguration(
                    CameraCapturerConfiguration.CAPTURER_OUTPUT_PREFERENCE.CAPTURER_OUTPUT_PREFERENCE_AUTO,
                    CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_REAR
                )
            )

            //调用创建本地视图
            val mLocalSurfaceView = RtcEngine.CreateRendererView(App.ApplicationINSTANCE)
            localSurfaceView = Pair(uid, mLocalSurfaceView)
            //扔出去渲染UI去
            Ivideo.LocalSurfaceView(mLocalSurfaceView)
            // 设置本地视图。
            mRtcEngine!!.setupLocalVideo(
                VideoCanvas(
                    mLocalSurfaceView,
                    VideoCanvas.RENDER_MODE_FIT,
                    uid
                )
            )


        } catch (e: Exception) {
            //调用初始化失败
            Ivideo.onError(VideoError.INITERROR)
        }
    }

    /**
     * 加入频道
     */
    fun joinChannel(
        channelName: String,
        token: String? = null,
        optionalInfo: String = "",
        optionalUid: Int = 0
    ) {
        mRtcEngine?.joinChannel(token, channelName, optionalInfo, optionalUid)
    }


    /**
     * 开启或者关闭本地音频输入
     */
    fun setAudioState(state: Boolean) {
        mRtcEngine!!.muteLocalAudioStream(state)
    }

    /**
     *切换摄像头
     */
    fun switchCamera() {
        mRtcEngine!!.switchCamera()
    }

    /**
     * 切换到大屏幕
     */
    fun switchBigContainerShow(position: Int) {

        val small = mSurfaceView.get(position)
        mSurfaceView.removeAt(position)
        if (localSurfaceView != null) {
            //添加到列表这个数据
            (localSurfaceView!!.second.parent as ViewGroup).removeAllViews()
            localSurfaceView!!.second.setZOrderMediaOverlay(true)
            mSurfaceView.add(position, localSurfaceView!!)
        }
        (small.second.parent as ViewGroup).removeAllViews()
        small.second.setZOrderMediaOverlay(false)
        localSurfaceView = small
        Ivideo.LocalSurfaceView(localSurfaceView!!.second)
        Ivideo.onRemoteChanged(mSurfaceView)

    }


    /**
     * 离开频道
     */
    fun leaveChannel() {
        //异步方法
        mRtcEngine!!.leaveChannel()
        //同步方法
        RtcEngine.destroy()

        isCalling = false
    }




}