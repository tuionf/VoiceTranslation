package com.example.tuion.voicetranslation;

import android.app.Application;

import com.iflytek.cloud.SpeechUtility;

/**
 * Created by tuion on 2017/3/27.
 */

public class VoiceTranslation extends Application {
    @Override
    public void onCreate() {
        SpeechUtility.createUtility(VoiceTranslation.this, "appid=" + "58d4d7e4");
        super.onCreate();
    }
}
