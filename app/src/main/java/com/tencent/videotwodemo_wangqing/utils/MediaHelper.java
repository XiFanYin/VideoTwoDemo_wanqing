package com.tencent.videotwodemo_wangqing.utils;

import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;

import java.io.FileDescriptor;
import java.io.IOException;

public class MediaHelper {

    private static MediaPlayer mPlayer;

    public static void playSound(AssetFileDescriptor file) {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
        } else {
            mPlayer.reset();
        }
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setLooping(true);
        try {
            mPlayer.setDataSource(file.getFileDescriptor(),file.getStartOffset(),file.getLength() );
            mPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPlayer.start();

    }


    public static void stop() {
        if (mPlayer!=null){
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }

    }


}
