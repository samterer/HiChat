package com.Hi961.HiChat.activities.main.welcome;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.Hi961.HiChat.R;
import com.Hi961.HiChat.api.APIHelper;
import com.Hi961.HiChat.app.AppConstants;
import com.Hi961.HiChat.app.EndPoints;
import com.Hi961.HiChat.fragments.bottomSheets.BottomSheetEditProfile;
import com.Hi961.HiChat.helpers.AppHelper;
import com.Hi961.HiChat.helpers.images.ImageUtils;
import com.Hi961.HiChat.helpers.images.WhatsCloneImageLoader;
import com.Hi961.HiChat.models.users.Pusher;
import com.Hi961.HiChat.models.users.contacts.ProfileResponse;
import com.Hi961.HiChat.presenters.users.EditProfilePresenter;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import okhttp3.MediaType;
import okhttp3.RequestBody;

import static com.Hi961.HiChat.app.AppConstants.EVENT_BUS_IMAGE_PROFILE_PATH;

/**
 * Created by Abderrahim El imame on 4/1/17.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class CompleteRegistrationActivity extends AppCompatActivity implements View.OnClickListener {

    @BindView(R.id.username_input)
    AppCompatEditText usernameInput;

    @BindView(R.id.userAvatar)
    ImageView userAvatar;

    @BindView(R.id.addAvatar)
    FloatingActionButton addAvatar;

    @BindView(R.id.progress_bar_edit_profile)
    ProgressBar progressBar;

    @BindView(R.id.completeRegistration)
    TextView completeRegistration;


    @BindView(R.id.registerBtn)
    TextView registerBtn;

    @BindView(R.id.completeRegistrationLayout)
    NestedScrollView mView;


    private String PicturePath;
    private EditProfilePresenter mEditProfilePresenter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.complete_registration_activity);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        setTypeFaces();
        mEditProfilePresenter = new EditProfilePresenter(this);
        mEditProfilePresenter.onCreate();
        registerBtn.setOnClickListener(this);
        addAvatar.setOnClickListener(v -> {
            BottomSheetEditProfile bottomSheetEditProfile = new BottomSheetEditProfile();
            bottomSheetEditProfile.show(getSupportFragmentManager(), bottomSheetEditProfile.getTag());
        });

    }


    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            completeRegistration.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            registerBtn.setTypeface(AppHelper.setTypeFace(this, "Futura"));
            usernameInput.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.registerBtn:
                complete();
                break;

        }
    }

    /**
     * method of EventBus
     *
     * @param pusher this is parameter of onEventMainThread method
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Pusher pusher) {
        switch (pusher.getAction()) {
            case EVENT_BUS_IMAGE_PROFILE_PATH:
                progressBar.setVisibility(View.VISIBLE);
                PicturePath = String.valueOf(pusher.getData());
                if (PicturePath != null) {
                    try {
                        new UploadFileToServer().execute();
                    } catch (Exception e) {
                        AppHelper.LogCat(e);
                        AppHelper.CustomToast(this, getString(R.string.oops_something));
                    }

                }
                break;

        }

    }

    private void complete() {
        String username = usernameInput.getText().toString().trim();
        mEditProfilePresenter.EditCurrentName(username, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEditProfilePresenter.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void setImage(String ImageUrl) {
        BitmapImageViewTarget target = new BitmapImageViewTarget(userAvatar) {
            @Override
            public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                super.onResourceReady(bitmap, anim);
                userAvatar.setImageBitmap(bitmap);

            }

            @Override
            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                super.onLoadFailed(e, errorDrawable);
                userAvatar.setImageDrawable(errorDrawable);
            }

            @Override
            public void onLoadStarted(Drawable placeholder) {
                super.onLoadStarted(placeholder);
                userAvatar.setImageDrawable(placeholder);
            }
        };
        WhatsCloneImageLoader.loadCircleImage(this, EndPoints.EDIT_PROFILE_IMAGE_URL + ImageUrl, target, R.drawable.image_holder_ur_circle, AppConstants.EDIT_PROFILE_IMAGE_SIZE);

    }

    /**
     * Uploading the image  to server
     */
    private class UploadFileToServer extends AsyncTask<Void, Integer, ProfileResponse> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            AppHelper.LogCat("onPreExecute  image ");
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            AppHelper.LogCat("progress image " + (int) (progress[0]));
        }

        @Override
        protected ProfileResponse doInBackground(Void... params) {
            return uploadFile();
        }


        private ProfileResponse uploadFile() {

            RequestBody requestFile;
            final ProfileResponse profileResponse = null;
            if (PicturePath != null) {
                byte[] imageByte = ImageUtils.compressImage(PicturePath);
                // create RequestBody instance from file
                requestFile = RequestBody.create(MediaType.parse("image/*"), imageByte);
            } else {
                requestFile = null;
            }
            APIHelper.initialApiUsersContacts().uploadImage(requestFile).subscribe(response -> {
                if (response.isSuccess()) {

                    if (PicturePath != null) {
                        File file = new File(PicturePath);
                        file.delete();
                    }


                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        AppHelper.CustomToast(CompleteRegistrationActivity.this, response.getMessage());
                        setImage(response.getUserImage());
                    });
                }else {
                    AppHelper.CustomToast(CompleteRegistrationActivity.this, response.getMessage());
                }
            }, throwable -> {
                AppHelper.CustomToast(CompleteRegistrationActivity.this, getString(R.string.failed_upload_image));
                AppHelper.LogCat("Failed  upload your image " + throwable.getMessage());
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));
            });

            return profileResponse;
        }

        @Override
        protected void onPostExecute(ProfileResponse response) {
            super.onPostExecute(response);
            // AppHelper.LogCat("Response from server: " + response);

        }


    }

}
