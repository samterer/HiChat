package com.Hi961.HiChat.helpers.webview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.util.Map;

/**
 * Created by Abderrahim El imame on 1/16/17.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class VideoEnabledWebView extends WebView {
    public class JavascriptInterface {
        @android.webkit.JavascriptInterface
        public void notifyVideoEnd() // Must match Javascript interface method of VideoEnabledWebChromeClient
        {
            // This code is not executed in the UI thread, so we must force that to happen
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (videoEnabledWebChromeClient != null) {
                        videoEnabledWebChromeClient.onHideCustomView();
                    }
                }
            });
        }
    }

    private VideoEnabledWebChromeClient videoEnabledWebChromeClient;
    private boolean addedJavascriptInterface;


    private OnScrollChangedCallback mOnScrollChangedCallback;


    @Override
    protected void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mOnScrollChangedCallback != null) mOnScrollChangedCallback.onScroll(l, t);
    }

    public OnScrollChangedCallback getOnScrollChangedCallback() {
        return mOnScrollChangedCallback;
    }

    public void setOnScrollChangedCallback(final OnScrollChangedCallback onScrollChangedCallback) {
        mOnScrollChangedCallback = onScrollChangedCallback;
    }

    /**
     * Implement in the activity/fragment/view that you want to listen to the webview
     */
    public interface OnScrollChangedCallback {
        void onScroll(int l, int t);
    }

    public VideoEnabledWebView(Context context) {
        super(context);
        addedJavascriptInterface = false;
    }

    @SuppressWarnings("unused")
    public VideoEnabledWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        addedJavascriptInterface = false;
    }

    @SuppressWarnings("unused")
    public VideoEnabledWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        addedJavascriptInterface = false;
    }

    /**
     * Indicates if the video is being displayed using a custom view (typically full-screen)
     *
     * @return true it the video is being displayed using a custom view (typically full-screen)
     */
    public boolean isVideoFullscreen() {
        return videoEnabledWebChromeClient != null && videoEnabledWebChromeClient.isVideoFullscreen();
    }

    /**
     * Pass only a VideoEnabledWebChromeClient instance.
     */
    @Override
    @SuppressLint("SetJavaScriptEnabled")
    public void setWebChromeClient(WebChromeClient client) {
        getSettings().setJavaScriptEnabled(true);

        if (client instanceof VideoEnabledWebChromeClient) {
            this.videoEnabledWebChromeClient = (VideoEnabledWebChromeClient) client;
        }

        super.setWebChromeClient(client);
    }

    @Override
    public void loadData(String data, String mimeType, String encoding) {
        addJavascriptInterface();
        super.loadData(data, mimeType, encoding);
    }

    @Override
    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        addJavascriptInterface();
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    @Override
    public void loadUrl(String url) {
        addJavascriptInterface();
        super.loadUrl(url);
    }

    @Override
    public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
        addJavascriptInterface();
        super.loadUrl(url, additionalHttpHeaders);
    }

    private void addJavascriptInterface() {
        if (!addedJavascriptInterface) {
            // Add javascript interface to be called when the video ends (must be done before page load)
            addJavascriptInterface(new JavascriptInterface(), "_VideoEnabledWebView"); // Must match Javascript interface name of VideoEnabledWebChromeClient

            addedJavascriptInterface = true;
        }
    }

}
