package io.ap1.braveheart;

import android.app.FragmentTransaction;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.perples.recosdk.RECOBeacon;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.ap1.braveheart.Utils.APICaller;
import io.ap1.braveheart.Utils.ActivityBeaconDetectionByRECO;
import io.ap1.braveheart.Utils.MySingletonRequestQueue;

public class ActivityMain extends ActivityBeaconDetectionByRECO implements View.OnClickListener{

    // TODO: change this to your own Firebase URL
    public static String url_firebaseServer;
    public static String url_video_prefix;
    public static String url_stopVideo_prefix = "http://apex.apengage.io/screens/stop.php?macaddress="; //Apex
    public static String url_check_in_prefix = "http://apex.apengage.io/screens/checkin.php?macaddress="; //Apex
    final private String UUID_AprilBrother = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0";
    final private String UUID_Reco = "24DDF411-8CF1-440C-87CD-E368DAF9C93E";

    final static String urlBase = "http://";

    private TextView user;
    private TextView tvStatus;

    private String videoId;
    private String videoName; //this is assigned when firbase child is added, but never accessed
    private long videoLength; //this is assigned when firbase child is added, but never accessed
    private String screenId;
    private String userId;

    private Firebase rootRef;
    private boolean foundBeacon = false;

    private Map<String, Object> newPost;
    private WebView wv_video;
    private String childId;
    private ChildEventListener childEventListener;

    private StringRequest requestCheckin;
    private StringRequest requestStopVideo;

    private MyApplication myApplication;
    private APICaller apiCaller;
    private MySingletonRequestQueue mySingletonRequestQueue;

    private int count = 0;
    private ArrayList<String> firebaseIDs = new ArrayList<>();
    Timer waitForClickEventOnPrompt;

    FragmentTransaction ft;
    FragmentPrompt fragmentPrompt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myApplication = (MyApplication)getApplication();

        if(getIntent().getStringExtra("networkStatus").equals("bad")) //ActivityLogin check network and shows feedback here
            Toast.makeText(getApplicationContext(), "Network is currently unavailable", Toast.LENGTH_SHORT).show();
        userId = getIntent().getStringExtra("UserID");

        getLocalSettings(); //read settings data from SharedPreferences file

        apiCaller = myApplication.getApiCaller();
        mySingletonRequestQueue = myApplication.getMyRequestQueue();
        Firebase.setAndroidContext(this);  //initialize firebase
        rootRef = new Firebase(url_firebaseServer);

        //The Reco Region arguments, different for the type of beacons.
        assignRegionArgs(UUID_AprilBrother, 99, -65); //April Beacons with minor of 1 and 2.
        //assignRegionArgs(UUID_Reco, 9876, -80); //Krishna's Reco beacon. Just assigned here as a test.
        //assignRegionArgs(UUID_Reco, 7000, -70); //Reco beacons for canadian tire



        //wv_video = (WebView) findViewById(R.id.wv_video);
        tvStatus = (TextView) findViewById(R.id.tv_status);

        findViewById(R.id.btn_startScanning).setOnClickListener(this);
        findViewById(R.id.btn_stopScanning).setOnClickListener(this);
        //findViewById(R.id.btn_stopVideo).setOnClickListener(this);
        //findViewById(R.id.btn_settings).setOnClickListener(this);

        WebSettings webSettings = wv_video.getSettings();
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        ft = getFragmentManager().beginTransaction();
        fragmentPrompt = new FragmentPrompt();
        ft.add(fragmentPrompt, "FragmentPrompt");
        ft.commit();

    }

    private void getLocalSettings(){
        url_firebaseServer = myApplication.getAppSettings().getValue("firebase_server");
        url_video_prefix = myApplication.getAppSettings().getValue("videopath");
    }

    @Override
    public void onClick(View v){  //the method implemented for interface View.OnClickListener
        switch (v.getId()){
            case R.id.btn_startScanning:
                start(definedRegions);
                tvStatus.setText("Scanning status: ON");
                alreadyPlaying = false;
                break;
            case R.id.btn_stopScanning:
                stop(definedRegions);
                tvStatus.setText("Scanning status: OFF");
                break;
            /*
            case R.id.btn_stopVideo:
                sendStopVideoRequest(screenId);
                alreadyPlaying = false;
                break;
            */
        }
    }

    public String getMacAddress(Context context, WifiManager wifiManager) {
        String macStr;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo.getMacAddress() != null) {
            macStr = wifiInfo.getMacAddress();
        } else {
            macStr = "not find the beacon";
        }
        return macStr;
    }

    @Override
    protected void actionOnEnter(RECOBeacon recoBeacon) { //Called when the phone checks in with the assigned beacon region.
        Toast.makeText(getApplicationContext(), "beacon detected: " + recoBeacon.getMinor(), Toast.LENGTH_SHORT).show();
        //check in with AprilBrother beacon's UUID, change to  UUID_Reco if when using RECO beacon
        //sendCheckInRequest(macAddress, UUID_AprilBrother, String.valueOf(recoBeacon.getMajor()), String.valueOf(recoBeacon.getMinor()), "1", "1");
        apiCaller.setAPI(urlBase, "urlPath", "?userid=" + userId, Request.Method.GET); //check in
        apiCaller.execAPI(new APICaller.VolleyCallback() {
            @Override
            public void onDelivered(String result) {
                Log.e("response", result);
                ft.show(fragmentPrompt);
            }
        });

        final Timer waitForDispatcher = new Timer(); //If there is no response from the dispatcher(firebase child added) after 10 seconds, popup warning.
        waitForDispatcher.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(DateUtils.SECOND_IN_MILLIS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.e("Timer 10 seconds", "");
                        Toast.makeText(getApplicationContext(), "10 seconds have past, and nothing was received from dispatcher.", Toast.LENGTH_LONG).show(); //Change this to whatever you want it to do after 10 seconds.
                    }
                });
            }
        }, 10000);

        childEventListener = rootRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                waitForDispatcher.cancel();
                Log.e("onChildAdded", "Child was added.");
                newPost = (Map<String, Object>) dataSnapshot.getValue();
                Log.e("newPost", newPost.toString());
                if (newPost.get("cmd").toString().equals("stop")) { //If the "stop" command is read, then just ignore it and move to the next Firebase node.
                    newPost.clear();
                    String key = dataSnapshot.getKey();
                    Log.i("dataSnapshot stop", dataSnapshot.getKey());
                    rootRef.child(key).removeValue();
                    return;
                }
                Log.e("macAddress", newPost.get("macaddress").toString());
                /*
                if ((compareMacAddress(newPost.get("macaddress").toString()))) { //Launch FragmentPrompt.
                    childId = dataSnapshot.getKey();
                    firebaseIDs.add(childId);
                    Log.e("childId", childId); //The ID that associates with the Firebase record.
                    videoName = newPost.get("showname").toString();
                    videoId = newPost.get("show").toString();
                    screenId = newPost.get("screen").toString();
                    videoLength =  Long.parseLong(newPost.get("length").toString());
                    ft.show(fragmentPrompt);
                    waitForClickEventOnPrompt = new Timer();
                    waitForClickEventOnPrompt.schedule(new TimerTask() {
                        public void run() {
                            fragmentPrompt.dismiss(); //not sure if this works
                        }
                    }, 30 * 1000);// Set to 30 seconds.
                    Log.e("after prompt", "");
                    rootRef.child(childId).removeValue();
                    rootRef.removeEventListener(childEventListener);
                }
                */
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                //System.out.println("The updated online user list is " + dataSnapshot.child("OnlineUsers").getValue());
            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                //Map<String, Object> newPost = (Map<String, Object>) dataSnapshot.getValue();
            }
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });
    }

    private void sendCheckInRequest(String macAddress, String uuid, String major, String minor, String proximity, String accuracy){
        requestCheckin = new StringRequest(Request.Method.GET, url_check_in_prefix + macAddress + "&uuid=" + uuid + "&major=" + major +
                "&minor=" + minor + "&proximity=" + proximity + "&accuracy" + accuracy, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                requestCheckin.markDelivered();
                response = response.replace("\\", "");
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    //potential check here
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                requestCheckin.markDelivered();
                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        requestCheckin.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MySingletonRequestQueue.getInstance(this).add(requestCheckin); //execute request
    }


    public void signUp(){
        waitForClickEventOnPrompt.cancel();
        alreadyPlaying = true;
        Log.e("pull video:",url_video_prefix + "/viewshow.php?videoid=" + videoId);
        count++;
        Log.w("Count", "" + count);

        wv_video.loadUrl(url_video_prefix + "/viewshow.php?videoid=" + videoId);
        rootRef.removeEventListener(childEventListener);
        rootRef.child(childId).removeValue();
        ActivityMain.this.stop(definedRegions);
        tvStatus.setText("Scanning status: OFF");
    }

    public void shareToOthers(){
        waitForClickEventOnPrompt.cancel();
        //do survey
    }

    public void notInterested(){
        waitForClickEventOnPrompt.cancel();
        //get perk
    }

    public void showNextTime(){ //this method is just for removing the 'waitForClickEventOnPrompt' timer
        waitForClickEventOnPrompt.cancel();
    }

    @Override
    protected void onDestroy() {
        //rootRef.removeValue(); //Removes ALL Firebase records. Really sensitive.
        for (int i = 0; i < firebaseIDs.size(); i++) //Should remove all Firebase IDs that the phone created through the app.
            rootRef.child(firebaseIDs.get(i)).removeValue();
        super.onDestroy();
    }
}