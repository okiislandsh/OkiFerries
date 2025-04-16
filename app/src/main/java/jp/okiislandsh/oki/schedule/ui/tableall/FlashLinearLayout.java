package jp.okiislandsh.oki.schedule.ui.tableall;

import android.content.Context;
import android.util.Pair;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import jp.okiislandsh.library.android.For;
import jp.okiislandsh.library.android.LogDB;
import jp.okiislandsh.library.android.MyUtil;
import jp.okiislandsh.library.android.live.LiveClock;
import jp.okiislandsh.library.android.view.live.LifecycleLinearLayout;
import jp.okiislandsh.library.core.HMInt;
import jp.okiislandsh.library.core.Pairs;
import jp.okiislandsh.library.core.YMDInt;
import jp.okiislandsh.oki.schedule.MyApp;

public class FlashLinearLayout extends LifecycleLinearLayout {

    /** ログクラス */
    protected static final @NonNull LogDB.ILog<CharSequence> Log = LogDB.getStringInstance();

    /** updateループを実現 */
    private final @NonNull LiveData<Pairs.Immutable.NonNull._3<YMDInt, HMInt, Integer>> liveFlash;

    final @NonNull List<Pair<YMDInt, YMDInt>> spans = new ArrayList<>();
    final @NonNull HMInt beginTime;
    final @NonNull HMInt endTime;

    private static final @NonNull YMDInt Y1M1D1 = new YMDInt(10101);
    private static final @NonNull YMDInt Y9999M12MD31 = new YMDInt(99991231);
    private static final @NonNull HMInt H0M0 = new HMInt(0);
    private static final @NonNull HMInt H23M59 = new HMInt(2359);

    /** 日付にかかわらず指定時間内で点滅 */
    FlashLinearLayout(@NonNull Context context, @Nullable HMInt begin, @Nullable HMInt end, @Nullable LiveClock liveFlash){
        this(context, null, begin, end, liveFlash);
    }
    /**
     * 特定の日付に点滅
     * @param day 点滅日
     * @param beginTime 点滅開始時間
     * @param endTime   点滅終了時間
     */
    FlashLinearLayout(@NonNull Context context, @Nullable Pair<YMDInt, YMDInt> day, @Nullable HMInt beginTime, @Nullable HMInt endTime, @Nullable LiveClock liveFlash){
        this(context, day, beginTime, endTime, MyApp.newLiveClockToYMDHM(liveFlash==null ? LiveClock.newInstance(TimeZone.getTimeZone("Japan"), LiveClock.TIME_SPAN.SECONDLY) : liveFlash));
    }
    private FlashLinearLayout(@NonNull Context context, @Nullable Pair<YMDInt, YMDInt> day, @Nullable HMInt beginTime, @Nullable HMInt endTime, @NonNull LiveData<Pairs.Immutable.NonNull._3<YMDInt, HMInt, Integer>> liveFlash){
        super(context);
        if(day==null) {
            this.spans.add(new Pair<>(Y1M1D1, Y9999M12MD31));
        }else{
            this.spans.add(new Pair<>(MyUtil.nvl(day.first, Y1M1D1), MyUtil.nvl(day.second, Y9999M12MD31)));
        }
        this.beginTime = MyUtil.nvl(beginTime, H0M0);
        this.endTime = MyUtil.nvl(endTime, H23M59);
        this.liveFlash = liveFlash;
        observe(this.liveFlash, pairs-> update(pairs.f, pairs.s, pairs.t));
    }

    /**
     * 特定の期間や日付に点滅
     * @param spans 点滅期間
     * @param beginTime 点滅開始時間
     * @param endTime   点滅終了時間
     */
    FlashLinearLayout(@NonNull Context context, @Nullable List<Pair<YMDInt, YMDInt>> spans, @Nullable List<YMDInt> days, @Nullable HMInt beginTime, @Nullable HMInt endTime, @Nullable LiveClock liveFlash){
        this(context, spans, days, beginTime, endTime, MyApp.newLiveClockToYMDHM(liveFlash==null ? LiveClock.newInstance(TimeZone.getTimeZone("Japan"), LiveClock.TIME_SPAN.SECONDLY) : liveFlash));
    }
    private FlashLinearLayout(@NonNull Context context, @Nullable List<Pair<YMDInt, YMDInt>> spans, @Nullable List<YMDInt> days, @Nullable HMInt beginTime, @Nullable HMInt endTime, @NonNull LiveData<Pairs.Immutable.NonNull._3<YMDInt, HMInt, Integer>> liveFlash){
        super(context);
        if(spans!=null) {
            for (Pair<YMDInt, YMDInt> pair : spans) {
                if (pair.first == null || pair.second == null) {
                    pair = new Pair<>(MyUtil.nvl(pair.first, Y1M1D1), MyUtil.nvl(pair.second, Y9999M12MD31));
                }
                this.spans.add(pair);
            }
        }
        if(days!=null) {
            for(YMDInt day: days){
                this.spans.add(new Pair<>(day, day));
            }
        }
        this.beginTime = MyUtil.nvl(beginTime, H0M0);
        this.endTime = MyUtil.nvl(endTime, H23M59);
        this.liveFlash = liveFlash;
        observe(this.liveFlash, pairs-> update(pairs.f, pairs.s, pairs.t));
    }

    private boolean betweenDate(@NonNull YMDInt ymd){
        for(Pair<YMDInt, YMDInt> pair: spans){
            if(ymd.isBetween(pair.first, pair.second)){
                return true;
            }
        }
        return false;
    }

    /** 表示を更新する。表示するかどうかは時間内かつ点滅タイミングによって変わる */
    public void update(@NonNull YMDInt nowYMD, @NonNull HMInt nowHM, int sec){
        final boolean flash = betweenDate(nowYMD) && //期間内 かつ
                                nowHM.isBetween(beginTime, endTime) && //時間内 かつ
                                sec%2==0; //点滅タイミング
        for(@NonNull View v: For.iterable(this))
            v.setVisibility(flash ? INVISIBLE : VISIBLE);
    }

}