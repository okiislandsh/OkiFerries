package jp.okiislandsh.oki.schedule.ui.tabledozen;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import jp.okiislandsh.library.android.For;
import jp.okiislandsh.library.android.LogDB;
import jp.okiislandsh.library.android.live.LiveClock;
import jp.okiislandsh.library.android.live.LiveUtil;
import jp.okiislandsh.library.android.view.ViewBuilderFunction;
import jp.okiislandsh.library.core.HMInt;
import jp.okiislandsh.library.core.Pairs;
import jp.okiislandsh.library.core.YMDInt;
import jp.okiislandsh.oki.schedule.MyApp;
import jp.okiislandsh.oki.schedule.R;

public class DozenTimeTableRow extends TableRow implements LifecycleOwner, ViewBuilderFunction.OnViewGroup {

    protected static final @NonNull LogDB.ILog<CharSequence> Log = LogDB.getStringInstance();

    private final @NonNull LiveData<Pairs.Immutable.NonNull._3<YMDInt, HMInt, Integer>> liveFlash;

    final @Nullable YMDInt date;
    final @Nullable HMInt beginTime;
    final @Nullable HMInt endTime;

    private final @NonNull TextView labelShip = addText(null, null, null);
    private final @NonNull TextView labelDeparturePort = addText(null, null, null);
    private final @NonNull TextView labelDepartureTime = addText(null, null, null);
    private final @NonNull TextView labelKeiyu = addText(null, null, null);
    private final @NonNull TextView labelArriveTime = addText(null, null, null);
    private final @NonNull TextView labelArrivePort = addText(null, null, null);

    public DozenTimeTableRow(@NonNull Context context) {
        this(context, null, null, null, LiveClock.newInstance(TimeZone.getTimeZone("Japan"), LiveClock.TIME_SPAN.SECONDLY));
    }

    public DozenTimeTableRow(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);

        this.date = null;
        this.beginTime = null;
        this.endTime = null;
        this.liveFlash = MyApp.newLiveClockToYMDHM(LiveClock.newInstance(TimeZone.getTimeZone("Japan"), LiveClock.TIME_SPAN.SECONDLY));
        observe(this.liveFlash, pairs-> update(pairs.f, pairs.s, pairs.t));
    }

    /**
     * @param date 指定日のみ点滅、nullの場合常に点滅
     * @param beginTime 点滅開始時刻、時刻がいずれかnullの場合常に<span style="color:red;">点滅しない</span>
     * @param endTime   点滅終了時刻、時刻がいずれかnullの場合常に<span style="color:red;">点滅しない</span>
     */
    public DozenTimeTableRow(@NonNull Context context, @Nullable YMDInt date, @Nullable HMInt beginTime, @Nullable HMInt endTime, @NonNull LiveClock liveFlash) {
        super(context);

        this.date = date;
        this.beginTime = (beginTime == null || endTime == null) ? null : beginTime;
        this.endTime = (beginTime == null || endTime == null) ? null : endTime;
        this.liveFlash = MyApp.newLiveClockToYMDHM(liveFlash);
        observe(this.liveFlash, pairs-> update(pairs.f, pairs.s, pairs.t));
    }

    //region ライフサイクル
    //Lifecycle.Stateについて https://developer.android.com/topic/libraries/architecture/lifecycle#lc
    //Viewのライフサイクル判定 参考 https://stackoverflow.com/questions/22368720/can-a-custom-view-know-that-onpause-has-been-called
    private final @NonNull LifecycleRegistry registry = new LifecycleRegistry(this);
    {registry.setCurrentState(Lifecycle.State.CREATED);}

    //noinspection MismatchedQueryAndUpdateOfCollection LiveData自体を誰かが参照しないと消えると思う
    private final @NonNull Set<LiveData<?>> strongReferences = new HashSet<>();

    /** 直接{@link LiveData#observe}をコールしてよいが、もしLiveDataの参照を誰も保持しないようならこのメソッドを使うことで、LiveDataの揮発を防げる。 */
    public <T> void observe(@NonNull LiveData<T> liveData, @NonNull Observer<? super T> observer){
        synchronized (strongReferences) {
            strongReferences.add(liveData);
            liveData.observe(this, observer);
        }
    }
    /** {@link #observe}をコールすると同時に、observerを強制コール */
    public <T> void observeAndCall(@NonNull LiveData<T> liveData, @NonNull Observer<? super T> observer){
        synchronized (strongReferences) {
            strongReferences.add(liveData);
            LiveUtil.observeAndCall(this, liveData, observer);
        }
    }
    public <T> void removeObserver(@NonNull LiveData<T> liveData, @NonNull Observer<? super T> observer){
        synchronized (strongReferences) {
            strongReferences.remove(liveData);
            liveData.removeObserver(observer);
        }
    }
    public void removeObservers(@NonNull LiveData<?> liveData){
        synchronized (strongReferences) {
            strongReferences.clear();
            liveData.removeObservers(this);
        }
    }
    /** {@link #observe}を使用した場合にのみ有効に機能する */
    public void removeObservers(){
        synchronized (strongReferences) {
            for (@NonNull LiveData<?> liveData : strongReferences) {
                liveData.removeObservers(this);
            }
            strongReferences.clear();
        }
    }

    @Override
    public @NonNull Lifecycle getLifecycle() {
        return registry;
    }
    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE) { //onResume called
            registry.setCurrentState(Lifecycle.State.RESUMED);
        } else { // onPause() called
            registry.setCurrentState(Lifecycle.State.STARTED);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) { //onResume() called
            registry.setCurrentState(Lifecycle.State.RESUMED);
        } else { // onPause() called
            registry.setCurrentState(Lifecycle.State.STARTED);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        registry.setCurrentState(Lifecycle.State.STARTED);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        registry.setCurrentState(Lifecycle.State.CREATED);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        registry.setCurrentState(Lifecycle.State.CREATED);
        return super.onSaveInstanceState();
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        registry.setCurrentState(Lifecycle.State.STARTED);
    }

    //endregion

    /** Viewが表示されている時、およそ1秒毎に実行される */
    public void update(@NonNull YMDInt nowYMD, @NonNull HMInt nowHM, int sec){
        final boolean flash = (this.date==null || this.date.equals(nowYMD)) && //nullもしくは当日 かつ
                beginTime!=null && endTime!=null && nowHM.isBetween(beginTime, endTime) && //null以外で時間内 かつ
                sec%2==0; //点滅タイミング
        for(@NonNull View v: For.iterable(this))
            v.setVisibility(flash ? INVISIBLE : VISIBLE);
    }

    /** ヘッダー用のテキストを設定する */
    public void setTextHeader(){
        labelShip.setText(R.string.label_dtt_ship);
        labelDeparturePort.setText(R.string.label_dtt_dep_port);
        labelDepartureTime.setText(R.string.label_dtt_dep_time);
        labelKeiyu.setText(R.string.label_dtt_keiyu);
        labelArriveTime.setText(R.string.label_dtt_arr_time);
        labelArrivePort.setText(R.string.label_dtt_arr_port);
    }

    /** テキストを一括設定 */
    public void setTextAll(@NonNull String ship, @NonNull String depPort, @NonNull String depTime,
                           @NonNull String keiyu, @NonNull String arrTime, @NonNull String arrPort){
        labelShip.setText(ship);
        labelDeparturePort.setText(depPort);
        labelDepartureTime.setText(depTime);
        labelKeiyu.setText(keiyu);
        labelArriveTime.setText(arrTime);
        labelArrivePort.setText(arrPort);
    }

    /** テキストサイズを一括設定 */
    public void setTextSizePx(float fontSizePx){
        labelShip.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizePx);
        labelDeparturePort.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizePx);
        labelDepartureTime.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizePx);
        labelKeiyu.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizePx*.7f);
        labelArriveTime.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizePx);
        labelArrivePort.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizePx);
    }

    /** テキストカラーを一括設定 */
    public void setTextColor(int color){
        labelShip.setTextColor(color);
        labelDeparturePort.setTextColor(color);
        labelDepartureTime.setTextColor(color);
        labelKeiyu.setTextColor(color);
        labelArriveTime.setTextColor(color);
        labelArrivePort.setTextColor(color);
    }

    /** テキストサイズを一括設定 */
    public void setTextGravity(int gravity){
        ((LayoutParams)labelShip.getLayoutParams()).gravity = gravity;
        ((LayoutParams)labelDeparturePort.getLayoutParams()).gravity = gravity;
        ((LayoutParams)labelDepartureTime.getLayoutParams()).gravity = gravity;
        ((LayoutParams)labelKeiyu.getLayoutParams()).gravity = gravity;
        ((LayoutParams)labelArriveTime.getLayoutParams()).gravity = gravity;
        ((LayoutParams)labelArrivePort.getLayoutParams()).gravity = gravity;
    }

    /** テキストサイズを一括設定 */
    public void setTextPadding(int left, int top, int right, int bottom){
        labelShip.setPadding(left, top, right, bottom);
        labelDeparturePort.setPadding(left, top, right, bottom);
        labelDepartureTime.setPadding(left, top, right, bottom);
        labelKeiyu.setPadding(left, top, right, bottom);
        labelArriveTime.setPadding(left, top, right, bottom);
        labelArrivePort.setPadding(left, top, right, bottom);
    }

    //region 太字処理
    /** 太字フラグ */
    private boolean isBold = false;

    public void setTextReverseBold(){
        setTextBold(!isBold);
    }

    private void setTextBold(boolean isBold){
        this.isBold = isBold;
        labelShip.setTypeface(isBold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        labelDeparturePort.setTypeface(isBold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        labelDepartureTime.setTypeface(isBold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        labelKeiyu.setTypeface(isBold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        labelArriveTime.setTypeface(isBold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        labelArrivePort.setTypeface(isBold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
    }
    //endregion

    //region 強調処理
    /** 強調フラグ */
    private boolean isStrong = false;
    private int labelShipSavedColor = 0;
    private int labelDeparturePortSavedColor = 0;
    private int labelDepartureTimeSavedColor = 0;
    private int labelKeiyuSavedColor = 0;
    private int labelArriveTimeSavedColor = 0;
    private int labelArrivePortSavedColor = 0;

    public void setReverseStrong(int strongColor){
        setStrong(!isStrong, strongColor);
    }

    /** 色設定を保存し必要があればstrongColorに変更する */
    public void setStrong(boolean isStrong, int strongColor){
        if(!this.isStrong){
            //元の色を保存する
            labelShipSavedColor = labelShip.getCurrentTextColor();
            labelDeparturePortSavedColor = labelDeparturePort.getCurrentTextColor();
            labelDepartureTimeSavedColor = labelDepartureTime.getCurrentTextColor();
            labelKeiyuSavedColor = labelKeiyu.getCurrentTextColor();
            labelArriveTimeSavedColor = labelArriveTime.getCurrentTextColor();
            labelArrivePortSavedColor = labelArrivePort.getCurrentTextColor();
        }
        this.isStrong = isStrong;
        labelShip.setTextColor(isStrong ? strongColor : labelShipSavedColor);
        labelDeparturePort.setTextColor(isStrong ? strongColor : labelDeparturePortSavedColor);
        labelDepartureTime.setTextColor(isStrong ? strongColor : labelDepartureTimeSavedColor);
        labelKeiyu.setTextColor(isStrong ? strongColor : labelKeiyuSavedColor);
        labelArriveTime.setTextColor(isStrong ? strongColor : labelArriveTimeSavedColor);
        labelArrivePort.setTextColor(isStrong ? strongColor : labelArrivePortSavedColor);
    }
    //endregion

    public @NonNull TextView labelShip(){
        return labelShip;
    }
    public @NonNull TextView labelDeparturePort(){
        return labelDeparturePort;
    }
    public @NonNull TextView labelDepartureTime(){
        return labelDepartureTime;
    }
    public @NonNull TextView labelKeiyu(){
        return labelKeiyu;
    }
    public @NonNull TextView labelArriveTime(){
        return labelArriveTime;
    }
    public @NonNull TextView labelArrivePort(){
        return labelArrivePort;
    }

}
