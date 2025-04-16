package jp.okiislandsh.oki.schedule.ui.web;

import static jp.okiislandsh.library.core.MyUtil.nvl;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import jp.okiislandsh.library.android.live.MutableNonNullLiveData;
import jp.okiislandsh.oki.schedule.MyApp;
import jp.okiislandsh.oki.schedule.R;
import jp.okiislandsh.oki.schedule.util.MessageData;
import jp.okiislandsh.oki.schedule.util.P;

public class WebViewModel extends AndroidViewModel {

    /** ViewModel:URL */
    public final @NonNull MutableNonNullLiveData<String> mUrl;

    public final @NonNull LiveData<MessageData> mMessage = MyApp.newLiveMessageData();

    public final @NonNull MutableLiveData<Void> mInvalidateOptionMenuNotifier = new MutableLiveData<>();

    public final @NonNull LiveData<Long> mLastReadMessageNumber;

    public WebViewModel(@NonNull Application application, @NonNull SavedStateHandle state) {
        super(application);

        mUrl = new MutableNonNullLiveData<>(nvl(state.get("mUrl"), application.getString(R.string.url_okikisen)));
        mLastReadMessageNumber = P.LAST_READ_MESSAGE_NUMBER.getLive(application);

    }

}