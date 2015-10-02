package io.ap1.braveheart;

import android.app.Application;

import io.ap1.braveheart.Utils.APICaller;
import io.ap1.braveheart.Utils.AppSettings;
import io.ap1.braveheart.Utils.MySingletonRequestQueue;

/**
 * Created by Admin on 28/09/2015.
 */
public class MyApplication extends Application {
    public APICaller apiCaller;
    public MySingletonRequestQueue mRequestQueue;
    public AppSettings appSettings;

    public void onCreate(){
        super.onCreate();
        //appSettings = new AppSettings(getApplicationContext());  BraveHeart doesn't use AppSettings module temporarily
        mRequestQueue = MySingletonRequestQueue.getInstance(this);
        apiCaller = APICaller.getInstance(this, mRequestQueue);
    }

    public MySingletonRequestQueue getMyRequestQueue(){
        return mRequestQueue;
    }

    public APICaller getApiCaller(){
        return apiCaller;
    }

    public AppSettings getAppSettings(){
        return appSettings;
    }
}
