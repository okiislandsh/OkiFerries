package jp.okiislandsh.oki.schedule.ui.tableall;

import static jp.okiislandsh.library.android.MyUtil.BR;
import static jp.okiislandsh.library.android.MyUtil.requireNonNull;
import static jp.okiislandsh.library.core.Function.with;
import static jp.okiislandsh.library.core.MyUtil.isJa;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import jp.okiislandsh.library.android.For;
import jp.okiislandsh.library.android.LogDB;
import jp.okiislandsh.library.android.MyUtil;
import jp.okiislandsh.library.android.live.LiveClock;
import jp.okiislandsh.library.android.view.ViewBuilderFunction;
import jp.okiislandsh.library.core.HMInt;
import jp.okiislandsh.library.core.MathUtil;
import jp.okiislandsh.library.core.YMDInt;
import jp.okiislandsh.oki.schedule.R;
import jp.okiislandsh.oki.schedule.databinding.TimeTableViewBinding;
import jp.okiislandsh.oki.schedule.util.MY_COLORS;
import jp.okiislandsh.oki.schedule.util.P;
import jp.okiislandsh.oki.schedule.util.PORT;
import jp.okiislandsh.oki.schedule.util.PortsBool;
import jp.okiislandsh.oki.schedule.util.SHIP;
import jp.okiislandsh.oki.schedule.util.TimeTableData;

@SuppressLint("ViewConstructor") //プログラムから時刻表を設定しないと意味がないViewのため
public class TimeTableView extends LinearLayout implements ViewBuilderFunction.OnViewGroup {

    protected static final @NonNull LogDB.ILog<CharSequence> Log = LogDB.getStringInstance();

    private final @NonNull Context context;
    /** 表示データ */
    private final @NonNull TimeTableData.Parts ttParts;

    /** 日本語化どうか */
    private final boolean isJa = isJa(); //あえてstaticにしない

    /** 船名とメタ情報を表示するのに必要な高さ */
    public int headerHeight = 0;
    /** 船名を1行で表示するのに必要な幅 */
    public int shipWidth = 0;
    /** メタ情報1行の最大幅 */
    public int metaWidth = 0;
    /** 発着時刻を折り返さず表示するのに必要な全体幅 */
    public int portTimeWidth = 0;

    /** レイアウト */
    public final @NonNull TimeTableViewBinding bind;

    public TimeTableView(@NonNull Context context, @NonNull TimeTableData.Parts ttParts) {
        super(context);
        this.context = context;
        this.ttParts = ttParts; //データ

        //View生成
        bind = TimeTableViewBinding.inflate(LayoutInflater.from(context), this, true);

        //構築
        init();
    }

    private void init(){
        final @NonNull MY_COLORS colors = P.getMyColors(context); //色
        final @NonNull PortsBool portsBool = P.getPortsBool(context); //強調表示

        Log.d("start "+ttParts.ship+ttParts.info);

        //文字サイズ
        final int textSize = P.getTimeTableFontSizePx(context);
        //今何年？
        final String nowYearPrefix = (new Date().getYear()+1900)+"/"; //デフォルトロケールに準じる
        //ヘッダー構築
        bind.headerContainer.setBackgroundColor(colors.color_back);
        bind.headerContainer.setOnClickListener(v -> onHeaderClickListenerHelper.onHeaderClick(ttParts));
        //ヘッダータイトル
        final @Nullable SHIP shipEnum = SHIP.of(ttParts.ship);
        final @NonNull String shipTrans = shipEnum==null ? ttParts.ship : (isJa? shipEnum.ship : shipEnum.shipEn); //辞書から引ければ翻訳
        final @NonNull String shipText = ttParts.rinji.isEmpty() ? shipTrans : (ttParts.rinji + " " + shipTrans); //臨時便であることを時刻表表示に含める
        bind.headerShip.setText(shipText);
        bind.headerShip.setTextColor(colors.color_text);
        bind.headerShip.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        shipWidth = (int) MyUtil.measureText(textSize, shipText)+textSize; //実際の船名から計算、1文字多め
        //ヘッダーメタ
        bind.headerMeta.setTextColor(colors.color_text);
        bind.headerMeta.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize*.7f);
        final int metaLengthOfLine = 23; //1行の文字数 半角10+全角1+半角12 "yyyy/mm/dd-yyyy/mm/dd, "
        metaWidth = (int) MyUtil.measureText(bind.headerMeta.getTextSize(), "9999/99/99-9999/99/99, ");
        final @NonNull StringBuilder metaBuf = new StringBuilder();
        if(ttParts.portTimes.isEmpty()) {
            metaBuf.append(getResources().getString(R.string.error_no_data));
        }else{
            for(Pair<YMDInt, YMDInt> pair: ttParts.spans){
                if(0<metaBuf.length()){
                    metaBuf.append(", ");
                }
                metaBuf.append(pair.first.toString().replace(nowYearPrefix, "")).append("-").append(pair.second.toString().replace(nowYearPrefix, ""));
            }
            for(YMDInt ymdInt: ttParts.days){
                if(0<metaBuf.length()){
                    metaBuf.append(", ");
                }
                metaBuf.append(ymdInt.toString().replace(nowYearPrefix, ""));
            }
        }
        final @NonNull String metaString = metaBuf.toString();
        bind.headerMeta.setText(metaString);

        //タイトルの1行高さ
        final @NonNull Paint.FontMetrics shipFontMetrics = bind.headerShip.getPaint().getFontMetrics();
        final float shipLineHeight = (Math.abs(shipFontMetrics.top)) + (Math.abs(shipFontMetrics.descent));
        //メタの1行高さ
        final @NonNull Paint.FontMetrics metaFontMetrics = bind.headerMeta.getPaint().getFontMetrics();
        final float metaLineHeight = (Math.abs(metaFontMetrics.top)) + (Math.abs(metaFontMetrics.descent));
        //メタの行数
        final int metaLineCount = (int) Math.ceil((float)MyUtil.countCharSize(metaString) / metaLengthOfLine);
        //全部足してヘッダー高さ
        headerHeight = (int)(shipLineHeight + (metaLineHeight *  metaLineCount)); //ヘッダー行高さ + (行高さ * 切り上げ行数)
        //Log.d(shipLineHeight+"+("+metaLineHeight+"*"+metaLineCount+")="+headerHeight);

        final @NonNull LiveClock liveFlash = LiveClock.newInstance(TimeZone.getTimeZone("Japan"), LiveClock.TIME_SPAN.SECONDLY);

        //ボディ構築
        @Nullable HMInt oldHMInt = null;
        for(TimeTableData.PortTime portTime: ttParts.portTimes){
            final @Nullable PORT portEnum = PORT.of(portTime.port);
            final @Nullable HMInt begin = oldHMInt;
            final @Nullable HMInt end = (portTime.departure==null? portTime.arrive: portTime.departure);
            final @NonNull String portTrans = portEnum==null? portTime.port : (isJa? portEnum.port : portEnum.portEn); //辞書から引ければ翻訳
            final int textColor = (portEnum==null? colors.color_text : portEnum.getBooledPortColor(portsBool, colors)); //港名と強調表示Boolから色を決定
            oldHMInt = (portTime.departure==null ? null : portTime.departure.clone().addMinute(1)); //最後の便の時null。最後の便の場合ここで計算した値は使わない。

            //コンテナ
            with(new FlashLinearLayout(context, ttParts.spans, ttParts.days, begin, end, liveFlash), portTimeContainer->{
                portTimeContainer.setLayoutParams(setMarginBottom(newParamsMW(0f), 1));
                portTimeContainer.setBackgroundColor(colors.color_back);
                portTimeContainer.setOrientation(HORIZONTAL);
                portTimeContainer.setOnClickListener((v)->onPortClickListenerHelper.onPortClick(portTimeContainer, portEnum, portTime));
                bind.bodyContainer.addView(portTimeContainer);

                //時刻
                final @NonNull String arr;
                final @NonNull String dep;
                final @NonNull StringBuilder timeBuf = new StringBuilder();
                if(portTime.arrive!=null){
                    arr = arrAutoTrans(portTime.arrive);
                    timeBuf.append(arr);
                }else{
                    arr = "";
                }
                if(portTime.departure!=null){
                    if(0<timeBuf.length()){
                        timeBuf.append(BR);
                    }
                    dep = depAutoTrans(portTime.departure);
                    timeBuf.append(dep);
                }else{
                    dep = "";
                }

                with(portTimeContainer.addText(portTrans, with(newParams0W(1f), p->p.gravity=Gravity.CENTER_VERTICAL), textSize),
                        portTimeContainer.addText(timeBuf.toString(), with(newParams0W(1f), p->p.gravity=Gravity.CENTER_VERTICAL), textSize),
                        (portText, timeText)->{
                            //港
                            portText.setGravity(Gravity.RIGHT); //両方必要
                            portText.setTextColor(textColor);

                            //時刻
                            timeText.setGravity(Gravity.LEFT); //両方必要
                            timeText.setTextColor(textColor);

                            //Paintから描画サイズを計算する
                            final @NonNull Paint portPaint = portText.getPaint();
                            final @NonNull Paint timePaint = timeText.getPaint();

                            //港・時刻の全体幅の最大を更新
                            portTimeWidth = MathUtil.max(portTimeWidth, (int) portPaint.measureText(portTrans), (int) timePaint.measureText(arr), (int) timePaint.measureText(dep));

                        }
                );

            });

        }

        portTimeWidth = (int) (portTimeWidth*2.2f); //重要！ 計算前は港または時刻の幅が入っている。2倍で全体幅。
        //portTimeWidth *= 2; 適当に余裕を持たさないとうまくいかない。

    }

    public interface OnHeaderClickListener{
        void onHeaderClick(@NonNull TimeTableData.Parts parts);
    }
    public static class OnHeaderClickListenerHelper extends AtomicReference<OnHeaderClickListener> implements OnHeaderClickListener {
        @Override
        public void onHeaderClick(@NonNull TimeTableData.Parts parts) {
            synchronized (this){
                requireNonNull(get(), o->o.onHeaderClick(parts));
            }
        }
    }
    private final @NonNull OnHeaderClickListenerHelper onHeaderClickListenerHelper = new OnHeaderClickListenerHelper();
    public void setListener(@Nullable OnHeaderClickListener listener){
        onHeaderClickListenerHelper.set(listener);
    }


    public interface OnPortClickListener{
        void onPortClick(@NonNull FlashLinearLayout container, @Nullable PORT port, @NonNull TimeTableData.PortTime portTime);
    }
    public static class OnPortClickListenerHelper extends AtomicReference<OnPortClickListener> implements OnPortClickListener {
        @Override
        public void onPortClick(@NonNull FlashLinearLayout container, @Nullable PORT port, @NonNull TimeTableData.PortTime portTime) {
            synchronized (this){
                requireNonNull(get(), o->o.onPortClick(container, port, portTime));
            }
        }
    }
    private final @NonNull OnPortClickListenerHelper onPortClickListenerHelper = new OnPortClickListenerHelper();
    public void setListener(@Nullable OnPortClickListener listener){
        onPortClickListenerHelper.set(listener);
    }

    private @NonNull String arrAutoTrans(@NonNull HMInt hmInt){
        return hmInt+(isJa?"着":"ARR");
    }

    private @NonNull String depAutoTrans(@NonNull HMInt hmInt){
        return hmInt+(isJa?"発":"DEP");
    }

    /** 色設定をやり直す */
    public void refreshColors(){
        //設定再読み込み
        final @NonNull MY_COLORS colors = P.getMyColors(context); //色
        final @NonNull PortsBool portsBool = P.getPortsBool(context); //強調表示

        //ヘッダ色設定
        bind.headerContainer.setBackgroundColor(colors.color_back);
        bind.headerShip.setTextColor(colors.color_text);
        bind.headerMeta.setTextColor(colors.color_text);

        //子ビューをまわす
        for(@NonNull View portTimeContainer: For.iterable(bind.bodyContainer)){
            if(portTimeContainer instanceof FlashLinearLayout){ //一応型チェック
                //背景色設定
                portTimeContainer.setBackgroundColor(colors.color_back);

                //港名を調べる
                @Nullable PORT port = null;
                for(@NonNull View textView: For.iterable((FlashLinearLayout) portTimeContainer)){
                    if(textView instanceof TextView){ //一応型チェック
                        port = PORT.of(((TextView) textView).getText().toString());
                        if(port!=null){
                            break;
                        }
                    }
                }
                //港名から色を決定
                final int textColor = (port==null? colors.color_text : port.getBooledPortColor(portsBool, colors)); //港名と強調表示Boolから色を決定
                for(@NonNull View textView: For.iterable((FlashLinearLayout) portTimeContainer)){
                    if(textView instanceof TextView){
                        ((TextView) textView).setTextColor(textColor);
                    }
                }
            }
        }
    }

}
