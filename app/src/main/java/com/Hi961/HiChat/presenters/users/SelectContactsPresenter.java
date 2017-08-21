package com.Hi961.HiChat.presenters.users;


import com.Hi961.HiChat.activities.BlockedContactsActivity;
import com.Hi961.HiChat.activities.NewConversationContactsActivity;
import com.Hi961.HiChat.activities.messages.TransferMessageContactsActivity;
import com.Hi961.HiChat.api.APIService;
import com.Hi961.HiChat.app.WhatsCloneApplication;
import com.Hi961.HiChat.helpers.AppHelper;
import com.Hi961.HiChat.interfaces.Presenter;
import com.Hi961.HiChat.api.apiServices.UsersContacts;

import io.realm.Realm;

/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class SelectContactsPresenter implements Presenter {
    private NewConversationContactsActivity newConversationContactsActivity;
    private TransferMessageContactsActivity transferMessageContactsActivity;
    private BlockedContactsActivity blockedContactsActivity;
    private Realm realm;
    private boolean selector;

    public SelectContactsPresenter(NewConversationContactsActivity newConversationContactsActivity) {
        this.newConversationContactsActivity = newConversationContactsActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
        selector = true;
    }

    public SelectContactsPresenter(TransferMessageContactsActivity transferMessageContactsActivity) {
        this.transferMessageContactsActivity = transferMessageContactsActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
        selector = false;
    }
    public SelectContactsPresenter(BlockedContactsActivity blockedContactsActivity) {
        this.blockedContactsActivity = blockedContactsActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
        selector = false;
    }


    @Override
    public void onStart() {

    }

    @Override
    public void onCreate() {
        if (selector) {
            APIService mApiService = APIService.with(this.newConversationContactsActivity);
            UsersContacts mUsersContacts = new UsersContacts(realm, this.newConversationContactsActivity, mApiService);
            mUsersContacts.getLinkedContacts().subscribe(newConversationContactsActivity::ShowContacts, throwable -> {
                AppHelper.LogCat("Error contacts selector " + throwable.getMessage());
            });

        } else {
            if (transferMessageContactsActivity != null) {
                APIService mApiService = APIService.with(this.transferMessageContactsActivity);
                UsersContacts mUsersContacts = new UsersContacts(realm, this.transferMessageContactsActivity, mApiService);
                mUsersContacts.getLinkedContacts().subscribe(transferMessageContactsActivity::ShowContacts, throwable -> {
                    AppHelper.LogCat("Error contacts selector " + throwable.getMessage());
                });
            }else {
                APIService mApiService = APIService.with(this.blockedContactsActivity);
                UsersContacts mUsersContacts = new UsersContacts(realm, this.blockedContactsActivity, mApiService);
                mUsersContacts.getBlockedContacts().subscribe(blockedContactsActivity::ShowContacts, throwable -> {
                    AppHelper.LogCat("Error contacts selector " + throwable.getMessage());
                });
            }
        }
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onDestroy() {
        realm.close();
    }

    @Override
    public void onLoadMore() {

    }

    @Override
    public void onRefresh() {

    }

    @Override
    public void onStop() {

    }
}