package com.example.midBR;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.net.URLEncoder;
import java.security.MessageDigest;


/**
 * Created by Evertonj on 07/01/2017.
 */

public class HttpActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static WebView web;
    private static String username = "";
    private static String keyid = "";
    private static ProgressDialog dialog;
    private final int AUTH = 2;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.http_activity);

        PreferenceManager.setDefaultValues(ApplicationContextDoorLock.getContext(), R.xml.pref_general, false);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationContextDoorLock.getContext());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarHttp);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.layout_http);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        reload(null);
    }

    private void updateMainLayout(ServerResponseReg serverResponseReg) {
        if (serverResponseReg.getStatus().equals("SUCCESS")) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString("usernameMain", serverResponseReg.getUsername());
            editor.putString("keyid", serverResponseReg.getAuthenticator().getKeyID());
            editor.putString("aaid", serverResponseReg.getAuthenticator().getAAID());
            editor.putString("pubkey", serverResponseReg.getPublicKey());
            editor.putString("server", mSharedPreferences.getString("fido_server_endpoint", ""));
            editor.apply();
        }
    }

    private void reload(ServerResponse responseReg) {
        try {
            clearCookies(this);

            if(responseReg != null) {
                username = responseReg.getUsername();
                keyid = responseReg.getKeyID();
            }else {
                Bundle bundle = getIntent().getExtras();
                username = bundle.getString("username");
                keyid = bundle.getString("keyid");
            }

            web = (WebView) findViewById(R.id.web);
            web.clearCache(true);
            web.clearFormData();
            web.clearHistory();
            web.clearSslPreferences();
            web.clearMatches();
            web.clearAnimation();
            WebSettings webSettings = web.getSettings();
            webSettings.setJavaScriptEnabled(true);
            web.getSettings().setJavaScriptEnabled(true);
            web.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            web.setVisibility(View.INVISIBLE);
            String urlParameters = "";
            urlParameters = "otp=" + URLEncoder.encode((username + keyid), "UTF-8");

            final String finalUrlParameters = urlParameters;
            web.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    if (url.contains("StateId")) {
                        if (dialog.isShowing())
                            dialog.dismiss();
                        web.setVisibility(View.VISIBLE);
                    }
                    if (url.contains("AuthState")) {
                        web.postUrl(url, finalUrlParameters.getBytes());
                    }
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    goBack();
                }
            });

            long time = Integer.parseInt(mSharedPreferences.getString("timeout", "10")) * 1000;
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    if(dialog != null)
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                            goBack();
                        }
                }
            }, time);

            HttpActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog = ProgressDialog.show(HttpActivity.this, "", "Acessando "+ MainActivity.urlService+" ...");
                    Toast.makeText(HttpActivity.this, MainActivity.urlService, Toast.LENGTH_LONG).show();
                    web.loadUrl(MainActivity.urlService);
                }
            });

        } catch (Exception e) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    @SuppressWarnings("deprecation")
    public static void clearCookies(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Log.d("COOKIE", "Using clearCookies code for API >=" + String.valueOf(Build.VERSION_CODES.LOLLIPOP_MR1));
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else {
            Log.d("COOKIE", "Using clearCookies code for API <" + String.valueOf(Build.VERSION_CODES.LOLLIPOP_MR1));
            CookieSyncManager cookieSyncMngr = CookieSyncManager.createInstance(context);
            cookieSyncMngr.startSync();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.stopSync();
            cookieSyncMngr.sync();
        }
    }

    private void goBack() {
        if(dialog != null)
            if(dialog.isShowing())
                dialog.dismiss();

        Toast.makeText(this, "Serviço não está disponível", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.action_appsign:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.app_sign_dialog)
                        .setMessage(getFacetId())
                        .setNeutralButton(R.string.button_done, null).show();

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public String getFacetId() {
        StringBuffer ret = new StringBuffer();
        String comma = "";
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature sign : packageInfo.signatures) {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
                messageDigest.update(sign.toByteArray());
                String currentSignature = Base64.encodeToString(messageDigest.digest(), Base64.DEFAULT);
                ret.append("android:apk-key-hash:");
                ret.append(currentSignature.substring(0, currentSignature.length() - 2));
                ret.append(comma);
                comma = ",";
            }
            return ret.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data != null) {
            Bundle extra = data.getExtras();
            Toast toast;
            String result;
            String[] status;
            String response;
            Gson gson = new Gson();
            result = extra.getString("result");
            if (!result.isEmpty()) {
                // **************************************************
                // It is specific for eBay RP Server Response - github.com/eBay/UAF
                status = result.split("#ServerResponse\n");
                if (status.length > 1) {
                    response = status[1].trim();
                    response = response.substring(1, response.length() - 1);
                    ServerResponse serverResponse = gson.fromJson(response, ServerResponse.class);
                    //TODO Do something instead of toast message!
                    reload(serverResponse);
                } else {
                    Toast.makeText(this, "Something is wrong", Toast.LENGTH_SHORT).show();
                }
                // **************************************************
            } else {
                Toast.makeText(this, "Something is wrong", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.layout_main);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_sp04) {
            MainActivity.urlService = "https://sp04.redes.eng.br/index.php/Especial:Autenticar-se";
            Intent intent = new Intent(this, AuthenticationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivityForResult(intent, AUTH);
        } else if (id == R.id.nav_sprnp) {
            MainActivity.urlService = "https://sp-saml.gidlab.rnp.br/index.php/Especial:Autenticar-se";
            Intent intent = new Intent(this, AuthenticationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivityForResult(intent, AUTH);
        } else if (id == R.id.nav_home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.layout_http);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
