package com.tencent.videotwodemo_wangqing.videoutil

import android.view.SurfaceView
import java.util.*


interface IVideo {


    fun  LocalSurfaceView(mlocalSurfaceView: SurfaceView?)


    fun onRemoteChanged(mRemoteSurfaceView: LinkedList<Pair<Int, SurfaceView>>)



    fun onError(error:VideoError)


}