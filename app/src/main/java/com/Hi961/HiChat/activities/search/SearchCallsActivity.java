package com.Hi961.HiChat.activities.search;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.Hi961.HiChat.R;
import com.Hi961.HiChat.adapters.others.TextWatcherAdapter;
import com.Hi961.HiChat.adapters.recyclerView.calls.CallsAdapter;
import com.Hi961.HiChat.animations.AnimationsUtil;
import com.Hi961.HiChat.app.AppConstants;
import com.Hi961.HiChat.app.WhatsCloneApplication;
import com.Hi961.HiChat.helpers.AppHelper;
import com.Hi961.HiChat.models.calls.CallsModel;
import com.Hi961.HiChat.presenters.calls.SearchCallsPresenter;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmList;

/**
 * Created by Abderrahim El imame on 8/12/16.
 *
 * @Email : abderrahim.elimame@gmail.com
 * @Author : https://twitter.com/bencherif_el
 */

public class SearchCallsActivity extends AppCompatActivity {


    @BindView(R.id.close_btn_search_view)
    ImageView closeBtn;
    @BindView(R.id.search_input)
    TextInputEditText searchInput;
    @BindView(R.id.clear_btn_search_view)
    ImageView clearBtn;
    @BindView(R.id.searchList)
    RecyclerView searchList;

    @BindView(R.id.empty)
    LinearLayout emptyLayout;
    private CallsAdapter mCallsAdapter;
    private SearchCallsPresenter mSearchCallsPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        searchInput.setFocusable(true);
        initializerSearchView(searchInput, clearBtn);
        initializerView();
        setTypeFaces();
        mSearchCallsPresenter = new SearchCallsPresenter(this);
        mSearchCallsPresenter.onCreate();
    }


    private void setTypeFaces() {
        if (AppConstants.ENABLE_FONTS_TYPES) {
            searchInput.setTypeface(AppHelper.setTypeFace(this, "Futura"));
        }
    }

    /**
     * method to initialize the  view
     */
    private void initializerView() {
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getApplicationContext());
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        searchList.setLayoutManager(mLinearLayoutManager);
        mCallsAdapter = new CallsAdapter(this, null);
        searchList.setAdapter(mCallsAdapter);
        closeBtn.setOnClickListener(v -> closeSearchView());
        clearBtn.setOnClickListener(v -> clearSearchView());
    }

    /**
     * method to show calls list
     *
     * @param contactsModelList this is parameter for  ShowContacts method
     */
    public void ShowCalls(List<CallsModel> contactsModelList) {


        if (contactsModelList.size() != 0) {
            RealmList<CallsModel> callsModels = new RealmList<CallsModel>();
            for (CallsModel callsModel : contactsModelList) {
                callsModels.add(callsModel);
            }
            mCallsAdapter.setCalls(callsModels);
            searchList.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
        } else {
            searchList.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * method to clear/reset the search view
     */
    public void clearSearchView() {
        if (searchInput.getText() != null) {
            searchInput.setText("");
            mSearchCallsPresenter.onCreate();
            searchList.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
        }
    }

    /**
     * method to close the search view
     */
    public void closeSearchView() {
        finish();
        AnimationsUtil.setSlideOutAnimation(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AnimationsUtil.setSlideOutAnimation(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSearchCallsPresenter.onDestroy();
    }


    /**
     * method to initialize the search view
     */
    public void initializerSearchView(TextInputEditText searchInput, ImageView clearSearchBtn) {

        final Context context = this;
        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }

        });
        searchInput.addTextChangedListener(new TextWatcherAdapter() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                clearSearchBtn.setVisibility(View.GONE);
            }

            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCallsAdapter.setString(s.toString());
                Search(s.toString().trim());
                clearSearchBtn.setVisibility(View.VISIBLE);
            }

            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() == 0) {
                    clearSearchBtn.setVisibility(View.GONE);
                    mSearchCallsPresenter.onCreate();
                }
            }
        });

    }

    public void onErrorLoading(Throwable throwable) {
        AppHelper.LogCat("Search contacts " + throwable.getMessage());
    }

    /**
     * method to start searching
     *
     * @param string this  is parameter for Search method
     */
    public void Search(String string) {

        List<CallsModel> filteredModelList;
        filteredModelList = FilterList(string);
        if (filteredModelList.size() != 0) {
            searchList.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
            mCallsAdapter.animateTo(filteredModelList);
            searchList.scrollToPosition(0);
        } else {
            searchList.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * method to filter the list of calls
     *
     * @param query this parameter for FilterList  method
     * @return this for what method will return
     */
    private List<CallsModel> FilterList(String query) {
        Realm realm = WhatsCloneApplication.getRealmDatabaseInstance();

        List<CallsModel> callsModels = realm.where(CallsModel.class)
                .contains("phone", query, Case.INSENSITIVE)
                .findAll();

        realm.close();
        return callsModels;
    }
}
