package io.ap1.braveheart.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.perples.recosdk.RECOBeacon;
import com.perples.recosdk.RECOBeaconManager;
import com.perples.recosdk.RECOBeaconRegion;
import com.perples.recosdk.RECOErrorCode;
import com.perples.recosdk.RECORangingListener;
import com.perples.recosdk.RECOServiceConnectListener;

import java.util.ArrayList;
import java.util.Collection;


public class ActivityBeaconDetectionByRECO extends Activity implements RECOServiceConnectListener, RECORangingListener{

    private final boolean DISCONTINUOUS_SCAN = false;

    protected boolean entered = false;
    private int exitCount = 0;
    private boolean exited = false;
    private int rssiBorder = 0;
    private int currentMinor = 0;
    private int previousCount = 0;

    protected RECOBeaconManager mRecoManager = RECOBeaconManager.getInstance(this, false, false);
    protected ArrayList<RECOBeaconRegion> definedRegions;
    protected boolean alreadyPlaying = false;

    protected void assignRegionArgs(String uuid, int borderValue){
        definedRegions = generateBeaconRegion(uuid);
        rssiBorder = borderValue;
    }

    protected void assignRegionArgs(String uuid, int major, int borderValue){
        definedRegions = generateBeaconRegion(uuid, major);
        rssiBorder = borderValue;
    }

    protected void assignRegionArgs(String uuid, int major, int minor, int borderValue){
        definedRegions = generateBeaconRegion(uuid, major, minor);
        rssiBorder = borderValue;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRecoManager.setRangingListener(this);
        mRecoManager.bind(this);
    }

    @Override
    protected void onStart() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE); //Checks if wifi is on first.
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (!mWifi.isConnected()) {
            Intent enableWifiIntent = new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK);
            startActivityForResult(enableWifiIntent, 1234);
        }
        super.onStart();
    }

    protected void start(ArrayList<RECOBeaconRegion> regions) {
        for(RECOBeaconRegion region : regions) {
            try {
                mRecoManager.startRangingBeaconsInRegion(region);
                Log.e("start detecting", region.describeContents() + "");
            } catch (RemoteException e) {
                Log.i("RECORangingActivity", "Remote Exception");
                e.printStackTrace();
            } catch (NullPointerException e) {
                Log.i("RECORangingActivity", "Null Pointer Exception");
                e.printStackTrace();
            }
        }
    }

    protected void stop(ArrayList<RECOBeaconRegion> regions) {
        Log.e("stop detecting", "...");
        for(RECOBeaconRegion region : regions) {
            try {
                mRecoManager.stopRangingBeaconsInRegion(region);
                entered = false;
            } catch (RemoteException e) {
                Log.i("RECORangingActivity", "Remote Exception");
                e.printStackTrace();
            } catch (NullPointerException e) {
                Log.i("RECORangingActivity", "Null Pointer Exception");
                e.printStackTrace();
            }
        }
    }

    private ArrayList<RECOBeaconRegion> generateBeaconRegion(String uuid) {
        ArrayList<RECOBeaconRegion> regions = new ArrayList<>();
        RECOBeaconRegion recoRegion;
        recoRegion = new RECOBeaconRegion(uuid, "Defined Region");
        regions.add(recoRegion);
        return regions;
    }

    private ArrayList<RECOBeaconRegion> generateBeaconRegion(String uuid, int major) {
        ArrayList<RECOBeaconRegion> regions = new ArrayList<>();
        RECOBeaconRegion recoRegion;
        recoRegion = new RECOBeaconRegion(uuid, major, "Defined Region");
        regions.add(recoRegion);
        return regions;
    }

    private ArrayList<RECOBeaconRegion> generateBeaconRegion(String uuid, int major, int minor) {
        ArrayList<RECOBeaconRegion> regions = new ArrayList<>();
        RECOBeaconRegion recoRegion;
        recoRegion = new RECOBeaconRegion(uuid, major, minor, "Defined Region");
        regions.add(recoRegion);
        return regions;
    }

    @Override
    public void onServiceConnect() {
        Log.e("RangingActivity", "onServiceConnect()");
        mRecoManager.setDiscontinuousScan(DISCONTINUOUS_SCAN);
        //start(definedRegions);
    }

    @Override
    public void onServiceFail(RECOErrorCode recoErrorCode) {
        Log.e("RECO service error:", recoErrorCode.toString());
    }

    protected void actionOnEnter(RECOBeacon recoBeacon){}

    protected void actionOnExit(RECOBeacon recoBeacon){}

    private void inOut(int theRssi, RECOBeacon recoBeacon){
        if(theRssi > rssiBorder){
            if(!entered && !alreadyPlaying){
                exitCount = 0;
                entered = true;
                exited = false;
                Log.e("put a checkin", " with beacon " + recoBeacon.getMinor());
                currentMinor = recoBeacon.getMinor();
                actionOnEnter(recoBeacon);
            }else{
                Log.e("entered already", ")");

            }
        }else{
            if(recoBeacon.getMinor() == currentMinor){
                if(exitCount < 3){
                    exitCount++;
                }else {
                    if(!exited){
                        entered = false;
                        exited = true;
                        currentMinor = 0;
                        actionOnExit(recoBeacon);
                    }else {
                        Log.e("exited already", ")");
                    }
                }
            }else{
                Log.e("not this beacon", String.valueOf(recoBeacon.getMinor()));
            }
        }
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<RECOBeacon> recoBeacons, RECOBeaconRegion recoBeaconRegion) {
        synchronized (recoBeacons){
            for(RECOBeacon recoBeacon: recoBeacons){
                Log.e("beacon detected, rssi", String.valueOf(recoBeacon.getRssi()));
                inOut(recoBeacon.getRssi(), recoBeacon);
            }
        }
    }

    @Override
    public void rangingBeaconsDidFailForRegion(RECOBeaconRegion recoBeaconRegion, RECOErrorCode recoErrorCode) {
        Log.e("RECO ranging error:", recoErrorCode.toString());
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        try{
            mRecoManager.unbind();
        }catch (RemoteException e){
            Log.e("on destroy error", e.toString());
        }
    }
}