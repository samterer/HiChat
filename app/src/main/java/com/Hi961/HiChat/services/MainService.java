package com.Hi961.HiChat.services;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.Hi961.HiChat.R;
import com.Hi961.HiChat.activities.call.IncomingCallActivity;
import com.Hi961.HiChat.activities.messages.MessagesActivity;
import com.Hi961.HiChat.activities.popups.MessagesPopupActivity;
import com.Hi961.HiChat.api.APIHelper;
import com.Hi961.HiChat.app.AppConstants;
import com.Hi961.HiChat.app.WhatsCloneApplication;
import com.Hi961.HiChat.helpers.AppHelper;
import com.Hi961.HiChat.helpers.PreferenceManager;
import com.Hi961.HiChat.helpers.UtilsPhone;
import com.Hi961.HiChat.helpers.UtilsString;
import com.Hi961.HiChat.helpers.UtilsTime;
import com.Hi961.HiChat.helpers.notifications.NotificationsManager;
import com.Hi961.HiChat.models.groups.GroupsModel;
import com.Hi961.HiChat.models.messages.ConversationsModel;
import com.Hi961.HiChat.models.messages.MessagesModel;
import com.Hi961.HiChat.models.messages.UpdateMessageModel;
import com.Hi961.HiChat.models.notifications.NotificationsModel;
import com.Hi961.HiChat.models.users.Pusher;
import com.Hi961.HiChat.models.users.contacts.ContactsModel;
import com.Hi961.HiChat.models.users.contacts.PusherContacts;
import com.Hi961.HiChat.models.users.contacts.UsersBlockModel;
import com.Hi961.HiChat.receivers.MessagesReceiverBroadcast;

import org.greenrobot.eventbus.EventBus;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.Sort;
import io.socket.client.Socket;
import io.socket.client.SocketIOException;
import io.socket.emitter.Emitter;

import static com.Hi961.HiChat.app.AppConstants.EVENT_BUS_MESSAGE_IS_DELIVERED_FOR_CONVERSATIONS;
import static com.Hi961.HiChat.app.AppConstants.EVENT_BUS_MESSAGE_IS_SEEN_FOR_CONVERSATIONS;
import static com.Hi961.HiChat.app.AppConstants.EVENT_BUS_NEW_MESSAGE_IS_SENT_FOR_CONVERSATIONS;


/**
 * Created by Abderrahim El imame on 6/21/16.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/bencherif_el
 */

public class MainService extends IntentService {

    private Context mContext;
    public static Socket mSocket;
    private MessagesReceiverBroadcast mChangeListener;
    private Intent mIntent;
    private static Handler handler;
    private int mTries = 0;

    //to keep socket connected
    PowerManager.WakeLock wakeLock;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public MainService() {
        super(AppConstants.TAG);
    }

    /**
     * method to disconnect user form server
     */
    public static void disconnectSocket() {

        if (mSocket != null) {
            mSocket.off(Socket.EVENT_CONNECT);
            mSocket.off(Socket.EVENT_DISCONNECT);
            mSocket.off(Socket.EVENT_CONNECT_TIMEOUT);
            mSocket.off(Socket.EVENT_RECONNECT);

            mSocket.off(AppConstants.SOCKET_PING);
            mSocket.off(AppConstants.SOCKET_PONG);
            mSocket.off(AppConstants.SOCKET_IS_ONLINE);
            //user messages

            mSocket.off(AppConstants.SOCKET_SAVE_NEW_MESSAGE);
            mSocket.off(AppConstants.SOCKET_IS_LAST_SEEN);
            mSocket.off(AppConstants.SOCKET_IS_MESSAGE_DELIVERED);
            mSocket.off(AppConstants.SOCKET_IS_MESSAGE_SEEN);
            mSocket.off(AppConstants.SOCKET_UPDATE_REGISTER_ID);
            mSocket.off(AppConstants.SOCKET_IS_STOP_TYPING);
            mSocket.off(AppConstants.SOCKET_IS_TYPING);
            mSocket.off(AppConstants.SOCKET_CONNECTED);
            mSocket.off(AppConstants.SOCKET_DISCONNECTED);
            mSocket.off(AppConstants.SOCKET_NEW_USER_JOINED);
            mSocket.off(AppConstants.SOCKET_IMAGE_PROFILE_UPDATED);
            mSocket.off(AppConstants.SOCKET_IMAGE_GROUP_UPDATED);
            //groups
            mSocket.off(AppConstants.SOCKET_SAVE_NEW_MESSAGE_GROUP);
            mSocket.off(AppConstants.SOCKET_IS_MEMBER_STOP_TYPING);
            mSocket.off(AppConstants.SOCKET_IS_MEMBER_TYPING);
            mSocket.off(AppConstants.SOCKET_IS_MESSAGE_GROUP_DELIVERED);
            mSocket.off(AppConstants.SOCKET_IS_MESSAGE_GROUP_SEEN);

            //calls
            mSocket.off(AppConstants.SOCKET_CALL_USER_PING);
            mSocket.off(AppConstants.SOCKET_RESET_SOCKET_ID);
            mSocket.off(AppConstants.SOCKET_SIGNALING_SERVER);
            mSocket.off(AppConstants.SOCKET_MAKE_NEW_CALL);
            mSocket.off(AppConstants.SOCKET_RECEIVE_NEW_CALL);
            mSocket.off(AppConstants.SOCKET_REJECT_NEW_CALL);
            mSocket.off(AppConstants.SOCKET_ACCEPT_NEW_CALL);
            mSocket.off(AppConstants.SOCKET_HANGUP_CALL);


            mSocket.disconnect();
            mSocket.close();
            mSocket = null;

        }
        AppHelper.LogCat("disconnect in service");
    }

    /**
     * method for server connection initialization
     */
    public void connectToServer(Context mContext) {

        WhatsCloneApplication.connectSocket();
        WhatsCloneApplication app = (WhatsCloneApplication) getApplication();

        mSocket = app.getSocket();
        if (!mSocket.connected())
            mSocket.connect();

        mSocket.once(Socket.EVENT_CONNECT, args -> {
            mTries = 0;
            AppHelper.LogCat("New Connection chat is created " + mSocket.id());

            if (mSocket.id() != null) {
                PreferenceManager.setSocketID(this, mSocket.id());

                JSONObject json = new JSONObject();
                try {
                    json.put("connected", true);
                    json.put("connectedId", PreferenceManager.getID(mContext));
                    json.put("userToken", PreferenceManager.getToken(mContext));
                    json.put("socketId", PreferenceManager.getSocketID(mContext));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (mSocket != null)
                    mSocket.emit(AppConstants.SOCKET_CONNECTED, json);
            } else {
                reconnect(mContext);
            }
        }).on(Socket.EVENT_CONNECT_ERROR, args -> {
            AppHelper.LogCat("EVENT_CONNECT_ERROR");
            for (Object o : args) {
                AppHelper.LogCat("object: " + o.toString());
                if (o instanceof SocketIOException)
                    ((SocketIOException) o).printStackTrace();
            }
        }).on(Socket.EVENT_DISCONNECT, args -> {
            AppHelper.LogCat("You  lost connection to chat server " + mSocket.id());


            JSONObject jsonConnected = new JSONObject();
            try {
                jsonConnected.put("connectedId", PreferenceManager.getID(mContext));
                jsonConnected.put("userToken", PreferenceManager.getToken(mContext));
                jsonConnected.put("socketId", PreferenceManager.getSocketID(mContext));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (mSocket != null)
                mSocket.emit(AppConstants.SOCKET_DISCONNECTED, jsonConnected);

        }).on(Socket.EVENT_CONNECT_TIMEOUT, args -> {
            AppHelper.LogCat("Socket timeout ");
            //reconnect(mContext);
        }).on(Socket.EVENT_RECONNECT, args -> {
            AppHelper.LogCat("Reconnect");
            reconnect(mContext);
        }).on(AppConstants.SOCKET_PING, onPing);

        SenderMarkMessageAsDelivered();
        SenderMarkMessageAsSeen();
        MemberMarkMessageAsDelivered();
        notifyOtherUser();
        getNotifyFromOtherNewUser();
        getNotifyForImageProfileChanged();
        onReceiveNewCall();


        isUserConnected(mContext);
        checkIfUserIsOnline();
        updateRegisterId();

    }

    /**
     * method to reconnect sockets
     */
    public void reconnect(Context mContext) {
        if (mTries < 5) {
            mTries++;
            AppHelper.restartService();
            handler.postDelayed(() -> updateStatusDeliveredOffline(mContext), 1500);
        } else {
            WhatsCloneApplication.getInstance().stopService(new Intent(WhatsCloneApplication.getInstance(), MainService.class));
        }

    }

    private Emitter.Listener onPing = args -> {
//        AppHelper.LogCat("socket ping");

        if (mSocket == null) {
            WhatsCloneApplication.connectSocket();
            WhatsCloneApplication app = (WhatsCloneApplication) getApplication();
            mSocket = app.getSocket();
        }


        if (mSocket != null) {
            if (!mSocket.connected())
                mSocket.connect();

            JSONObject data = (JSONObject) args[0];
            String ping;
            try {
                ping = data.getString("beat");
            } catch (JSONException e) {
                return;
            }
            if (ping.equals("1")) {
                mSocket.emit(AppConstants.SOCKET_PONG);
            }

        }

    };

    /**
     * method to receive notification if a new user Joined
     */
    private void getNotifyFromOtherNewUser() {
        mSocket.on(AppConstants.SOCKET_NEW_USER_JOINED, args -> {
            final JSONObject jsonObject = (JSONObject) args[0];
            try {
                int senderId = jsonObject.getInt("senderId");
                String phone = jsonObject.getString("phone");
                if (senderId == PreferenceManager.getID(mContext)) return;
                if (UtilsPhone.checkIfContactExist(mContext, phone)) {
                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_USER_JOINED, jsonObject));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private boolean checkIfGroupExist(int groupId, Realm realm) {
        RealmQuery<GroupsModel> query = realm.where(GroupsModel.class).equalTo("id", groupId);
        return query.count() != 0;

    }

    /**
     * method when a user change the image profile
     */
    private void getNotifyForImageProfileChanged() {
        mSocket.on(AppConstants.SOCKET_IMAGE_PROFILE_UPDATED, args -> {
            final JSONObject jsonObject = (JSONObject) args[0];
            try {
                int senderId = jsonObject.getInt("senderId");
                String phone = jsonObject.getString("phone");
                if (senderId == PreferenceManager.getID(mContext)) return;
                if (UtilsPhone.checkIfContactExist(mContext, phone)) {
                    EventBus.getDefault().post(new PusherContacts(AppConstants.EVENT_BUS_IMAGE_PROFILE_UPDATED));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        mSocket.on(AppConstants.SOCKET_IMAGE_GROUP_UPDATED, args -> {
            final JSONObject jsonObject = (JSONObject) args[0];
            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            try {
                int groupId = jsonObject.getInt("groupId");
                if (!checkIfGroupExist(groupId, realm)) return;
                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_IMAGE_GROUP_UPDATED, groupId));

            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (!realm.isClosed()) realm.close();
        });
    }

    /**
     * method to send notification if i join to the app
     */
    private void notifyOtherUser() {
        if (PreferenceManager.isNewUser(mContext)) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("senderId", PreferenceManager.getID(mContext));
                jsonObject.put("phone", PreferenceManager.getPhone(mContext));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mSocket.emit(AppConstants.SOCKET_NEW_USER_JOINED, jsonObject);
            PreferenceManager.setIsNewUser(mContext, false);
        }
    }


    /**
     * method to check if user is online or not
     */
    private void checkIfUserIsOnline() {

        if (mSocket != null) {
            mSocket.on(AppConstants.SOCKET_IS_ONLINE, args -> {
                Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                final JSONObject data = (JSONObject) args[0];
                try {
                    int senderID = data.getInt("senderId");
                    if (senderID == PreferenceManager.getID(mContext)) return;
                    if (data.getBoolean("connected")) {
                        realm.executeTransaction(realm1 -> {
                            ContactsModel userModel = realm1.where(ContactsModel.class).equalTo("id", senderID).findFirst();
                            if (userModel != null) {
                                userModel.setUserState(AppConstants.STATUS_USER_CONNECTED_STATE);
                                realm1.copyToRealmOrUpdate(userModel);
                            }
                        });
                    } else {
                        realm.executeTransaction(realm1 -> {
                            ContactsModel userModel = realm1.where(ContactsModel.class).equalTo("id", senderID).findFirst();
                            if (userModel != null) {
                                userModel.setUserState(AppConstants.STATUS_USER_DISCONNECTED_STATE);
                                realm1.copyToRealmOrUpdate(userModel);
                            }
                        });
                    }
                } catch (JSONException e) {
                    AppHelper.LogCat(e);
                }
                if (!realm.isClosed())
                    realm.close();
            });

            mSocket.on(AppConstants.SOCKET_IS_LAST_SEEN, args -> {
                Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                final JSONObject data = (JSONObject) args[0];
                try {

                    int senderID = data.getInt("senderId");
                    int recipientID = data.getInt("recipientId");
                    String lastSeen = data.getString("lastSeen");
                    if (recipientID != PreferenceManager.getID(this)) return;
                    DateTime messageDate = UtilsTime.getCorrectDate(lastSeen);
                    String finalDate = UtilsTime.convertDateToString(this, messageDate);
                    realm.executeTransaction(realm1 -> {
                        ContactsModel userModel = realm1.where(ContactsModel.class).equalTo("id", senderID).findFirst();
                        if (userModel != null) {
                            userModel.setUserState(AppConstants.STATUS_USER_LAST_SEEN_STATE + " " + finalDate);
                            realm1.copyToRealmOrUpdate(userModel);
                        }
                    });
                } catch (JSONException e) {
                    AppHelper.LogCat(e);
                }
                if (!realm.isClosed())
                    realm.close();
            });
        }
    }

    /**
     * method to check if user is connected to server
     *
     * @param mContext
     */
    private static void isUserConnected(Context mContext) {
        if (mSocket != null) {
            mSocket.on(AppConstants.SOCKET_CONNECTED, args -> {
                final JSONObject data = (JSONObject) args[0];
                try {
                    int connectedId = data.getInt("connectedId");
                    String socketId = data.getString("socketId");
                    boolean connected = data.getBoolean("connected");

                    if (socketId.equals(PreferenceManager.getSocketID(mContext))) return;
                    try {
                        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
                        ContactsModel contactsModel = realm.where(ContactsModel.class).equalTo("id", connectedId).findFirst();
                        if (contactsModel != null && UtilsPhone.checkIfContactExist(mContext, contactsModel.getPhone())) {
                            if (connected) {
                                AppHelper.LogCat("User with id  --> " + connectedId + " is connected <---");
                                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_UPDATE_USER_STATE, mContext.getString(R.string.isOnline)));
                                realm.executeTransactionAsync(realm1 -> {
                                    ContactsModel contactsModel1 = realm1.where(ContactsModel.class).equalTo("id", connectedId).findFirst();
                                    if (contactsModel1 != null) {
                                        contactsModel1.setSocketId(socketId);
                                        contactsModel1.setUserState(AppConstants.STATUS_USER_CONNECTED_STATE);
                                        realm1.copyToRealmOrUpdate(contactsModel1);
                                    }
                                });
                            } else {
                                realm.executeTransactionAsync(realm1 -> {
                                    ContactsModel contactsModel1 = realm1.where(ContactsModel.class).equalTo("id", connectedId).findFirst();
                                    if (contactsModel1 != null) {
                                        contactsModel1.setSocketId(null);
                                        contactsModel1.setUserState(AppConstants.STATUS_USER_DISCONNECTED_STATE);
                                        realm1.copyToRealmOrUpdate(contactsModel1);
                                    }
                                });
                                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_UPDATE_USER_STATE, mContext.getString(R.string.isOffline)));
                                AppHelper.LogCat("User with id  --> " + connectedId + " is disconnected  <---");
                            }
                        }
                        realm.close();
                    } catch (Exception e) {
                        AppHelper.LogCat(" isUserConnected Exception mainService " + e.getMessage());
                    } //// TODO: 4/7/17 hna luser is connected


                } catch (JSONException e) {
                    AppHelper.LogCat(e);
                }

            });
        }
    }

    private static boolean checkIfUnsentMessagesExist(int recipientId, Realm realm, Context mContext) {
        RealmQuery<MessagesModel> query = realm.where(MessagesModel.class)
                .equalTo("status", AppConstants.IS_WAITING)
                .equalTo("recipientID", recipientId)
                .equalTo("isGroup", false)
                .equalTo("isFileUpload", true)
                .equalTo("senderID", PreferenceManager.getID(mContext));

        return query.count() != 0;

    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppHelper.LogCat("MainService  has Created");
        PowerManager pMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getString(R.string.app_name) + "WakeLock");
        wakeLock.acquire();

        mContext = getApplicationContext();
        handler = new Handler();
        connectToServer(mContext);
        mChangeListener = new MessagesReceiverBroadcast() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            protected void MessageReceived(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case "new_user_message_notification_whatsclone":
                        String Application = intent.getExtras().getString("app");
                        String file = intent.getExtras().getString("file");
                        String userphone = intent.getExtras().getString("phone");
                        String messageBody = intent.getExtras().getString("message");
                        int recipientId = intent.getExtras().getInt("recipientID");
                        int senderId = intent.getExtras().getInt("senderId");
                        int conversationID = intent.getExtras().getInt("conversationID");
                        String userImage = intent.getExtras().getString("userImage");

                        /**
                         * this for default activity
                         */
                        Intent messagingIntent = new Intent(mContext, MessagesActivity.class);
                        messagingIntent.putExtra("conversationID", conversationID);
                        messagingIntent.putExtra("recipientID", recipientId);
                        messagingIntent.putExtra("isGroup", false);
                        messagingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        /**
                         * this for popup activity
                         */
                        Intent messagingPopupIntent = new Intent(mContext, MessagesPopupActivity.class);
                        messagingPopupIntent.putExtra("conversationID", conversationID);
                        messagingPopupIntent.putExtra("recipientID", recipientId);
                        messagingPopupIntent.putExtra("isGroup", false);
                        messagingPopupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        if (Application != null && Application.equals(mContext.getPackageName())) {
                            if (AppHelper.isActivityRunning(mContext, "activities.messages.MessagesActivity")) {
                                NotificationsModel notificationsModel = new NotificationsModel();
                                notificationsModel.setConversationID(conversationID);
                                notificationsModel.setFile(file);
                                notificationsModel.setGroup(false);
                                notificationsModel.setImage(userImage);
                                notificationsModel.setPhone(userphone);
                                notificationsModel.setMessage(messageBody);
                                notificationsModel.setRecipientId(recipientId);
                                notificationsModel.setSenderId(senderId);
                                notificationsModel.setAppName(Application);
                                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_USER_NOTIFICATION, notificationsModel));
                            } else {
                                if (file != null) {
                                    NotificationsManager.showUserNotification(mContext, messagingIntent, messagingPopupIntent, userphone, file, recipientId, userImage);
                                } else {
                                    NotificationsManager.showUserNotification(mContext, messagingIntent, messagingPopupIntent, userphone, messageBody, recipientId, userImage);
                                }
                            }
                        }


                        break;
                    case "new_group_message_notification_whatsclone":
                        String application = intent.getExtras().getString("app");
                        String File = intent.getExtras().getString("file");
                        String userPhone = intent.getExtras().getString("senderPhone");
                        String groupName = UtilsString.unescapeJava(intent.getExtras().getString("groupName"));
                        String messageGroupBody = intent.getExtras().getString("message");
                        int groupID = intent.getExtras().getInt("groupID");
                        String groupImage = intent.getExtras().getString("groupImage");
                        int conversationId = intent.getExtras().getInt("conversationID");
                        String memberName;
                        String name = UtilsPhone.getContactName(mContext, userPhone);
                        if (name != null) {
                            memberName = name;
                        } else {
                            memberName = userPhone;
                        }


                        String message;
                        String userName = UtilsPhone.getContactName(mContext, userPhone);
                        switch (messageGroupBody) {
                            case AppConstants.CREATE_GROUP:
                                if (userName != null) {
                                    message = "" + userName + mContext.getString(R.string.he_created_this_group);
                                } else {
                                    message = "" + userPhone + mContext.getString(R.string.he_created_this_group);
                                }


                                break;
                            case AppConstants.LEFT_GROUP:
                                if (userName != null) {
                                    message = "" + userName + mContext.getString(R.string.he_left);
                                } else {
                                    message = "" + userPhone + mContext.getString(R.string.he_left);
                                }


                                break;
                            default:
                                message = messageGroupBody;
                                break;
                        }

                        /**
                         * this for default activity
                         */
                        Intent messagingGroupIntent = new Intent(mContext, MessagesActivity.class);
                        messagingGroupIntent.putExtra("conversationID", conversationId);
                        messagingGroupIntent.putExtra("groupID", groupID);
                        messagingGroupIntent.putExtra("isGroup", true);
                        messagingGroupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        /**
                         * this for popup activity
                         */
                        Intent messagingGroupPopupIntent = new Intent(mContext, MessagesPopupActivity.class);
                        messagingGroupPopupIntent.putExtra("conversationID", conversationId);
                        messagingGroupPopupIntent.putExtra("groupID", groupID);
                        messagingGroupPopupIntent.putExtra("isGroup", true);
                        messagingGroupPopupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        if (application != null && application.equals(mContext.getPackageName())) {
                            if (AppHelper.isActivityRunning(mContext, "activities.messages.MessagesActivity")) {
                                NotificationsModel notificationsModel = new NotificationsModel();
                                notificationsModel.setConversationID(conversationId);
                                notificationsModel.setFile(File);
                                notificationsModel.setGroup(true);
                                notificationsModel.setImage(groupImage);
                                notificationsModel.setPhone(userPhone);
                                notificationsModel.setMessage(messageGroupBody);
                                notificationsModel.setMemberName(memberName);
                                notificationsModel.setGroupID(groupID);
                                notificationsModel.setGroupName(groupName);
                                notificationsModel.setAppName(application);
                                EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_NEW_GROUP_NOTIFICATION, notificationsModel));
                            } else {
                                if (File != null) {
                                    NotificationsManager.showGroupNotification(mContext, messagingGroupIntent, messagingGroupPopupIntent, groupName, memberName + " : " + File, groupID, groupImage);
                                } else {
                                    NotificationsManager.showGroupNotification(mContext, messagingGroupIntent, messagingGroupPopupIntent, groupName, memberName + " : " + message, groupID, groupImage);
                                }
                            }
                        }


                        break;
                    case "new_user_joined_notification_whatsclone":
                        String Userphone = intent.getExtras().getString("phone");
                        String MessageBody = intent.getExtras().getString("message");
                        int RecipientId = intent.getExtras().getInt("recipientID");
                        int ConversationID = intent.getExtras().getInt("conversationID");
                        /**
                         * this for default activity
                         */
                        Intent MessagingIntent = new Intent(mContext, MessagesActivity.class);
                        MessagingIntent.putExtra("conversationID", ConversationID);
                        MessagingIntent.putExtra("recipientID", RecipientId);
                        MessagingIntent.putExtra("isGroup", false);
                        MessagingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        /**
                         * this for popup activity
                         */
                        Intent MessagingPopupIntent = new Intent(mContext, MessagesPopupActivity.class);
                        MessagingPopupIntent.putExtra("conversationID", ConversationID);
                        MessagingPopupIntent.putExtra("recipientID", RecipientId);
                        MessagingPopupIntent.putExtra("isGroup", false);
                        MessagingPopupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        NotificationsManager.showUserNotification(mContext, MessagingIntent, MessagingPopupIntent, Userphone, MessageBody, RecipientId, null);
                        break;
/*                    case "new_message_server_whatsclone":

                        break;*/
                }

            }
        };


        getApplication().registerReceiver(mChangeListener, new IntentFilter("new_user_message_notification_whatsclone"));
        getApplication().registerReceiver(mChangeListener, new IntentFilter("new_group_message_notification_whatsclone"));
        getApplication().registerReceiver(mChangeListener, new IntentFilter("new_user_joined_notification_whatsclone"));


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AppHelper.LogCat("MainService  has started");
        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    @Override
    public void onDestroy() {
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
        super.onDestroy();

        AppHelper.LogCat("MainService has stopped");
        NotificationsManager.SetupBadger(mContext);
        // service finished
        if (mChangeListener != null)
            mContext.unregisterReceiver(mChangeListener);
        disconnectSocket();
        handler.removeCallbacksAndMessages(null);

    }

    /**
     * method to check  for all unsent messages
     */
    public synchronized static void unSentMessages(Context mContext) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();

        List<MessagesModel> messagesModelsList = realm.where(MessagesModel.class)
                .equalTo("status", AppConstants.IS_WAITING)
                .equalTo("isGroup", false)
                .equalTo("isFileUpload", true)
                .equalTo("senderID", PreferenceManager.getID(mContext))
                .findAllSorted("id", Sort.ASCENDING);

        AppHelper.LogCat("size " + messagesModelsList.size());
        if (messagesModelsList.size() != 0) {

            for (MessagesModel messagesModel : messagesModelsList) {
                sendMessages(messagesModel);
            }
        }
        realm.close();

    }

    /**
     * method to send unsentMessages
     *
     * @param messagesModel this i parameter for sendMessages method
     */
    public static void sendMessages(MessagesModel messagesModel) {

        UpdateMessageModel updateMessageModel = new UpdateMessageModel();
        updateMessageModel.setSenderId(messagesModel.getSenderID());
        updateMessageModel.setRecipientId(messagesModel.getRecipientID());
        updateMessageModel.setMessageId(messagesModel.getId());
        updateMessageModel.setConversationId(messagesModel.getConversationID());
        updateMessageModel.setMessageBody(messagesModel.getMessage());
        updateMessageModel.setSenderName(messagesModel.getUsername());
        updateMessageModel.setSenderImage("null");
        updateMessageModel.setPhone(messagesModel.getPhone());
        updateMessageModel.setDate(messagesModel.getDate());
        updateMessageModel.setVideo(messagesModel.getVideoFile());
        updateMessageModel.setThumbnail(messagesModel.getVideoThumbnailFile());
        updateMessageModel.setImage(messagesModel.getImageFile());
        updateMessageModel.setAudio(messagesModel.getAudioFile());
        updateMessageModel.setDocument(messagesModel.getDocumentFile());
        updateMessageModel.setFileSize(messagesModel.getFileSize());
        updateMessageModel.setDuration(messagesModel.getDuration());
        updateMessageModel.setGroup(messagesModel.isGroup());
        updateMessageModel.setRegistered_id(getRegisteredId(messagesModel.getRecipientID()));

        if (!messagesModel.isFileUpload()) return;
        MainService.sendMessage(updateMessageModel, false);

    }

    public static void sendMessage(UpdateMessageModel updateMessageModel, boolean forGroup) {
        if (forGroup) {
            APIHelper.initialApiUsersContacts().sendGroupMessage(updateMessageModel).subscribe(response -> {
                if (response.isSuccess()) {
                    MemberMarkMessageAsSent(updateMessageModel.getGroupID());
                }
            }, throwable -> {

            });
        } else {
            APIHelper.initialApiUsersContacts().sendMessage(updateMessageModel).subscribe(response -> {
                if (response.isSuccess()) {
                    if (response.isSuccess()) {
                        makeMessageAsSent(updateMessageModel.getSenderId(), updateMessageModel.getMessageId());
                    }
                }
            }, throwable -> {

            });
        }

    }

    private static String getRegisteredId(int recipientId) {
        String registered_id;
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        ContactsModel contactsModel = realm.where(ContactsModel.class).equalTo("id", recipientId).findFirst();
        registered_id = contactsModel.getRegistered_id();
        if (!realm.isClosed()) realm.close();
        return registered_id;
    }

    /**
     * method to send unsentMessages who has files
     *
     * @param messagesModel this i parameter for sendMessages method
     */
    public static void sendMessagesFiles(Context mContext, MessagesModel messagesModel) {
        UpdateMessageModel updateMessageModel = new UpdateMessageModel();
        updateMessageModel.setSenderId(messagesModel.getSenderID());
        updateMessageModel.setRecipientId(messagesModel.getRecipientID());
        updateMessageModel.setMessageId(messagesModel.getId());
        updateMessageModel.setConversationId(messagesModel.getConversationID());
        updateMessageModel.setMessageBody(messagesModel.getMessage());
        updateMessageModel.setSenderName(messagesModel.getUsername());
        updateMessageModel.setSenderImage("null");
        updateMessageModel.setPhone(messagesModel.getPhone());
        updateMessageModel.setDate(messagesModel.getDate());
        updateMessageModel.setVideo(messagesModel.getVideoFile());
        updateMessageModel.setThumbnail(messagesModel.getVideoThumbnailFile());
        updateMessageModel.setImage(messagesModel.getImageFile());
        updateMessageModel.setAudio(messagesModel.getAudioFile());
        updateMessageModel.setDocument(messagesModel.getDocumentFile());
        updateMessageModel.setFileSize(messagesModel.getFileSize());
        updateMessageModel.setDuration(messagesModel.getDuration());
        updateMessageModel.setGroup(messagesModel.isGroup());
        updateMessageModel.setRegistered_id(getRegisteredId(messagesModel.getRecipientID()));

        if (!messagesModel.isFileUpload()) return;
        MainService.sendMessage(updateMessageModel, false);
    }


    /**
     * method to  update status delivered when user was offline and come online
     * and he has a new messages (unread)
     *
     * @param mContext
     */

    private static void updateStatusDeliveredOffline(Context mContext) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        List<MessagesModel> messagesModels = realm.where(MessagesModel.class)
                .notEqualTo("recipientID", PreferenceManager.getID(mContext))
                .equalTo("status", AppConstants.IS_WAITING).findAll();
        if (messagesModels.size() != 0) {
            for (MessagesModel messagesModel : messagesModels) {
                RecipientMarkMessageAsDelivered(mContext, messagesModel.getId());
            }
        }
    }

    /**
     * method to mark messages as delivered by recipient
     *
     * @param mContext
     * @param messageId this is the  parameter for RecipientMarkMessageAsDelivered method
     */
    public static void RecipientMarkMessageAsDelivered(Context mContext, int messageId) {
        try {
            JSONObject json = new JSONObject();
            json.put("senderId", PreferenceManager.getID(mContext));
            json.put("messageId", messageId);

            if (mSocket != null) {
                mSocket.emit(AppConstants.SOCKET_IS_MESSAGE_DELIVERED, json);
            }


        } catch (Exception e) {
            AppHelper.LogCat(e);
        }
        AppHelper.LogCat("--> Recipient mark message as  delivered <--");
    }

    /**
     * method to emit that message are seen by user
     */
    public static void emitMessageSeen(Context mContext, int senderId) {
        JSONObject json = new JSONObject();
        try {
            json.put("recipientId", senderId);
            json.put("senderId", PreferenceManager.getID(mContext));

            if (mSocket != null)
                mSocket.emit(AppConstants.SOCKET_IS_MESSAGE_SEEN, json);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * method to mark messages as delivered by recipient
     *
     * @param mContext
     * @param groupId  this is the  parameter for RecipientMarkMessageAsDeliveredGroup method
     */
    public static void RecipientMarkMessageAsDeliveredGroup(Context mContext, int groupId) {
        try {
            JSONObject json = new JSONObject();
            json.put("senderId", PreferenceManager.getID(mContext));
            json.put("groupId", groupId);

            if (mSocket != null) {
                mSocket.emit(AppConstants.SOCKET_IS_MESSAGE_GROUP_DELIVERED, json);
            }


        } catch (Exception e) {
            AppHelper.LogCat(e);
        }
        AppHelper.LogCat("--> Recipient mark message as  delivered <--");
    }

    /**
     * method to mark messages as seen by recipient
     *
     * @param mContext
     * @param groupId  this is the  parameter for RecipientMarkMessageAsSeenGroup method
     */
    public static void RecipientMarkMessageAsSeenGroup(Context mContext, int groupId) {
        try {
            JSONObject json = new JSONObject();
            json.put("senderId", PreferenceManager.getID(mContext));
            json.put("groupId", groupId);

            if (mSocket != null) {
                mSocket.emit(AppConstants.SOCKET_IS_MESSAGE_GROUP_SEEN, json);
            }


        } catch (Exception e) {
            AppHelper.LogCat(e);
        }
        AppHelper.LogCat("--> Recipient mark message as  delivered <--");
    }

    /**
     * method to update status for a specific  message (as delivered by sender)
     */
    private void SenderMarkMessageAsDelivered() {

        mSocket.on(AppConstants.SOCKET_IS_MESSAGE_DELIVERED, args -> {

            JSONObject data = (JSONObject) args[0];
            try {
                int senderId = data.getInt("senderId");
                if (senderId == PreferenceManager.getID(mContext))
                    return;
                updateDeliveredStatus(data);
                AppHelper.LogCat("--> Sender mark message as  delivered: update status  <--");

            } catch (Exception e) {
                AppHelper.LogCat(e);
            }

        });
    }


    /**
     * method to update status for a specific  message (as delivered by sender) in realm database
     *
     * @param data this is parameter for  updateDeliveredStatus
     */
    private void updateDeliveredStatus(JSONObject data) {
        try {
            int messageId = data.getInt("messageId");
            int senderId = data.getInt("senderId");
            if (senderId == PreferenceManager.getID(mContext)) return;
            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            realm.executeTransaction(realm1 -> {
                MessagesModel messagesModel = realm1.where(MessagesModel.class).equalTo("id", messageId).equalTo("status", AppConstants.IS_SENT).findFirst();
                if (messagesModel != null) {
                    messagesModel.setStatus(AppConstants.IS_DELIVERED);
                    realm1.copyToRealmOrUpdate(messagesModel);
                    AppHelper.LogCat("Delivered successfully");
                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MESSAGE_IS_DELIVERED_FOR_MESSAGES, messageId));
                    EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_IS_DELIVERED_FOR_CONVERSATIONS, messagesModel.getConversationID()));
                } else {
                    AppHelper.LogCat("Delivered failed ");
                }
            });
            realm.close();
        } catch (JSONException e) {
            AppHelper.LogCat("Save data to realm delivered JSONException " + e.getMessage());
        }
    }


    public static boolean checkIfUserBlockedExist(int userId, Realm realm) {
        RealmQuery<UsersBlockModel> query = realm.where(UsersBlockModel.class).equalTo("contactsModel.id", userId);
        return query.count() != 0;
    }

    /**
     * method to make message as sent
     */
    private static void makeMessageAsSent(int SenderID, int messageId) {
        if (SenderID != PreferenceManager.getID(WhatsCloneApplication.getInstance()))
            return;
        updateStatusAsSentBySender(messageId, AppConstants.IS_SENT);


    }

    /**
     * method to update user register id firbase
     */
    private void updateRegisterId() {

        mSocket.on(AppConstants.SOCKET_UPDATE_REGISTER_ID, args -> {
            JSONObject dataOn = (JSONObject) args[0];

            try {
                int userId = dataOn.getInt("userId");
                if (userId == PreferenceManager.getID(mContext))
                    return;
                updateRegisterId(dataOn);
            } catch (JSONException e) {
                AppHelper.LogCat("Recipient is online  MainService" + e.getMessage());
            }

        });
    }

    private void updateRegisterId(JSONObject data) {
        try {
            int userId = data.getInt("userId");
            String register_id = data.getString("register_id");

            try {
                Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();

                realm.executeTransaction(realm1 -> {
                    ContactsModel contactsModel = realm1.where(ContactsModel.class).equalTo("id", userId).findFirst();
                    contactsModel.setRegistered_id(register_id);
                    realm1.copyToRealmOrUpdate(contactsModel);
                });
                if (!realm.isClosed())
                    realm.close();

            } catch (Exception e) {
                AppHelper.LogCat("null object Exception MainService" + e.getMessage());
            }


        } catch (JSONException e) {
            AppHelper.LogCat("updateRegisterId error  MainService" + e.getMessage());
        }
    }

    /**
     * method to update status as seen by sender (if recipient have been seen the message)
     */
    private void SenderMarkMessageAsSeen() {
        mSocket.on(AppConstants.SOCKET_IS_MESSAGE_SEEN, args -> {
            JSONObject data = (JSONObject) args[0];
            updateSeenStatus(data);
        });

    }


    /**
     * method to get a conversation id by groupId
     *
     * @param groupId this is the first parameter for getConversationId method
     * @param realm   this is the second parameter for getConversationId method
     * @return conversation id
     */
    private static int getConversationIdByGroupId(int groupId, Realm realm) {
        try {
            ConversationsModel conversationsModelNew = realm.where(ConversationsModel.class)
                    .equalTo("groupID", groupId)
                    .findAll().first();
            return conversationsModelNew.getId();
        } catch (Exception e) {
            AppHelper.LogCat("Conversation id  (group) Exception MainService  " + e.getMessage());
            return 0;
        }
    }

    /**
     * method to update status as seen by sender (if recipient have been seen the message)
     */
    private static void MemberMarkMessageAsSent(int groupId) {
        updateGroupSentStatus(groupId);
    }

    /**
     * method to update status as delivered by sender (if recipient have been seen the message)
     */
    private void MemberMarkMessageAsDelivered() {
        mSocket.on(AppConstants.SOCKET_IS_MESSAGE_GROUP_DELIVERED, args -> {
            JSONObject data = (JSONObject) args[0];
            // AppHelper.LogCat("SOCKET_IS_MESSAGE_GROUP_DELIVERED ");
            updateGroupDeliveredStatus(data);
        });
        mSocket.on(AppConstants.SOCKET_IS_MESSAGE_GROUP_SEEN, args -> {
            JSONObject data = (JSONObject) args[0];
            // AppHelper.LogCat("SOCKET_IS_MESSAGE_GROUP_SEEN ");

            updateGroupSeenStatus(data);
        });

    }

    /**
     * method to update status as delivered by sender
     *
     * @param data this is parameter for updateSeenStatus method
     */
    private void updateGroupDeliveredStatus(JSONObject data) {


        try {
            int groupId = data.getInt("groupId");
            int senderId = data.getInt("senderId");
            AppHelper.LogCat("groupId " + groupId);
            AppHelper.LogCat("sen hhh " + senderId);
            if (senderId == PreferenceManager.getID(mContext)) return;

            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            int ConversationID = getConversationIdByGroupId(groupId, realm);
            AppHelper.LogCat("conversation  id seen " + ConversationID);
            List<MessagesModel> messagesModelsRealm = realm.where(MessagesModel.class)
                    .equalTo("conversationID", ConversationID)
                    .equalTo("isGroup", true)
                    .equalTo("groupID", groupId)
                    .equalTo("status", AppConstants.IS_SENT)
                    .findAll();
            if (messagesModelsRealm.size() != 0) {
                for (MessagesModel messagesModel1 : messagesModelsRealm) {

                    realm.executeTransaction(realm1 -> {
                        MessagesModel messagesModel = realm1.where(MessagesModel.class)
                                .equalTo("groupID", groupId)
                                .equalTo("id", messagesModel1.getId())
                                .equalTo("status", AppConstants.IS_SENT).findFirst();
                        if (messagesModel != null) {
                            messagesModel.setStatus(AppConstants.IS_DELIVERED);
                            realm1.copyToRealmOrUpdate(messagesModel);
                            AppHelper.LogCat("Delivered successfully MainService");

                            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MESSAGE_IS_DELIVERED_FOR_MESSAGES, messagesModel.getId()));

                        } else {
                            AppHelper.LogCat("Seen  failed MainService ");
                        }
                    });

                }
            }
            realm.close();
            EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_IS_DELIVERED_FOR_CONVERSATIONS, ConversationID));

        } catch (JSONException e) {
            AppHelper.LogCat("Save to realm seen MainService " + e.getMessage());
        }

    }

    /**
     * method to update status as seen by sender (group)
     *
     * @param data this is parameter for updateSeenStatus method
     */
    private void updateGroupSeenStatus(JSONObject data) {

        try {
            int groupId = data.getInt("groupId");
            int senderId = data.getInt("senderId");
            AppHelper.LogCat("groupId " + groupId);
            AppHelper.LogCat("sen " + senderId);
            if (senderId == PreferenceManager.getID(mContext)) return;
            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            int ConversationID = getConversationIdByGroupId(groupId, realm);
            AppHelper.LogCat("conversation  id seen " + ConversationID);
            List<MessagesModel> messagesModelsRealm = realm.where(MessagesModel.class)
                    .equalTo("conversationID", ConversationID)
                    .equalTo("isGroup", true)
                    .equalTo("groupID", groupId)
                    .beginGroup()
                    .equalTo("status", AppConstants.IS_SENT)
                    .or()
                    .equalTo("status", AppConstants.IS_DELIVERED)
                    .endGroup()
                    .findAll();
            if (messagesModelsRealm.size() != 0) {
                for (MessagesModel messagesModel1 : messagesModelsRealm) {

                    realm.executeTransaction(realm1 -> {
                        MessagesModel messagesModel = realm1.where(MessagesModel.class)
                                .equalTo("groupID", groupId)
                                .equalTo("id", messagesModel1.getId())
                                .beginGroup()
                                .equalTo("status", AppConstants.IS_SENT)
                                .or()
                                .equalTo("status", AppConstants.IS_DELIVERED)
                                .endGroup()
                                .findFirst();
                        if (messagesModel != null) {
                            messagesModel.setStatus(AppConstants.IS_SEEN);
                            realm1.copyToRealmOrUpdate(messagesModel);
                            AppHelper.LogCat("seen successfully");
                            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MESSAGE_IS_SEEN_FOR_MESSAGES, messagesModel.getId()));

                        } else {
                            AppHelper.LogCat("Seen  failed MainService (group)");
                        }
                    });
                }
            }
            realm.close();
            EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_IS_SEEN_FOR_CONVERSATIONS, ConversationID));

        } catch (JSONException e) {
            AppHelper.LogCat("Save to realm seen " + e);
        }

    }


    /**
     * method to update status as sent by sender
     */
    private static void updateGroupSentStatus(int groupId) {
        int senderId = PreferenceManager.getID(WhatsCloneApplication.getInstance());
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
        int ConversationID = getConversationIdByGroupId(groupId, realm);
        List<MessagesModel> messagesModelsRealm = realm.where(MessagesModel.class)
                .equalTo("conversationID", ConversationID)
                .equalTo("isGroup", true)
                .equalTo("groupID", groupId)
                .equalTo("senderID", senderId)
                .equalTo("status", AppConstants.IS_WAITING)
                .findAll();
        if (messagesModelsRealm.size() != 0) {
            for (MessagesModel messagesModel1 : messagesModelsRealm) {

                realm.executeTransaction(realm1 -> {
                    MessagesModel messagesModel = realm1.where(MessagesModel.class)
                            .equalTo("isGroup", true)
                            .equalTo("isFileUpload", true)
                            .equalTo("groupID", groupId)
                            .equalTo("senderID", senderId)
                            .equalTo("id", messagesModel1.getId())
                            .equalTo("status", AppConstants.IS_WAITING).findFirst();
                    if (messagesModel != null) {
                        messagesModel.setStatus(AppConstants.IS_SENT);
                        realm1.copyToRealmOrUpdate(messagesModel);
                        AppHelper.LogCat("Sent successfully MainService");
                        EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MESSAGE_IS_SENT_FOR_MESSAGES, messagesModel.getId()));
                    } else {
                        AppHelper.LogCat("Sent  failed  MainService");
                    }
                    EventBus.getDefault().post(new Pusher(EVENT_BUS_NEW_MESSAGE_IS_SENT_FOR_CONVERSATIONS, ConversationID));
                });

            }
        }
        if (!realm.isClosed())
            realm.close();

    }

    /**
     * method to update status as seen by sender (if recipient have been seen the message)  in realm database
     *
     * @param data this is parameter for updateSeenStatus method
     */
    private void updateSeenStatus(JSONObject data) {

        try {
            int recipientId = data.getInt("recipientId");
            int senderId = data.getInt("senderId");
            if (senderId == PreferenceManager.getID(mContext)) return;
            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            int ConversationID = getConversationId(senderId, recipientId, realm);
            List<MessagesModel> messagesModelsRealm = realm.where(MessagesModel.class)
                    .equalTo("conversationID", ConversationID)
                    .equalTo("isGroup", false)
                    .beginGroup()
                    .equalTo("status", AppConstants.IS_DELIVERED)
                    .or()
                    .equalTo("status", AppConstants.IS_SENT)
                    .endGroup()
                    .findAll();
            if (messagesModelsRealm.size() != 0) {
                for (MessagesModel messagesModel1 : messagesModelsRealm) {

                    realm.executeTransaction(realm1 -> {
                        MessagesModel messagesModel = realm1.where(MessagesModel.class)
                                .equalTo("recipientID", senderId)
                                .equalTo("senderID", recipientId)
                                .equalTo("id", messagesModel1.getId())
                                .beginGroup()
                                .equalTo("status", AppConstants.IS_DELIVERED)
                                .or()
                                .equalTo("status", AppConstants.IS_SENT)
                                .endGroup()
                                .findFirst();
                        if (messagesModel != null) {
                            messagesModel.setStatus(AppConstants.IS_SEEN);
                            realm1.copyToRealmOrUpdate(messagesModel);
                            AppHelper.LogCat("Seen successfully MainService");
                            EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MESSAGE_IS_SEEN_FOR_MESSAGES, messagesModel.getId()));
                        } else {
                            AppHelper.LogCat("Seen  failed  MainService");
                        }
                    });
                }
            }
            EventBus.getDefault().post(new Pusher(EVENT_BUS_MESSAGE_IS_SEEN_FOR_CONVERSATIONS, ConversationID));
            realm.close();
        } catch (JSONException e) {
            AppHelper.LogCat("Save to realm seen  Exception" + e.getMessage());
        }

    }

    /**
     * method to get a conversation id
     *
     * @param recipientId this is the first parameter for getConversationId method
     * @param senderId    this is the second parameter for getConversationId method
     * @param realm       this is the thirded parameter for getConversationId method
     * @return conversation id
     */
    public static int getConversationId(int recipientId, int senderId, Realm realm) {
        try {
            ConversationsModel conversationsModelNew = realm.where(ConversationsModel.class)
                    .beginGroup()
                    .equalTo("RecipientID", recipientId)
                    .or()
                    .equalTo("RecipientID", senderId)
                    .endGroup().findAll().first();
            return conversationsModelNew.getId();
        } catch (Exception e) {
            AppHelper.LogCat("Conversation id Exception MainService" + e.getMessage());
            return 0;
        }
    }


    /**
     * method to update status for the send message by sender  (as sent message ) in realm  database
     *
     * @param messageId this is the first parameter for updateStatusAsSentBySender method
     * @param isSent    this is the second parameter for updateStatusAsSentBySender method
     */
    private static void updateStatusAsSentBySender(int messageId, int isSent) {


        try {
            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            try {
                realm.executeTransaction(realm1 -> {
                    MessagesModel messagesModel = realm1.where(MessagesModel.class).equalTo("id", messageId).findFirst();
                    messagesModel.setStatus(isSent);
                    realm1.copyToRealmOrUpdate(messagesModel);
                    EventBus.getDefault().post(new Pusher(AppConstants.EVENT_BUS_MESSAGE_IS_SENT_FOR_MESSAGES, messageId));
                    EventBus.getDefault().post(new Pusher(EVENT_BUS_NEW_MESSAGE_IS_SENT_FOR_CONVERSATIONS, messagesModel.getConversationID()));
                });
            } catch (Exception e) {
                AppHelper.LogCat(" Is sent messages Realm Error" + e.getMessage());
            }

            realm.close();

        } catch (Exception e) {
            AppHelper.LogCat("null object Exception MainService" + e.getMessage());
        }


    }


    /**
     * method to send group messages
     *
     * @param messagesModel this is parameter of sendMessagesGroup method
     */
    public static void sendMessagesGroup(Activity activity, ContactsModel mUsersModel, GroupsModel mGroupsModel, MessagesModel messagesModel) {

        JSONObject message = new JSONObject();
        try {

            if (mUsersModel != null && mUsersModel.getUsername() != null) {
                message.put("senderName", mUsersModel.getUsername());
            } else {
                message.put("senderName", "null");
            }
            if (mUsersModel != null)
                message.put("phone", mUsersModel.getPhone());
            else
                message.put("phone", null);


            if (mGroupsModel != null && mGroupsModel.getGroupImage() != null)
                message.put("GroupImage", mGroupsModel.getGroupImage());
            else
                message.put("GroupImage", "null");
            if (mGroupsModel != null && mGroupsModel.getGroupName() != null)
                message.put("GroupName", mGroupsModel.getGroupName());
            else
                message.put("GroupName", "null");

            message.put("messageBody", messagesModel.getMessage());
            message.put("senderId", messagesModel.getSenderID());
            message.put("recipientId", messagesModel.getRecipientID());
            if (mGroupsModel != null && mGroupsModel.getGroupName() != null)
                message.put("groupID", mGroupsModel.getId());
            else
                message.put("groupID", messagesModel.getGroupID());
            message.put("date", messagesModel.getDate());
            message.put("isGroup", true);
            message.put("image", messagesModel.getImageFile());
            message.put("video", messagesModel.getVideoFile());
            message.put("audio", messagesModel.getAudioFile());
            message.put("thumbnail", messagesModel.getVideoThumbnailFile());
            message.put("document", messagesModel.getDocumentFile());

            if (!messagesModel.getFileSize().equals("0"))
                message.put("fileSize", messagesModel.getFileSize());
            else
                message.put("fileSize", "0");

            if (!messagesModel.getDuration().equals("0"))
                message.put("duration", messagesModel.getDuration());
            else
                message.put("duration", "0");

            message.put("userToken", PreferenceManager.getToken(activity));


            UpdateMessageModel updateMessageModel = new UpdateMessageModel();
            try {
                updateMessageModel.setSenderId(message.getInt("senderId"));
                updateMessageModel.setRecipientId(message.getInt("recipientId"));
                updateMessageModel.setMessageBody(message.getString("messageBody"));
                updateMessageModel.setSenderName(message.getString("senderName"));
                updateMessageModel.setGroupName(message.getString("GroupName"));
                updateMessageModel.setGroupImage(message.getString("GroupImage"));
                updateMessageModel.setGroupID(message.getInt("groupID"));
                updateMessageModel.setDate(message.getString("date"));
                updateMessageModel.setPhone(message.getString("phone"));
                updateMessageModel.setVideo(message.getString("video"));
                updateMessageModel.setThumbnail(message.getString("thumbnail"));
                updateMessageModel.setImage(message.getString("image"));
                updateMessageModel.setAudio(message.getString("audio"));
                updateMessageModel.setDocument(message.getString("document"));
                updateMessageModel.setFileSize(message.getString("fileSize"));
                updateMessageModel.setDuration(message.getString("duration"));
                updateMessageModel.setGroup(message.getBoolean("isGroup"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            MainService.sendMessage(updateMessageModel, true);
        } catch (JSONException e) {
            AppHelper.LogCat(e.getMessage());
        }


    }


    private void onReceiveNewCall() {
        mSocket.on(AppConstants.SOCKET_RECEIVE_NEW_CALL, onReceiveNewCall);
    }

    /**
     * Receive call emitter callback when others call you.
     *
     * @param args json value contain callerid, userid and caller name
     */
    private Emitter.Listener onReceiveNewCall = args -> {
        AppHelper.LogCat("onReceiveNewCall called");
        JSONObject data = (JSONObject) args[0];
        try {
            String callerSocketId = data.getString("from");
            String callerPhone = data.getString("callerPhone");
            int callerID = data.getInt("callerID");
            String callerImage = data.getString("callerImage");
            boolean isVideoCall = data.getBoolean("isVideoCall");

            Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();
            if (!checkIfUserBlockedExist(callerID, realm)) {
                if (!realm.isClosed())
                    realm.close();
                Intent intent = new Intent(getApplicationContext(), IncomingCallActivity.class);
                intent.setAction(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(AppConstants.CALLER_SOCKET_ID, callerSocketId);
                intent.putExtra(AppConstants.USER_SOCKET_ID, PreferenceManager.getSocketID(this));
                intent.putExtra(AppConstants.CALLER_PHONE, callerPhone);
                intent.putExtra(AppConstants.CALLER_IMAGE, callerImage);
                intent.putExtra(AppConstants.CALLER_ID, callerID);
                intent.putExtra(AppConstants.IS_VIDEO_CALL, isVideoCall);
                intent.putExtra(AppConstants.USER_PHONE, PreferenceManager.getPhone(this));
                getApplicationContext().startActivity(intent);
            } else {
                try {
                    JSONObject message = new JSONObject();
                    message.put("userSocketId", PreferenceManager.getSocketID(this));
                    message.put("callerSocketId", callerSocketId);
                    message.put("reason", AppConstants.NO_ANSWER);
                    mSocket.emit(AppConstants.SOCKET_REJECT_NEW_CALL, message);
                } catch (JSONException e) {
                    AppHelper.LogCat(" JSONException IncomingCallActivity rejectCall " + e.getMessage());
                }
            }
        } catch (JSONException e) {
            AppHelper.LogCat("JSONException Call" + e.getMessage());
        }

    };


}