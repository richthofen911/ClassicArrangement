package io.ap1.braveheart.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by admin on 18/08/15.
 */
public class AppSettings {
    private static SharedPreferences spAppSettings;
    private SharedPreferences.Editor editorAppSettings;
    private final String urlCheckLastUpdateTimestamp = "http://104.236.111.213/aloha/api/latestupdate.php";
    private final String urlGetNewSettingsData = "http://104.236.111.213/aloha/api/settings.php";
    private String networkStatus = "bad";
    private Context context;
    private StringRequest requestCheckLastUpdateTimestamp;
    private StringRequest requestGetNewSettings;

    public AppSettings(Context appContext) {
        context = appContext;
        spAppSettings = PreferenceManager.getDefaultSharedPreferences(context);
        editorAppSettings = spAppSettings.edit();
    }

    public void initSettings() {
        compareUpdateTimestamp();
    }

    public String getValue(String keyName) {
        return spAppSettings.getString(keyName, "undefined");
    }

    public String getNetworkStatus() {
        return networkStatus;
    }

    private void compareUpdateTimestamp() {
        //get local last settings update timestamp, for new installed app, it's 0
        final long lastUpdateTimestamp = spAppSettings.getLong("lastUpdateTimestamp", 0);
        //compare the local timestamp with the one retrieving from remote server
        requestCheckLastUpdateTimestamp = new StringRequest(Request.Method.GET, urlCheckLastUpdateTimestamp, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                requestCheckLastUpdateTimestamp.markDelivered();
                response = response.replace("\\", "");
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    long remoteLastSettingsUpdateTimestamp = jsonObject.getLong("last update");
                    if (remoteLastSettingsUpdateTimestamp > lastUpdateTimestamp) {
                        editorAppSettings.putLong("lastUpdateTimestamp", remoteLastSettingsUpdateTimestamp).commit();
                        getNewSettingsData();
                    } else {
                        networkStatus = "good";
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                requestCheckLastUpdateTimestamp.markDelivered();
                Toast.makeText(context, error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        requestCheckLastUpdateTimestamp.setRetryPolicy(new DefaultRetryPolicy(3000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MySingletonRequestQueue.getInstance(context).add(requestCheckLastUpdateTimestamp); //execute request
    }

    //this method retrieves new settings data from remote server
    private void getNewSettingsData() {
        requestGetNewSettings = new StringRequest(Request.Method.GET, urlGetNewSettingsData, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                requestGetNewSettings.markDelivered();
                response = response.replace("\\", "");
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    networkStatus = "good";
                    for (int i = 0; i < jsonObject.names().length(); i++) {
                        String keyName = jsonObject.names().getString(i);
                        editorAppSettings.putString(keyName, jsonObject.getString(keyName));
                    }
                    editorAppSettings.commit();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                requestGetNewSettings.markDelivered();
                Toast.makeText(context, error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        requestGetNewSettings.setRetryPolicy(new DefaultRetryPolicy(3000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MySingletonRequestQueue.getInstance(context).add(requestGetNewSettings);
    }

    public void cancelSettingsRequest() {
        if (!requestCheckLastUpdateTimestamp.hasHadResponseDelivered() && !requestCheckLastUpdateTimestamp.isCanceled())
            requestCheckLastUpdateTimestamp.cancel();
        if (!requestGetNewSettings.hasHadResponseDelivered() && !requestGetNewSettings.isCanceled())
            requestGetNewSettings.cancel();
    }

    public boolean hasHadResponseDelivered() {
        return (requestCheckLastUpdateTimestamp.hasHadResponseDelivered() && (requestGetNewSettings == null || requestGetNewSettings.hasHadResponseDelivered()));
    }
}