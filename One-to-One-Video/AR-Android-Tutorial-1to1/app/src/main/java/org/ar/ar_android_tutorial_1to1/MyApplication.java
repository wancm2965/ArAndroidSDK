package org.ar.ar_android_tutorial_1to1;

import android.app.Application;

import com.tencent.bugly.Bugly;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Bugly.init(getApplicationContext(), "249e5b7508", true);
    }
}
