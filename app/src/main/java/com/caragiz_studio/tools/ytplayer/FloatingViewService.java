package com.caragiz_studio.tools.ytplayer;

import android.animation.Animator;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LayoutAnimationController;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Created by caragiz on 13/4/17.
 */
public class FloatingViewService extends Service {
    private WindowManager mWindowManager;
    private View mFloatingView;
    private WindowManager.LayoutParams params;
    View collapsedView;
    View expandedView;
    private boolean fullScreen = false;
    private boolean searching = false;

    public FloatingViewService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    Long time;
    FirebaseAnalytics analytics;

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Bundle bundle = new Bundle();
        time = System.currentTimeMillis() - time;
        bundle.putString("Start_time", String.valueOf(System.currentTimeMillis()));
        analytics.logEvent("Test_Data", bundle);

    }

    @Override
    public void onCreate() {
        super.onCreate();
        analytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();
        time = System.currentTimeMillis();
        bundle.putString("Start_time", String.valueOf(System.currentTimeMillis()));
        analytics.logEvent("Test_Data", bundle);
        //Inflate the floating view layout we created
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null);

        collapsedView = mFloatingView.findViewById(R.id.collapse_view);
        //The root element of the expanded view layout
        expandedView = mFloatingView.findViewById(R.id.expanded_container);


        final WebView webView = (WebView) mFloatingView.findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("http://www.youtube.com");
//        webView.setWebViewClient(new myWebClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });
        webView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_BACK:
                            if (webView.canGoBack())
                                webView.goBack();
                            return true;
                    }
                }
                return false;
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int deviceWidth = metrics.widthPixels;
        int deviceHeight = metrics.heightPixels;

        RelativeLayout.LayoutParams prams = new RelativeLayout.LayoutParams(deviceWidth, deviceHeight / 2);
        expandedView.setLayoutParams(prams);

        //Add the view to the window.
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //Specify the view position
        params.gravity = Gravity.TOP | Gravity.LEFT;        //Initially view will be added to top-left corner
        params.x = 0;
        params.y = 100;

        //Add the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);

        //The root element of the collapsed view layout


        //Set the close button
        final ImageView closeButton = (ImageView) mFloatingView.findViewById(R.id.minimise_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webView.animate().alpha(0).setDuration(200);
                collapsedView.setVisibility(View.VISIBLE);
                expandedView.setVisibility(View.GONE);
                if(searching) {
                    mFloatingView.findViewById(R.id.searchContainer).setVisibility(View.INVISIBLE);
                    searching = !searching;
                }

                webView.clearFocus();
                mFloatingView.clearFocus();

                params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_PHONE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);
                params.gravity = Gravity.LEFT;
                //Add the view to the window
                if (mWindowManager == null)
                    mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                mWindowManager.updateViewLayout(mFloatingView, params);
            }
        });


        //Open the application on thi button click
        ImageView openButton = (ImageView) mFloatingView.findViewById(R.id.close_button);
        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Open the application  click.
                Log.d("Status", "Stopping");
                //close the service and remove view from the view hierarchy
                stopSelf();
            }
        });
        ImageView settingsBtn = (ImageView) mFloatingView.findViewById(R.id.settings_button);
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                RelativeLayout.LayoutParams prams;
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                int deviceWidth = metrics.widthPixels;
                int deviceHeight = metrics.heightPixels;
                if (fullScreen) {
                    prams = new RelativeLayout.LayoutParams(deviceWidth, deviceHeight / 2);
                } else {
                    prams = new RelativeLayout.LayoutParams(deviceWidth, deviceHeight-200);
                }
                fullScreen = !fullScreen;

                expandedView.setLayoutParams(prams);
            }
        });

        //Drag and move floating view using user's touch action.
        mFloatingView.findViewById(R.id.root_container).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;


            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        //remember the initial position.
                        initialX = params.x;
                        initialY = params.y;

                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);


                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        //So that is click event.
                        if (Xdiff < 10 && Ydiff < 10) {
                            if (isViewCollapsed()) {
                                int cx = expandedView.getWidth() / 2;
                                int cy = expandedView.getHeight() / 2;

// get the final radius for the clipping circle
                                float finalRadius = (float) Math.hypot(cx, cy);

// create the animator for this view (the start radius is zero)
                                Animator anim =
                                        ViewAnimationUtils.createCircularReveal(expandedView, cx, cy, 0, finalRadius);

// make the view visible and start the animation
                                collapsedView.setVisibility(View.GONE);
                                expandedView.setVisibility(View.VISIBLE);
                                webView.animate().alpha(1).setDuration(500);
                                anim.start();

                                params = new WindowManager.LayoutParams(
                                        WindowManager.LayoutParams.WRAP_CONTENT,
                                        WindowManager.LayoutParams.WRAP_CONTENT,
                                        WindowManager.LayoutParams.TYPE_PHONE,
                                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                                        PixelFormat.TRANSLUCENT);
                                params.gravity = Gravity.LEFT;
                                //Add the view to the window
                                if (mWindowManager == null)
                                    mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                                mWindowManager.updateViewLayout(mFloatingView, params);
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);

                        if (event.getRawX() > getResources().getDisplayMetrics().widthPixels / 2)
                            params.gravity = Gravity.RIGHT;
                        else
                            params.gravity = Gravity.LEFT;
                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mFloatingView, params);
                        return true;
                }
                return false;
            }
        });

        final ImageView search = (ImageView)mFloatingView.findViewById(R.id.start_search);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout searchContainer = (LinearLayout)mFloatingView.findViewById(R.id.searchContainer);
                if(!searching){

                    searchContainer.setVisibility(View.VISIBLE);
                    if(fullScreen){
                        ViewGroup.LayoutParams currentparams = expandedView.getLayoutParams();
                        currentparams.height = currentparams.height - (2*searchContainer.getLayoutParams().height);
                        expandedView.setLayoutParams(currentparams);
                    }
                }else {
                    searchContainer.setVisibility(View.INVISIBLE);
                    if(fullScreen){
                        ViewGroup.LayoutParams currentparams = expandedView.getLayoutParams();
                        currentparams.height = getResources().getDisplayMetrics().heightPixels-200;
                        expandedView.setLayoutParams(currentparams);
                    }
                }

                searching = !searching;
            }
        });

        ImageButton searchButton = (ImageButton)mFloatingView.findViewById(R.id.search_btn);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String search = ((EditText)mFloatingView.findViewById(R.id.searchBox)).getText().toString();
                webView.loadUrl("https://www.youtube.com/results?search_query="+search);
                mFloatingView.findViewById(R.id.searchBox).clearFocus();
            }
        });
    }


    /**
     * Detect if the floating view is collapsed or expanded.
     *
     * @return true if the floating view is collapsed.
     */
    private boolean isViewCollapsed() {
        return mFloatingView == null || mFloatingView.findViewById(R.id.collapse_view).getVisibility() == View.VISIBLE;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }
}