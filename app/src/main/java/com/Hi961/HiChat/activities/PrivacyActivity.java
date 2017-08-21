package com.Hi961.HiChat.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import com.Hi961.HiChat.R;
import com.Hi961.HiChat.animations.AnimationsUtil;
import com.Hi961.HiChat.app.AppConstants;
import com.Hi961.HiChat.helpers.AppHelper;
import com.Hi961.HiChat.presenters.users.ContactsPresenter;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Abderrahim El imame on 3/13/17.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class PrivacyActivity extends AppCompatActivity {

    @BindView(R.id.privacy_termes)
    TextView privacyTermes;

    @BindView(R.id.app_bar)
    Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_help);
        ButterKnife.bind(this);
        setTypeFaces();
        setupToolbar();
        ContactsPresenter contactsPresenter = new ContactsPresenter(this);
        contactsPresenter.onCreate();
    }


    /**
     * method to setup the toolbar
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(R.string.title_activity_privacy);
    }

    private void setTypeFaces() {

        if (AppConstants.ENABLE_FONTS_TYPES)
            privacyTermes.setTypeface(AppHelper.setTypeFace(this, "Futura"));
    }

    public void showPrivcay(String privacy) {
        //String htmlText = "<html><body style=\"text-align:left\"> %s </body></Html>";
        privacyTermes.setText(privacy);
        // privacyTermes.setText(Html.fromHtml(String.format(htmlText, privacy)));
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            AnimationsUtil.setSlideOutAnimation(this);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AnimationsUtil.setSlideOutAnimation(this);
    }
}
