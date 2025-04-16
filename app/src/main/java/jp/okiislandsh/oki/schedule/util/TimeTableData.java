package jp.okiislandsh.oki.schedule.util;

import static jp.okiislandsh.library.android.MyUtil.BR;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jp.okiislandsh.library.android.LogDB;
import jp.okiislandsh.library.core.HMInt;
import jp.okiislandsh.library.core.MyUtil;
import jp.okiislandsh.library.core.YMDInt;

/** "特定の船の特定のダイヤ"のリスト */
public class TimeTableData extends ArrayList<TimeTableData.Parts> {

    private static final @NonNull LogDB.ILog<CharSequence> Log = LogDB.getStringInstance();

    private static final @NonNull String  KEY_ROOT = "array";
    private static final @NonNull String  KEY_SHIP = "ship";
    private static final @NonNull String  KEY_SPANS = "spans";
    private static final @NonNull String  KEY_DAYS = "days";
    private static final @NonNull String  KEY_RINJI = "rinji";
    private static final @NonNull String  KEY_INFO = "info";
    private static final @NonNull String  KEY_DATA = "data";

    /** ダイヤのメタ情報と時刻表のセット */
    public static class Parts implements Comparable<Parts> {
        /** 船名 */
        public @NonNull final String ship;
        /** 時刻表の有効期間(from-to) */
        public @NonNull final List<Pair<YMDInt, YMDInt>> spans;
        /** 時刻表の有効期間(日指定) */
        public @NonNull final List<YMDInt> days;
        /** 臨時ダイヤ 2024年いそかぜ来居抜港の時刻表が作られた */
        public @NonNull final String rinji;
        /** 表示用の補助情報 */
        public @NonNull final String info;
        /** 時刻データ */
        public @NonNull final List<PortTime> portTimes;
        public Parts(@NonNull String ship, @NonNull List<Pair<YMDInt, YMDInt>> spans, @NonNull List<YMDInt> days, @NonNull String rinji, @NonNull String info, @NonNull List<PortTime> portTimes) {
            this.ship = ship;
            this.spans = spans;
            this.days = days;
            this.rinji = rinji;
            this.info = info;
            this.portTimes = portTimes;
        }
        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof Parts &&
                    Objects.equals(this.ship, ((Parts) obj).ship) &&
                    Objects.equals(this.spans, ((Parts) obj).spans) &&
                    Objects.equals(this.days, ((Parts) obj).days) &&
                    Objects.equals(this.info, ((Parts) obj).info) &&
                    //Objects.equals(this.info, ((Parts) obj).info) &&
                    Objects.equals(this.portTimes, ((Parts) obj).portTimes);
        }
        @Override
        public int hashCode() {
            return ship.hashCode() | spans.hashCode() | days.hashCode();
        }
        @Override
        public @NonNull String toString() {
            return ship +
                    MyUtil.toStringOf(spans, BR, "null", (buf1, ymdIntYMDIntPair) -> buf1.append(ymdIntYMDIntPair.first).append("-").append(ymdIntYMDIntPair.second)) +
                    MyUtil.toStringOf(days, BR, "null", StringBuilder::append) +
                    rinji + " " +
                    info +
                    MyUtil.toStringOf(portTimes, BR, "null", StringBuilder::append);
        }

        @Override
        public int compareTo(@NonNull Parts that) {
            try {
                final int shipCompare = Objects.requireNonNull(SHIP.of(ship), "of失敗。"+ship).compareTo(Objects.requireNonNull(SHIP.of(that.ship), "of失敗。"+ship));
                if (shipCompare != 0) return shipCompare;
            }catch (Exception e){ //ヌルぽがありうる
                Log.w("ヌルぽ？", e);
                final int shipCompare = ship.compareTo(that.ship);
                if(shipCompare!=0) return shipCompare;
            }
            {
                final int rinjiCompare = rinji.compareTo(that.rinji);
                if (rinjiCompare != 0) return rinjiCompare;
            }
            {
                //final int ymdCompare = YMDInt.compareTo(minYMDInt(this), minYMDInt(that));
                //if (ymdCompare != 0) return ymdCompare;
                return YMDInt.compareTo(minYMDInt(this), minYMDInt(that));
            }
            //return 0;
        }

        private static @Nullable YMDInt minYMDInt(@NonNull Parts parts){
            final @Nullable YMDInt minSpan = minSpan(parts.spans);
            final @Nullable YMDInt minDay = minDay(parts.days);
            return minSpan==null ? minDay : (
                    minDay==null ? minSpan : YMDInt.min(minDay, minSpan)
            );
        }
        private static @Nullable YMDInt minSpan(@NonNull List<Pair<YMDInt, YMDInt>> spans){
            @Nullable YMDInt min = null;
            for(@NonNull Pair<YMDInt, YMDInt> pair: spans){
                min = min == null ? pair.first : YMDInt.min(min, pair.first);
                min = min == null ? pair.second : YMDInt.min(min, pair.second);
            }
            return min;
        }
        private static @Nullable YMDInt minDay(@NonNull List<YMDInt> days){
            @Nullable YMDInt min = null;
            for(@NonNull YMDInt ymdInt: days){
                min = min == null ? ymdInt : YMDInt.min(min, ymdInt);
            }
            return min;
        }
    }

    /** 時刻データ(港、到着、出発) */
    public static class PortTime {
        public @NonNull final String port;
        /** 到着 */
        public @Nullable final HMInt arrive;
        /** 出発 */
        public @Nullable final HMInt departure;
        public PortTime(@NonNull String port, @NonNull String arrive, @NonNull String departure) {
            this.port = port;
            this.arrive = (arrive.isEmpty() ? null : new HMInt(arrive));
            this.departure = (departure.isEmpty() ? null : new HMInt(departure));
        }
        public PortTime(@NonNull String port) {
            this(port, (HMInt) null, (HMInt) null);
        }
        public PortTime(@NonNull String port, @Nullable HMInt arrive, @Nullable HMInt departure) {
            this.port = port;
            this.arrive = arrive;
            this.departure = departure;
        }
        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof PortTime &&
                port.equals(((PortTime) obj).port) &&
                    Objects.equals(arrive, ((PortTime) obj).arrive) &&
                    Objects.equals(departure, ((PortTime) obj).departure);
        }
        @Override
        public int hashCode() {
            return port.hashCode() | (arrive==null ? 0 : arrive.hashCode()) | (departure==null ? 0 : departure.hashCode());
        }
        @Override
        public @NonNull String toString() {
            return port + " " + arrive + " " + departure;
        }
    }

    public TimeTableData(){
        super();
    }
    public TimeTableData(@NonNull String yyyyAllJSONString){
        super();

        try {
            final @NonNull JSONObject json = new JSONObject(yyyyAllJSONString);
            final @NonNull JSONArray jsonArray = json.getJSONArray(KEY_ROOT);
            for(int i=0; i<jsonArray.length(); i++){
                final @NonNull JSONObject jsonParts = jsonArray.getJSONObject(i);
                try {
                    final @NonNull JSONArray jsonSpans = jsonParts.getJSONArray(KEY_SPANS);
                    final @NonNull List<Pair<YMDInt, YMDInt>> spans = new ArrayList<>();
                    for (int j = 0; j < jsonSpans.length(); j++) {
                        String[] split = jsonSpans.getString(j).split("-");
                        Pair<YMDInt, YMDInt> pair = new Pair<>(new YMDInt(split[0]), new YMDInt(split[1]));
                        spans.add(pair);
                    }
                    final @NonNull JSONArray jsonDays = jsonParts.getJSONArray(KEY_DAYS);
                    final @NonNull List<YMDInt> days = new ArrayList<>();
                    for (int j = 0; j < jsonDays.length(); j++) {
                        days.add(new YMDInt(jsonDays.getString(j)));
                    }
                    final @NonNull JSONArray jsonData = jsonParts.getJSONArray(KEY_DATA);
                    final @NonNull List<PortTime> data = new ArrayList<>();
                    for (int j = 0; j < jsonData.length(); j++) {
                        final @NonNull JSONArray jsonPortTime = jsonData.getJSONArray(j);
                        data.add(new PortTime(jsonPortTime.getString(0), jsonPortTime.getString(1), jsonPortTime.getString(2)));
                    }
                    add(new Parts(jsonParts.getString(KEY_SHIP), spans, days, jsonParts.optString(KEY_RINJI), jsonParts.getString(KEY_INFO), data));
                }catch(Exception e2){
                    Log.e("on error resume."+BR+jsonParts.toString(), e2);
                }
            }
        } catch (Exception e) {
            Log.e("JSON parse 致命的なエラー."+BR+yyyyAllJSONString, e);
        }
    }

    public boolean noContainsShip(@NonNull String ship){
        for(@NonNull Parts parts: this){
            if(ship.equals(parts.ship)){
                return false;
            }
        }
        return true;
    }

}
