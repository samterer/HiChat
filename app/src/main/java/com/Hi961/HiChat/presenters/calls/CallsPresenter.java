package com.Hi961.HiChat.presenters.calls;

import com.Hi961.HiChat.R;
import com.Hi961.HiChat.activities.call.CallDetailsActivity;
import com.Hi961.HiChat.activities.call.IncomingCallActivity;
import com.Hi961.HiChat.animations.AnimationsUtil;
import com.Hi961.HiChat.api.APIService;
import com.Hi961.HiChat.api.apiServices.UsersContacts;
import com.Hi961.HiChat.app.AppConstants;
import com.Hi961.HiChat.app.WhatsCloneApplication;
import com.Hi961.HiChat.fragments.home.CallsFragment;
import com.Hi961.HiChat.helpers.AppHelper;
import com.Hi961.HiChat.interfaces.Presenter;
import com.Hi961.HiChat.models.calls.CallsInfoModel;
import com.Hi961.HiChat.models.calls.CallsModel;
import com.Hi961.HiChat.models.users.Pusher;

import org.greenrobot.eventbus.EventBus;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Abderrahim El imame on 12/3/16.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class CallsPresenter implements Presenter {

    private CallsFragment callsFragment;
    private CallDetailsActivity callDetailsActivity;
    private IncomingCallActivity incomingCallActivity;
    private Realm realm;
    private UsersContacts mUsersContacts;
    private int userID;
    private int callID;

    public Realm getRealm() {
        return realm;
    }

    public CallsPresenter(CallsFragment callsFragment) {
        this.callsFragment = callsFragment;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();

    }

    public CallsPresenter(IncomingCallActivity incomingCallActivity, int userID) {
        this.incomingCallActivity = incomingCallActivity;
        this.userID = userID;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }

    public CallsPresenter(CallDetailsActivity callDetailsActivity) {
        this.callDetailsActivity = callDetailsActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onCreate() {
        if (incomingCallActivity != null) {
            APIService mApiService = APIService.with(incomingCallActivity);
            mUsersContacts = new UsersContacts(realm, incomingCallActivity, mApiService);
            getCallerInfo(userID);
        } else if (callDetailsActivity != null) {
            APIService mApiService = APIService.with(callDetailsActivity);
            mUsersContacts = new UsersContacts(realm, callDetailsActivity, mApiService);
            callID = callDetailsActivity.getIntent().getIntExtra("callID", 0);
            userID = callDetailsActivity.getIntent().getIntExtra("userID", 0);


            getCallerDetailsInfo(userID);
            getCallDetails(callID);
            getCallsDetailsList(callID);
        } else {
            if (!EventBus.getDefault().isRegistered(callsFragment))
                EventBus.getDefault().register(callsFragment);
            APIService mApiService = APIService.with(callsFragment.getActivity());
            mUsersContacts = new UsersContacts(realm, callsFragment.getActivity(), mApiService);
            getCallsList();
        }
    }

    private void getCallerDetailsInfo(int userID) {
        mUsersContacts.getContact(userID).subscribe(contactsModel -> {
            callDetailsActivity.showUserInfo(contactsModel);
        }, AppHelper::LogCat);
    }

    private void getCallDetails(int callID) {
        mUsersContacts.getCallDetails(callID).subscribe(callsModel -> {
            callDetailsActivity.showCallInfo(callsModel);
        }, AppHelper::LogCat);
    }

    private void getCallsDetailsList(int callID) {

        try {
            mUsersContacts.getAllCallsDetails(callID).subscribe(callsInfoModels -> {
                callDetailsActivity.UpdateCallsDetailsList(callsInfoModels);
            }, AppHelper::LogCat);

        } catch (Exception e) {
            AppHelper.LogCat("calls presenter " + e.getMessage());
        }
    }

    private void getCallsList() {

        callsFragment.onShowLoading();
        try {
            mUsersContacts.getAllCalls().subscribe(callsModels -> {
                callsFragment.UpdateCalls(callsModels);
                callsFragment.onHideLoading();
            }, callsFragment::onErrorLoading, callsFragment::onHideLoading);

        } catch (Exception e) {
            AppHelper.LogCat("calls presenter " + e.getMessage());
        }
    }

    private void getCallerInfo(int userID) {
        mUsersContacts.getContactInfo(userID).subscribe(contactsModel -> {
            AppHelper.LogCat("getContactInfo ");
            incomingCallActivity.showUserInfo(contactsModel);
        }, AppHelper::LogCat);
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onDestroy() {
        if (callsFragment != null)
            EventBus.getDefault().unregister(callsFragment);
        realm.close();
    }

    public void removeCall() {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        AppHelper.showDialog(callDetailsActivity, callDetailsActivity.getString(R.string.delete_call_dialog));
        realm.executeTransactionAsync(realm1 -> {
            CallsModel callsModel = realm1.where(CallsModel.class).equalTo("id", callID).findFirst();
            RealmResults<CallsInfoModel> callsInfoModel = realm1.where(CallsInfoModel.class).equalTo("callId", callsModel.getId()).findAll();
            callsInfoModel.deleteAllFromRealm();
            callsModel.deleteFromRealm();
        }, () -> {
            AppHelper.hideDialog();
            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_DELETE_CALL_ITEM, callID));
            callDetailsActivity.finish();
            AnimationsUtil.setSlideOutAnimation(callDetailsActivity);
        }, error -> {
            AppHelper.LogCat(error.getMessage());
            AppHelper.hideDialog();
        });

    }

    @Override
    public void onLoadMore() {

    }

    @Override
    public void onRefresh() {
        getCallsList();
    }

    @Override
    public void onStop() {

    }
}
