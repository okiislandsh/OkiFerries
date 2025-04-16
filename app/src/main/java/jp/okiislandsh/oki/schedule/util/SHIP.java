package jp.okiislandsh.oki.schedule.util;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jp.okiislandsh.oki.schedule.ui.tableall.FerryAttributeBool;

public enum SHIP implements Parcelable {
    OKI("フェリーおき", "Ferry Oki", "Fおき", "F.Oki",
            new FerryAttributeBool(true, false, true, true, true, true)),
    KUNIGA("フェリーくにが", "Ferry Kuniga", "Fくにが", "F.Kuniga",
            new FerryAttributeBool(true, false, true, true, true, true)),
    SHIRASHIMA("フェリーしらしま", "Ferry Shirashima", "Fしらしま", "F.Shirashima",
            new FerryAttributeBool(true, false, true, true, true, true)),
    RAINBOW("レインボージェット", "Rainbow Jet", "レインボー", "Rainbow",
            new FerryAttributeBool(true, true, false, false, false, true)),
    DOZEN("フェリーどうぜん", "Ferry Dozen", "Fどうぜん", "F.Dozen",
            new FerryAttributeBool(true, false, false, true, false, true)),
    ISOKAZE("いそかぜ", "Isokaze", "いそかぜ", "Isokaze",
            new FerryAttributeBool(true, false, false, false, false, false)),
    ;
    public final @NonNull String ship;
    public final @NonNull String shipEn;
    public final @NonNull String shortName;
    public final @NonNull String shortNameEn;

    public final @NonNull FerryAttributeBool ferryAttributeBool;

    SHIP(@NonNull String ship, @NonNull String shipEn, @NonNull String shortName, @NonNull String shortNameEn, @NonNull FerryAttributeBool ferryAttributeBool){
        this.ship = ship;
        this.shipEn = shipEn;
        this.shortName = shortName;
        this.shortNameEn = shortNameEn;
        this.ferryAttributeBool = ferryAttributeBool;
    }
    public static @Nullable SHIP of(@NonNull String s){
        for(SHIP ship : values()){
            if(ship.ship.equals(s) || ship.shipEn.equals(s) || ship.shortName.equals(s) || ship.shortNameEn.equals(s)){
                return ship;
            }
        }
        return null;
    }

    public int toSerialize(){
        switch (this){
            default:
            case OKI:       return 1;
            case KUNIGA:    return 2;
            case SHIRASHIMA:return 3;
            case RAINBOW:   return 4;
            case DOZEN:     return 5;
            case ISOKAZE:   return 6;
        }
    }
    public static @NonNull SHIP parse(int serialValue){
        switch (serialValue){
            default:
            case 1: return OKI;
            case 2: return KUNIGA;
            case 3: return SHIRASHIMA;
            case 4: return RAINBOW;
            case 5: return DOZEN;
            case 6: return ISOKAZE;
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

    public static final Creator<SHIP> CREATOR = new Creator<SHIP>() {
        @Override
        public SHIP createFromParcel(Parcel source) {
            return parse(source.readInt());
        }
        @Override
        public SHIP[] newArray(int size) {
            return new SHIP[size];
        }
    };

}
