package io.ap1.braveheart;

import android.annotation.SuppressLint;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
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
//???? does BraveHeart project work with firebase?, if not, remove all firebase related stuff
    // TODO: change this to your own Firebase URL
    final private String UUID_AprilBrother = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0";
    final private String UUID_Reco = "24DDF411-8CF1-440C-87CD-E368DAF9C93E";

    final static String urlBase = "http://104.236.111.213/braveheart";

    private TextView tvStatus;

    private String userId;

    private boolean foundBeacon = false;

    private MyApplication myApplication;
    private APICaller apiCaller;
    //private MySingletonRequestQueue mySingletonRequestQueue;

    private ArrayList<String> firebaseIDs = new ArrayList<>();
    //Timer waitForClickEventOnPrompt;

    FragmentTransaction ft;
    FragmentPrompt fragmentPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null)
            bluetoothAdapter.enable(); // if Bluetooth Adapter exists, force enabling it.
        else
            Toast.makeText(getApplicationContext(), "Bluetooth chip not found", Toast.LENGTH_SHORT).show();

        myApplication = (MyApplication)getApplication();

        /* BraveHeart project doesn't use AppSettings temporarily
        if(getIntent().getStringExtra("networkStatus").equals("bad")) //ActivityLogin check network and shows feedback here
            Toast.makeText(getApplicationContext(), "Network is currently unavailable", Toast.LENGTH_SHORT).show();
        */
        userId = getIntent().getStringExtra("UserID");

        //getLocalSettings(); //read settings data from SharedPreferences file

        apiCaller = myApplication.getApiCaller();
        //mySingletonRequestQueue = myApplication.getMyRequestQueue();

        //The Reco Region arguments, different for the type of beacons.
        assignRegionArgs(UUID_AprilBrother, 90, 90, -65); //April Beacons with minor of 1 and 2.
        //assignRegionArgs(UUID_Reco, 9876, -80); //Krishna's Reco beacon. Just assigned here as a test.
        //assignRegionArgs(UUID_Reco, 7000, -70); //Reco beacons for canadian tire

        //wv_video = (WebView) findViewById(R.id.wv_video);
        tvStatus = (TextView) findViewById(R.id.tv_status);

        findViewById(R.id.btn_startScanning).setOnClickListener(this);
        findViewById(R.id.btn_stopScanning).setOnClickListener(this);

        /*
        WebSettings webSettings = wv_video.getSettings();
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        */
    }

    private void getLocalSettings(){
    }

    @Override
    public void onClick(View v){  //the method implemented for interface View.OnClickListener
        switch (v.getId()){
            case R.id.btn_startScanning:
                start(definedRegions);
                /*
                String checkInBeaconTemp = "&uuid=1&major=1&minor=1"; // this is for debug testing
                apiCaller.setAPI(urlBase, "/checkin.php", "?userid=" + userId + checkInBeaconTemp, Request.Method.GET); //this
                apiCaller.execAPI(new APICaller.VolleyCallback() { //is
                    @Override                                      // for debugging
                    public void onDelivered(String result) {       // delete the
                        Log.e("resp checkin", result); // this is for debug testing
                        ft = getFragmentManager().beginTransaction(); // this is for debug testing
                        fragmentPrompt = new FragmentPrompt(); // this is for debug testing
                        ft.add(fragmentPrompt, "FragmentPrompt"); // this is for debug testing
                        ft.commit();                              // check in
                        ft.show(fragmentPrompt);                   // here
                    }                                              // when
                });                                                // done
                */
                tvStatus.setText("Scanning status: ON");
                break;
            case R.id.btn_stopScanning:
                stop(definedRegions);
                tvStatus.setText("Scanning status: OFF");
                break;
        }
    }

    @Override
    protected void actionOnEnter(RECOBeacon recoBeacon) { //Called when the phone checks in with the assigned beacon region.
        Toast.makeText(getApplicationContext(), "beacon detected: " + recoBeacon.getMinor(), Toast.LENGTH_SHORT).show();
        //check in with AprilBrother beacon's UUID, change to  UUID_Reco if when using RECO beacon
        //sendCheckInRequest(macAddress, UUID_AprilBrother, String.valueOf(recoBeacon.getMajor()), String.valueOf(recoBeacon.getMinor()), "1", "1");
        apiCaller.setAPI(urlBase, "/checkin.php", "?userid=" + userId + "&screen=Screen1" +
                "&uuid=E2C56DB5DFFB48D2B060D0F5A71096E0&major=99&minor=101&debug=1", Request.Method.GET); //check in
        apiCaller.execAPI(new APICaller.VolleyCallback() {
            @Override
            public void onDelivered(String result) {
                Log.e("resp checkin", result);
                try{
                    JSONObject jsonObject = new JSONObject(result.replace("\\", ""));
                    if(jsonObject.getInt("result") == 1){
                        try{
                            Thread.sleep(2000);
                        }catch (InterruptedException e){
                            Log.e("Thread sleep error", e.toString());
                        }
                        ft = getFragmentManager().beginTransaction();
                        fragmentPrompt = new FragmentPrompt();
                        ft.add(fragmentPrompt, "FragmentPrompt");
                        ft.commit();
                        ft.show(fragmentPrompt);
                    }else
                        Toast.makeText(getApplicationContext(), "Login failure", Toast.LENGTH_SHORT).show();
                }catch (JSONException e){
                    Log.e("checkin error", e.toString());
                }
            }
        });
    }

    private void checkOut() {
        apiCaller.setAPI(urlBase, "/checkin.php", "?userid=" + userId + "&cmd=checkout", Request.Method.GET); //check out
        apiCaller.execAPI(new APICaller.VolleyCallback() {
            @Override
            public void onDelivered(String result) {
                Log.e("resp checkout", result);
            }
        });
    }

    public void signUp(){ //this method is used in fragment_prompt
        //waitForClickEventOnPrompt.cancel();
        checkOut();
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://104.236.111.213/samplelandingpage.html")));
        //ActivityMain.this.stop(definedRegions);
        //tvStatus.setText("Scanning status: OFF");
    }

    public void shareToOthers(){ //this method is used in fragment_prompt
        //waitForClickEventOnPrompt.cancel();
        checkOut();
    }

    public void notInterested(){ //this method is used in fragment_prompt
        //waitForClickEventOnPrompt.cancel();
        Toast.makeText(getApplicationContext(), "Thank you for your participation", Toast.LENGTH_SHORT).show();
        checkOut();
    }

    public void showNextTime(){ //this method is just for removing the 'waitForClickEventOnPrompt' timer
        //waitForClickEventOnPrompt.cancel();
        apiCaller.setAPI(urlBase, "/shownexttime.php", "?userid=" + userId, Request.Method.GET); //show me next time
        apiCaller.execAPI(new APICaller.VolleyCallback() {
            @Override
            public void onDelivered(String result) {
                Log.e("resp showNextTime", result);
            }
        });
        checkOut();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}