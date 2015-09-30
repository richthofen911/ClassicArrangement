package io.ap1.braveheart;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import io.ap1.braveheart.Utils.AppSettings;

public class ActivityLogin extends AppCompatActivity {

    private WebView wv_display;
    private MyJavaScriptInterface myJavaScriptInterface;
    private String isLogin;
    private SharedPreferences spLoginStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if(((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo() == null)
            Toast.makeText(getApplicationContext(), "Network is currently unavailable", Toast.LENGTH_SHORT).show();
        spLoginStatus = getApplication().getSharedPreferences("LoginStatus", 0);
        isLogin = spLoginStatus.getString("isLogin", "no");

        myJavaScriptInterface = new MyJavaScriptInterface(this);

        String url = "http://104.236.111.213/aloha/dashboard/whoami.php";

        wv_display = (WebView)findViewById(R.id.wv_display);
        wv_display.getSettings().setJavaScriptEnabled(true);
        wv_display.setWebViewClient(new SSOWebViewClient());
        wv_display.loadUrl(url);
        wv_display.addJavascriptInterface(myJavaScriptInterface, "HtmlViewer");
    }


    private class SSOWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if(url.equals("http://104.236.111.213/aloha/dashboard/whoami.php") && isLogin.equals("yes")){
                view.loadUrl("javascript:HtmlViewer.showHTML" +
                        "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
            }
            if(url.equals("http://104.236.111.213/aloha/dashboard/whoami.php?from_sso_server=1")){
                view.loadUrl("javascript:HtmlViewer.showHTML" +
                        "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
                spLoginStatus.edit().putString("isLogin", "yes").commit(); //this value need to be written in local file, integrate it in the settings module.
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            //if (wv_display != null && wv_display.canGoBack()) {
            //    wv_display.goBack();
            //    return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void checkAppSettings(final String userId){
        final AppSettings appSettings = new AppSettings(getApplicationContext());
        appSettings.initSettings();
        Thread countTime = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    Thread.sleep(3000);
                }catch (InterruptedException e){
                    Log.e("count settings err", e.toString());
                }
                if(!appSettings.hasHadResponseDelivered())//if AppSettings didn't get right result, cancel it
                    appSettings.cancelSettingsRequest();
                startActivity(new Intent(ActivityLogin.this, ActivityMain.class)
                        .putExtra("networkStatus", appSettings.getNetworkStatus())
                        .putExtra("UserID", userId));
                finish();
            }
        });
        countTime.start();
    }

    class MyJavaScriptInterface{
        private Context cxt;
        private String userId;
        private String loginStatus;

        MyJavaScriptInterface(Context cxt){
            this.cxt = cxt;
        }

        @JavascriptInterface
        public void showHTML(String html){
            String json = html.substring(25, 51);
            Log.e("resp content", json);
            try{
                JSONObject jsonObject = new JSONObject(json);
                userId = jsonObject.getString("UserID");
                //loginStatus = jsonObject.get("isLogin");

                //startActivity(new Intent(ActivityLogin.this, ActivityMain.class).putExtra("UserID", userId));
                checkAppSettings(userId);
            }catch (JSONException e){
                e.printStackTrace();
            }
        }

        public String getUserId(){
            return userId;
        }
    }
}
