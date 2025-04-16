package jp.okiislandsh.oki.schedule.ui.tabledozen;

import static jp.okiislandsh.library.core.MyUtil.nvl;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.SavedStateHandle;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import jp.okiislandsh.library.android.live.MutableNonNullLiveData;

public class TableDozenViewModel extends AndroidViewModel {

    /** 日付 */
    public final @NonNull MutableNonNullLiveData<Calendar> mDate;

    public TableDozenViewModel(@NonNull Application application, @NonNull SavedStateHandle state) {
        super(application);

        mDate = new MutableNonNullLiveData<>(nvl(state.get("mDate"), GregorianCalendar.getInstance(TimeZone.getTimeZone("Japan"))));

    }

}