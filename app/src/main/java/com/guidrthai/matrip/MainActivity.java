package com.guidrthai.matrip;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
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

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_CHOOSER_REQUEST = 4001;
    private WebViewAssetLoader assetLoader;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assetLoader = new WebViewAssetLoader.Builder().addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        webView = new WebView(this); setContentView(webView);
        WebSettings settings=webView.getSettings(); settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true); settings.setAllowContentAccess(true); settings.setJavaScriptCanOpenWindowsAutomatically(true); settings.setSupportMultipleWindows(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) { return assetLoader.shouldInterceptRequest(request.getUrl()); }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { Uri u=request.getUrl(); if("appassets.androidplatform.net".equals(u.getHost()))return false; return openExternal(u); }
            @Override public boolean shouldOverrideUrlLoading(WebView view,String url) { Uri u=Uri.parse(url); if("appassets.androidplatform.net".equals(u.getHost()))return false; return openExternal(u); }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> callback,FileChooserParams params) {
                if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=callback;
                Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
                try{startActivityForResult(i,FILE_CHOOSER_REQUEST);return true;}catch(ActivityNotFoundException e){fileCallback=null;Toast.makeText(MainActivity.this,"파일 선택 앱을 찾을 수 없습니다.",Toast.LENGTH_LONG).show();return false;}
            }
            @Override public boolean onCreateWindow(WebView view,boolean isDialog,boolean isUserGesture,android.os.Message resultMsg) {
                WebView popup=new WebView(MainActivity.this);popup.setWebViewClient(new WebViewClient(){
                    @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return openExternal(r.getUrl());}
                    @Override public boolean shouldOverrideUrlLoading(WebView v,String u){return openExternal(Uri.parse(u));}
                });((WebView.WebViewTransport)resultMsg.obj).setWebView(popup);resultMsg.sendToTarget();return true;
            }
        });
        if(savedInstanceState==null)webView.loadUrl("https://appassets.androidplatform.net/assets/TripPocket_travel_app.html");else webView.restoreState(savedInstanceState);
    }
    private boolean openExternal(Uri uri){String s=uri.getScheme();if("http".equals(s)||"https".equals(s)||"mailto".equals(s)||"tel".equals(s)){try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(ActivityNotFoundException e){Toast.makeText(this,"열 수 있는 앱을 찾을 수 없습니다.",Toast.LENGTH_SHORT).show();}return true;}return false;}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent intent){super.onActivityResult(requestCode,resultCode,intent);if(requestCode==FILE_CHOOSER_REQUEST&&fileCallback!=null){Uri[] results=null;if(resultCode==RESULT_OK&&intent!=null){if(intent.getClipData()!=null){int n=intent.getClipData().getItemCount();results=new Uri[n];for(int i=0;i<n;i++)results[i]=intent.getClipData().getItemAt(i).getUri();}else if(intent.getData()!=null)results=new Uri[]{intent.getData()};}fileCallback.onReceiveValue(results);fileCallback=null;}}
    @Override protected void onSaveInstanceState(Bundle outState){webView.saveState(outState);super.onSaveInstanceState(outState);}
    @Override public void onBackPressed(){if(webView!=null&&webView.canGoBack())webView.goBack();else super.onBackPressed();}
}