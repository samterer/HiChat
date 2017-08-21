package com.Hi961.HiChat.presenters.groups;

import com.Hi961.HiChat.activities.groups.AddNewMembersToGroupActivity;
import com.Hi961.HiChat.api.APIService;
import com.Hi961.HiChat.app.WhatsCloneApplication;
import com.Hi961.HiChat.helpers.AppHelper;
import com.Hi961.HiChat.interfaces.Presenter;
import com.Hi961.HiChat.api.apiServices.UsersContacts;

import io.realm.Realm;

/**
 * Created by Abderrahim El imame on 26/03/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class AddNewMembersToGroupPresenter implements Presenter {
    private  AddNewMembersToGroupActivity view;
    private  Realm realm;


    public AddNewMembersToGroupPresenter(AddNewMembersToGroupActivity addMembersToGroupActivity) {
        this.view = addMembersToGroupActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();

    }


    @Override
    public void onStart() {

    }

    @Override
    public void onCreate() {
        APIService mApiService = APIService.with(view);
        UsersContacts mUsersContacts = new UsersContacts(realm, view, mApiService);
        mUsersContacts.getLinkedContacts().subscribe(view::ShowContacts, throwable -> AppHelper.LogCat("AddNewMembersToGroupPresenter "+throwable.getMessage()));
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