package jp.okiislandsh.oki.schedule.ui.tabledozen;

import static jp.okiislandsh.library.core.Function.with;
import static jp.okiislandsh.library.core.MyUtil.isJa;
import static jp.okiislandsh.library.core.MyUtil.requireNonNull;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import jp.okiislandsh.library.android.AbsBaseFragment;
import jp.okiislandsh.library.android.LogDB;
import jp.okiislandsh.library.android.MyUtil;
import jp.okiislandsh.library.android.live.LiveDataTask;
import jp.okiislandsh.library.android.live.NonNullLiveData;
import jp.okiislandsh.library.core.Pairs;
import jp.okiislandsh.library.core.YMDInt;
import jp.okiislandsh.library.core.choice.AtomicChoice;
import jp.okiislandsh.oki.schedule.MyApp;
import jp.okiislandsh.oki.schedule.R;
import jp.okiislandsh.oki.schedule.databinding.FragmentTableDozenBinding;
import jp.okiislandsh.oki.schedule.util.MY_COLORS;
import jp.okiislandsh.oki.schedule.util.P;
import jp.okiislandsh.oki.schedule.util.PORT_DOZEN;
import jp.okiislandsh.oki.schedule.util.RandomDrawable;
import jp.okiislandsh.oki.schedule.util.SHIP;
import jp.okiislandsh.oki.schedule.util.TimeTableData;

public class TableDozenFragment extends AbsBaseFragment {

    /** 非同期で構築される時刻表データ、MyAppで生成処理などタスク管理 */
    private final @NonNull LiveData<TimeTableData> liveNaikouMixData = MyApp.newLiveNaikouMixData();

    private final @NonNull LiveDataTask<TimeTableFindParam, TimeTableFindResult> liveDataTask = new LiveDataTask<TimeTableFindParam, TimeTableFindResult>(){
        @Override
        protected TimeTableFindResult run(@Nullable TimeTableFindParam param, @NonNull AtomicBoolean cancel) throws Exception {
            if(param==null) throw new RuntimeException("Type is null");
            //読込中メッセージ
            postValue(TimeTableFindResult.newLoadingInstance());
            @NonNull String depPort = param.depPort.toPort().port;
            @NonNull String arrPort = param.arrPort.toPort().port;
            //検索 いき、かえり
            return new TimeTableFindResult(
                    new IkiKaeri(
                            find(liveNaikouMixData.getValue(), depPort, arrPort, param.date, param.carOnly),
                            find(liveNaikouMixData.getValue(), arrPort, depPort, param.date, param.carOnly)
                    ), null
            );
        }
        @Override
        protected void onTaskError(@NonNull Exception e) {
            super.onTaskError(e);
            showToastL(isJa("時刻表 抽出処理でエラーが発生しました。", "Error at find time-table."), e);
        }
    };

    /** パラメータ */
    private static class TimeTableFindParam extends Pairs.Immutable.Nullable._4<PORT_DOZEN, PORT_DOZEN, YMDInt, Boolean> {
        public final @NonNull PORT_DOZEN depPort;
        public final @NonNull PORT_DOZEN arrPort;
        public final @NonNull YMDInt date;
        public final boolean carOnly;
        /**
         * @param depPort 出発港
         * @param arrPort 到着港
         * @param date    基準日
         * @param carOnly 車両縛り有無
         */
        public TimeTableFindParam(@NonNull PORT_DOZEN depPort, @NonNull PORT_DOZEN arrPort, @NonNull YMDInt date, boolean carOnly) {
            super(depPort, arrPort, date, carOnly);
            this.depPort = depPort;
            this.arrPort = arrPort;
            this.date = date;
            this.carOnly = carOnly;
        }
    }

    /** 結果セットまたはメッセージ */
    private static class TimeTableFindResult extends AtomicChoice.NonNull<IkiKaeri, String>{
        public TimeTableFindResult(IkiKaeri data, String message) {
            super(data, message);
        }
        public static @androidx.annotation.NonNull TimeTableFindResult newLoadingInstance(){
            return new TimeTableFindResult(null, isJa("読込中", "Loading..."));
        }
    }

    private static class IkiKaeri extends Pairs.Immutable.NonNull._2<MixTimeTableData, MixTimeTableData>{
        public final @NonNull MixTimeTableData iki;
        public final @NonNull MixTimeTableData kaeri;
        public IkiKaeri(@NonNull MixTimeTableData iki, @NonNull MixTimeTableData kaeri) {
            super(iki, kaeri);
            this.iki = iki;
            this.kaeri = kaeri;
        }
    }

    /**
     * 内航船ミックス時刻表検索
     * @param depPort 出発港
     * @param arrPort 到着港
     * @param date    基準日
     * @param carOnly 車両縛り有無
     */
    private static @NonNull MixTimeTableData find(@Nullable TimeTableData dttAll, @NonNull String depPort, @NonNull String arrPort, @NonNull YMDInt date, boolean carOnly) throws Exception {

        final @NonNull MixTimeTableData ret = new MixTimeTableData(date);

        if(dttAll==null) return ret;

        for(TimeTableData.Parts parts: dttAll){
            final @Nullable SHIP ship = SHIP.of(parts.ship);
            if(ship==null){
                Log.w("TimeTable#find() ship="+parts.ship+", 船名識別失敗");
                continue;
            }
            if(carOnly &&
                    (ship==SHIP.ISOKAZE || ship==SHIP.RAINBOW)){
                Log.d("TimeTable#find() can't put car. ship="+ship.name());
                continue;
            }
            //臨時ダイヤ除外 例えばいそかぜが知夫へいかなくてもどうぜんが知夫へいくかも。組み合わせが増えすぎて使えない。
            if(!parts.rinji.isEmpty()){
                Log.d("TimeTable#find() 臨時除外 rinji="+parts.rinji);
                continue;
            }
            //region 日付抽出
            //spans検索
            boolean match = false;
            for(Pair<YMDInt, YMDInt> pair:parts.spans){
                if(date.isBetween(pair.first, pair.second)){
                    match = true;
                    break;
                }
            }
            //days検索
            if(!match){
                for(YMDInt ymdInt:parts.days){
                    if(date.equals(ymdInt)){
                        match = true;
                        break;
                    }
                }
            }
            if(!match){
                Log.d("TimeTable#find() out of date. spans={"+
                        MyUtil.toStringOf(parts.spans, ", ", "null", (buf1, ymdIntYMDIntPair) -> buf1.append(ymdIntYMDIntPair.first).append("-").append(ymdIntYMDIntPair.second)) +
                        "} days={"+MyUtil.toStringOf(parts.days, ", ", "null", (buf1, ymdInt) -> buf1.append(ymdInt.toString())) +
                        "} 検索日="+date);
                continue;
            }
            //endregion
            //region 目的地へ着くデータか確認し、結果へ追加する
            for(int i = 1, oldI = -1; i<parts.portTimes.size(); i++){ //1件目は遡れないため無視、前回のiを保存する
                final @NonNull TimeTableData.PortTime pt1 = parts.portTimes.get(i);
                if(pt1.port.equals(arrPort) && pt1.arrive!=null){ //目的地に着いたので出発港が一致するか確認
                    final @NonNull ArrayList<String> keiyu = new ArrayList<>();
                    for(int j=i-1; j>oldI;j--){ //降順に出発港一致データを検索、前回のiより上に行く必要はない ※降順にサーチする場合かならず出発時刻が入っている
                        final @NonNull TimeTableData.PortTime pt2 = parts.portTimes.get(j);
                        if(pt2.port.equals(arrPort)){ //出発港が一致する前に元の港についてしまった… 目的地から目的地へ着いた
                            oldI = i;
                            break;
                        }else if(pt2.port.equals(depPort) && pt2.departure!=null){ //出発港が一致！！！
                            ret.add(new MixTimeTableData.Parts(
                                    parts.ship,
                                    depPort,
                                    pt2.departure,
                                    keiyu,
                                    pt1.port,
                                    pt1.arrive)
                            );
                            oldI = i;
                            break;
                        }
                        keiyu.add(pt2.port);
                    }
                }
            }
            //endregion
        }

        return ret;
    }

    private TableDozenViewModel vm;
    private FragmentTableDozenBinding bind;

    private NonNullLiveData<Integer> liveDeparture;
    private NonNullLiveData<Integer> liveArrive;
    private NonNullLiveData<Boolean> liveCarOnly;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        //ViewModelインスタンス化、owner引数=thisはFragmentActivityまたはFragmentを設定する
        vm = new ViewModelProvider(this).get(TableDozenViewModel.class);

        //設定値LiveData
        liveDeparture = P.PORTS_BOOL_DOZEN_DEPARTURE.getLive(requireContext());
        liveArrive = P.PORTS_BOOL_DOZEN_ARRIVE.getLive(requireContext());
        liveCarOnly = P.CAR_ONLY.getLive(requireContext());

        bind = FragmentTableDozenBinding.inflate(inflater, container, false);

        //ボタンクリックイベント
        bind.btnChibuDep.setOnClickListener(v -> P.setPortsBoolDozenDeparture(requireContext(), PORT_DOZEN.CHIBU));
        bind.btnAmaDep.setOnClickListener(v -> P.setPortsBoolDozenDeparture(requireContext(), PORT_DOZEN.AMA));
        bind.btnNishinoshimaDep.setOnClickListener(v -> P.setPortsBoolDozenDeparture(requireContext(), PORT_DOZEN.NISHINOSHIMA));
        bind.btnChibuArr.setOnClickListener(v -> P.setPortsBoolDozenArrive(requireContext(), PORT_DOZEN.CHIBU));
        bind.btnAmaArr.setOnClickListener(v -> P.setPortsBoolDozenArrive(requireContext(), PORT_DOZEN.AMA));
        bind.btnNishinoshimaArr.setOnClickListener(v -> P.setPortsBoolDozenArrive(requireContext(), PORT_DOZEN.NISHINOSHIMA));
        bind.btnCar.setOnClickListener(v -> P.setReverseCarOnly(requireContext()));
        bind.btnTitle.setOnClickListener(v-> vm.mDate.postValue(GregorianCalendar.getInstance(TimeZone.getTimeZone("Japan"))));
        if(bind.btnCalendar!=null) bind.btnCalendar.setOnClickListener(v->showCalendarDialog());

        //ランダム背景
        bind.dragAndPinchLayout.setBackground(RandomDrawable.getRandomDrawable(getResources()));

        //検索オブザーバ
        vm.mDate.observe(getViewLifecycleOwner(), this::onChangedLiveData);
        liveDeparture.observe(getViewLifecycleOwner(), dummy -> onChangedLiveData(vm.mDate.getValue()));
        liveArrive.observe(getViewLifecycleOwner(), dummy -> onChangedLiveData(vm.mDate.getValue()));
        liveCarOnly.observe(getViewLifecycleOwner(), dummy -> onChangedLiveData(vm.mDate.getValue()));

        //時刻表
        liveDataTask.observe(getViewLifecycleOwner(), this::onChangeTimeTable);

        //オプションメニュー
        // 不要 setHasOptionsMenu(true); オプションメニュー利用フラグ
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            }
            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                MenuProvider.super.onPrepareMenu(menu); //一応
                //日付変更
                with(menu.add(1, R.string.option_menu_change_date, 1, R.string.option_menu_change_date), item->{
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    item.setIcon(R.drawable.icon_rainbow_calendar);
                });
                //フォントサイズ + and -
                with(menu.add(1, R.string.option_menu_font_plus, 1, R.string.option_menu_font_plus), item->{
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                    item.setIcon(R.drawable.icon_font_plus);
                });
                with(menu.add(1, R.string.option_menu_font_minus, 1, R.string.option_menu_font_minus), item->{
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                    item.setIcon(R.drawable.icon_font_minus);
                });
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                final int itemId = menuItem.getItemId();
                if (itemId == R.string.option_menu_change_date) { //日付変更
                    showCalendarDialog();
                    return true;
                }else if (itemId == R.string.option_menu_font_plus) { //フォントサイズ +
                    if(P.setTimeTableFontSizePlus(requireContext())){
                        requireNonNull(liveDataTask.getValue(), v->onChangeTimeTable(v)); //リポスト
                    }else{
                        showToastS(isJa("これ以上大きくできない", "Can't be bigger."));
                    }
                    return true;
                }else if (itemId == R.string.option_menu_font_minus) { //フォントサイズ -
                    if(P.setTimeTableFontSizeMinus(requireContext())){
                        requireNonNull(liveDataTask.getValue(), v->onChangeTimeTable(v)); //リポスト
                    }else{
                        showToastS(isJa("これ以上小さくできない", "Can't be smaller."));
                    }
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED); //FragmentでaddMenuProviderする時の作法

        return bind.getRoot();

    }

    private void showCalendarDialog(){
        //カレンダーダイアログ起動
        final @NonNull Calendar cal = vm.mDate.getValue();
        new DatePickerDialog(requireActivity(),
                (view, year, month, dayOfMonth) -> {
                    try {
                        final @NonNull Calendar newCal = GregorianCalendar.getInstance(TimeZone.getTimeZone("Japan"));
                        newCal.set(year, month, dayOfMonth);
                        vm.mDate.postValue(newCal);
                    } catch (Exception e) {
                        LogDB.getStringInstance().w("Calendar Dialog 通知エラー。 date="+year+" / "+month+" / "+dayOfMonth, e);
                    }
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    /** 港選択状況変更時、検索処理 */
    private void onChangedLiveData(@NonNull Calendar cal) {

        //タイトル
        final @NonNull Calendar now = GregorianCalendar.getInstance(TimeZone.getTimeZone("Japan"));
        if(TimeZone.getDefault().getRawOffset()==cal.getTimeZone().getRawOffset() && //タイムゾーンが日本以外の場合、時差で日付が変わることを考慮すれば常にYMD表示すべき
                now.get(Calendar.YEAR)==cal.get(Calendar.YEAR) &&
                now.get(Calendar.MONTH)==cal.get(Calendar.MONTH) &&
                now.get(Calendar.DAY_OF_MONTH)==cal.get(Calendar.DAY_OF_MONTH)) {
            bind.btnTitle.setText(R.string.label_dtt_title);
        }else{
            DateFormat dateFormat = isJa() ?
                    new SimpleDateFormat("yyyy年 M月d日", Locale.JAPANESE) :
                    new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH);
            dateFormat.setTimeZone(cal.getTimeZone());
            bind.btnTitle.setText(dateFormat.format(cal.getTimeInMillis()));
        }

        //機能に応じて抽出し、時刻表Viewを生成
        final @NonNull YMDInt ymdInt = new YMDInt(cal);
        final @NonNull PORT_DOZEN depPortDozen = PORT_DOZEN.parse(liveDeparture.getValue());
        final @NonNull PORT_DOZEN arrPortDozen = PORT_DOZEN.parse(liveArrive.getValue());
        final boolean carOnly = liveCarOnly.getValue();

        final @NonNull TimeTableFindParam param = new TimeTableFindParam(depPortDozen, arrPortDozen, ymdInt, carOnly);
        if (!liveDataTask.parameterEquals(param)) { //検索条件が変わらないのにsetParamするとremoveAllViewのみコールされて何も表示されなくなる
            //クリア
            bind.dragAndPinchLayout.reset();
            bind.ttContainer.removeAllViews();
            //抽出処理開始
            liveDataTask.setParam(param); //非同期
        }

        //ボタンの色
        final int colorActiveBack = getResources().getColor(R.color.timeTableButtonActiveBack);
        final int colorActiveText = getResources().getColor(R.color.timeTableButtonActiveText);
        final int colorNormalBack = getResources().getColor(R.color.timeTableButtonBack);
        final int colorNormalText = getResources().getColor(R.color.timeTableButtonText);

        bind.btnChibuDep.setBackgroundColor(depPortDozen== PORT_DOZEN.CHIBU ? colorActiveBack : colorNormalBack);
        bind.btnChibuDep.setTextColor(depPortDozen== PORT_DOZEN.CHIBU ? colorActiveText : colorNormalText);
        bind.btnAmaDep.setBackgroundColor(depPortDozen== PORT_DOZEN.AMA ? colorActiveBack : colorNormalBack);
        bind.btnAmaDep.setTextColor(depPortDozen== PORT_DOZEN.AMA ? colorActiveText : colorNormalText);
        bind.btnNishinoshimaDep.setBackgroundColor(depPortDozen== PORT_DOZEN.NISHINOSHIMA ? colorActiveBack : colorNormalBack);
        bind.btnNishinoshimaDep.setTextColor(depPortDozen== PORT_DOZEN.NISHINOSHIMA ? colorActiveText : colorNormalText);

        bind.btnChibuArr.setBackgroundColor(arrPortDozen== PORT_DOZEN.CHIBU ? colorActiveBack : colorNormalBack);
        bind.btnChibuArr.setTextColor(arrPortDozen== PORT_DOZEN.CHIBU ? colorActiveText : colorNormalText);
        bind.btnAmaArr.setBackgroundColor(arrPortDozen== PORT_DOZEN.AMA ? colorActiveBack : colorNormalBack);
        bind.btnAmaArr.setTextColor(arrPortDozen== PORT_DOZEN.AMA ? colorActiveText : colorNormalText);
        bind.btnNishinoshimaArr.setBackgroundColor(arrPortDozen== PORT_DOZEN.NISHINOSHIMA ? colorActiveBack : colorNormalBack);
        bind.btnNishinoshimaArr.setTextColor(arrPortDozen== PORT_DOZEN.NISHINOSHIMA ? colorActiveText : colorNormalText);

        bind.btnCar.setBackgroundColor(carOnly ? colorActiveBack : colorNormalBack);
        bind.btnCar.setTextColor(carOnly ? colorActiveText : colorNormalText);

    }

    private void onChangeTimeTable(@NonNull TimeTableFindResult result){
        result.switchCase(this::onChangeTimeTable, this::onChangeTimeTable);
    }
    /** 検索中表示 */
    private void onChangeTimeTable(@NonNull String message){
        //クリア
        bind.dragAndPinchLayout.reset();
        bind.ttContainer.removeAllViews();
        //結果展開
        final @NonNull MY_COLORS colors = P.getMyColors(requireContext());
        final int fontSizePx = P.getTimeTableFontSizePx(requireContext());
        final @NonNull TextView textView = newText(message, null, fontSizePx);
        textView.setTextColor(colors.color_text);
        textView.setBackgroundColor(colors.color_back);
        bind.ttContainer.addView(textView);

    }
    /** 検索結果表示 */
    private void onChangeTimeTable(@NonNull IkiKaeri result){
        onChangeTimeTable(result.iki, result.kaeri);
    }
    /** 検索結果表示 */
    private void onChangeTimeTable(@NonNull MixTimeTableData iki, @NonNull MixTimeTableData kaeri){

        //0件の結果表示
        if(iki.list.isEmpty() && kaeri.list.isEmpty()) {
            onChangeTimeTable(isJa("抽出結果が0件です。", "No found result."));
            return;
        }

        //クリア
        bind.dragAndPinchLayout.reset();
        bind.ttContainer.removeAllViews();

        //行き
        final @NonNull DozenTimeTableView viewIki = new DozenTimeTableView(requireContext(), iki);
        //final @NonNull FrameLayout.LayoutParams ikiParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //ikiParams.bottomMargin = 30;
        viewIki.setPadding(20,0,0,100);
        bind.ttContainer.addView(viewIki);

        //帰り
        final @NonNull DozenTimeTableView viewKaeri = new DozenTimeTableView(requireContext(), kaeri);
        viewKaeri.setPadding(0,0,20,100);
        bind.ttContainer.addView(viewKaeri);

    }

    @Override
    public void onDestroyView() {
        vm = null;
        liveDeparture = null;
        liveArrive = null;
        liveCarOnly = null;
        bind = null;
        super.onDestroyView();
    }
}