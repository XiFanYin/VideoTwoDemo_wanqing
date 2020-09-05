package com.tencent.videotwodemo_wangqing.videoutil

import android.os.Handler
import android.util.Log
import io.agora.rtc.IRtcEngineEventHandler

/**
 * 第三方视频回调类
 */
class VideoEvent(val handler: Handler) : IRtcEngineEventHandler() {

    //当缘短视频加入的时候回调
    override fun onFirstRemoteVideoDecoded(
        uid: Int,
        width: Int,
        height: Int,
        elapsed: Int
    ) {
        val message=handler.obtainMessage(VideoEventCode.REMOTEENTER,uid)
        handler.sendMessage(message)

        Log.e("rrrrrrrrrr","onFirstRemoteVideoDecoded")
    }


    //当远端离开页面的时候
    override fun onUserOffline(uid: Int, reason: Int) {
        val message=handler.obtainMessage(VideoEventCode.REMOTELEAVE,uid)
        handler.sendMessage(message)
    }











}