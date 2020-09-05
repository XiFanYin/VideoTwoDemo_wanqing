package com.tencent.videotwodemo_wangqing.videoactivity

import android.content.Context
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import com.ehealth.machine.base.adapter.BaseAdapter
import com.ehealth.machine.base.adapter.CommonViewHolder
import com.tencent.videotwodemo_wangqing.R
import java.util.*

class RemoteListAdapter(context: Context, data: LinkedList<Pair<Int, SurfaceView>>?) :
    BaseAdapter<Pair<Int, SurfaceView>>(
        context,
        R.layout.item_remote, data
    ) {

    override fun convert(holder: CommonViewHolder, data: Pair<Int, SurfaceView>, position: Int) {
        val patient = holder.getView<FrameLayout>(R.id.item)
        //移除父布局从新渲染到页面
        (data.second.parent as? ViewGroup)?.removeAllViews()
        patient.addView(data.second)
    }


}
