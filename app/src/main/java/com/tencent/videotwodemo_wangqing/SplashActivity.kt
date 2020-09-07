package com.tencent.videotwodemo_wangqing

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.tbruyelle.rxpermissions2.RxPermissions
import com.tencent.videotwodemo_wangqing.utils.MediaHelper
import com.tencent.videotwodemo_wangqing.videoactivity.VideoActivity
import com.tencent.videotwodemo_wangqing.videoservice.VideoService
import kotlinx.android.synthetic.main.activity_splash.*


class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        setContentView(R.layout.activity_splash)
        //开启服务，内部开启websocket
        startService(Intent(this, VideoService::class.java))
        //发送视频
        btn.setOnClickListener {
            RxPermissions(this).request(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ).subscribe({
                if (it) {
                    //向后台发生推送给谁的列表
                    val map = mapOf("hanlderType" to "VIDEO", "id" to "111111","username" to "秦荣双")
                    VideoService.wsManager?.sendMessage(Gson().toJson(map))
                    //自己先进入房间
                    startActivity(Intent(this, VideoActivity::class.java))
                    //播放一个等待的音乐
                    MediaHelper.playSound(assets.openFd("most_lucky.m4a"))
                } else {
                    Toast.makeText(this, "请给权限", Toast.LENGTH_LONG).show()
                }
            })


        }


    }


}