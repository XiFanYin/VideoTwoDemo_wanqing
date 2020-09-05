package com.tencent.videotwodemo_wangqing.socketservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.tencent.videotwodemo_wangqing.R;
import com.tencent.videotwodemo_wangqing.app.App;
import com.tencent.videotwodemo_wangqing.dialog.AlertDialog;
import com.tencent.videotwodemo_wangqing.websocket.WsManager;
import com.tencent.videotwodemo_wangqing.websocket.listener.WsStatusListener;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okio.ByteString;

public class SocketService extends Service {

    private static WsManager wsManager;
    private SocketUser user;
    private Gson gson;

    private AlertDialog call;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        user = new SocketUser("123", "张三");
        gson = new Gson();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //连接后台服务
        connectService();
        return START_REDELIVER_INTENT;
    }

    /**
     * 连接后台服务器，建立长连接
     */
    private void connectService() {

        if (wsManager != null) {
            wsManager.stopConnect();
            wsManager = null;
        }

        //创建websocket管理者
        wsManager = new WsManager.Builder(App.ApplicationINSTANCE)
                .client(
                        new OkHttpClient().newBuilder()
                                .pingInterval(10, TimeUnit.SECONDS)
                                .retryOnConnectionFailure(true)
                                .build()
                )
                .needReconnect(true)
                .wsUrl("ws://192.168.1.196:9326?user=" + gson.toJson(user))
                .build();
        //设置监听
        wsManager.setWsStatusListener(wsStatusListener);
        //开始连接
        wsManager.startConnect();

    }


    //websocket监听
    private WsStatusListener wsStatusListener = new WsStatusListener() {
        @Override
        public void onOpen(Response response) {
            //连接后台成功
        }

        @Override
        public void onMessage(String text) {
            Log.e("rrrrrrrr", text);
            //接收到消息，然后跳转页面，是否接听
            showCallDialog();


        }

        @Override
        public void onMessage(ByteString bytes) {

        }

        @Override
        public void onReconnect() {
        }

        @Override
        public void onClosing(int code, String reason) {
            Log.e("rrrrrrr", "WsManager-----onClosing");

        }

        @Override
        public void onClosed(int code, String reason) {
            Log.e("rrrrrrr", "WsManager-----onClosed");

        }

        @Override
        public void onFailure(Throwable t, Response response) {
            //正在连接后台
        }
    };


    public static WsManager getWsManager() {
        return wsManager;
    }

    //显示被呼叫的dialog
    private void showCallDialog() {
        if (call == null) {
            call = new AlertDialog.Builder(this)
                    .setContentView(R.layout.dialog_call)
                    .setWidthAndHeight(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    .setCancelable(false)
                    .fromBottom(true)
                    .setBackgroundTransparence(1)
                    .setback(true)
                    .showSystem();
        }


    }


}
