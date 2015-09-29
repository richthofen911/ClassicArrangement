package io.ap1.braveheart.Utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

/**
 * Created by Tuotuo on 13/09/2015.
 */
public class APICaller {
    private String APIUrlStr;
    private String APIUrlEncoded;

    private int requestMethod; //Request.Method.GET is an int
    private StringRequest requestCallAPI;
    private MySingletonRequestQueue requestQueue;
    private Context context;
    private static APICaller singletonInstance;

    public APICaller(Context cxt, MySingletonRequestQueue myRequestQueue){
        this.context = cxt;
        requestQueue = myRequestQueue;
    }

    public static synchronized APICaller getInstance(Context cxt, MySingletonRequestQueue myRequestQueue){
        if(singletonInstance == null){
            singletonInstance = new APICaller(cxt, myRequestQueue);
        }
        return singletonInstance;
    }

    public void setAPI(String urlBase, String urlPath, String urlParams, int method){
        if(urlParams == null)
            APIUrlStr = urlBase + urlPath;
        else
            APIUrlStr = urlBase + urlPath + urlParams;
        APIUrlEncoded = Uri.encode(APIUrlStr).replace("%3A", ":");
        APIUrlEncoded = APIUrlEncoded.replace("%2F", "/");
        APIUrlEncoded = APIUrlEncoded.replace("%3F", "?");
        APIUrlEncoded = APIUrlEncoded.replace("%3D", "=");
        APIUrlEncoded = APIUrlEncoded.replace("%26", "&");
        Log.e("url encoded", APIUrlEncoded);
        requestMethod = method;
    }

    public interface VolleyCallback{
        void onDelivered(String result);
    }

    public void execAPI(final VolleyCallback callback){
        if(APIUrlEncoded == null){
            callback.onDelivered("API has not been set yet");
        }else {
            requestCallAPI = new StringRequest(requestMethod, APIUrlEncoded, new Response.Listener<String>(){
                @Override
                public void onResponse(String response){
                    requestCallAPI.markDelivered();
                    callback.onDelivered(response.replace("\\", ""));
                }
            }, new Response.ErrorListener(){
                @Override
                public void onErrorResponse(VolleyError error){
                    requestCallAPI.markDelivered();
                    callback.onDelivered(error.toString());
                }
            });
            requestQueue.add(requestCallAPI);
        }
    }
}
