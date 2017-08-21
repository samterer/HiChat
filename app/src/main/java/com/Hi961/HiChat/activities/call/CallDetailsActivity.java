package com.Hi961.HiChat.activities.call;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.Hi961.HiChat.R;
import com.Hi961.HiChat.activities.messages.MessagesActivity;
import com.Hi961.HiChat.adapters.recyclerView.calls.CallsDetailsAdapter;
import com.Hi961.HiChat.animations.AnimationsUtil;
import com.Hi961.HiChat.api.APIHelper;
import com.Hi961.HiChat.app.AppConstants;
import com.Hi961.HiChat.app.EndPoints;
import com.Hi961.HiChat.app.WhatsCloneApplication;
import com.Hi961.HiChat.helpers.AppHelper;
import com.Hi961.HiChat.helpers.Files.backup.RealmBackupRestore;
import com.Hi961.HiChat.helpers.Files.cache.ImageLoader;
import com.Hi961.HiChat.helpers.Files.cache.MemoryCache;
import com.Hi961.HiChat.helpers.UtilsPhone;
import com.Hi961.HiChat.helpers.call.CallManager;
import com.Hi961.HiChat.helpers.images.WhatsCloneImageLoader;
import com.Hi961.HiChat.models.calls.CallsInfoModel;
import com.Hi961.HiChat.models.calls.CallsModel;
import com.Hi961.HiChat.models.users.contacts.ContactsModel;
import com.Hi961.HiChat.models.users.contacts.UsersBlockModel;
import com.Hi961.HiChat.presenters.calls.CallsPresenter;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * Created by Abderrahim El imame on 12/18/16.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class CallDetailsActivity extends AppCompatActivity {

    @BindView(R.id.CallsDetailsList)
    RecyclerView CallsList;

    @BindView(R.id.user_image)
    ImageView userImage;

    @BindView(R.id.username)
    TextView username;

    @BindView(R.id.CallVideoBtn)
    AppCompatImageView CallVideoBtn;

    @BindView(R.id.CallBtn)
    AppCompatImageView CallBtn;

    @BindView(R.id.app_bar)
    Toolbar toolbar;

    private CallsDetailsAdapter mCallsAdapter;
    private CallsPresenter mCallsPresenter;

    private int userID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_details);
        ButterKnife.bind(this);
        initializerView();
        setupToolbar();
        setTypeFaces();
        mCallsPresenter = new CallsPresenter(this);
        mCallsPresenter.onCreate();
        userID = getIntent().getIntExtra("userID", 0);
    }

    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            username.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
    }

    /**
     * method to initialize the view
     */
    private void initializerView() {
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mCallsAdapter = new CallsDetailsAdapter(this);
        CallsList.setLayoutManager(mLinearLayoutManager);
        CallsList.setAdapter(mCallsAdapter);
        CallsList.setItemAnimator(new DefaultItemAnimator());
        CallsList.getItemAnimator().setChangeDuration(0);
        CallBtn.setOnClickListener(v -> CallManager.callContact(this, true, false, userID));
        CallVideoBtn.setOnClickListener(v -> CallManager.callContact(this, true, true, userID));
    }

    /**
     * method to setup the toolbar
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(R.string.title_calls);
    }

    public void UpdateCallsDetailsList(RealmResults<CallsInfoModel> callsModelList) {
        if (callsModelList.size() != 0) {
            RealmList<CallsInfoModel> callsModels = new RealmList<CallsInfoModel>();
            for (CallsInfoModel callsModel : callsModelList) {
                callsModels.add(callsModel);
            }
            mCallsAdapter.setCalls(callsModels);
        }
    }

    private boolean checkIfUserBlockedExist(int userId, Realm realm) {
        RealmQuery<UsersBlockModel> query = realm.where(UsersBlockModel.class).equalTo("contactsModel.id", userId);
        return query.count() != 0;
    }

    public void refreshMenu() {
        invalidateOptionsMenu();
        supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        refreshMenu();
        if (checkIfUserBlockedExist(userID, mCallsPresenter.getRealm())) {
            getMenuInflater().inflate(R.menu.calls_info_menu_unblock, menu);
        } else {
            getMenuInflater().inflate(R.menu.calls_info_menu, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            AnimationsUtil.setSlideOutAnimation(this);
        } else if (item.getItemId() == R.id.new_message) {
            Intent messagingIntent = new Intent(this, MessagesActivity.class);
            messagingIntent.putExtra("conversationID", 0);
            messagingIntent.putExtra("recipientID", userID);
            messagingIntent.putExtra("isGroup", false);
            startActivity(messagingIntent);
            AnimationsUtil.setSlideInAnimation(this);
            finish();
        } else if (item.getItemId() == R.id.remove_from_log) {
            mCallsPresenter.removeCall();

        } else if (item.getItemId() == R.id.block_user) {
            Realm realm2 = WhatsCloneApplication.getRealmDatabaseInstance();
            AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
            builder2.setMessage(R.string.block_user_make_sure);
            builder2.setPositiveButton(R.string.Yes, (dialog, whichButton) -> {

                APIHelper.initialApiUsersContacts().unbBlock(userID).subscribe(blockResponse -> {
                    if (blockResponse.isSuccess()) {
                        realm2.executeTransactionAsync(realm1 -> {
                            ContactsModel contactsModel = realm1.where(ContactsModel.class).equalTo("id", userID).findFirst();
                            UsersBlockModel usersBlockModel = new UsersBlockModel();
                            usersBlockModel.setId(RealmBackupRestore.getBlockUserLastId());
                            usersBlockModel.setContactsModel(contactsModel);
                            realm1.copyToRealmOrUpdate(usersBlockModel);
                        }, this::refreshMenu, error -> {
                            AppHelper.LogCat("Block user" + error.getMessage());

                        });
                        AppHelper.CustomToast(this, blockResponse.getMessage());
                    } else {
                        AppHelper.CustomToast(this, blockResponse.getMessage());
                    }
                }, throwable -> {
                    AppHelper.CustomToast(this, getString(R.string.oops_something));
                });


            });

            builder2.setNegativeButton(R.string.No, (dialog, whichButton) -> {

            });

            builder2.show();
            if (!realm2.isClosed())
                realm2.close();
        } else if (item.getItemId() == R.id.unblock_user) {
            Realm realmUnblock = WhatsCloneApplication.getRealmDatabaseInstance();
            AlertDialog.Builder builderUnblock = new AlertDialog.Builder(this);
            builderUnblock.setMessage(R.string.unblock_user_make_sure);
            builderUnblock.setPositiveButton(R.string.Yes, (dialog, whichButton) -> {

                APIHelper.initialApiUsersContacts().unbBlock(userID).subscribe(blockResponse -> {
                    if (blockResponse.isSuccess()) {
                        realmUnblock.executeTransactionAsync(realm1 -> {
                            UsersBlockModel usersBlockModel = realm1.where(UsersBlockModel.class).equalTo("contactsModel.id", userID).findFirst();
                            usersBlockModel.deleteFromRealm();

                        }, this::refreshMenu, error -> {
                            AppHelper.LogCat("Block user" + error.getMessage());

                        });
                        AppHelper.CustomToast(this, blockResponse.getMessage());
                    } else {
                        AppHelper.CustomToast(this, blockResponse.getMessage());
                    }
                }, throwable -> {
                    AppHelper.CustomToast(this, getString(R.string.oops_something));
                });



            });

            builderUnblock.setNegativeButton(R.string.No, (dialog, whichButton) -> {

            });

            builderUnblock.show();
            if (!realmUnblock.isClosed())
                realmUnblock.close();
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCallsPresenter.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AnimationsUtil.setSlideOutAnimation(this);
    }

    public void showUserInfo(ContactsModel contactsModel) {
        String name = UtilsPhone.getContactName(this, contactsModel.getPhone());
        if (name != null) {
            username.setText(name);
        } else {
            username.setText(contactsModel.getPhone());
        }

        MemoryCache memoryCache = new MemoryCache();
        Bitmap bitmap;
        bitmap = ImageLoader.GetCachedBitmapImage(memoryCache, contactsModel.getImage(), this, contactsModel.getId(), AppConstants.USER, AppConstants.ROW_PROFILE);
        if (bitmap != null) {
            ImageLoader.SetBitmapImage(bitmap, userImage);
        } else {

            BitmapImageViewTarget target = new BitmapImageViewTarget(userImage) {
                @Override
                public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                    super.onResourceReady(bitmap, anim);
                    userImage.setImageBitmap(bitmap);
                }

                @Override
                public void onLoadFailed(Exception e, Drawable errorDrawable) {
                    super.onLoadFailed(e, errorDrawable);
                    userImage.setImageDrawable(errorDrawable);
                }

                @Override
                public void onLoadStarted(Drawable placeholder) {
                    super.onLoadStarted(placeholder);
                    userImage.setImageDrawable(placeholder);
                }
            };
            WhatsCloneImageLoader.loadCircleImage(this, EndPoints.ROWS_IMAGE_URL + contactsModel.getImage(), target, R.drawable.image_holder_ur_circle, AppConstants.ROWS_IMAGE_SIZE);

        }
    }

    public void showCallInfo(CallsModel callsModel) {
        if (callsModel.getType().equals(AppConstants.VIDEO_CALL)) {
            showVideoButton();
        } else if (callsModel.getType().equals(AppConstants.VOICE_CALL)) {
            hideVideoButton();
        }
    }


    void showVideoButton() {
        CallVideoBtn.setVisibility(View.VISIBLE);
        CallBtn.setVisibility(View.GONE);
    }

    void hideVideoButton() {
        CallVideoBtn.setVisibility(View.GONE);
        CallBtn.setVisibility(View.VISIBLE);
    }

}
