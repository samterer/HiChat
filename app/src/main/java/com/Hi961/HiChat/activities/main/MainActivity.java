package com.Hi961.HiChat.activities.main;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.firebase.iid.FirebaseInstanceId;
import com.Hi961.HiChat.R;
import com.Hi961.HiChat.activities.NewConversationContactsActivity;
import com.Hi961.HiChat.activities.groups.AddMembersToGroupActivity;
import com.Hi961.HiChat.activities.main.welcome.WelcomeActivity;
import com.Hi961.HiChat.activities.messages.TransferMessageContactsActivity;
import com.Hi961.HiChat.activities.search.SearchCallsActivity;
import com.Hi961.HiChat.activities.search.SearchContactsActivity;
import com.Hi961.HiChat.activities.search.SearchConversationsActivity;
import com.Hi961.HiChat.activities.settings.SettingsActivity;
import com.Hi961.HiChat.activities.status.StatusActivity;
import com.Hi961.HiChat.adapters.others.HomeTabsAdapter;
import com.Hi961.HiChat.animations.AnimationsUtil;
import com.Hi961.HiChat.api.APIHelper;
import com.Hi961.HiChat.app.AppConstants;
import com.Hi961.HiChat.app.WhatsCloneApplication;
import com.Hi961.HiChat.helpers.AppHelper;
import com.Hi961.HiChat.helpers.ForegroundRuning;
import com.Hi961.HiChat.helpers.OutDateHelper;
import com.Hi961.HiChat.helpers.PermissionHandler;
import com.Hi961.HiChat.helpers.PreferenceManager;
import com.Hi961.HiChat.helpers.RateHelper;
import com.Hi961.HiChat.helpers.notifications.NotificationsManager;
import com.Hi961.HiChat.interfaces.NetworkListener;
import com.Hi961.HiChat.models.calls.CallsInfoModel;
import com.Hi961.HiChat.models.calls.CallsModel;
import com.Hi961.HiChat.models.messages.ConversationsModel;
import com.Hi961.HiChat.models.users.Pusher;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import org.greenrobot.eventbus.EventBus;
import io.realm.Realm;
import io.realm.RealmResults;
import io.socket.client.Socket;

import static com.Hi961.HiChat.app.AppConstants.EVENT_BUS_MESSAGE_COUNTER;
import static com.Hi961.HiChat.app.AppConstants.EVENT_BUS_NEW_USER_JOINED;
import static com.Hi961.HiChat.app.AppConstants.EVENT_BUS_START_REFRESH;
import static com.Hi961.HiChat.app.AppConstants.EVENT_BUS_STOP_REFRESH;

/**
 * Created by Abderrahim El imame on 01/03/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class MainActivity extends AppCompatActivity implements NetworkListener {

    @BindView(R.id.viewpager)
    ViewPager viewPager;
    @BindView(R.id.main_activity)
    View mView;
    @BindView(R.id.adParentLyout)
    LinearLayout adParentLyout;
    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.app_bar)
    Toolbar toolbar;
    @BindView(R.id.main_view)
    LinearLayout MainView;
    @BindView(R.id.toolbar_progress_bar)
    ProgressBar toolbarProgressBar;

    @BindView(R.id.floatingBtnMain)
    FloatingActionButton floatingBtnMain;


    InterstitialAd mInterstitialAd;
    boolean actionModeStarted = false;


    private Socket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Permissions();
        initializerView();
        setupToolbar();
        setTypeFaces();
        EventBus.getDefault().register(this);
        checkIfUserSession();
        loadCounter();
        if (PreferenceManager.ShowInterstitialrAds(this)) {
            if (PreferenceManager.getUnitInterstitialAdID(this) != null) {
                initializerAds();
            }
        }

        showMainAds();
        RateHelper.appLaunched(this);
        connectToServer();
        PreferenceManager.setIsNeedInfo(this, false);
        registerFCM();


    }

    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            ((TextView) findViewById(R.id.title_tabs_contacts)).setTypeface(AppHelper.setTypeFace(this, "Futura"));
            ((TextView) findViewById(R.id.title_tabs_messages)).setTypeface(AppHelper.setTypeFace(this, "Futura"));
            ((TextView) findViewById(R.id.title_tabs_calls)).setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
    }

    private void connectToServer() {

        WhatsCloneApplication app = (WhatsCloneApplication) getApplication();
        mSocket = app.getSocket();
        if (mSocket != null) {
            if (!mSocket.connected())
                mSocket.connect();

            JSONObject json = new JSONObject();
            try {
                json.put("connected", true);
                json.put("senderId", PreferenceManager.getID(this));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mSocket.emit(AppConstants.SOCKET_IS_ONLINE, json);
        }
    }


    private void initializerAds() {
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(PreferenceManager.getUnitInterstitialAdID(this));
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
                AppHelper.LaunchActivity(MainActivity.this, SettingsActivity.class);
            }
        });

        requestNewInterstitial();
    }


    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mInterstitialAd.loadAd(adRequest);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_conversations:
                RateHelper.significantEvent(this);
                if (AppHelper.isAndroid5()) {
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, new Pair<>(toolbar, "searchBar"));
                    Intent mIntent = new Intent(this, SearchConversationsActivity.class);
                    startActivity(mIntent, options.toBundle());
                } else {
                    AppHelper.LaunchActivity(this, SearchConversationsActivity.class);
                }
                break;
            case R.id.search_contacts:
                if (AppHelper.isAndroid5()) {
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, new Pair<>(toolbar, "searchBar"));
                    Intent mIntent = new Intent(this, SearchContactsActivity.class);
                    startActivity(mIntent, options.toBundle());
                } else {
                    AppHelper.LaunchActivity(this, SearchContactsActivity.class);
                }
                break;
            case R.id.search_calls:
                if (AppHelper.isAndroid5()) {
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, new Pair<>(toolbar, "searchBar"));
                    Intent mIntent = new Intent(this, SearchCallsActivity.class);
                    startActivity(mIntent, options.toBundle());
                } else {
                    AppHelper.LaunchActivity(this, SearchCallsActivity.class);
                }

                break;
            case R.id.new_group:
                RateHelper.significantEvent(this);
                PreferenceManager.clearMembers(this);
                AppHelper.LaunchActivity(this, AddMembersToGroupActivity.class);
                break;
            case R.id.settings:
                RateHelper.significantEvent(this);
                if (PreferenceManager.ShowInterstitialrAds(this)) {
                    if (mInterstitialAd.isLoaded()) {
                        mInterstitialAd.show();
                    } else {
                        AppHelper.LaunchActivity(this, SettingsActivity.class);
                    }
                } else {
                    AppHelper.LaunchActivity(this, SettingsActivity.class);
                }
                break;
            case R.id.status:
                RateHelper.significantEvent(this);
                AppHelper.LaunchActivity(this, StatusActivity.class);
                break;

            case R.id.clear_log_call:
                RateHelper.significantEvent(this);
                removeCallsLog();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void removeCallsLog() {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        AppHelper.showDialog(this, getString(R.string.delete_call_dialog));
        realm.executeTransactionAsync(realm1 -> {
            RealmResults<CallsModel> callsModels = realm1.where(CallsModel.class).findAll();
            for (CallsModel callsModel : callsModels) {
                RealmResults<CallsInfoModel> callsInfoModel = realm1.where(CallsInfoModel.class).equalTo("callId", callsModel.getId()).findAll();
                callsInfoModel.deleteAllFromRealm();
            }
            callsModels.deleteAllFromRealm();
        }, () -> {
            AppHelper.hideDialog();
            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_DELETE_CALL_ITEM, 0));
        }, error -> {
            AppHelper.LogCat(error.getMessage());
            AppHelper.hideDialog();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        switch (tabLayout.getSelectedTabPosition()) {
            case 0:
                getMenuInflater().inflate(R.menu.calls_menu, menu);
                break;
            case 1:
                getMenuInflater().inflate(R.menu.conversations_menu, menu);
                break;
            case 2:
                getMenuInflater().inflate(R.menu.contacts_menu, menu);
                break;
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * method to setup toolbar
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(false);
        }
    }

    /**
     * method to initialize the view
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void initializerView() {

        if (PreferenceManager.isOutDate(this)) {
            OutDateHelper.appLaunched(this);
            OutDateHelper.significantEvent(this);
        }
        viewPager.setAdapter(new HomeTabsAdapter(getSupportFragmentManager()));
        viewPager.setOffscreenPageLimit(2);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        viewPager.setCurrentItem(1);
        tabLayout.getTabAt(0).setCustomView(R.layout.custom_tab_calls);
        tabLayout.getTabAt(1).setCustomView(R.layout.custom_tab_messages);
        tabLayout.getTabAt(2).setCustomView(R.layout.custom_tab_contacts);
        ((TextView) findViewById(R.id.title_tabs_contacts)).setTextColor(AppHelper.getColor(this, R.color.colorUnSelected));
        ((TextView) findViewById(R.id.title_tabs_calls)).setTextColor(AppHelper.getColor(this, R.color.colorUnSelected));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Drawable icon = AppHelper.getVectorDrawable(MainActivity.this, R.drawable.ic_chat_white_24dp);
                switch (tab.getPosition()) {
                    case 0:
                        icon = AppHelper.getVectorDrawable(MainActivity.this, R.drawable.ic_call_white_24dp);
                        viewPager.setCurrentItem(0);
                        findViewById(R.id.counterTabMessages).setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        findViewById(R.id.counterTabCalls).setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter));
                        ((TextView) findViewById(R.id.title_tabs_calls)).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorWhite));
                        break;
                    case 1:
                        icon = AppHelper.getVectorDrawable(MainActivity.this, R.drawable.ic_chat_white_24dp);
                        viewPager.setCurrentItem(1);
                        findViewById(R.id.counterTabMessages).setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter));
                        findViewById(R.id.counterTabCalls).setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        ((TextView) findViewById(R.id.title_tabs_messages)).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorWhite));
                        break;
                    case 2:
                        icon = AppHelper.getVectorDrawable(MainActivity.this, R.drawable.ic_person_add_24dp);
                        viewPager.setCurrentItem(2);
                        findViewById(R.id.counterTabMessages).setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        findViewById(R.id.counterTabCalls).setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        ((TextView) findViewById(R.id.title_tabs_contacts)).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorWhite));
                        break;
                    default:
                        break;
                }
                floatingBtnMain.setImageDrawable(icon);
                if (tab.getPosition() != 1) {
                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_ACTION_MODE_FINISHED));
                }
                final Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.scale_for_button_animtion_enter);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        floatingBtnMain.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                floatingBtnMain.startAnimation(animation);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        findViewById(R.id.counterTabCalls).setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        ((TextView) findViewById(R.id.title_tabs_calls)).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorUnSelected));
                        break;
                    case 1:
                        findViewById(R.id.counterTabMessages).setBackground(AppHelper.getDrawable(MainActivity.this, R.drawable.bg_circle_tab_counter_unselected));
                        ((TextView) findViewById(R.id.title_tabs_messages)).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorUnSelected));
                        break;
                    case 2:
                        findViewById(R.id.counterTabMessages).setBackground(getResources().getDrawable(R.drawable.bg_circle_tab_counter_unselected));
                        findViewById(R.id.counterTabCalls).setBackground(getResources().getDrawable(R.drawable.bg_circle_tab_counter_unselected));
                        ((TextView) findViewById(R.id.title_tabs_contacts)).setTextColor(AppHelper.getColor(MainActivity.this, R.color.colorUnSelected));
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {


            }
        });

        floatingBtnMain.setOnClickListener(view -> {
            switch (tabLayout.getSelectedTabPosition()) {
                case 0:
                    RateHelper.significantEvent(this);
                    Intent intent = new Intent(this, TransferMessageContactsActivity.class);
                    intent.putExtra("forCall", true);
                    startActivity(intent);
                    AnimationsUtil.setSlideInAnimation(this);
                    break;
                case 1:
                    RateHelper.significantEvent(this);
                    AppHelper.LaunchActivity(this, NewConversationContactsActivity.class);
                    break;
                case 2:
                    RateHelper.significantEvent(this);
                    try {
                        Intent mIntent = new Intent(Intent.ACTION_INSERT);
                        mIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                        mIntent.putExtra(ContactsContract.Intents.Insert.PHONE, "");
                        startActivityForResult(mIntent, 50);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        });


    }


    private void showMainAds() {
        if (PreferenceManager.ShowBannerAds(this)) {
            adParentLyout.setVisibility(View.VISIBLE);
            if (PreferenceManager.getUnitBannerAdsID(this) != null) {
                AdView mAdView = new AdView(this);
                mAdView.setAdSize(AdSize.BANNER);
                mAdView.setAdUnitId(PreferenceManager.getUnitBannerAdsID(this));
                AdRequest adRequest = new AdRequest.Builder()
                        .build();
                if (mAdView.getAdSize() != null || mAdView.getAdUnitId() != null)
                    mAdView.loadAd(adRequest);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                adParentLyout.addView(mAdView, params);
            }
        } else {
            adParentLyout.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainView.setVisibility(View.GONE);
        WhatsCloneApplication.getInstance().setConnectivityListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);

        DisConnectFromServer();
    }


    private void Permissions() {
        if (PermissionHandler.checkPermission(this, Manifest.permission.READ_CONTACTS)) {
            AppHelper.LogCat("Read contact data permission already granted.");
        } else {
            AppHelper.LogCat("Please request Read contact data permission.");
            AppHelper.showPermissionDialog(this);
            PermissionHandler.requestPermission(this, Manifest.permission.READ_CONTACTS);
        }
        if (PermissionHandler.checkPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            AppHelper.LogCat("Read storage data permission already granted.");
        } else {
            AppHelper.LogCat("Please request Read storage data permission.");
            PermissionHandler.requestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AppConstants.CONTACTS_PERMISSION_REQUEST_CODE) {
            AppHelper.hidePermissionsDialog();
            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_CONTACTS_PERMISSION));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AppConstants.CONTACTS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AppHelper.hidePermissionsDialog();
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_CONTACTS_PERMISSION));
            }
        }
    }

    /**
     * method of EventBus
     *
     * @param pusher this is parameter of onEventMainThread method
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Pusher pusher) {
        switch (pusher.getAction()) {
            case EVENT_BUS_START_REFRESH:
                toolbarProgressBar.setVisibility(View.VISIBLE);
                toolbarProgressBar.getIndeterminateDrawable().setColorFilter(AppHelper.getColor(this, R.color.colorWhite), PorterDuff.Mode.SRC_IN);
                break;
            case EVENT_BUS_STOP_REFRESH:
                toolbarProgressBar.setVisibility(View.GONE);
                break;
            case EVENT_BUS_MESSAGE_COUNTER:
                new Handler().postDelayed(this::loadCounter, 500);
                break;
            case EVENT_BUS_NEW_USER_JOINED:
                JSONObject jsonObject = pusher.getJsonObject();
                try {
                    String phone = jsonObject.getString("phone");
                    int senderId = jsonObject.getInt("senderId");
                    new Handler().postDelayed(() -> {
                        Intent mIntent = new Intent("new_user_joined_notification_whatsclone");
                        mIntent.putExtra("conversationID", 0);
                        mIntent.putExtra("recipientID", senderId);
                        mIntent.putExtra("phone", phone);
                        mIntent.putExtra("message", AppConstants.JOINED_MESSAGE_SMS);
                        sendBroadcast(mIntent);
                    }, 2500);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case AppConstants.EVENT_BUS_NEW_CONTACT_ADDED:
                break;
            case AppConstants.EVENT_BUS_START_CONVERSATION:
                if (viewPager.getCurrentItem() == 3)
                    viewPager.setCurrentItem(1);
                break;
            case AppConstants.EVENT_BUS_ACTION_MODE_STARTED:
                actionModeStarted();
                break;
            case AppConstants.EVENT_BUS_ACTION_MODE_DESTROYED:
                actionModeDestroyed();
                break;

        }


    }


    private void actionModeDestroyed() {
        if (actionModeStarted) {
            actionModeStarted = false;
            tabLayout.setBackgroundColor(AppHelper.getColor(this, R.color.colorPrimary));
            if (AppHelper.isAndroid5()) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(AppHelper.getColor(this, R.color.colorPrimaryDark));
            }
        }
    }

    private void actionModeStarted() {
        if (!actionModeStarted) {
            actionModeStarted = true;
            tabLayout.setBackgroundColor(AppHelper.getColor(this, R.color.colorActionMode));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(AppHelper.getColor(this, R.color.colorActionMode));
            }
        }

    }

    /**
     * Callback will be triggered when there is change in
     * network connection
     */
    @Override
    public void onNetworkConnectionChanged(boolean isConnecting, boolean isConnected) {
        if (!isConnecting && !isConnected) {
            AppHelper.Snackbar(this, mView, getString(R.string.connection_is_not_available), AppConstants.MESSAGE_COLOR_ERROR, AppConstants.TEXT_COLOR);
        } else if (isConnecting && isConnected) {
            AppHelper.Snackbar(this, mView, getString(R.string.connection_is_available), AppConstants.MESSAGE_COLOR_SUCCESS, AppConstants.TEXT_COLOR);
        } else {
            AppHelper.Snackbar(this, mView, getString(R.string.waiting_for_network), AppConstants.MESSAGE_COLOR_WARNING, AppConstants.TEXT_COLOR);

        }
    }


    /**
     * methdo to loadCircleImage number of unread messages
     */
    private void loadCounter() {
        int messageCounter = 0;
        try {
            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            List<ConversationsModel> conversationsModel1 = realm.where(ConversationsModel.class)
                    .notEqualTo("UnreadMessageCounter", "0")
                    .findAll();
            if (conversationsModel1.size() != 0) {
                messageCounter = conversationsModel1.size();
            }

            if (messageCounter == 0) {
                findViewById(R.id.counterTabMessages).setVisibility(View.GONE);
            } else {
                findViewById(R.id.counterTabMessages).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.counterTabMessages)).setText(String.valueOf(messageCounter));
            }
            if (!realm.isClosed())
                realm.close();

        } catch (Exception e) {
            AppHelper.LogCat("loadCounter main activity " + e.getMessage());
        }
        NotificationsManager.SetupBadger(this);

    }

    /**
     * method to disconnect from socket server
     */
    private void DisConnectFromServer() {

        try {
            JSONObject json = new JSONObject();
            try {
                json.put("connected", false);
                json.put("senderId", PreferenceManager.getID(this));

            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (mSocket != null)
                mSocket.emit(AppConstants.SOCKET_IS_ONLINE, json);
        } catch (Exception e) {
            AppHelper.LogCat("User is offline  Exception MainActivity" + e.getMessage());
        }
    }


    /**
     * method to check if user connect in an other device
     */
    public void checkIfUserSession() {
        APIHelper.initialApiUsersContacts().checkIfUserSession().subscribe(networkModel -> {
            if (!networkModel.isConnected()) {
                if (ForegroundRuning.get().isForeground()) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                    alert.setMessage(R.string.your_session_expired);
                    alert.setPositiveButton(R.string.ok, (dialog, which) -> {
                        PreferenceManager.setToken(MainActivity.this, null);
                        PreferenceManager.setID(MainActivity.this, 0);
                        PreferenceManager.setSocketID(MainActivity.this, null);
                        PreferenceManager.setPhone(MainActivity.this, null);
                        PreferenceManager.setIsWaitingForSms(MainActivity.this, false);
                        PreferenceManager.setMobileNumber(MainActivity.this, null);
                        Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    });
                    alert.setCancelable(false);
                    alert.show();
                }
            }
        }, throwable -> {
            AppHelper.LogCat("checkIfUserSession MainActivity " + throwable.getMessage());
        });
    }


    private void registerFCM() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        AppHelper.LogCat("refreshedToken " + refreshedToken);
        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(final String token) {
        if (token != null) {
            updateRegisteredId(token);
        }
    }


    public void updateRegisteredId(String registeredId) {
        APIHelper.initialApiUsersContacts().updateRegisteredId(registeredId).subscribe(qResponse -> {
            if (qResponse.isSuccess()) {

                JSONObject json = new JSONObject();
                try {
                    json.put("userId", PreferenceManager.getID(this));
                    json.put("register_id", qResponse.getRegistered_id());

                    mSocket.emit(AppConstants.SOCKET_UPDATE_REGISTER_ID, json);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            AppHelper.LogCat(qResponse.getMessage());
        }, throwable -> {
            AppHelper.LogCat(throwable.getMessage());
        });
    }

}