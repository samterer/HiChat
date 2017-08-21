package com.Hi961.HiChat.presenters.users;


import android.Manifest;
import android.os.Handler;
import android.support.v7.app.AlertDialog;

import com.Hi961.HiChat.R;
import com.Hi961.HiChat.activities.PrivacyActivity;
import com.Hi961.HiChat.api.APIService;
import com.Hi961.HiChat.api.apiServices.UsersContacts;
import com.Hi961.HiChat.app.AppConstants;
import com.Hi961.HiChat.app.WhatsCloneApplication;
import com.Hi961.HiChat.fragments.home.ContactsFragment;
import com.Hi961.HiChat.helpers.AppHelper;
import com.Hi961.HiChat.helpers.OutDateHelper;
import com.Hi961.HiChat.helpers.PermissionHandler;
import com.Hi961.HiChat.helpers.PreferenceManager;
import com.Hi961.HiChat.helpers.UtilsPhone;
import com.Hi961.HiChat.interfaces.Presenter;
import com.Hi961.HiChat.models.users.contacts.ContactsModel;
import com.Hi961.HiChat.models.users.contacts.PusherContacts;
import com.Hi961.HiChat.models.users.contacts.SyncContacts;

import java.util.List;

import org.greenrobot.eventbus.EventBus;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class ContactsPresenter implements Presenter {
    private ContactsFragment contactsFragmentView;
    private PrivacyActivity privacyActivity;
    private Realm realm;
    private UsersContacts mUsersContacts;
    private boolean isImageUpdated = false;

    public ContactsPresenter(ContactsFragment contactsFragment) {
        this.contactsFragmentView = contactsFragment;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }


    public ContactsPresenter(PrivacyActivity privacyActivity) {
        this.privacyActivity = privacyActivity;
        this.realm = WhatsCloneApplication.getRealmDatabaseInstance();
    }


    @Override
    public void onStart() {
    }

    @Override
    public void onCreate() {
        if (contactsFragmentView != null) {
            if (!EventBus.getDefault().isRegistered(contactsFragmentView))
                EventBus.getDefault().register(contactsFragmentView);
            Handler handler = new Handler();
            APIService mApiService = APIService.with(contactsFragmentView.getActivity());
            mUsersContacts = new UsersContacts(realm, contactsFragmentView.getActivity(), mApiService);
            loadDataFromServer();
            getContacts(false);

            handler.postDelayed(() -> {
                try {
                    mUsersContacts.getContactInfo(PreferenceManager.getID(contactsFragmentView.getActivity())).subscribe(contactsModel -> AppHelper.LogCat("info user ContactsPresenter"), throwable -> AppHelper.LogCat("On error ContactsPresenter"));
                    mUsersContacts.getUserStatus().subscribe(statusModels -> AppHelper.LogCat("status user ContactsPresenter"), throwable -> AppHelper.LogCat("On error ContactsPresenter"));
                } catch (Exception e) {
                    AppHelper.LogCat("contact info Exception ContactsPresenter ");
                }
            }, 1500);
        } else if (privacyActivity != null) {
            APIService mApiService = APIService.with(privacyActivity);
            mUsersContacts = new UsersContacts(realm, privacyActivity, mApiService);
            getPrivacyTerms();
        }

    }


    public void getContacts(boolean isRefresh) {
        try {
            mUsersContacts.getAllContacts().subscribe(contactsModels -> {
                contactsFragmentView.ShowContacts(contactsModels, isRefresh);
            }, contactsFragmentView::onErrorLoading, contactsFragmentView::onHideLoading);
            mUsersContacts.getLinkedContacts().subscribe(contactsModels -> {
                try {
                    PreferenceManager.setContactSize(contactsFragmentView.getActivity(), contactsModels.size());
                } catch (Exception e) {
                    AppHelper.LogCat(" Exception size contact fragment");
                }
            }, throwable -> AppHelper.LogCat("contactsFragmentView " + throwable.getMessage()));
        } catch (Exception e) {
            AppHelper.LogCat("getAllContacts Exception ContactsPresenter ");
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
        if (contactsFragmentView != null)
            EventBus.getDefault().unregister(contactsFragmentView);
    }

    @Override
    public void onLoadMore() {

    }


    @Override
    public void onRefresh() {

        if (PermissionHandler.checkPermission(contactsFragmentView.getActivity(), Manifest.permission.READ_CONTACTS)) {
            AppHelper.LogCat("Read contact data permission already granted.");
            if (!isImageUpdated)
                contactsFragmentView.onShowLoading();
            Observable.create((ObservableOnSubscribe<List<ContactsModel>>) subscriber -> {
                try {
                    List<ContactsModel> contactsList = UtilsPhone.GetPhoneContacts(contactsFragmentView.getActivity());
                    subscriber.onNext(contactsList);
                    subscriber.onComplete();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(contactsList -> {
                SyncContacts syncContacts = new SyncContacts();
                syncContacts.setContactsModelList(contactsList);
                mUsersContacts.updateContacts(syncContacts).subscribe(contactsModelList -> {
                    contactsFragmentView.updateContacts(contactsModelList);
                    if (!isImageUpdated)
                        AppHelper.CustomToast(contactsFragmentView.getActivity(), contactsFragmentView.getString(R.string.success_response_contacts));
                }, throwable -> {
                    if (!isImageUpdated)
                        contactsFragmentView.onErrorLoading(throwable);
                    AlertDialog.Builder alert = new AlertDialog.Builder(contactsFragmentView.getActivity());
                    alert.setMessage(contactsFragmentView.getString(R.string.error_response_contacts));
                    alert.setPositiveButton(R.string.ok, (dialog, which) -> {
                    });
                    alert.setCancelable(false);
                    alert.show();

                }, () -> {
                    if (!isImageUpdated) contactsFragmentView.onHideLoading();
                });

            }, throwable -> {
                if (!isImageUpdated)
                    contactsFragmentView.onErrorLoading(throwable);
            });
            mUsersContacts.getContactInfo(PreferenceManager.getID(contactsFragmentView.getActivity())).subscribe(contactsModel -> AppHelper.LogCat(""), AppHelper::LogCat);

        } else {
            AppHelper.LogCat("Please request Read contact data permission.");
            PermissionHandler.requestPermission(contactsFragmentView.getActivity(), Manifest.permission.READ_CONTACTS);
        }

    }

    @Override
    public void onStop() {

    }

    public void onEventMainThread(PusherContacts pusher) {
        switch (pusher.getAction()) {
            case AppConstants.EVENT_BUS_CONTACTS_PERMISSION:
                isImageUpdated = false;
                onRefresh();
                break;
            case AppConstants.EVENT_BUS_IMAGE_PROFILE_UPDATED:
                isImageUpdated = true;
                onRefresh();
                break;
        }
    }

    private void loadDataFromServer() {
        Observable.create((ObservableOnSubscribe<List<ContactsModel>>) subscriber -> {
            try {

                List<ContactsModel> contactsList = UtilsPhone.GetPhoneContacts(contactsFragmentView.getActivity());
                subscriber.onNext(contactsList);
                subscriber.onComplete();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(contactsList -> {
                    SyncContacts syncContacts = new SyncContacts();
                    syncContacts.setContactsModelList(contactsList);
                    mUsersContacts.updateContacts(syncContacts).subscribe(contactsModelList -> {
                        if (contactsModelList != null)
                            contactsFragmentView.updateContacts(contactsModelList);
                        new Handler().postDelayed(this::checkAppVersion, 2000);
                    }, throwable -> {
                        contactsFragmentView.onErrorLoading(throwable);
                        new Handler().postDelayed(this::checkAppVersion, 2000);
                    });

                }, throwable -> {
                    contactsFragmentView.onErrorLoading(throwable);
                    new Handler().postDelayed(this::checkAppVersion, 2000);
                });

    }

    private void checkAppVersion() {
        mUsersContacts.getApplicationVersion().subscribe(versionResponse -> {
            AppHelper.LogCat(" currentAppVersion " + versionResponse.getMessage());
            int currentAppVersion;
            if (PreferenceManager.getVersionApp(contactsFragmentView.getActivity()) != 0) {
                currentAppVersion = PreferenceManager.getVersionApp(contactsFragmentView.getActivity());
            } else {
                currentAppVersion = AppHelper.getAppVersionCode(contactsFragmentView.getActivity());
            }
            if (currentAppVersion != 0 && currentAppVersion < versionResponse.getMessage()) {
                PreferenceManager.setVersionApp(contactsFragmentView.getActivity(), currentAppVersion);
                PreferenceManager.setIsOutDate(contactsFragmentView.getActivity(), true);
                OutDateHelper.significantEvent(contactsFragmentView.getActivity());
                AppHelper.LogCat(" currentAppVersion " + currentAppVersion);
            } else {
                PreferenceManager.setIsOutDate(contactsFragmentView.getActivity(), false);
            }

        }, throwable -> {
            AppHelper.LogCat(" " + throwable.getMessage());
        });
    }

    private void getPrivacyTerms() {
        mUsersContacts.getPrivacyTerms().subscribe(statusResponse -> {
            if (statusResponse.isSuccess()) {
                privacyActivity.showPrivcay(statusResponse.getMessage());
            } else {
                AppHelper.LogCat(" " + statusResponse.getMessage());
            }

        }, throwable -> {
            AppHelper.LogCat(" " + throwable.getMessage());
        });
    }

}