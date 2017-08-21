package com.Hi961.HiChat.presenters.users;


import com.Hi961.HiChat.R;
import com.Hi961.HiChat.activities.profile.ProfilePreviewActivity;
import com.Hi961.HiChat.api.APIService;
import com.Hi961.HiChat.api.apiServices.GroupsService;
import com.Hi961.HiChat.api.apiServices.UsersContacts;
import com.Hi961.HiChat.app.AppConstants;
import com.Hi961.HiChat.app.WhatsCloneApplication;
import com.Hi961.HiChat.helpers.AppHelper;
import com.Hi961.HiChat.helpers.PreferenceManager;
import com.Hi961.HiChat.interfaces.Presenter;
import com.Hi961.HiChat.models.messages.ConversationsModel;
import com.Hi961.HiChat.models.users.Pusher;

import org.greenrobot.eventbus.EventBus;
import io.realm.Realm;

/**
 * Created by Abderrahim El imame on 20/02/2016. Email : abderrahim.elimame@gmail.com
 */
public class ProfilePreviewPresenter implements Presenter {
    private ProfilePreviewActivity profilePreviewActivity;
    private Realm realm;
    private UsersContacts mUsersContacts;


    public ProfilePreviewPresenter(ProfilePreviewActivity profilePreviewActivity) {
        this.profilePreviewActivity = profilePreviewActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();

    }


    @Override
    public void onStart() {

    }

    @Override
    public void
    onCreate() {
        if (profilePreviewActivity != null) {
            APIService mApiService = APIService.with(profilePreviewActivity);
            UsersContacts mUsersContacts = new UsersContacts(realm, profilePreviewActivity, mApiService);
            GroupsService mGroupsService = new GroupsService(realm, profilePreviewActivity, mApiService);
            if (profilePreviewActivity.getIntent().hasExtra("userID")) {
                int userID = profilePreviewActivity.getIntent().getExtras().getInt("userID");

                try {

                    mUsersContacts.getContactInfo(userID).subscribe(contactsModel -> {
                        profilePreviewActivity.ShowContact(contactsModel);
                        int ConversationID = getConversationId(contactsModel.getId(), PreferenceManager.getID(profilePreviewActivity), realm);
                        if (ConversationID != 0) {
                            realm.executeTransaction(realm1 -> {
                                ConversationsModel conversationsModel = realm1.where(ConversationsModel.class).equalTo("id", ConversationID).findFirst();
                                conversationsModel.setRecipientImage(contactsModel.getImage());
                                realm1.copyToRealmOrUpdate(conversationsModel);
                                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_UPDATE_CONVERSATION_OLD_ROW, ConversationID));
                            });
                        }

                    }, profilePreviewActivity::onErrorLoading);
                    mUsersContacts.getContact(userID).subscribe(profilePreviewActivity::ShowContact, profilePreviewActivity::onErrorLoading);
                } catch (Exception e) {
                    AppHelper.LogCat("Here getContact profile preview" + e.getMessage());
                }

            }

            if (profilePreviewActivity.getIntent().hasExtra("groupID")) {
                int groupID = profilePreviewActivity.getIntent().getExtras().getInt("groupID");
                try {
                    mGroupsService.getGroup(groupID).subscribe(profilePreviewActivity::ShowGroup, throwable -> AppHelper.LogCat("ProfilePreview " + throwable.getMessage()));
                    mGroupsService.getGroupInfo(groupID).subscribe(groupsModel -> {
                        profilePreviewActivity.ShowGroup(groupsModel);
                        int ConversationID = getConversationGroupId(groupsModel.getId(), realm);
                        if (ConversationID != 0) {
                            realm.executeTransaction(realm1 -> {
                                ConversationsModel conversationsModel = realm1.where(ConversationsModel.class).equalTo("id", ConversationID).findFirst();
                                conversationsModel.setRecipientImage(groupsModel.getGroupImage());
                                conversationsModel.setRecipientUsername(groupsModel.getGroupName());
                                realm1.copyToRealmOrUpdate(conversationsModel);
                                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_UPDATE_CONVERSATION_OLD_ROW, ConversationID));
                            });
                        }
                    }, profilePreviewActivity::onErrorLoading);
                } catch (Exception e) {
                    AppHelper.LogCat("Null group info " + e.getMessage());
                }
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

    /**
     * method to get a conversation id
     *
     * @param recipientId this is the first parameter for getConversationId method
     * @param senderId    this is the second parameter for getConversationId method
     * @return conversation id
     */
    private int getConversationId(int recipientId, int senderId, Realm realm) {
        try {
            ConversationsModel conversationsModelNew = realm.where(ConversationsModel.class)
                    .beginGroup()
                    .equalTo("RecipientID", recipientId)
                    .or()
                    .equalTo("RecipientID", senderId)
                    .endGroup().findAll().first();
            return conversationsModelNew.getId();
        } catch (Exception e) {
            AppHelper.LogCat("Conversation id Exception ContactFragment" + e.getMessage());
            return 0;
        }
    }

    private int getConversationGroupId(int GroupID, Realm realm) {
        try {
            ConversationsModel conversationsModel = realm.where(ConversationsModel.class).equalTo("groupID", GroupID).findFirst();
            return conversationsModel.getId();
        } catch (Exception e) {
            AppHelper.LogCat("Conversation id Exception ContactFragment" + e.getMessage());
            return 0;
        }
    }
}