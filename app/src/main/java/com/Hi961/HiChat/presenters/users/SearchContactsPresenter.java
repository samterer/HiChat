package com.Hi961.HiChat.presenters.users;


import com.Hi961.HiChat.activities.search.SearchContactsActivity;
import com.Hi961.HiChat.api.APIService;
import com.Hi961.HiChat.app.WhatsCloneApplication;
import com.Hi961.HiChat.interfaces.Presenter;
import com.Hi961.HiChat.api.apiServices.UsersContacts;

import io.realm.Realm;

/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class SearchContactsPresenter implements Presenter {
    private SearchContactsActivity mSearchContactsActivity;
    private Realm realm;
    private UsersContacts mUsersContacts;


    public SearchContactsPresenter(SearchContactsActivity mSearchContactsActivity) {
        this.mSearchContactsActivity = mSearchContactsActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }


    @Override
    public void onStart() {

    }

    @Override
    public void onCreate() {
        APIService mApiService = APIService.with(this.mSearchContactsActivity);
        mUsersContacts = new UsersContacts(realm, this.mSearchContactsActivity, mApiService);
        loadLocalData();
    }

    private void loadLocalData() {
        mUsersContacts.getAllContacts().subscribe(mSearchContactsActivity::ShowContacts, mSearchContactsActivity::onErrorLoading);
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