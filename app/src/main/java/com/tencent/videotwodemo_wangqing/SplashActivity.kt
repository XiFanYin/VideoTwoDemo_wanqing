package com.tencent.videotwodemo_wangqing

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.tbruyelle.rxpermissions2.RxPermissions
import com.tencent.videotwodemo_wangqing.socketservice.SocketService
import com.tencent.videotwodemo_wangqing.videoactivity.VideoActivity
import kotlinx.android.synthetic.main.activity_splash.*

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        setContentView(R.layout.activity_splash)

        connect.setOnClickListener {
            startService(Intent(this, SocketService::class.java))
        }

        sendMessage.setOnClickListener {
          val map =   mapOf("hanlderType" to "VIDEO","456" to "李四" )
            SocketService.getWsManager().sendMessage( Gson().toJson(map))
        }



        btn.setOnClickListener {
            RxPermissions(this).request(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ).subscribe({
                if (it) {

                    startActivity(Intent(this, VideoActivity::class.java))
                } else {
                    Toast.makeText(this, "请给权限", Toast.LENGTH_LONG).show()
                }
            })


        }


    }


}