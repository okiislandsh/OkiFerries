package jp.okiislandsh.oki.schedule.ui.tableall;

import static jp.okiislandsh.library.android.MyUtil.BR;
import static jp.okiislandsh.library.core.Function.with;
import static jp.okiislandsh.library.core.MathUtil.toPrimitiveArrayNonNull;
import static jp.okiislandsh.library.core.MyUtil.isJa;
import static jp.okiislandsh.library.core.MyUtil.requireNonNull;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jp.okiislandsh.library.android.AbsBaseFragment;
import jp.okiislandsh.library.android.For;
import jp.okiislandsh.library.android.LogDB;
import jp.okiislandsh.library.android.MyUtil;
import jp.okiislandsh.library.android.SizeUtil;
import jp.okiislandsh.library.android.drawable.BorderDrawable;
import jp.okiislandsh.library.android.live.LiveDataTask;
import jp.okiislandsh.library.android.view.live.MultiStateImageButton;
import jp.okiislandsh.library.core.MathUtil;
import jp.okiislandsh.library.core.YMDInt;
import jp.okiislandsh.library.core.choice.AtomicChoice;
import jp.okiislandsh.oki.schedule.MyApp;
import jp.okiislandsh.oki.schedule.R;
import jp.okiislandsh.oki.schedule.databinding.FragmentTableAllBinding;
import jp.okiislandsh.oki.schedule.util.MY_COLORS;
import jp.okiislandsh.oki.schedule.util.P;
import jp.okiislandsh.oki.schedule.util.RandomDrawable;
import jp.okiislandsh.oki.schedule.util.SHIP;
import jp.okiislandsh.oki.schedule.util.TimeTableData;
import jp.okiislandsh.oki.schedule.util.TimeTableDownloadTaskManager;

public class TableAllFragment extends AbsBaseFragment implements Observer<TIMETABLE> {

    /** 非同期で構築される時刻表データ、MyAppで生成処理などタスク管理 */
    private final @NonNull LiveData<TimeTableDownloadTaskManager.TimeTableTuple> liveTimeTableTuple = MyApp.newLiveTimeTableData();
    private @Nullable TimeTableData liveTTData_getValue(){
        final @Nullable TimeTableDownloadTaskManager.TimeTableTuple tuple = liveTimeTableTuple.getValue();
        return tuple==null ? null : tuple.ttData();
    }

    private final @NonNull LiveDataTask<AtomicChoice.NonNull<TimeTableFindParam, TimeTableFindShipAndYearParam>, TimeTableFindResult> liveDataTask = new LiveDataTask<AtomicChoice.NonNull<TimeTableFindParam, TimeTableFindShipAndYearParam>, TimeTableFindResult>() {
        @Override
        protected TimeTableFindResult run(@Nullable AtomicChoice.NonNull<TimeTableFindParam, TimeTableFindShipAndYearParam> choice, @NonNull AtomicBoolean cancel) throws Exception{
            //読込中メッセージ
            postValue(TimeTableFindResult.newLoadingInstance());
            //検索
            if(choice==null) throw new RuntimeException("Type is null");
            switch (choice.getType()){
                case A: return new TimeTableFindResult(find(liveTTData_getValue(), Objects.requireNonNull(choice.getA())), null);
                case B: return new TimeTableFindResult(find(liveTTData_getValue(), Objects.requireNonNull(choice.getB())), null);
                default: throw new RuntimeException("Unknown type="+choice.getType());
            }
        }
        @Override
        protected void onTaskError(@NonNull Exception e) {
            super.onTaskError(e);
            showToastL(isJa("時刻表 抽出処理でエラーが発生しました。", "Error at find time-table."), e);
        }
    };

    /** パラメータ */
    private static class TimeTableFindParam { //配列を子に持つのでPairsを継承しない
        public final @Nullable String[] shipArray;
        public final @Nullable String[] infoArray;
        public final @Nullable YMDInt date1;
        public final @Nullable YMDInt date2;
        public final boolean rinji;
        /**
         * 抽出はandで行われる
         *
         * @param shipArray 抽出したい船名リスト ※完全一致
         * @param infoArray 抽出したいinfo文字列 ※Like
         * @param date1     抽出したい基準日
         * @param date2     date1と両方指定された場合、date1～date2の範囲で抽出する
         * @param rinji     臨時ダイヤ
         */
        public TimeTableFindParam(@Nullable String[] shipArray, @Nullable String[] infoArray, @Nullable YMDInt date1, @Nullable YMDInt date2, boolean rinji) {
            this.shipArray = shipArray;
            this.infoArray = infoArray;
            this.date1 = date1;
            this.date2 = date2;
            this.rinji = rinji;
        }
        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof TimeTableFindParam &&
                    Arrays.equals(shipArray, ((TimeTableFindParam) obj).shipArray) &&
                    Arrays.equals(infoArray, ((TimeTableFindParam) obj).infoArray) &&
                    Objects.equals(date1, ((TimeTableFindParam) obj).date1) &&
                    Objects.equals(date2, ((TimeTableFindParam) obj).date2) &&
                    rinji==((TimeTableFindParam) obj).rinji;
        }
    }

    /** パラメータ */
    private static class TimeTableFindShipAndYearParam { //配列を子に持つのでPairsを継承しない
        public final @Nullable SHIP_AND_INFO[] shipAndInfoArray;
        public final @Nullable int[] yearArray;
        public final boolean rinji;
        /**
         * 抽出はandで行われる
         * "隠岐汽船フェリー yyyy通常"と"フェリーどうぜん yyyy通常"を正確に抽出するための専用処理
         *
         * @param shipAndInfoArray 抽出したい船名リスト ※完全一致
         * @param yearArray        年で抽出
         * @param rinji            臨時ダイヤ
         */
        public TimeTableFindShipAndYearParam(@Nullable SHIP_AND_INFO[] shipAndInfoArray, @Nullable int[] yearArray, boolean rinji) {
            this.shipAndInfoArray = shipAndInfoArray;
            this.yearArray = yearArray;
            this.rinji = rinji;
        }
        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof TimeTableFindShipAndYearParam &&
                    Arrays.equals(shipAndInfoArray, ((TimeTableFindShipAndYearParam) obj).shipAndInfoArray) &&
                    Arrays.equals(yearArray, ((TimeTableFindShipAndYearParam) obj).yearArray) &&
                    rinji==((TimeTableFindShipAndYearParam) obj).rinji;
        }
    }

    /** 結果セットまたはメッセージ */
    private static class TimeTableFindResult extends AtomicChoice.NonNull<TimeTableData, String>{
        public TimeTableFindResult(TimeTableData data, String message) {
            super(data, message);
        }
        public static @androidx.annotation.NonNull TimeTableFindResult newLoadingInstance(){
            return new TimeTableFindResult(null, isJa("読込中", "Loading..."));
        }
    }

    private static @NonNull TimeTableData find(@Nullable TimeTableData ttAll, @NonNull TimeTableFindParam param) throws Exception {
        return find(ttAll, param.shipArray, param.infoArray, param.date1, param.date2, param.rinji);
    }

    private static @NonNull TimeTableData find(@Nullable TimeTableData ttAll, @NonNull TimeTableFindShipAndYearParam param) throws Exception {
        return find(ttAll, param.shipAndInfoArray, param.yearArray, param.rinji);
    }

    /**
     * 抽出はandで行われる
     * @param shipArray 抽出したい船名リスト ※完全一致
     * @param infoArray 抽出したいinfo文字列 ※Like
     * @param date1 抽出したい基準日
     * @param date2 date1と両方指定された場合、date1～date2の範囲で抽出する
     * @param rinji 臨時タイヤ検索
     */
    private static @NonNull TimeTableData find(@Nullable TimeTableData ttAll, @Nullable String[] shipArray, @Nullable String[] infoArray, @Nullable YMDInt date1, @Nullable YMDInt date2, boolean rinji) throws Exception{

        final @NonNull TimeTableData ret = new TimeTableData();

        if(ttAll==null){
            Log.e("TimeTable#find() 時刻表データが構築されていない。");
            return ret;
        }

        if(date1==null && date2!=null){
            Log.e("TimeTable#find() プログラムミス", new Exception("date2だけ指定"));
            return ret;
        }

        for(TimeTableData.Parts parts:ttAll){
            //船名抽出
            if(shipArray!=null && 0<shipArray.length){
                boolean match = false;
                for(@NonNull String ship:shipArray){
                    if(ship.equals(parts.ship)){
                        match = true;
                        break;
                    }
                }
                if(!match){
                    continue;
                }
            }
            //臨時ダイヤ抽出
            if(rinji == parts.rinji.isEmpty()){ //!=除外条件：通常ダイヤなら臨時empty、臨時ダイヤなら臨時found
                continue;
            }
            //info抽出
            if(infoArray!=null && 0<infoArray.length){
                boolean match = false;
                for(@NonNull String info:infoArray){
                    if(parts.info.contains(info)){ //Like
                        match = true;
                        break;
                    }
                }
                if(!match){
                    continue;
                }
            }
            //日付抽出
            if(date1!=null){
                //spans検索
                boolean match = false;
                for(Pair<YMDInt, YMDInt> pair:parts.spans){
                    if (date2 == null) {
                        if (date1.isBetween(pair.first, pair.second)) {
                            match = true;
                            break;
                        }
                    } else {
                        if (YMDInt.isOverlap(date1, date2, pair.first, pair.second)) {
                            match = true;
                            break;
                        }
                    }
                }
                //days検索
                if(!match){
                    for(YMDInt ymdInt:parts.days){
                        if (date2 == null) {
                            if (date1.equals(ymdInt)) {
                                match = true;
                                break;
                            }
                        } else {
                            if (YMDInt.isBetween(date1, date2, ymdInt)) {
                                match = true;
                                break;
                            }
                        }
                    }
                }
                if(!match){
                    continue;
                }
            }
            Log.d("抽出該当："+parts.ship);
            ret.add(parts);
        }
        //要求された船名が存在しない場合ダミーデータを生成する
        if(shipArray!=null && 0<shipArray.length){
            for(@NonNull String ship: shipArray){
                if(ret.noContainsShip(ship)) {
                    ret.add(new TimeTableData.Parts(ship,
                            Collections.emptyList(),
                            Collections.emptyList(),
                            "",
                            "",
                            Collections.emptyList())
                    );
                }
            }
        }
        return ret;
    }

    /**
     * "隠岐汽船フェリー yyyy通常"と"フェリーどうぜん yyyy通常"を正確に抽出するための専用処理
     * @param shipAndInfoArray 抽出したい船名リスト ※完全一致
     * @param yearArray 年で抽出
     * @param rinji 臨時タイヤ検索
     */
    private static @NonNull TimeTableData find(@Nullable TimeTableData ttAll, @Nullable SHIP_AND_INFO[] shipAndInfoArray, @Nullable int[] yearArray, boolean rinji) throws Exception {

        final @NonNull TimeTableData ret = new TimeTableData();

        if(ttAll==null){
            Log.e("TimeTable#find() 時刻表データが構築されていない。");
            return ret;
        }

        if(shipAndInfoArray==null || shipAndInfoArray.length==0){
            Log.e("TimeTable#find() プログラムミス", new Exception("船情報が指定されていない"));
            return ret;
        }
        if(yearArray==null || yearArray.length==0){
            Log.e("TimeTable#find() プログラムミス", new Exception("年が指定されていない"));
            return ret;
        }

        final @NonNull Set<String> shipNameSet = new HashSet<>();
        //船名・ダイヤ(通常とか冬とか)ごとに各年抽出される
        //船情報ループ
        //  船名ループ
        //    infoループ
        for(@NonNull SHIP_AND_INFO s: shipAndInfoArray){
            final @NonNull String[] ships = s.getShips();
            //あとでダミーデータ作る用
            shipNameSet.addAll(Arrays.asList(ships));
            //infoKey抽出のための文字列を事前に作る
            final @NonNull String[] infoKeyArray = new String[yearArray.length];
            for (int i = 0; i < yearArray.length; i++) {
                infoKeyArray[i] = yearArray[i] + s.infoKey;
            }
            for(@NonNull String ship: ships) {
                //抽出開始
                for (TimeTableData.Parts parts : ttAll) {
                    //船名抽出
                    if (!ship.equals(parts.ship)) {
                        continue;
                    }
                    if(rinji == parts.rinji.isEmpty()){ //!=除外条件：通常ダイヤなら臨時empty、臨時ダイヤなら臨時found
                        continue;
                    }
                    //info抽出
                    boolean match = false;
                    for (@NonNull String infoKey : infoKeyArray) {
                        if (parts.info.contains(infoKey)) { //Like
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        continue;
                    }
                    Log.d("抽出該当：" + parts.ship);
                    ret.add(parts);
                }
            }
        }
        //要求された船名が存在しない場合ダミーデータを生成する
        if(!shipNameSet.isEmpty()){
            for(@NonNull String ship: shipNameSet){
                if(ret.noContainsShip(ship)) {
                    ret.add(new TimeTableData.Parts(ship,
                            Collections.emptyList(),
                            Collections.emptyList(),
                            "",
                            "",
                            Collections.emptyList())
                    );
                }
            }
        }
        return ret;
    }

    /**
     * アセット内の時刻表データに存在する、年のSet
     */
    private static @NonNull Set<Integer> getContainInfoYears(@Nullable TimeTableData ttAll) throws Exception {

        final @NonNull Set<Integer> yearSet = new HashSet<>();
        if(ttAll==null) return yearSet;

        for(TimeTableData.Parts parts: ttAll){
                /*for (Pair<YMDInt, YMDInt> span : parts.spans) {
                    yearSet.add(span.first.getYear());
                    yearSet.add(span.second.getYear());
                }
                for (YMDInt day: parts.days) {
                    yearSet.add(day.getYear());
                }*/
            try {
                //Infoから年を取り出す
                final @NonNull String strInfoYear = parts.info.substring(0, 4);
                final int infoYear = Integer.parseInt(strInfoYear);
                //正しく取得できなかった場合に備えテストする
                if (strInfoYear.equals(Integer.toString(infoYear))) {
                    yearSet.add(infoYear);
                } else {
                    Log.w("正常にinfoから年を取り出せなかった。 strInfoYear=" + strInfoYear + ", infoYear=" + infoYear);
                }
            }catch (Exception e){
                Log.e("正常にinfoから年を取り出せなかった。info="+parts.info, e);
            }
        }
        return yearSet;
    }

    private TableAllViewModel vm;
    private FragmentTableAllBinding bind;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        //ViewModelインスタンス化、owner引数=thisはFragmentActivityまたはFragmentを設定する
        vm = new ViewModelProvider(this).get(TableAllViewModel.class);

        bind = FragmentTableAllBinding.inflate(inflater, container, false);

        //ボタンクリックイベント
        bind.btnHonshuOki.setOnClickListener(v -> vm.postTimeTable(TIMETABLE.HONSHU_OKI));
        bind.btnBon.setOnClickListener(v->vm.postTimeTable(TIMETABLE.BON));
        bind.btnWinter.setOnClickListener(v->vm.postTimeTable(TIMETABLE.WINTER));
        bind.btnRainbow.setOnClickListener(v->vm.postTimeTable(TIMETABLE.RAINBOW));
        bind.btnDozen.setOnClickListener(v->vm.postTimeTable(TIMETABLE.DOZEN));
        bind.btnIsokaze.setOnClickListener(v->vm.postTimeTable(TIMETABLE.ISOKAZE));
        bind.btnDate.setOnClickListener(v->{
            vm.mDate.setValue(GregorianCalendar.getInstance(TimeZone.getTimeZone("Japan")));
            vm.postTimeTable(TIMETABLE.DATE);
        });
        if(bind.btnCalendar!=null) bind.btnCalendar.setOnClickListener(v -> showCalendarDialog()); //横向きレイアウトでのみ表示されるボタン
        if(bind.btnEx!=null) bind.btnEx.setOnClickListener(v -> showExDialog()); //横向きレイアウトでのみ表示されるボタン

        //ランダム背景
        bind.getRoot().setBackground(RandomDrawable.getRandomDrawable(getResources()));

        //強調表示設定オブザーバ
        vm.livePortsBool.observe(getViewLifecycleOwner(), serialValue -> {
            //既存時刻表をぐるぐる回して色再設定
            for(@NonNull View v: For.iterable(bind.ttContainer)){
                if(v instanceof TimeTableView){
                    ((TimeTableView) v).refreshColors();
                }
            }
        });

        //検索設定オブザーバ
        vm.mTimeTable.observe(getViewLifecycleOwner(), this);

        //時刻表
        liveDataTask.observe(getViewLifecycleOwner(), this::onChanged);

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
                //Exフィルタ
                with(menu.add(1, R.string.option_menu_ex_filter, 1, R.string.option_menu_ex_filter), item->{
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    item.setIcon(R.drawable.icon_ex_filter);
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
                }else if (itemId == R.string.option_menu_ex_filter) { //Exフィルタ
                    showExDialog();
                    return true;
                }else if (itemId == R.string.option_menu_font_plus) { //フォントサイズ +
                    if(P.setTimeTableFontSizePlus(requireContext())){
                        requireNonNull(liveDataTask.getValue(), v->onChanged(v)); //リポスト
                    }else{
                        showToastS(isJa("これ以上大きくできない", "Can't be bigger."));
                    }
                    return true;
                }else if (itemId == R.string.option_menu_font_minus) { //フォントサイズ -
                    if(P.setTimeTableFontSizeMinus(requireContext())){
                        requireNonNull(liveDataTask.getValue(), v->onChanged(v)); //リポスト
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
                        vm.postTimeTable(TIMETABLE.DATE); //抽出
                    } catch (Exception e) {
                        LogDB.getStringInstance().w("Calendar Dialog 通知エラー。 date="+year+" / "+month+" / "+dayOfMonth, e);
                    }
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show();

    }

    private void showExDialog(){

        //memo:DialogFragment方式にするのが大変
        // 値を返す系のダイアログなので、FragmentResultListenerが使えない
        // FragmentにListener実装方式ならいけるが、直接Dialog出した方が早い

        final @Nullable TimeTableData ttAll = liveTTData_getValue();

        if(ttAll==null) {
            showToastS(isJa("時刻表データが構築されていません。", "The timetable data has not been constructed."));
            return;
        }

        //Exダイアログ起動
        final @NonNull Set<Integer> yearSet;
        try {
            yearSet = getContainInfoYears(ttAll); //時間がかかる可能性があるため、ほんとは非同期にしないといけない
            if(yearSet.isEmpty()) throw new RuntimeException("YearSet is Empty.");
        } catch (Exception e) {
            showToastS(isJa("ダイアログ起動に失敗しました。年リストの取得に失敗。", "Failed to show dialog. Can not get the year list."), e);
            return;
        }

        final @NonNull int[] years = toPrimitiveArrayNonNull(yearSet.toArray(new Integer[0]));
        Arrays.sort(years);
        final @NonNull SHIP_AND_INFO[] ships = SHIP_AND_INFO.values();
        final @Nullable int[] selectedYears = vm.mYears.getValue();
        final @Nullable SHIP_AND_INFO[] selectedShips = vm.mShips.getValue();

        final @NonNull Set<Integer> newSelectedYears = new HashSet<>();
        final @NonNull MutableLiveData<Void> liveSelectedYearsNotify = new MutableLiveData<>(null);
        for (int year : selectedYears) {
            newSelectedYears.add(year);
        }
        final @NonNull Set<SHIP_AND_INFO> newSelectedShips = new HashSet<>();
        final @NonNull MutableLiveData<Void> liveSelectedShipsNotify = new MutableLiveData<>(null);
        Collections.addAll(newSelectedShips, selectedShips);
        final @NonNull AtomicBoolean selectedRinji = new AtomicBoolean(vm.mRinji.getValue());
        final @NonNull MutableLiveData<Void> liveSelectedRinjiNotify = new MutableLiveData<>(null);

        //View構築
        //  年チェックボックス && 船名チェックボックス
        //    横LinearLayout ※2分割
        //      年リスト
        //      船名リスト
        final int textSizePx = P.TT_TEXT_SIZE.get(requireContext()).px(requireContext());
        final @Nullable Drawable trueDrawable = getDrawable(jp.okiislandsh.library.android.R.drawable.ic_checkbox_on);
        if (trueDrawable != null) trueDrawable.setTint(Color.BLACK);
        final @Nullable Drawable falseDrawable = getDrawable(jp.okiislandsh.library.android.R.drawable.ic_checkbox_off);
        if (falseDrawable != null) falseDrawable.setTint(Color.BLACK);
        final @NonNull Drawable noticeDrawable = new BorderDrawable(Color.RED);
        final @NonNull LinearLayout contentView = newLinearLayout(LinearLayout.VERTICAL, newParamsM0(1f),
                newText(isJa("年と船情報をそれぞれ一つ以上選択してください。", "Please select one or more years and ship information."), newParamsMW()),
                newLinearLayout(LinearLayout.HORIZONTAL, newParamsM0(1f),
                        with(new ListView(requireContext()), list -> {
                            list.setLayoutParams(newParams0M(1f));
                            list.setAdapter(new BaseAdapter() {
                                @Override
                                public int getCount() {
                                    return years.length;
                                }

                                @Override
                                public Object getItem(int position) {
                                    return years[position];
                                }

                                @Override
                                public long getItemId(int position) {
                                    return years[position];
                                }

                                @Override
                                public @NonNull
                                View getView(int position, View convertView, ViewGroup parent) {
                                    final int year = years[position];
                                    final boolean selected = newSelectedYears.contains(year);
                                    if (convertView == null) {
                                        convertView = new MyView(requireContext(), year, selected);
                                    } else {
                                        ((MyView) convertView).reset(year, selected);
                                    }
                                    return convertView;
                                }

                                class MyView extends LinearLayout {
                                    int year;
                                    final @NonNull
                                    TextView label;
                                    final @NonNull
                                    MultiStateImageButton checkView;

                                    public MyView(Context context, int pYear, boolean initialSelected) {
                                        super(context);
                                        this.year = pYear;
                                        label = newText(Integer.toString(year), TableAllFragment.this.setGravity(newParams0W(1f), Gravity.CENTER_VERTICAL), textSizePx);
                                        checkView = new2StateImageButton(initialSelected,
                                                trueDrawable,
                                                falseDrawable,
                                                null, TableAllFragment.this.setGravity(newLinearParams(textSizePx, textSizePx, 0f), Gravity.CENTER_VERTICAL),
                                                aBoolean -> {
                                                    if (aBoolean) {
                                                        newSelectedYears.add(year);
                                                    } else {
                                                        newSelectedYears.remove(year);
                                                    }
                                                    if (newSelectedYears.isEmpty()) {
                                                        //選択を促す
                                                        list.setBackground(noticeDrawable);
                                                    } else {
                                                        list.setBackground(null);
                                                    }
                                                });
                                        label.setOnClickListener(v -> checkView.performClick()); //ラベルクリックでチェック反転
                                        checkView.observe(liveSelectedYearsNotify, //一括選択ボタンに反応して自身のチェック状態を変化させる
                                                unused -> checkView.state.set(newSelectedYears.contains(year) ? 1 : 0));
                                        setOrientation(HORIZONTAL);
                                        addView(checkView);
                                        addView(label);
                                    }

                                    public void reset(int pYear, boolean selected) {
                                        this.year = pYear;
                                        label.setText(String.format(Locale.US, "%d", year));
                                        checkView.state.set(selected ? 1 : 0);
                                    }
                                }
                            });
                        }),
                        newLinearLayout(LinearLayout.VERTICAL, newParams0M(3f),
                                newLinearLayout(LinearLayout.HORIZONTAL, newLinearParams(ViewGroup.LayoutParams.MATCH_PARENT, textSizePx, 0f),
                                        newTextDrawableButton(isJa("☑ 汽船F", "KisenF"), newParams0M(1f), false, v->{
                                            //リストの選択状況に影響を加えたい
                                            newSelectedShips.addAll(Arrays.asList(SHIP_AND_INFO.getKisenFerryList()));
                                            liveSelectedShipsNotify.postValue(null); //リストへ変更を通知
                                        }),
                                        newTextDrawableButton(isJa("☑ レインボー", "Rainbow"), newParams0M(1f), false, v->{
                                            //リストの選択状況に影響を加えたい
                                            newSelectedShips.addAll(Arrays.asList(SHIP_AND_INFO.getRainbowList()));
                                            liveSelectedShipsNotify.postValue(null); //リストへ変更を通知
                                        }),
                                        newTextDrawableButton(isJa("☑ Fどうぜん", "F.Dozen"), newParams0M(1f), false, v->{
                                            //リストの選択状況に影響を加えたい
                                            newSelectedShips.addAll(Arrays.asList(SHIP_AND_INFO.getDozenList()));
                                            liveSelectedShipsNotify.postValue(null); //リストへ変更を通知
                                        }),
                                        newTextDrawableButton(isJa("☑ いそかぜ", "Isokaze"), newParams0M(1f), false, v->{
                                            //リストの選択状況に影響を加えたい
                                            newSelectedShips.addAll(Arrays.asList(SHIP_AND_INFO.getIsokazeList()));
                                            liveSelectedShipsNotify.postValue(null); //リストへ変更を通知
                                        })
                                ),
                                with(new ListView(requireContext()), list -> {
                                    list.setLayoutParams(newParamsM0(1f));
                                    list.setAdapter(new BaseAdapter() {
                                        @Override
                                        public int getCount() {
                                            return ships.length;
                                        }

                                        @Override
                                        public Object getItem(int position) {
                                            return ships[position];
                                        }

                                        @Override
                                        public long getItemId(int position) {
                                            return ships[position].hashCode();
                                        }

                                        @Override
                                        public @NonNull
                                        View getView(int position, View convertView, ViewGroup parent) {
                                            final @NonNull SHIP_AND_INFO ship = ships[position];
                                            final boolean selected = newSelectedShips.contains(ship);
                                            if (convertView == null) {
                                                convertView = new MyView(requireContext(), ship, selected);
                                            } else {
                                                ((MyView) convertView).reset(ship, selected);
                                            }
                                            return convertView;
                                        }

                                        class MyView extends LinearLayout {
                                            @NonNull SHIP_AND_INFO ship;
                                            final @NonNull TextView label;
                                            final @NonNull MultiStateImageButton checkView;

                                            public MyView(Context context, @NonNull SHIP_AND_INFO pShip, boolean initialSelected) {
                                                super(context);
                                                this.ship = pShip;
                                                label = newText(ship.toString(), TableAllFragment.this.setGravity(newParams0W(1f), Gravity.CENTER_VERTICAL), textSizePx);
                                                checkView = new2StateImageButton(initialSelected,
                                                        trueDrawable, falseDrawable,
                                                        null, TableAllFragment.this.setGravity(newLinearParams(textSizePx, textSizePx, 0f), Gravity.CENTER_VERTICAL),
                                                        aBoolean -> {
                                                            if (aBoolean) {
                                                                newSelectedShips.add(ship);
                                                            } else {
                                                                newSelectedShips.remove(ship);
                                                            }
                                                            if (newSelectedShips.isEmpty()) {
                                                                //選択を促す
                                                                list.setBackground(noticeDrawable);
                                                            } else {
                                                                list.setBackground(null);
                                                            }
                                                        });
                                                label.setOnClickListener(v -> checkView.performClick()); //ラベルクリックでチェック反転
                                                checkView.observe(liveSelectedShipsNotify, //一括選択ボタンに反応して自身のチェック状態を変化させる
                                                        unused -> checkView.state.set(newSelectedShips.contains(ship) ? 1 : 0));
                                                setOrientation(HORIZONTAL);
                                                addView(checkView);
                                                addView(label);
                                            }

                                            public void reset(@NonNull SHIP_AND_INFO pShip, boolean selected) {
                                                this.ship = pShip;
                                                label.setText(ship.toString());
                                                checkView.state.set(selected ? 1 : 0);
                                            }
                                        }
                                    });
                                })
                        )
                ),
                newLinearLayout(LinearLayout.HORIZONTAL, newParamsMW(),
                        rinjiContainer->{
                            final @NonNull MultiStateImageButton checkButton = new2StateImageButton(selectedRinji.get(),
                                    trueDrawable, falseDrawable, null, setGravity(newLinearParams(textSizePx, textSizePx, 0f), Gravity.CENTER_VERTICAL),
                                    newValue -> selectedRinji.set(newValue!=null && newValue) );
                            checkButton.observe(liveSelectedRinjiNotify, //一括選択ボタンに反応して自身のチェック状態を変化させる
                                    unused -> checkButton.state.set(selectedRinji.get() ? 1 : 0));
                            final @NonNull TextView label = newText(isJa("臨時便", "Rinji"), setGravity(newParamsWW(0f), Gravity.CENTER_VERTICAL), textSizePx);
                            label.setOnClickListener(v->checkButton.performClick());
                            rinjiContainer.addView(checkButton);
                            rinjiContainer.addView(label);
                        }
                )
        );

        final @NonNull AtomicReference<Dialog> dialogRef = new AtomicReference<>();
        final @NonNull Runnable filterOnClick = ()->{
            //バリデータ
            final @NonNull int[] resultYears = toPrimitiveArrayNonNull(newSelectedYears.toArray(new Integer[0]));
            if(resultYears.length == 0){
                showToastS(isJa("１つ以上の年を選択してください。", "Please select a year."));
                return;
            }
            final @NonNull SHIP_AND_INFO[] resultShips = newSelectedShips.toArray(new SHIP_AND_INFO[0]);
            if(resultShips.length == 0){
                showToastS(isJa("１つ以上の船情報を選択してください。", "Please select a ship info."));
                return;
            }
            //値を保存する。次回ダイアログの初期値に設定される。
            vm.mYears.postValue(resultYears);
            vm.mShips.postValue(resultShips);
            vm.mRinji.postValue(selectedRinji.get());
            //抽出
            vm.postTimeTable(TIMETABLE.EX_FILTER);
            //終了
            dialogRef.get().dismiss();
        };
        dialogRef.set(newDialogXClose(newAbsoluteSizeSpan("Ex Filter", textSizePx), newAbsoluteSizeSpan(" x ", textSizePx),
                contentView,
                newLinearLayout(LinearLayout.HORIZONTAL, newParamsMW(),
                        with(newButton(newColorSpan("チェッククリア", getColorFromAttr(android.R.attr.colorPrimary), null),
                                newParamsWW(1f), v-> {
                                    //チェッククリア
                                    newSelectedYears.clear();
                                    liveSelectedYearsNotify.postValue(null);
                                    newSelectedShips.clear();
                                    liveSelectedShipsNotify.postValue(null);
                                    selectedRinji.set(false);
                                    liveSelectedRinjiNotify.postValue(null);
                                }), button->{
                            button.setAllCaps(false);
                            setPadding(button, 0);
                        }),
                        with(newButton(newColorSpan("Filter", getColorFromAttr(android.R.attr.colorPrimary), null),
                                newParamsWW(1f), v-> filterOnClick.run()), button->{
                            button.setAllCaps(false);
                            setPadding(button, 0);
                        })),
                jp.okiislandsh.library.android.R.style.Size90Dialog, Color.WHITE,
                (dialog, window, linearLayout, titleView, closeButton) -> {
                    titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    dialog.show();
                }
        ));
    }

    /** 検索設定オブザーバ */
    @Override
    public void onChanged(TIMETABLE timetable) {

        //クリア
        if(timetable!=TIMETABLE.DATE && timetable!=TIMETABLE.EX_FILTER) { //日付とExフィルタ以外なら
            bind.btnDate.setText(R.string.button_date_today); //ラベルを戻しとく
        }
        //ボタンリスト、一括設定のためリスト化
        final @NonNull List<Button> buttons = new ArrayList<>();
        buttons.add(bind.btnHonshuOki);
        buttons.add(bind.btnBon);
        buttons.add(bind.btnWinter);
        buttons.add(bind.btnDate);
        buttons.add(bind.btnRainbow);
        buttons.add(bind.btnDozen);
        buttons.add(bind.btnIsokaze);
        final @Nullable Button activeButton;
        //ボタンの色
        final int colorActiveBack = getResources().getColor(R.color.timeTableButtonActiveBack);
        final int colorActiveText = getResources().getColor(R.color.timeTableButtonActiveText);
        final int colorNormalBack = getResources().getColor(R.color.timeTableButtonBack);
        final int colorNormalText = getResources().getColor(R.color.timeTableButtonText);

        //機能に応じて抽出する
        final @NonNull Calendar now = GregorianCalendar.getInstance(TimeZone.getTimeZone("Japan"));
        final @NonNull YMDInt ymdInt = new YMDInt(now);

        //パラメータ決定、アクティブボタン決定
        final @Nullable AtomicChoice.NonNull<TimeTableFindParam, TimeTableFindShipAndYearParam> param;
        switch (timetable){
            case HONSHU_OKI:
                param = AtomicChoice.NonNull.newA(new TimeTableFindParam( //非同期検索処理
                        new String[]{SHIP.SHIRASHIMA.ship, SHIP.OKI.ship, SHIP.KUNIGA.ship},
                        new String[]{"通常"}, ymdInt.get0101OfYear(), ymdInt.get1231OfYear(),
                        false));
                activeButton = bind.btnHonshuOki;
                break;
            case BON:
                param = AtomicChoice.NonNull.newA(new TimeTableFindParam( //非同期検索処理
                        new String[]{SHIP.SHIRASHIMA.ship, SHIP.OKI.ship, SHIP.KUNIGA.ship},
                        new String[]{"盆"}, ymdInt.get0101OfYear(), ymdInt.get1231OfYear(),
                        false));
                activeButton = bind.btnBon;
                break;
            case WINTER:
                param = AtomicChoice.NonNull.newA(new TimeTableFindParam( //非同期検索処理
                        new String[]{SHIP.SHIRASHIMA.ship, SHIP.OKI.ship, SHIP.KUNIGA.ship},
                        new String[]{"厳冬", "初冬入渠"}, ymdInt.get0101OfYear(), ymdInt.get1231OfYear(), // 2024/10/4に臨時で初冬入渠ダイヤが追加された
                        false));
                activeButton = bind.btnWinter;
                break;
            case DATE:
                final @NonNull Calendar cal = vm.mDate.getValue();
                final @NonNull YMDInt findYMDDate = new YMDInt(cal);
                param = AtomicChoice.NonNull.newA(new TimeTableFindParam( //非同期検索処理
                        new String[]{SHIP.SHIRASHIMA.ship, SHIP.OKI.ship, SHIP.KUNIGA.ship, SHIP.RAINBOW.ship, SHIP.DOZEN.ship, SHIP.ISOKAZE.ship},
                        null, findYMDDate, null, //任意日の抽出
                        false));
                //ラベル表示
                if(TimeZone.getDefault().getRawOffset()==cal.getTimeZone().getRawOffset() && //タイムゾーンが日本以外の場合、時差で日付が変わることを考慮すれば常にYMD表示すべき
                        now.get(Calendar.YEAR)==cal.get(Calendar.YEAR) &&
                        now.get(Calendar.MONTH)==cal.get(Calendar.MONTH) &&
                        now.get(Calendar.DAY_OF_MONTH)==cal.get(Calendar.DAY_OF_MONTH)) {
                    //今日
                    bind.btnDate.setText(R.string.button_date_today);
                }else{ //任意日
                    final DateFormat dateFormat = isJa() ?
                            new SimpleDateFormat("yyyy年 M月d日", Locale.JAPANESE) :
                            new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH);
                    dateFormat.setTimeZone(cal.getTimeZone());
                    bind.btnDate.setText(dateFormat.format(cal.getTimeInMillis()));
                }
                activeButton = bind.btnDate;
                break;
            case RAINBOW:
                param = AtomicChoice.NonNull.newA(new TimeTableFindParam( //非同期検索処理
                        new String[]{SHIP.RAINBOW.ship},
                        null, ymdInt.get0101OfYear(), ymdInt.get1231OfYear(),
                        false));
                activeButton = bind.btnRainbow;
                break;
            case DOZEN:
                param = AtomicChoice.NonNull.newA(new TimeTableFindParam( //非同期検索処理
                        new String[]{SHIP.DOZEN.ship},
                        null, ymdInt.get0101OfYear(), ymdInt.get1231OfYear(),
                        false));
                activeButton = bind.btnDozen;
                break;
            case ISOKAZE:
                param = AtomicChoice.NonNull.newA(new TimeTableFindParam( //非同期検索処理
                        new String[]{SHIP.ISOKAZE.ship},
                        null, ymdInt.get0101OfYear(), ymdInt.get1231OfYear(),
                        false));
                activeButton = bind.btnIsokaze;
                break;
            case EX_FILTER:
                final @NonNull int[] years = vm.mYears.getValue();
                final @NonNull SHIP_AND_INFO[] shipAndInfoArray = vm.mShips.getValue();
                final boolean rinji = vm.mRinji.getValue();
                param = AtomicChoice.NonNull.newB(new TimeTableFindShipAndYearParam(shipAndInfoArray, years, rinji));
                //Exフィルタ適用中ラベル
                bind.btnDate.setText(isJa("Ex filter", "Exフィルタ"));
                activeButton = bind.btnDate;
                break;
            default:
                onChanged(new TimeTableFindResult(null, "Unknown "+timetable));
                param = null;
                activeButton = null;
                break; //ここへは来ない
        }

        if (!liveDataTask.parameterEquals(param)) { //検索条件が変わらないのにsetParamするとremoveAllViewのみコールされて何も表示されなくなる
            //クリア
            bind.ttContainer.removeAllViews();
            //抽出処理開始
            liveDataTask.setParam(param); //非同期
        }

        //アクティブボタン設定
        if(activeButton!=null) {
            buttons.remove(activeButton);
            activeButton.setBackgroundColor(colorActiveBack);
            activeButton.setTextColor(colorActiveText);
        }
        for(final @NonNull Button btn: buttons) {
            btn.setBackgroundColor(colorNormalBack);
            btn.setTextColor(colorNormalText);
        }
    }
    /** 検索結果オブザーバ */
    public void onChanged(@NonNull TimeTableFindResult result) {

        //クリア
        bind.ttContainer.removeAllViews();

        //結果展開
        result.switchCase(data-> {
            //抽出結果はリストなのでぐるぐる回す
            int timetableWidth = bind.ttContainer.getPaddingStart() + bind.ttContainer.getPaddingEnd(); //全ての時刻表の幅、加算していく
            int timetableHeaderHeightMax = 0;
            final @NonNull List<TimeTableView> ttViews = new ArrayList<>();
            Collections.sort(data); //なんかJSONObjectで並びが変わるみたい
            for (TimeTableData.Parts parts : data) {
                final @NonNull TimeTableView ttView = new TimeTableView(requireContext(), parts); //時刻表View
                //EXフィルタ時に抽出年を画面に強調表示する
                if (vm.mTimeTable.getValue() == TIMETABLE.EX_FILTER) {
                    final @NonNull String currentHeaderShipText = ttView.bind.headerShip.getText().toString();
                    if (!parts.info.isEmpty()) { //No TimeTable Foundの時はinfoが存在しない
                        //noinspection SetTextI18n
                        final @NonNull String newShipText = parts.info.substring(0, 4) + " " + currentHeaderShipText;
                        ttView.bind.headerShip.setText(newShipText);
                        final int textSize = P.getTimeTableFontSizePx(requireContext());
                        ttView.shipWidth = (int) MyUtil.measureText(textSize, newShipText) + textSize; //計算式はTimeTableViewより
                    }
                }
                final int width = MathUtil.max(ttView.shipWidth, ttView.metaWidth, ttView.portTimeWidth);
                timetableHeaderHeightMax = Math.max(timetableHeaderHeightMax, ttView.headerHeight);
                timetableWidth += width;
                ttView.setLayoutParams(new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
                ttView.setListener((ttParts) -> { //ヘッダークリック
                    final @Nullable SHIP ship = SHIP.of(ttParts.ship);
                    final @NonNull String toastText = (ship == null ?
                            ttParts.ship + BR :
                            isJa(ship.ship, ship.shipEn) + BR + ship.ferryAttributeBool.toDisplayString() + BR + BR
                    ) + ttParts.info;
                    Toast.makeText(requireContext(), toastText, Toast.LENGTH_LONG).show();
                });
                ttView.setListener((v, port, portTime) -> { //ラベルクリック
                    if (port == null) return;
                    switch (port) { //港ハイライトを反転する
                        case CHIBU: vm.getPortBoolPostReverseChibu(); break;
                        case AMA: vm.getPortBoolPostReverseAma(); break;
                        case NISHINOSHIMA: vm.getPortBoolPostReverseNishinoshima(); break;
                        case SHICHIRUI: vm.getPortBoolPostReverseShichirui(); break;
                        case SAKAIMINATO: vm.getPortBoolPostReverseSakaiminato(); break;
                        case DOGO: vm.getPortBoolPostReverseDogo(); break;
                    }
                });
                bind.ttContainer.addView(ttView); //表示
                ttViews.add(ttView); //後で高さ調整するためキャッシュ
            }
            //ヘッダー部の高さを全て同じにする
            for (TimeTableView ttView : ttViews) {
                ttView.bind.headerContainer.getLayoutParams().height = timetableHeaderHeightMax;
            }
            //スクロールセンタリング
            final int scrollWidth = bind.ttScrollView.getMeasuredWidth();
            if (0 < scrollWidth) { //起動直後は0が入る場合がある
                Log.d("時刻表抽出後のスクロール情報、scrollWidth" + scrollWidth + ", timetableWidth=" + timetableWidth);
                if (scrollWidth < timetableWidth) {
                    final int scrollXDP = (int) SizeUtil.px2dp((timetableWidth - scrollWidth) / 2f, requireContext());
                    //子のonMeasure前なのでScrollViewが認識しているスクロール幅が古い、そのため時間差で
                    new Handler().post(() -> {
                        bind.ttScrollView.smoothScrollTo(scrollXDP, 0); //スクロールする
                    });
                    bind.ttScrollView.setPadding(0, 0, 0, 0); //パディングをクリア
                } else {
                    bind.ttScrollView.setPadding((scrollWidth - timetableWidth) / 2, 0, 0, 0); //センターにくるようパディング変更
                }
            }

        }, message -> {
            final @NonNull MY_COLORS colors = P.getMyColors(requireContext());
            final int fontSizePx = P.getTimeTableFontSizePx(requireContext());
            final @NonNull TextView textView = newText(message, null, fontSizePx);
            textView.setTextColor(colors.color_text);
            textView.setBackgroundColor(colors.color_back);
            bind.ttContainer.addView(textView);
            bind.ttScrollView.setPadding(0, 0, 0, 0); //パディングをクリア
            new Handler().post(() -> {
                bind.ttScrollView.smoothScrollTo(0, 0); //スクロールする
            });
        });

    }

    @Override
    public void onDestroyView() {
        //ビューモデルのクリア、フラグメントの流儀
        vm = null;
        bind = null;

        super.onDestroyView();
    }

}