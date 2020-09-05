package com.tencent.videotwodemo_wangqing.websocket;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;


import com.tencent.videotwodemo_wangqing.websocket.listener.WsStatusListener;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * @author rabtman
 */

public class WsManager implements IWsManager {

    private final static int RECONNECT_INTERVAL = 3 * 1000;    //重连自增步长
    private final static long RECONNECT_MAX_TIME = 9 * 1000;   //最大重连间隔
    private Context mContext;
    private String wsUrl;
    private WebSocket mWebSocket;
    private OkHttpClient mOkHttpClient;
    private Request mRequest;
    private int mCurrentStatus = WsStatus.DISCONNECTED;     //websocket连接状态
    private boolean isNeedReconnect;          //是否需要断线自动重连
    private boolean isManualClose = false;         //是否为手动关闭websocket连接
    private WsStatusListener wsStatusListener;
    private Lock mLock;
    private Handler wsMainHandler = new Handler(Looper.getMainLooper());
    private int reconnectCount = 1;   //重连次数
    private Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            if (wsStatusListener != null) {
                wsStatusListener.onReconnect();
            }
            buildConnect();
        }
    };


    //创建websocket的回调
    private WebSocketListener mWebSocketListener = new WebSocketListener() {

        @Override//当websocket连接上后
        public void onOpen(WebSocket webSocket, final Response response) {
            mWebSocket = webSocket;
            //改变连接状态
            setCurrentStatus(WsStatus.CONNECTED);
            //有可能是重连成功，所以这里取消重连
            connected();
              //向外部回调发送连接上的响应告诉连接成功
            if (wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            wsStatusListener.onOpen(response);
                        }
                    });
                } else {
                    wsStatusListener.onOpen(response);
                }
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, final ByteString bytes) {
            if (wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            wsStatusListener.onMessage(bytes);
                        }
                    });
                } else {
                    wsStatusListener.onMessage(bytes);
                }
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, final String text) {
            if (wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            wsStatusListener.onMessage(text);
                        }
                    });
                } else {
                    wsStatusListener.onMessage(text);
                }
            }
        }

        @Override //长连接关闭中
        public void onClosing(WebSocket webSocket, final int code, final String reason) {
            if (wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            wsStatusListener.onClosing(code, reason);
                        }
                    });
                } else {
                    wsStatusListener.onClosing(code, reason);
                }
            }
        }

        @Override
        public void onClosed(WebSocket webSocket, final int code, final String reason) {
            if (wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            wsStatusListener.onClosed(code, reason);
                        }
                    });
                } else {
                    wsStatusListener.onClosed(code, reason);
                }
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, final Throwable t, final Response response) {
            tryReconnect();
            if (wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            wsStatusListener.onFailure(t, response);
                        }
                    });
                } else {
                    wsStatusListener.onFailure(t, response);
                }
            }
        }
    };


    //构造方法
    public WsManager(Builder builder) {
        mContext = builder.mContext;
        wsUrl = builder.wsUrl;
        isNeedReconnect = builder.needReconnect;
        mOkHttpClient = builder.mOkHttpClient;
        //创建乐观锁
        this.mLock = new ReentrantLock();
    }


    private void initWebSocket() {
        if (mOkHttpClient == null) {
            mOkHttpClient = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .build();
        }
        if (mRequest == null) {
            mRequest = new Request.Builder()
                    .url(wsUrl)
                    .build();
        }
        //取消线程池所有的请求
        mOkHttpClient.dispatcher().cancelAll();
        try {
           // 获取线程锁
            mLock.lockInterruptibly();
            try {
                //创建websocket
                mOkHttpClient.newWebSocket(mRequest, mWebSocketListener);
            } finally {
              //释放锁
                mLock.unlock();
            }
        } catch (InterruptedException e) {
        }
    }

    @Override
    public WebSocket getWebSocket() {
        return mWebSocket;
    }

    //设置websocket状态监听
    public void setWsStatusListener(WsStatusListener wsStatusListener) {
        this.wsStatusListener = wsStatusListener;
    }

    @Override
    public synchronized boolean isWsConnected() {
        return mCurrentStatus == WsStatus.CONNECTED;
    }

    @Override
    public synchronized int getCurrentStatus() {
        return mCurrentStatus;
    }

    @Override
    public synchronized void setCurrentStatus(int currentStatus) {
        this.mCurrentStatus = currentStatus;
    }

    //开始连接
    @Override
    public void startConnect() {
        isManualClose = false;
        buildConnect();
    }

    @Override
    public void stopConnect() {
        isManualClose = true;
        disconnect();
    }

    private void tryReconnect() {
        if (!isNeedReconnect | isManualClose) {
            return;
        }

        if (!isNetworkConnected(mContext)) {
            setCurrentStatus(WsStatus.DISCONNECTED);
            return;
        }

        setCurrentStatus(WsStatus.RECONNECT);

        long delay = reconnectCount * RECONNECT_INTERVAL;
        wsMainHandler
                .postDelayed(reconnectRunnable, delay > RECONNECT_MAX_TIME ? RECONNECT_MAX_TIME : delay);
        reconnectCount++;
    }
    //取消重连
    private void cancelReconnect() {
        wsMainHandler.removeCallbacks(reconnectRunnable);
        reconnectCount = 1;
    }
    //连接上调用
    private void connected() {
        cancelReconnect();
    }

    private void disconnect() {
        if (mCurrentStatus == WsStatus.DISCONNECTED) {
            return;
        }
        cancelReconnect();
        if (mOkHttpClient != null) {
            mOkHttpClient.dispatcher().cancelAll();
        }
        if (mWebSocket != null) {
            boolean isClosed = mWebSocket.close(WsStatus.CODE.NORMAL_CLOSE, WsStatus.TIP.NORMAL_CLOSE);
            //非正常关闭连接
            if (!isClosed) {
                if (wsStatusListener != null) {
                    wsStatusListener.onClosed(WsStatus.CODE.ABNORMAL_CLOSE, WsStatus.TIP.ABNORMAL_CLOSE);
                }
            }
        }
        setCurrentStatus(WsStatus.DISCONNECTED);
    }

    //连接
    private synchronized void buildConnect() {
        //判断是否有网络，如果没有网络，设置状态为断开
        if (!isNetworkConnected(mContext)) {
            setCurrentStatus(WsStatus.DISCONNECTED);
            return;
        }
        switch (getCurrentStatus()) {
            case WsStatus.CONNECTED:
            case WsStatus.CONNECTING:
                break;
            default:
                setCurrentStatus(WsStatus.CONNECTING);
                initWebSocket();
        }
    }

    //发送消息
    @Override
    public boolean sendMessage(String msg) {
        return send(msg);
    }

    @Override
    public boolean sendMessage(ByteString byteString) {
        return send(byteString);
    }

    private boolean send(Object msg) {
        boolean isSend = false;
        if (mWebSocket != null && mCurrentStatus == WsStatus.CONNECTED) {
            if (msg instanceof String) {
                isSend = mWebSocket.send((String) msg);
            } else if (msg instanceof ByteString) {
                isSend = mWebSocket.send((ByteString) msg);
            }
            //发送消息失败，尝试重连
            if (!isSend) {
                tryReconnect();
            }
        }
        return isSend;
    }


    //源码中如果没有网络，就不去尝试连接socket，这里注销掉，可以保证没有网络的情况也可以去尝试连接网络
    private boolean isNetworkConnected(Context context) {
//        if (context != null) {
//            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
//                    .getSystemService(Context.CONNECTIVITY_SERVICE);
//            NetworkInfo mNetworkInfo = mConnectivityManager
//                    .getActiveNetworkInfo();
//            if (mNetworkInfo != null) {
//                return mNetworkInfo.isAvailable();
//            }
//        }
//        return false;

        return true;
    }


    //builder模式
    public static final class Builder {

        private Context mContext;
        private String wsUrl;
        private boolean needReconnect = true;
        private OkHttpClient mOkHttpClient;

        public Builder(Context val) {
            mContext = val;
        }

        public Builder wsUrl(String val) {
            wsUrl = val;
            return this;
        }

        public Builder client(OkHttpClient val) {
            mOkHttpClient = val;
            return this;
        }

        public Builder needReconnect(boolean val) {
            needReconnect = val;
            return this;
        }

        public WsManager build() {
            return new WsManager(this);
        }
    }
}
