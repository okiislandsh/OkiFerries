package jp.okiislandsh.oki.schedule.ui.tableall;

import static jp.okiislandsh.library.core.MyUtil.isJa;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import jp.okiislandsh.oki.schedule.BuildConfig;
import jp.okiislandsh.oki.schedule.util.SHIP;

/** Exフィルタ用の船名＆infoの抽出項目 */
public enum SHIP_AND_INFO implements Parcelable {
    KISEN_FERRIES_TSUJO("通常", "隠岐汽船フェリー 通常", "Oki-Ferries Normally."),
    KISEN_FERRIES_BON("盆", "隠岐汽船フェリー 盆", "Oki-Ferries Bon Fest."),
    KISEN_FERRIES_WINTER("厳冬", "隠岐汽船フェリー 厳冬、初冬入渠", "Oki-Ferries Winter and Early winter dock."),
    RAINBOW_NENDO_MATSU("年度末", "レインボージェット 年度末", "Rainbow-Jet End of the Fiscal Year (～3/31)."),
    RAINBOW_NENDO_HAJIME("年度初め", "レインボージェット 年度初め(～2024)", "Rainbow-Jet Beginning of the Fiscal Year (～2024)."),
    RAINBOW_SPRING_EARLY_SUMMER("春初夏", "レインボージェット 春初夏(2025～)", "Rainbow-Jet Spring to Early Summer (2025～)."),
    RAINBOW_SPRING_SUMMER("春夏", "レインボージェット 春夏(～2024)", "Rainbow-Jet Spring to Summer (～2024)."),
    RAINBOW_GW_SUMMER_VACATION("GW夏休み", "レインボージェット GW夏休み(2025～)", "Rainbow-Jet Golden-week holidays and Summer vacation (2025～)."),
    RAINBOW_AUTUMN("秋", "レインボージェット 秋", "Rainbow-Jet Autumn."),
    RAINBOW_WINTER("初冬", "レインボージェット 初冬", "Rainbow-Jet Early Winter."),
    DOZEN_NENMATSU_NENSHI("年末年始", "フェリーどうぜん 年末年始", "Ferry Dozen New Year's Holidays."),
    DOZEN_GANJITSU("元日", "フェリーどうぜん 元日", "Ferry Dozen New Year's Day (1/1)."),
    DOZEN_TSUJO("通常", "フェリーどうぜん 通常", "Ferry Dozen Normally."),
    ISOKAZE_NENMATSU_NENSHI("年末年始", "いそかぜ 年末年始", "Inter-Islands-Ferry Isokaze Year-End and New-Year Holidays."),
    ISOKAZE_TSUJO("通常", "いそかぜ 通常", "Inter-Islands-Ferry Isokaze Normally."),
    ISOKAZE_ICHIBU_HENKO("一部変更", "いそかぜ 一部変更", "Inter-Islands-Ferry Isokaze Partial Changes.");

    public static @NonNull SHIP_AND_INFO[] getKisenFerryList(){
        return new SHIP_AND_INFO[]{KISEN_FERRIES_TSUJO, KISEN_FERRIES_BON, KISEN_FERRIES_WINTER};
    }
    public static @NonNull SHIP_AND_INFO[] getRainbowList(){
        return new SHIP_AND_INFO[]{RAINBOW_NENDO_MATSU, RAINBOW_NENDO_HAJIME, RAINBOW_SPRING_SUMMER, RAINBOW_AUTUMN, RAINBOW_WINTER, RAINBOW_SPRING_EARLY_SUMMER, RAINBOW_GW_SUMMER_VACATION};
    }
    public static @NonNull SHIP_AND_INFO[] getDozenList(){
        return new SHIP_AND_INFO[]{DOZEN_NENMATSU_NENSHI, DOZEN_GANJITSU, DOZEN_TSUJO};
    }
    public static @NonNull SHIP_AND_INFO[] getIsokazeList(){
        return new SHIP_AND_INFO[]{ISOKAZE_NENMATSU_NENSHI, ISOKAZE_TSUJO, ISOKAZE_ICHIBU_HENKO};
    }

    public final @NonNull String name;
    public final @NonNull String nameEn;
    public final @NonNull String infoKey;

    SHIP_AND_INFO(@NonNull String infoKey, @NonNull String name, @NonNull String nameEn) {
        this.name = name;
        this.nameEn = nameEn;
        this.infoKey = infoKey;
    }

    public @NonNull String[] getShips() {
        switch (this) {
            default:
                if (BuildConfig.DEBUG) throw new RuntimeException("存在しないenum値");
                return new String[0];
            case KISEN_FERRIES_TSUJO:
            case KISEN_FERRIES_BON:
            case KISEN_FERRIES_WINTER:
                return new String[]{SHIP.SHIRASHIMA.ship, SHIP.OKI.ship, SHIP.KUNIGA.ship};
            case RAINBOW_NENDO_MATSU:
            case RAINBOW_NENDO_HAJIME:
            case RAINBOW_SPRING_EARLY_SUMMER:
            case RAINBOW_SPRING_SUMMER:
            case RAINBOW_GW_SUMMER_VACATION:
            case RAINBOW_AUTUMN:
            case RAINBOW_WINTER:
                return new String[]{SHIP.RAINBOW.ship};
            case DOZEN_NENMATSU_NENSHI:
            case DOZEN_GANJITSU:
            case DOZEN_TSUJO:
                return new String[]{SHIP.DOZEN.ship};
            case ISOKAZE_NENMATSU_NENSHI:
            case ISOKAZE_TSUJO:
            case ISOKAZE_ICHIBU_HENKO:
                return new String[]{SHIP.ISOKAZE.ship};
        }
    }

    /*public boolean containsShip(@NonNull String ship){
        switch (this){
            default:
                if(BuildConfig.DEBUG) throw new RuntimeException("存在しないenum値");
                return false;
            case KISEN_FERRIES_TSUJO:
            case KISEN_FERRIES_BON:
            case KISEN_FERRIES_WINTER:
                return matchAny(ship, SHIP.SHIRASHIMA.ship, SHIP.OKI.ship, SHIP.KUNIGA.ship);
            case RAINBOW_NENDO_MATSU:
            case RAINBOW_NENDO_HAJIME:
            case RAINBOW_SPRING_SUMMER:
            case RAINBOW_AUTUMN:
            case RAINBOW_WINTER:
                return SHIP.RAINBOW.ship.equals(ship);
            case DOZEN_NENMATSU_NENSHI:
            case DOZEN_GANJITSU:
            case DOZEN_TSUJO:
                return SHIP.DOZEN.ship.equals(ship);
            case ISOKAZE_NENMATSU_NENSHI:
            case ISOKAZE_TSUJO:
            case ISOKAZE_ICHIBU_HENKO:
                return SHIP.ISOKAZE.ship.equals(ship);
        }
    }*/

    public int toSerialize() {
        switch (this) {
            default:
            case KISEN_FERRIES_TSUJO:
                return 1;
            case KISEN_FERRIES_BON:
                return 2;
            case KISEN_FERRIES_WINTER:
                return 3;

            case RAINBOW_NENDO_MATSU:
                return 4;
            case RAINBOW_NENDO_HAJIME:
                return 5;
            case RAINBOW_SPRING_EARLY_SUMMER:
                return 6;
            case RAINBOW_SPRING_SUMMER:
                return 7;
            case RAINBOW_GW_SUMMER_VACATION:
                return 8;
            case RAINBOW_AUTUMN:
                return 9;
            case RAINBOW_WINTER:
                return 10;

            case DOZEN_NENMATSU_NENSHI:
                return 11;
            case DOZEN_GANJITSU:
                return 12;
            case DOZEN_TSUJO:
                return 13;

            case ISOKAZE_NENMATSU_NENSHI:
                return 14;
            case ISOKAZE_TSUJO:
                return 15;
            case ISOKAZE_ICHIBU_HENKO:
                return 16;
        }
    }

    public static @NonNull SHIP_AND_INFO parse(int serialValue) {
        switch (serialValue) {
            default:
            case 1:
                return KISEN_FERRIES_TSUJO;
            case 2:
                return KISEN_FERRIES_BON;
            case 3:
                return KISEN_FERRIES_WINTER;

            case 4:
                return RAINBOW_NENDO_MATSU;
            case 5:
                return RAINBOW_NENDO_HAJIME;
            case 6:
                return RAINBOW_SPRING_EARLY_SUMMER;
            case 7:
                return RAINBOW_SPRING_SUMMER;
            case 8:
                return RAINBOW_GW_SUMMER_VACATION;
            case 9:
                return RAINBOW_AUTUMN;
            case 10:
                return RAINBOW_WINTER;

            case 11:
                return DOZEN_NENMATSU_NENSHI;
            case 12:
                return DOZEN_GANJITSU;
            case 13:
                return DOZEN_TSUJO;

            case 14:
                return ISOKAZE_NENMATSU_NENSHI;
            case 15:
                return ISOKAZE_TSUJO;
            case 16:
                return ISOKAZE_ICHIBU_HENKO;
        }
    }

    /** ダイアログ呼び出し用 */
    @Override
    public @NonNull String toString() {
        return isJa(name, nameEn);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(toSerialize());
    }

    public static final Creator<SHIP_AND_INFO> CREATOR = new Creator<SHIP_AND_INFO>() {
        @Override
        public SHIP_AND_INFO createFromParcel(Parcel source) {
            return parse(source.readInt());
        }

        @Override
        public SHIP_AND_INFO[] newArray(int size) {
            return new SHIP_AND_INFO[size];
        }
    };

}
