package com.Hi961.HiChat.activities.main.welcome;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.View;
import android.view.Window;

import com.Hi961.HiChat.R;
import com.Hi961.HiChat.animations.AnimationsUtil;
import com.Hi961.HiChat.helpers.AppHelper;
import com.Hi961.HiChat.helpers.TopAlignSuperscriptSpan;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Abderrahim El imame on 1/9/17.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class SplashScreenActivity extends AppCompatActivity {

    int SPLASH_TIME_OUT = 3000;

    @BindView(R.id.splash_app_name)
    AppCompatTextView splashAppName;
    @BindView(R.id.splash_message)
    AppCompatTextView splashMessage;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        if (AppHelper.isAndroid5()) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().setStatusBarColor(AppHelper.getColor(this, R.color.colorBlackLight));
        }
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);
        setSuperScriptSpan();
       AnimationsUtil.fadeInOut(splashAppName);
        setTypeFaces();
        new Handler().postDelayed(this::launchWelcomeActivity, SPLASH_TIME_OUT);
    }

    private void setSuperScriptSpan() {
        String s = getString(R.string.app_name);

        SpannableString spannableString = new SpannableString(s);
        spannableString.setSpan(new TopAlignSuperscriptSpan((float) 0.35), s.length() - 5, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        splashAppName.setText(spannableString);
    }


    private void setTypeFaces() {
        splashAppName.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        splashMessage.setTypeface(AppHelper.setTypeFace(this, "Futura"));
    }

    public void launchWelcomeActivity() {
        AnimationsUtil.cancelAnimationSet(splashAppName);
        Intent mainIntent = new Intent(this, WelcomeActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainIntent);
        finish();
        AnimationsUtil.setSlideInAnimation(this);

    }
}