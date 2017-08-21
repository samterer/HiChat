package com.Hi961.HiChat.api.apiServices;

import android.content.Context;

import com.Hi961.HiChat.api.APIGroups;
import com.Hi961.HiChat.api.APIService;
import com.Hi961.HiChat.app.AppConstants;
import com.Hi961.HiChat.app.EndPoints;
import com.Hi961.HiChat.helpers.Files.backup.RealmBackupRestore;
import com.Hi961.HiChat.helpers.PreferenceManager;
import com.Hi961.HiChat.models.groups.GroupResponse;
import com.Hi961.HiChat.models.groups.GroupsModel;
import com.Hi961.HiChat.models.groups.MembersGroupModel;
import com.Hi961.HiChat.models.messages.ConversationsModel;
import com.Hi961.HiChat.models.messages.MessagesModel;
import com.Hi961.HiChat.models.users.Pusher;

import java.util.List;

import org.greenrobot.eventbus.EventBus;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;

import static com.Hi961.HiChat.app.AppConstants.EVENT_BUS_DELETE_CONVERSATION_ITEM;

/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class GroupsService {
    private APIGroups mApiGroups;
    private Context mContext;
    private Realm realm;
    private APIService mApiService;
    private int lastConversationID;

    public GroupsService(Realm realm, Context context, APIService mApiService) {
        this.mContext = context;
        this.realm = realm;
        this.mApiService = mApiService;

    }

    /**
     * method to initialize the api groups
     *
     * @return return value
     */
    private APIGroups initializeApiGroups() {
        if (mApiGroups == null) {
            mApiGroups = this.mApiService.RootService(APIGroups.class, PreferenceManager.getToken(mContext), EndPoints.BACKEND_BASE_URL);
        }
        return mApiGroups;
    }

    /**
     * method to get all groups list
     *
     * @return return value
     */
    public Observable<List<GroupsModel>> getGroups() {
        List<GroupsModel> groups = realm.where(GroupsModel.class).findAll();
        return Observable.just(groups);
    }

    /**
     * method to update groups
     *
     * @return return value
     */
    public Observable<List<GroupsModel>> updateGroups() {
        return initializeApiGroups().groups()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(this::copyOrUpdateGroups);
    }

    /**
     * method to get single group information
     *
     * @param groupID this is parameter for  getGroupInfo method
     * @return return value
     */
    public Observable<GroupsModel> getGroupInfo(int groupID) {
        return initializeApiGroups().getGroup(groupID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(this::copyOrUpdateGroup);
    }

    /**
     * method to get group information from local
     *
     * @param groupID this is parameter for getGroup method
     * @return return value
     */
    public Observable<GroupsModel> getGroup(int groupID) {
        GroupsModel groupsModel = realm.where(GroupsModel.class).equalTo("id", groupID).findFirst();
        return Observable.just(groupsModel);
    }

    /**
     * method to copy or update groups list
     *
     * @param groups this is parameter for copyOrUpdateGroups method
     * @return return value
     */
    private List<GroupsModel> copyOrUpdateGroups(List<GroupsModel> groups) {
        List<GroupsModel> finalList = checkGroups(groups);
        realm.beginTransaction();
        List<GroupsModel> realmGroups = realm.copyToRealmOrUpdate(finalList);
        realm.commitTransaction();
        return realmGroups;
    }

    private boolean checkIfCreatedGroupMessageExist(int groupId, Realm realm, String message) {
        RealmQuery<MessagesModel> query = realm.where(MessagesModel.class).equalTo("groupID", groupId).equalTo("isGroup", true).equalTo("message", message);
        return query.count() != 0;

    }

    private List<GroupsModel> checkGroups(List<GroupsModel> groupsModels) {
        if (groupsModels.size() != 0) {
            for (GroupsModel groupsModel1 : groupsModels) {

                RealmList<MembersGroupModel> membersGroupModelRealmList = groupsModel1.getMembers();
                for (MembersGroupModel membersGroupModel : membersGroupModelRealmList) {
                    if (membersGroupModel.getUserId() == PreferenceManager.getID(mContext)) {
                        if (!membersGroupModel.isDeleted()) {
                            if (!checkIfGroupConversationExist(groupsModel1.getId())) {
                                RealmList<MessagesModel> newMessagesModelRealmList = new RealmList<MessagesModel>();
                                realm.executeTransaction(realm1 -> {
                                    int lastConversationID = RealmBackupRestore.getConversationLastId();
                                    int lastID = 1;
                                    RealmList<MessagesModel> messagesModelRealmList = groupsModel1.getMessages();

                                    for (MessagesModel messagesModel1 : messagesModelRealmList) {
                                        if (messagesModel1.getMessage().equals(AppConstants.CREATE_GROUP) && !checkIfCreatedGroupMessageExist(groupsModel1.getId(), realm1, messagesModel1.getMessage())) {
                                            lastID = RealmBackupRestore.getMessageLastId();
                                            MessagesModel messagesModel = new MessagesModel();
                                            messagesModel.setId(lastID);
                                            messagesModel.setDate(messagesModel1.getDate());
                                            messagesModel.setSenderID(messagesModel1.getSenderID());
                                            messagesModel.setRecipientID(0);
                                            messagesModel.setPhone(messagesModel1.getPhone());
                                            messagesModel.setStatus(AppConstants.IS_SEEN);
                                            messagesModel.setUsername(messagesModel1.getUsername());
                                            messagesModel.setGroup(true);
                                            messagesModel.setMessage(messagesModel1.getMessage());
                                            messagesModel.setGroupID(groupsModel1.getId());
                                            messagesModel.setConversationID(lastConversationID);
                                            messagesModel.setImageFile(messagesModel1.getImageFile());
                                            messagesModel.setVideoFile(messagesModel1.getVideoFile());
                                            messagesModel.setAudioFile(messagesModel1.getAudioFile());
                                            messagesModel.setDocumentFile(messagesModel1.getDocumentFile());
                                            messagesModel.setVideoThumbnailFile(messagesModel1.getVideoThumbnailFile());
                                            messagesModel.setFileUpload(messagesModel1.isFileUpload());
                                            messagesModel.setFileDownLoad(messagesModel1.isFileDownLoad());
                                            messagesModel.setFileSize(messagesModel1.getFileSize());
                                            messagesModel.setDuration(messagesModel1.getDuration());
                                            realm1.copyToRealmOrUpdate(messagesModel);
                                            newMessagesModelRealmList.add(messagesModel);
                                        } else if (!messagesModel1.getMessage().equals(AppConstants.CREATE_GROUP)) {
                                            lastID = RealmBackupRestore.getMessageLastId();
                                            MessagesModel messagesModel = new MessagesModel();
                                            messagesModel.setId(lastID);
                                            messagesModel.setDate(messagesModel1.getDate());
                                            messagesModel.setSenderID(messagesModel1.getSenderID());
                                            messagesModel.setRecipientID(0);
                                            messagesModel.setPhone(messagesModel1.getPhone());
                                            messagesModel.setStatus(AppConstants.IS_SEEN);
                                            messagesModel.setUsername(messagesModel1.getUsername());
                                            messagesModel.setGroup(true);
                                            messagesModel.setMessage(messagesModel1.getMessage());
                                            messagesModel.setGroupID(groupsModel1.getId());
                                            messagesModel.setConversationID(lastConversationID);
                                            messagesModel.setImageFile(messagesModel1.getImageFile());
                                            messagesModel.setVideoFile(messagesModel1.getVideoFile());
                                            messagesModel.setAudioFile(messagesModel1.getAudioFile());
                                            messagesModel.setDocumentFile(messagesModel1.getDocumentFile());
                                            messagesModel.setVideoThumbnailFile(messagesModel1.getVideoThumbnailFile());
                                            messagesModel.setFileUpload(messagesModel1.isFileUpload());
                                            messagesModel.setFileDownLoad(messagesModel1.isFileDownLoad());
                                            messagesModel.setFileSize(messagesModel1.getFileSize());
                                            messagesModel.setDuration(messagesModel1.getDuration());
                                            realm1.copyToRealmOrUpdate(messagesModel);
                                            newMessagesModelRealmList.add(messagesModel);
                                        }
                                    }
                                    groupsModel1.setMessages(newMessagesModelRealmList);
                                    realm1.copyToRealmOrUpdate(groupsModel1);
                                    ConversationsModel conversationsModel = new ConversationsModel();
                                    conversationsModel.setId(lastConversationID);
                                    conversationsModel.setLastMessageId(lastID);
                                    conversationsModel.setRecipientID(0);
                                    conversationsModel.setCreatorID(groupsModel1.getCreatorID());
                                    conversationsModel.setRecipientUsername(groupsModel1.getGroupName());
                                    conversationsModel.setRecipientImage(groupsModel1.getGroupImage());
                                    conversationsModel.setGroupID(groupsModel1.getId());
                                    conversationsModel.setMessageDate(groupsModel1.getCreatedDate());
                                    conversationsModel.setGroup(true);
                                    conversationsModel.setMessages(newMessagesModelRealmList);
                                    conversationsModel.setStatus(AppConstants.IS_SEEN);
                                    conversationsModel.setUnreadMessageCounter("0");
                                    conversationsModel.setCreatedOnline(true);
                                    realm1.copyToRealmOrUpdate(conversationsModel);
                                    this.lastConversationID = lastConversationID;
                                });

                                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_MESSAGE_CONVERSATION_NEW_ROW, this.lastConversationID));

                            }
                        } else {
                            realm.executeTransactionAsync(realm1 -> {
                                GroupsModel groupsModel = realm1.where(GroupsModel.class).equalTo("id", groupsModel1.getId()).findFirst();
                                ConversationsModel conversationsModel = realm1.where(ConversationsModel.class).equalTo("groupID", groupsModel1.getId()).findFirst();
                                if (conversationsModel != null && conversationsModel.getId() != 0) {
                                    EventBus.getDefault().post(new Pusher(EVENT_BUS_DELETE_CONVERSATION_ITEM, conversationsModel.getId()));
                                    RealmResults<MessagesModel> messagesModel1 = realm1.where(MessagesModel.class).equalTo("conversationID", conversationsModel.getId()).findAll();
                                    messagesModel1.deleteAllFromRealm();
                                    conversationsModel.deleteFromRealm();
                                    groupsModel.deleteFromRealm();
                                }
                            });
                        }
                        break;
                    }
                }
            }
        } else {
            realm.executeTransactionAsync(realm1 -> {
                RealmResults<GroupsModel> groupsModels1 = realm1.where(GroupsModel.class).findAll();
                groupsModels1.deleteAllFromRealm();
                RealmResults<ConversationsModel> conversationsModels = realm1.where(ConversationsModel.class).equalTo("isGroup", true).findAll();
                for (ConversationsModel conversationsModel : conversationsModels) {
                    EventBus.getDefault().post(new Pusher(EVENT_BUS_DELETE_CONVERSATION_ITEM, conversationsModel.getId()));
                    RealmResults<MessagesModel> messagesModel1 = realm1.where(MessagesModel.class).equalTo("conversationID", conversationsModel.getId()).findAll();
                    messagesModel1.deleteAllFromRealm();
                    conversationsModel.deleteFromRealm();
                }
            });
        }
        return groupsModels;
    }

    /**
     * method to copy or update a single group
     *
     * @param groupsModel this is parameter for copyOrUpdateGroup method
     * @return return value
     */
    private GroupsModel copyOrUpdateGroup(GroupsModel groupsModel) {
        realm.beginTransaction();
        GroupsModel realmGroups = realm.copyToRealmOrUpdate(groupsModel);
        realm.commitTransaction();
        return realmGroups;
    }

    /**
     * method to check if a group conversation exist
     *
     * @param groupID this is parameter for checkIfGroupConversationExist method
     * @return return value
     */
    private boolean checkIfGroupConversationExist(int groupID) {
        RealmQuery<ConversationsModel> query = realm.where(ConversationsModel.class).equalTo("groupID", groupID);
        return query.count() != 0;
    }

    /**
     * method to check for id 0
     *
     * @return return value
     */
    public boolean checkIfZeroExist() {
        RealmQuery<MessagesModel> query = realm.where(MessagesModel.class).equalTo("id", 0);
        return query.count() != 0;
    }


    /**
     * methods for get group members
     *
     * @param groupID this is parameter for getGroupMembers method
     * @return return value
     */
    public Observable<List<MembersGroupModel>> getGroupMembers(int groupID) {
        List<MembersGroupModel> groupsModel = realm.where(MembersGroupModel.class).equalTo("groupID", groupID)/*.equalTo("Deleted", false).equalTo("isLeft", false)*/.findAll();
        return Observable.just(groupsModel);
    }

    /**
     * method to update group members
     *
     * @param groupID this is parameter for updateGroupMembers method
     * @return return value
     */
    public Observable<List<MembersGroupModel>> updateGroupMembers(int groupID) {
        return initializeApiGroups().groupMembers(groupID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(this::copyOrUpdateGroupMembers);
    }

    /**
     * method to copy or update group members
     *
     * @param groupMembers this is parameter for copyOrUpdateGroupMembers method
     * @return return value
     */
    private List<MembersGroupModel> copyOrUpdateGroupMembers(List<MembersGroupModel> groupMembers) {
        List<MembersGroupModel> finalList = checkGroupMembers(groupMembers);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        realm.beginTransaction();
        List<MembersGroupModel> realmGroupMembers = realm.copyToRealmOrUpdate(finalList);
        realm.commitTransaction();
        return realmGroupMembers;
    }
    private List<MembersGroupModel> checkGroupMembers(List<MembersGroupModel> groupMembers) {
        if (groupMembers.size() != 0) {
            realm.executeTransactionAsync(realm1 -> {
                RealmResults<MembersGroupModel> groupMember = realm1.where(MembersGroupModel.class).findAll();
                groupMember.deleteAllFromRealm();
            });
        }
        return groupMembers;
    }

    /**
     * method to exit a group
     *
     * @param groupID this is parameter for ExitGroup method
     * @return return value
     */
    public Observable<GroupResponse> ExitGroup(int groupID) {
        return initializeApiGroups().exitGroup(groupID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }


    /**
     * method to delete group
     *
     * @param groupID this is parameter for DeleteGroup method
     * @return return value
     */
    public Observable<GroupResponse> DeleteGroup(int groupID) {
        return initializeApiGroups().deleteGroup(groupID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
