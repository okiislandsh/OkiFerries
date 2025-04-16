package jp.okiislandsh.oki.schedule.ui.tabledozen;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Objects;

import jp.okiislandsh.library.core.HMInt;
import jp.okiislandsh.library.core.SortCollection;
import jp.okiislandsh.library.core.YMDInt;

/** 内航船ミックス時刻表データ(出発時刻ソート機能付き) */
public class MixTimeTableData extends SortCollection<MixTimeTableData.Parts, Object> {
    /** 検索日時 */
    final @NonNull YMDInt date;

    MixTimeTableData(@NonNull YMDInt date) {
        super(new Object(), (o, o1, o2) -> o1.depTime.intValue() <= o2.depTime.intValue());
        this.date = date;
    }

    /** ミックス時刻表専用Partsクラス */
    public static class Parts {
        /** 船名 */
        final @NonNull String ship;
        /** 出発港 */
        final @NonNull String depPort;
        /** 出発時刻 */
        final @NonNull HMInt depTime;
        /** 経由港 */
        final @NonNull ArrayList<String> keiyu;
        /** 到着港 */
        final @NonNull String arrPort;
        /** 到着時刻 */
        final @NonNull HMInt arrTime;

        Parts(@NonNull String ship, @NonNull String depPort, @NonNull HMInt depTime, @NonNull ArrayList<String> keiyu, @NonNull String arrPort, @NonNull HMInt arrTime) {
            this.ship = ship;
            this.depPort = depPort;
            this.depTime = depTime;
            this.keiyu = keiyu;
            this.arrPort = arrPort;
            this.arrTime = arrTime;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof Parts &&
                    Objects.equals(this.ship, ((Parts) obj).ship) &&
                    Objects.equals(this.depPort, ((Parts) obj).depPort) &&
                    Objects.equals(this.depTime, ((Parts) obj).depTime) &&
                    Objects.equals(this.keiyu, ((Parts) obj).keiyu) &&
                    Objects.equals(this.arrPort, ((Parts) obj).arrPort) &&
                    Objects.equals(this.arrTime, ((Parts) obj).arrTime);
        }

        @Override
        public int hashCode() {
            return ship.hashCode() | depTime.hashCode() | arrTime.hashCode();
        }
    }
}
