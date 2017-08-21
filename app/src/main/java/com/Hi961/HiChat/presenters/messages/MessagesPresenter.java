package com.Hi961.HiChat.presenters.messages;


import android.os.Handler;

import com.Hi961.HiChat.activities.messages.MessagesActivity;
import com.Hi961.HiChat.api.APIHelper;
import com.Hi961.HiChat.api.APIService;
import com.Hi961.HiChat.api.apiServices.MessagesService;
import com.Hi961.HiChat.api.apiServices.UsersContacts;
import com.Hi961.HiChat.app.AppConstants;
import com.Hi961.HiChat.app.WhatsCloneApplication;
import com.Hi961.HiChat.helpers.AppHelper;
import com.Hi961.HiChat.helpers.PreferenceManager;
import com.Hi961.HiChat.helpers.notifications.NotificationsManager;
import com.Hi961.HiChat.interfaces.Presenter;
import com.Hi961.HiChat.models.messages.ConversationsModel;
import com.Hi961.HiChat.models.messages.MessagesModel;
import com.Hi961.HiChat.models.users.Pusher;

import java.util.List;

import org.greenrobot.eventbus.EventBus;
import io.realm.Realm;

import static com.Hi961.HiChat.app.AppConstants.EVENT_BUS_MESSAGE_COUNTER;
import static com.Hi961.HiChat.app.AppConstants.EVENT_BUS_MESSAGE_IS_READ;

/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class MessagesPresenter implements Presenter {
    private final MessagesActivity view;
    private final Realm realm;
    private int RecipientID, ConversationID, GroupID;
    private Boolean isGroup;
    private MessagesService mMessagesService;
    private UsersContacts mUsersContacts;

    public MessagesPresenter(MessagesActivity messagesActivity) {
        this.view = messagesActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }


    @Override
    public void onStart() {

    }

    @Override
    public void onCreate() {
        if (!EventBus.getDefault().isRegistered(view)) EventBus.getDefault().register(view);

        if (view.getIntent().getExtras() != null) {
            if (view.getIntent().hasExtra("conversationID")) {
                ConversationID = view.getIntent().getExtras().getInt("conversationID");
            }
            if (view.getIntent().hasExtra("recipientID")) {
                RecipientID = view.getIntent().getExtras().getInt("recipientID");
            }

            if (view.getIntent().hasExtra("groupID")) {
                GroupID = view.getIntent().getExtras().getInt("groupID");
            }

            if (view.getIntent().hasExtra("isGroup")) {
                isGroup = view.getIntent().getExtras().getBoolean("isGroup");
            }

        }

        APIService mApiService = APIService.with(view.getApplicationContext());
        mMessagesService = new MessagesService(realm);
        mUsersContacts = new UsersContacts(realm, view.getApplicationContext(), mApiService);

        if (isGroup) {
            mUsersContacts.getContactInfo(PreferenceManager.getID(view)).subscribe(view::updateContact, view::onErrorLoading);
            mUsersContacts.getContact(PreferenceManager.getID(view)).subscribe(view::updateContact, view::onErrorLoading);
            mMessagesService.getGroupInfo(GroupID).subscribe(view::updateGroupInfo, view::onErrorLoading);
            APIHelper.initializeApiGroups().getGroupMembers(GroupID).subscribe(view::ShowGroupMembers, view::onErrorLoading);
            loadLocalGroupData();
            new Handler().postDelayed(this::updateGroupConversationStatus, 500);
        } else {

            mUsersContacts.getContactInfo(PreferenceManager.getID(view)).subscribe(view::updateContact, view::onErrorLoading);
            mUsersContacts.getContact(PreferenceManager.getID(view)).subscribe(view::updateContact, view::onErrorLoading);
            getRecipientInfo();
            loadLocalData();
            new Handler().postDelayed(this::updateConversationStatus, 500);
        }

    }

    public void getRecipientInfo() {
        try {
            mUsersContacts.getContactInfo(RecipientID).subscribe(contactsModel -> {
                AppHelper.LogCat("contactsModel" + contactsModel.getId());
                view.updateContactRecipient(contactsModel);
                int ConversationID = getConversationId(contactsModel.getId(), PreferenceManager.getID(view), realm);
                if (ConversationID != 0) {
                    realm.executeTransaction(realm1 -> {
                        ConversationsModel conversationsModel = realm1.where(ConversationsModel.class).equalTo("id", ConversationID).findFirst();
                        conversationsModel.setRecipientImage(contactsModel.getImage());
                        realm1.copyToRealmOrUpdate(conversationsModel);
                        EventBus.getDefault().post(new Pusher(AppConstants.EVENT_UPDATE_CONVERSATION_OLD_ROW, ConversationID));
                    });
                }

            }, view::onErrorLoading);

            mUsersContacts.getContact(RecipientID).subscribe(view::updateContactRecipient, view::onErrorLoading);

        } catch (Exception e) {
            AppHelper.LogCat(" " + e.getMessage());
        }

    }

    public void updateConversationStatus() {
        try {
            realm.executeTransaction(realm1 -> {
                ConversationsModel conversationsModel1 = realm1.where(ConversationsModel.class).equalTo("id", ConversationID).equalTo("RecipientID", RecipientID).findFirst();

                if (conversationsModel1 != null) {
                    conversationsModel1.setStatus(AppConstants.IS_SEEN);
                    conversationsModel1.setUnreadMessageCounter("0");
                    realm1.copyToRealmOrUpdate(conversationsModel1);

                    List<MessagesModel> messagesModel = realm1.where(MessagesModel.class).equalTo("conversationID", ConversationID).equalTo("senderID", RecipientID).findAll();
                    for (MessagesModel messagesModel1 : messagesModel) {
                        if (messagesModel1.getStatus() == AppConstants.IS_WAITING) {
                            messagesModel1.setStatus(AppConstants.IS_SEEN);
                            realm1.copyToRealmOrUpdate(messagesModel1);
                        }
                    }
                    EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_COUNTER));

                    EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_IS_READ, ConversationID));
                    NotificationsManager.SetupBadger(view);
                }
            });
        } catch (Exception e) {
            AppHelper.LogCat("There is no conversation unRead MessagesPresenter ");
        }
    }

    public void updateGroupConversationStatus() {
        try {
            realm.executeTransaction(realm1 -> {
                ConversationsModel conversationsModel1 = realm1.where(ConversationsModel.class).equalTo("id", ConversationID).equalTo("groupID", GroupID).findFirst();

                if (conversationsModel1 != null) {
                    conversationsModel1.setStatus(AppConstants.IS_SEEN);
                    conversationsModel1.setUnreadMessageCounter("0");
                    realm1.copyToRealmOrUpdate(conversationsModel1);

                    List<MessagesModel> messagesModel = realm1.where(MessagesModel.class).equalTo("conversationID", ConversationID).notEqualTo("senderID", PreferenceManager.getID(view)).findAll();
                    for (MessagesModel messagesModel1 : messagesModel) {
                        if (messagesModel1.getStatus() == AppConstants.IS_WAITING) {
                            messagesModel1.setStatus(AppConstants.IS_SEEN);
                            realm1.copyToRealmOrUpdate(messagesModel1);
                        }
                    }
                    EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_COUNTER));
                    EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_IS_READ, ConversationID));
                    NotificationsManager.SetupBadger(view);
                }
            });
        } catch (Exception e) {
            AppHelper.LogCat("There is no conversation unRead MessagesPresenter ");
        }
    }

    private void loadLocalGroupData() {
        if (NotificationsManager.getManager())
            NotificationsManager.cancelNotification(GroupID);
        mMessagesService.getConversation(ConversationID).subscribe(view::ShowMessages, view::onErrorLoading, view::onHideLoading);
    }

    private void loadLocalData() {
        if (NotificationsManager.getManager())
            NotificationsManager.cancelNotification(RecipientID);
        try {
            mMessagesService.getConversation(ConversationID, RecipientID, PreferenceManager.getID(view)).subscribe(view::ShowMessages, view::onErrorLoading);
        } catch (Exception e) {
            AppHelper.LogCat("" + e.getMessage());
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
        EventBus.getDefault().unregister(view);
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
}
