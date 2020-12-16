package com.tencent.videotwodemo_wangqing.videoactivity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.tencent.videotwodemo_wangqing.R
import com.tencent.videotwodemo_wangqing.utils.MediaHelper
import com.tencent.videotwodemo_wangqing.videoservice.VideoService
import kotlinx.android.synthetic.main.activity_video.*

class VideoActivity : AppCompatActivity(), ServiceConnection {

    //调用服务中方法的句柄
    lateinit var mBinder: VideoService.MyBinder

    //展示下边列表的adapter
    lateinit var patientAdapter: RemoteListAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        //创建远程视频展示列表
        patientAdapter = RemoteListAdapter(this, null)
        mRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mRecyclerView.adapter = patientAdapter
        //设置Recycler禁止缓存
        mRecyclerView.getRecycledViewPool().setMaxRecycledViews(0,0)
        //判断服务是否正在运行
        VideoService. audioState = false
        if (VideoService.isStart) {
            btn_mute.setImageResource( if (VideoService.audioState) R.drawable.btn_mute else R.drawable.btn_unmute)
            bindService(Intent(this, VideoService::class.java), this, BIND_AUTO_CREATE)
        } else {
            startService(Intent(this, VideoService::class.java))
            bindService(Intent(this, VideoService::class.java), this, BIND_AUTO_CREATE)
        }

        //静音按钮
        btn_mute.setOnClickListener {
            VideoService. audioState = !VideoService.audioState
            mBinder.setAudioState(VideoService.audioState)
            val res = if (VideoService.audioState) R.drawable.btn_mute else R.drawable.btn_unmute
            btn_mute.setImageResource(res)
        }
        //切换摄像头
        btn_switch_camera.setOnClickListener {
            mBinder.switchCamera()
        }
        //挂断按钮关闭服务
        btn_call.setOnClickListener {
            finish()
            //离开频道
            mBinder.leaveChannel()
        }


        //设置条目被点击,切换大小屏
        patientAdapter.setItemClickListener { view, position, data ->
            mBinder.switchBigContainerShow(position)
        }
        //视频通话运行在后台
        iv_dismiss.setOnClickListener {
            //判断是否拥有悬浮窗权限，无则跳转悬浮窗权限授权页面
            if (Settings.canDrawOverlays(this)) {
                //通知服务开启悬浮窗
                mBinder.showFloatWindow()
                finish()
            } else {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }


        //设置点击动画
        big_container.setOnClickListener {

            if (head_layout.visibility == View.VISIBLE) {
                val headout = AnimationUtils.loadAnimation(this, R.anim.head_out)
                val bottomout = AnimationUtils.loadAnimation(this, R.anim.bottom_out)
                headout.setAnimationListener(object : Animation.AnimationListener {
                    //动画完全结束时候调用
                    override fun onAnimationEnd(animation: Animation?) {
                        head_layout.visibility = View.GONE
                        bottomButton.visibility = View.GONE
                    }

                    //动画开始时候调用
                    override fun onAnimationStart(animation: Animation?) {
                    }

                    //动画重复时候调用
                    override fun onAnimationRepeat(animation: Animation?) {
                    }
                })
                head_layout.startAnimation(headout)
                bottom_layout.startAnimation(bottomout)

            } else {
                val headin = AnimationUtils.loadAnimation(this, R.anim.head_in)
                val bottomin = AnimationUtils.loadAnimation(this, R.anim.bottom_in)

                headin.setAnimationListener(object : Animation.AnimationListener {
                    //动画完全结束时候调用
                    override fun onAnimationEnd(animation: Animation?) {
                        head_layout.visibility = View.VISIBLE
                        bottomButton.visibility = View.VISIBLE
                    }

                    //动画开始时候调用
                    override fun onAnimationStart(animation: Animation?) {
                    }

                    //动画重复时候调用
                    override fun onAnimationRepeat(animation: Animation?) {
                    }
                })
                head_layout.startAnimation(headin)
                bottom_layout.startAnimation(bottomin)
            }


        }

    }


    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        this.mBinder = service as VideoService.MyBinder
        if (intent.getStringExtra("from") == null) {
            //初始化并设置回调监听
            mBinder.initVideo(777777,{
                big_container.removeAllViews()
                if (it != null) {
                    big_container.addView(it)
                }
            }, {
                //停止本地音乐播放
                MediaHelper.stop()
                //设置条目被点击,切换大小屏
                patientAdapter.setNewData(it)
            })
            //加入频道
            mBinder.joinChannel("TTTTT",optionalUid = 777777)
        } else {
            //隐藏小窗口，显示到打窗口上
            mBinder.dismassFloatWindow({
                big_container.removeAllViews()
                if (it != null) {
                    big_container.addView(it)
                }
            }, {
                patientAdapter.setNewData(it.toMutableList())
            })
        }


    }

    //用户按下返回键
    override fun onBackPressed() {
        super.onBackPressed()
        //离开频道
        mBinder.leaveChannel()
    }



    override fun onDestroy() {
        super.onDestroy()
        //解绑服务
        unbindService(this)
        //停止本地音乐播放
        MediaHelper.stop()
    }

    override fun onServiceDisconnected(name: ComponentName?) {

    }


}