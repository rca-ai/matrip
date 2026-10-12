package com.guidrthai.matrip;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.content.ContentResolver;
import android.webkit.JavascriptInterface;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_CHOOSER_REQUEST = 4001;
    private static final int BACKUP_CREATE_REQUEST = 4002;
    private boolean pendingApkInstall = false;
    private String pendingBackupJson = null;
    private WebViewAssetLoader assetLoader;
    private boolean checkingLegacyStorage = true;
    private String legacyStorage = null;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assetLoader = new WebViewAssetLoader.Builder().addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        webView = new WebView(this); setContentView(webView);
        webView.addJavascriptInterface(new Object() { @JavascriptInterface public void saveBackup(String filename, String json) { runOnUiThread(() -> { pendingBackupJson = json; Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("application/json"); intent.putExtra(Intent.EXTRA_TITLE, filename); try { startActivityForResult(intent, BACKUP_CREATE_REQUEST); } catch (ActivityNotFoundException e) { pendingBackupJson = null; Toast.makeText(MainActivity.this, "백업 저장 위치를 선택할 수 없습니다.", Toast.LENGTH_LONG).show(); } }); } }, "AndroidBridge");
        WebSettings settings=webView.getSettings(); settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true); settings.setAllowContentAccess(true); settings.setJavaScriptCanOpenWindowsAutomatically(true); settings.setSupportMultipleWindows(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) { return assetLoader.shouldInterceptRequest(request.getUrl()); }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (checkingLegacyStorage && url.startsWith("file:///android_asset/")) {
                    checkingLegacyStorage = false;
                    view.evaluateJavascript("localStorage.getItem('matrip-data-v1')", value -> {
                        try {
                            Object parsed = new JSONTokener(value).nextValue();
                            if (parsed instanceof String && !((String) parsed).isEmpty()) legacyStorage = (String) parsed;
                        } catch (Exception ignored) { }
                        view.loadUrl("https://appassets.androidplatform.net/assets/TripPocket_travel_app.html");
                    });
                    return;
                }
                if (url.startsWith("https://appassets.androidplatform.net/assets/") && legacyStorage != null) {
                    String oldData = legacyStorage;
                    legacyStorage = null;
                    String quoted = JSONObject.quote(oldData);
                    view.evaluateJavascript("(function(){if(!localStorage.getItem('matrip-data-v1')){localStorage.setItem('matrip-data-v1'," + quoted + ");return true;}return false;})()", result -> {
                        if ("true".equals(result)) view.reload();
                    });
                }
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { Uri u=request.getUrl(); if("appassets.androidplatform.net".equals(u.getHost()))return false; return openExternal(u); }
            @Override public boolean shouldOverrideUrlLoading(WebView view,String url) { Uri u=Uri.parse(url); if("appassets.androidplatform.net".equals(u.getHost()))return false; return openExternal(u); }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> callback,FileChooserParams params) {
                if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=callback;
                Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);String[] accepts=params.getAcceptTypes();boolean wantsJson=false,wantsApk=false;java.util.ArrayList<String> mimeTypes=new java.util.ArrayList<>();if(accepts!=null){for(String accept:accepts){if(accept==null||accept.trim().isEmpty())continue;for(String part:accept.split(",")){String type=part.trim().toLowerCase(java.util.Locale.ROOT);if(type.contains("json")||type.endsWith(".json"))wantsJson=true;if(type.contains("vnd.android.package-archive")||type.endsWith(".apk"))wantsApk=true;if(type.contains("/"))mimeTypes.add(type);}}}pendingApkInstall=wantsApk;i.setType(wantsApk?"application/vnd.android.package-archive":(wantsJson?"*/*":(mimeTypes.size()==1?mimeTypes.get(0):"image/*")));if(wantsJson){i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/json","text/json","text/plain"});}i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,!wantsApk);
                try{startActivityForResult(i,FILE_CHOOSER_REQUEST);return true;}catch(ActivityNotFoundException e){fileCallback=null;Toast.makeText(MainActivity.this,"파일 선택 앱을 찾을 수 없습니다.",Toast.LENGTH_LONG).show();return false;}
            }
            @Override public boolean onCreateWindow(WebView view,boolean isDialog,boolean isUserGesture,android.os.Message resultMsg) {
                WebView popup=new WebView(MainActivity.this);popup.setWebViewClient(new WebViewClient(){
                    @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return openExternal(r.getUrl());}
                    @Override public boolean shouldOverrideUrlLoading(WebView v,String u){return openExternal(Uri.parse(u));}
                });((WebView.WebViewTransport)resultMsg.obj).setWebView(popup);resultMsg.sendToTarget();return true;
            }
        });
        if(savedInstanceState==null)webView.loadUrl("file:///android_asset/TripPocket_travel_app.html");else webView.restoreState(savedInstanceState);
    }
    private boolean openExternal(Uri uri){String s=uri.getScheme();if("http".equals(s)||"https".equals(s)||"mailto".equals(s)||"tel".equals(s)){try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(ActivityNotFoundException e){Toast.makeText(this,"열 수 있는 앱을 찾을 수 없습니다.",Toast.LENGTH_SHORT).show();}return true;}return false;}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent intent){super.onActivityResult(requestCode,resultCode,intent);if(requestCode==BACKUP_CREATE_REQUEST){ if(resultCode==RESULT_OK && intent!=null && intent.getData()!=null && pendingBackupJson!=null){ try(OutputStream out=getContentResolver().openOutputStream(intent.getData())){ if(out!=null){out.write(pendingBackupJson.getBytes(StandardCharsets.UTF_8));Toast.makeText(this,"백업 파일을 저장했습니다.",Toast.LENGTH_LONG).show();} }catch(Exception e){Toast.makeText(this,"백업 저장에 실패했습니다.",Toast.LENGTH_LONG).show();} } pendingBackupJson=null; return; } if(requestCode==FILE_CHOOSER_REQUEST){Uri selected=(resultCode==RESULT_OK&&intent!=null)?intent.getData():null;if(pendingApkInstall){pendingApkInstall=false;if(selected!=null){try{Intent install=new Intent(Intent.ACTION_VIEW);install.setDataAndType(selected,"application/vnd.android.package-archive");install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(install);}catch(Exception e){Toast.makeText(this,"APK 설치 화면을 열 수 없습니다. 파일 관리자에서 APK를 열어 주세요.",Toast.LENGTH_LONG).show();}}if(fileCallback!=null){fileCallback.onReceiveValue(null);fileCallback=null;}return;}if(fileCallback!=null){Uri[] results=null;if(resultCode==RESULT_OK&&intent!=null){if(intent.getClipData()!=null){int n=intent.getClipData().getItemCount();results=new Uri[n];for(int i=0;i<n;i++)results[i]=intent.getClipData().getItemAt(i).getUri();}else if(intent.getData()!=null)results=new Uri[]{intent.getData()};}fileCallback.onReceiveValue(results);fileCallback=null;}}}
    @Override protected void onSaveInstanceState(Bundle outState){webView.saveState(outState);super.onSaveInstanceState(outState);}
    @Override public void onBackPressed(){if(webView==null){super.onBackPressed();return;}webView.evaluateJavascript("(function(){try{var id=localStorage.getItem('matrip-current-trip');if(id)localStorage.setItem('matrip-current-trip',id);var modal=document.getElementById('imageViewer');if(modal&&!modal.hidden){modal.hidden=true;return 'handled';}var fx=document.getElementById('fxCalculator');if(fx&&!fx.hidden){fx.hidden=true;return 'handled';}var active=document.querySelector('.panel.active');if(active&&active.id!=='wallet'){document.querySelectorAll('[data-tab]').forEach(function(b){b.classList.toggle('active',b.dataset.tab==='wallet')});document.querySelectorAll('.panel').forEach(function(p){p.classList.toggle('active',p.id==='wallet')});return 'handled';}return 'exit';}catch(e){return 'exit';}})()", result->{if(result==null||result.contains("exit")){super.onBackPressed();}});}
}