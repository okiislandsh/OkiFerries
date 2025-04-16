package jp.okiislandsh.oki.schedule.ui.tableall;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public enum TIMETABLE implements Parcelable {
    HONSHU_OKI, BON, WINTER,
    /** DATEの時、日付が利用可能 */
    DATE,
    RAINBOW, DOZEN, ISOKAZE,
    EX_FILTER;

    public int toSerialize() {
        switch (this) {
            default:
            case HONSHU_OKI:
                return 1;
            case BON:
                return 2;
            case WINTER:
                return 3;
            case DATE:
                return 4;
            case RAINBOW:
                return 5;
            case DOZEN:
                return 6;
            case ISOKAZE:
                return 7;
            case EX_FILTER:
                return 8;
        }
    }

    public static @NonNull TIMETABLE parse(int serialValue) {
        switch (serialValue) {
            default:
            case 1:
                return HONSHU_OKI;
            case 2:
                return BON;
            case 3:
                return WINTER;
            case 4:
                return DATE;
            case 5:
                return RAINBOW;
            case 6:
                return DOZEN;
            case 7:
                return ISOKAZE;
            case 8:
                return EX_FILTER;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(toSerialize());
    }

    public static final Creator<TIMETABLE> CREATOR = new Creator<TIMETABLE>() {
        @Override
        public TIMETABLE createFromParcel(Parcel source) {
            return parse(source.readInt());
        }

        @Override
        public TIMETABLE[] newArray(int size) {
            return new TIMETABLE[size];
        }
    };

}
