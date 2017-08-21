package com.Hi961.HiChat.services.firebase;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.Hi961.HiChat.app.AppConstants;
import com.Hi961.HiChat.helpers.PreferenceManager;
import com.Hi961.HiChat.models.users.Pusher;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Abderrahim El imame on 4/11/17.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class RegistrationIntentService extends FirebaseInstanceIdService {
    // abbreviated tag name
    private static final String TAG = RegistrationIntentService.class.getName();

    @Override
    public void onTokenRefresh() {
        if (PreferenceManager.getToken(this) == null) return;
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        if (refreshedToken == null) return;
        EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_REFRESH_TOKEN_FCM, refreshedToken));
    }
}