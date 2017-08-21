package com.Hi961.HiChat.presenters.messages;


import com.Hi961.HiChat.activities.search.SearchConversationsActivity;
import com.Hi961.HiChat.app.WhatsCloneApplication;
import com.Hi961.HiChat.interfaces.Presenter;
import com.Hi961.HiChat.api.apiServices.ConversationsService;

import io.realm.Realm;

/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class SearchConversationsPresenter implements Presenter {
    private SearchConversationsActivity mSearchConversationsActivity;
    private Realm realm;


    public SearchConversationsPresenter(SearchConversationsActivity mSearchConversationsActivity) {
        this.mSearchConversationsActivity = mSearchConversationsActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }


    @Override
    public void onStart() {

    }

    @Override
    public void onCreate() {
        ConversationsService mConversationsService = new ConversationsService(realm);
        mConversationsService.getConversations().subscribe(mSearchConversationsActivity::ShowConversation, mSearchConversationsActivity::onErrorLoading);
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