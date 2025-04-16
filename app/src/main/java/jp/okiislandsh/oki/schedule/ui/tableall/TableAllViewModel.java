package jp.okiislandsh.oki.schedule.ui.tableall;

import static jp.okiislandsh.library.core.MyUtil.nvl;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import jp.okiislandsh.library.android.live.MutableNonNullLiveData;
import jp.okiislandsh.library.android.live.NonNullLiveData;
import jp.okiislandsh.oki.schedule.util.P;
import jp.okiislandsh.oki.schedule.util.PortsBool;

public class TableAllViewModel extends AndroidViewModel {

    /** ボタン */
    public final @NonNull MutableLiveData<TIMETABLE> mTimeTable;

    /** 日付 */
    public final @NonNull MutableNonNullLiveData<Calendar> mDate;

    /** Exフィルタ：年選択 */
    public final @NonNull MutableNonNullLiveData<int[]> mYears;

    /** Exフィルタ：船名選択 */
    public final @NonNull MutableNonNullLiveData<SHIP_AND_INFO[]> mShips;

    /** Exフィルタ：臨時フラグ */
    public final @NonNull MutableNonNullLiveData<Boolean> mRinji;

    public final @NonNull NonNullLiveData<Integer> livePortsBool;

    public TableAllViewModel(@NonNull Application application, @NonNull SavedStateHandle state) {
        super(application);

        mTimeTable = state.getLiveData("mTimeTable", TIMETABLE.DATE);
        mDate = new MutableNonNullLiveData<>(nvl(state.get("mDate"), GregorianCalendar.getInstance(TimeZone.getTimeZone("Japan"))));

        mYears = new MutableNonNullLiveData<>(nvl(state.get("mYears"), new int[0]));
        if(mYears.getValue().length==0){
            mYears.setValue(new int[]{new Date().getYear()+1900});
        }
        mShips = new MutableNonNullLiveData<>(nvl(state.get("mShips"), new SHIP_AND_INFO[0]));
        mRinji = new MutableNonNullLiveData<>(nvl(state.get("mRinji"), Boolean.FALSE));

        livePortsBool = P.PORTS_BOOL.getLive(application);
    }

    public void postTimeTable(TIMETABLE t){
        mTimeTable.postValue(t);
    }

    //region PortsBoolお手軽操作
    public void getPortBoolPostReverseChibu(){
        final @NonNull PortsBool portsBool = P.getPortsBool(getApplication());
        portsBool.chibu = !portsBool.chibu;
        P.setPortsBool(getApplication(), portsBool);
    }

    public void getPortBoolPostReverseAma(){
        final @NonNull PortsBool portsBool = P.getPortsBool(getApplication());
        portsBool.ama = !portsBool.ama;
        P.setPortsBool(getApplication(), portsBool);
    }

    public void getPortBoolPostReverseNishinoshima(){
        final @NonNull PortsBool portsBool = P.getPortsBool(getApplication());
        portsBool.nishinoshima = !portsBool.nishinoshima;
        P.setPortsBool(getApplication(), portsBool);
    }

    public void getPortBoolPostReverseShichirui(){
        final @NonNull PortsBool portsBool = P.getPortsBool(getApplication());
        portsBool.shichirui = !portsBool.shichirui;
        P.setPortsBool(getApplication(), portsBool);
    }

    public void getPortBoolPostReverseSakaiminato(){
        final @NonNull PortsBool portsBool = P.getPortsBool(getApplication());
        portsBool.sakaiminato = !portsBool.sakaiminato;
        P.setPortsBool(getApplication(), portsBool);
    }

    public void getPortBoolPostReverseDogo(){
        final @NonNull PortsBool portsBool = P.getPortsBool(getApplication());
        portsBool.dogo = !portsBool.dogo;
        P.setPortsBool(getApplication(), portsBool);
    }
    //endregion

}