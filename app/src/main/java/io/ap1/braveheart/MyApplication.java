package io.ap1.braveheart;

import android.app.Application;

import io.ap1.braveheart.Utils.APICaller;
import io.ap1.braveheart.Utils.MySingletonRequestQueue;

/**
 * Created by Admin on 28/09/2015.
 */
public class MyApplication extends Application {
    public static APICaller apiCaller;
    public static MySingletonRequestQueue mRequestQueue;

    public void onCreate(){
        super.onCreate();
        mRequestQueue = MySingletonRequestQueue.getInstance(this);
        apiCaller = APICaller.getInstance(this, mRequestQueue);
    }

    public MySingletonRequestQueue getMyRequestQueue(){
        return mRequestQueue;
    }

    public APICaller getApiCaller(){
        return apiCaller;
    }
}
