package com.tencent.videotwodemo_wangqing.videoutil

import android.view.SurfaceView
import java.util.*


interface IVideo {

    //本地视频加入频道成功
    fun  LocalSurfaceView(mlocalSurfaceView: SurfaceView?)

    //远程视频假如频道
    fun onRemoteChanged(mRemoteSurfaceView: LinkedList<Pair<Int, SurfaceView>>)


    fun onError(error:VideoError)


}