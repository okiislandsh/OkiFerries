package jp.okiislandsh.oki.schedule.ui.tabledozen;

import static jp.okiislandsh.library.core.MyUtil.isJa;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.TableLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.TimeZone;

import jp.okiislandsh.library.android.LogDB;
import jp.okiislandsh.library.android.live.LiveClock;
import jp.okiislandsh.library.core.HMInt;
import jp.okiislandsh.oki.schedule.databinding.DozenTimeTableViewBinding;
import jp.okiislandsh.oki.schedule.ui.guide.SimpleClickableOnTouchListener;
import jp.okiislandsh.oki.schedule.util.MY_COLORS;
import jp.okiislandsh.oki.schedule.util.P;
import jp.okiislandsh.oki.schedule.util.PORT;
import jp.okiislandsh.oki.schedule.util.SHIP;

@SuppressLint("ViewConstructor")
public class DozenTimeTableView extends TableLayout {

    protected static final @NonNull LogDB.ILog<CharSequence> Log = LogDB.getStringInstance();

    private final @NonNull Context context;
    /** 表示データ */
    private final @NonNull MixTimeTableData ttData;
    /** 色 */
    private @NonNull final MY_COLORS colors;

    /** 日本語化どうか */
    private final boolean isJa = isJa(); //あえてstaticにしない

    public final @NonNull DozenTimeTableViewBinding bind;

    /** ヘッダー部 */
    public DozenTimeTableRow headerRow;

    public DozenTimeTableView(@NonNull Context context, @NonNull MixTimeTableData ttData) {
        super(context);
        this.context = context;
        this.ttData = ttData; //データ

        //設定情報読み込み
        this.colors = P.getMyColors(context); //色

        //View生成
        bind = DozenTimeTableViewBinding.inflate(LayoutInflater.from(context), this, true);

        //構築
        init();

    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(){
        Log.d("init("+ttData.list.size()+"件)");

        //ヘッダー構築
        final int fontSizePx = P.getTimeTableFontSizePx(context);
        final int padLR = fontSizePx / 4;
        final int padTB = padLR /2;

        //ヘッダー構築
        bind.headerRow.setTextHeader();
        bind.headerRow.setBackgroundColor(colors.color_back);
        bind.headerRow.setTextColor(colors.color_text);
        bind.headerRow.setTextSizePx(fontSizePx);
        bind.headerRow.setTextGravity(Gravity.CENTER);
        bind.headerRow.setTextPadding(padLR, padTB, padLR, padTB);
        //行間
        ((LayoutParams)bind.headerRow.getLayoutParams()).bottomMargin = padTB;
        ((LayoutParams)bind.headerRow.getLayoutParams()).topMargin = padTB;

        final @NonNull LiveClock liveFlash = LiveClock.newInstance(TimeZone.getTimeZone("Japan"), LiveClock.TIME_SPAN.SECONDLY);

        //ボディ構築
        @Nullable HMInt oldHMInt = new HMInt(0); //次のデータで使われる点滅開始時刻
        for(int i=0; i<ttData.list.size(); i++){ //途中で前データを参照するためindexで回す
            final @NonNull MixTimeTableData.Parts parts = ttData.list.get(i);
            final @Nullable SHIP shipEnum = SHIP.of(parts.ship);
            final @Nullable PORT depPortEnum = PORT.of(parts.depPort);
            final @Nullable PORT arrPortEnum = PORT.of(parts.arrPort);
            final @Nullable HMInt begin = oldHMInt;
            final @Nullable HMInt end = parts.depTime;
            //辞書から引ければ翻訳
            final @NonNull String shipTrans = shipEnum==null? parts.ship : (isJa? shipEnum.shortName : shipEnum.shortNameEn);
            final @NonNull String depPortTrans = depPortEnum==null? parts.depPort : (isJa? depPortEnum.port : depPortEnum.portEn);
            final @NonNull String arrPortTrans = arrPortEnum==null? parts.arrPort : (isJa? arrPortEnum.port : arrPortEnum.portEn);
            final @Nullable ArrayList<String> keiyuTransList = new ArrayList<>();
            for(String tmp: parts.keiyu){
                final @Nullable PORT keiyuEnum = PORT.of(tmp);
                keiyuTransList.add(keiyuEnum==null? tmp : (isJa? keiyuEnum.port : keiyuEnum.portEn));
            }

            oldHMInt = parts.depTime.clone().addMinute(1); //出発時刻+1が次のデータの点滅開始時刻

            //カスタムTableRow
            final @NonNull DozenTimeTableRow tableRow = new DozenTimeTableRow(context, ttData.date, begin, end, liveFlash);
            tableRow.setTextAll(shipTrans,
                    depPortTrans, parts.depTime.toString(),
                    keiyuTransList.isEmpty() ? "" : keiyuTransList.toString(),
                    parts.arrTime.toString(), arrPortTrans);
            tableRow.setBackgroundColor(colors.color_back);
            tableRow.setTextColor(colors.color_text);
            tableRow.setTextSizePx(fontSizePx);
            tableRow.setTextGravity(Gravity.CENTER);
            tableRow.setTextPadding(padLR, padTB, padLR, padTB);
            if(i<ttData.list.size()-1 && ttData.list.get(i+1).arrTime.intValue() < parts.arrTime.intValue()) { //到着時刻が追い越されたら到着時刻を赤字にする
                //tableRow.setOvertaken();
                tableRow.labelArriveTime().setTextColor(colors.color_text_attention);
            }
            bind.dozenTimeTableView.addView(tableRow);
            //行間
            ((LayoutParams)tableRow.getLayoutParams()).bottomMargin = padTB;

            //クリック時の強調表示
            tableRow.setOnTouchListener(new SimpleClickableOnTouchListener() {
                public void onSimpleClick(){
                    tableRow.setReverseStrong(colors.color_text_dogo);
                }
            });/*
            tableRow.setOnClickListener(v -> {
                tableRow.setReverseStrong(colors.color_text_dogo);
                //tableRow.setTextReverseBold();
            });*/
        }

    }

}
