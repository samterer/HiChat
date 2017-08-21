package com.Hi961.HiChat.api;

import com.Hi961.HiChat.api.apiServices.GroupsService;
import com.Hi961.HiChat.api.apiServices.UsersContacts;
import com.Hi961.HiChat.app.WhatsCloneApplication;

/**
 * Created by Abderrahim El imame on 4/11/17.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/Ben__Cherif
 * @Skype : ben-_-cherif
 */

public class APIHelper {

    public static UsersContacts initialApiUsersContacts() {
        APIService mApiService = APIService.with(WhatsCloneApplication.getInstance());
        return new UsersContacts(WhatsCloneApplication.getRealmDatabaseInstance(), WhatsCloneApplication.getInstance(), mApiService);
    }


    public static GroupsService initializeApiGroups() {
        APIService mApiService = APIService.with(WhatsCloneApplication.getInstance());
        return new GroupsService(WhatsCloneApplication.getRealmDatabaseInstance(), WhatsCloneApplication.getInstance(), mApiService);
    }
}
