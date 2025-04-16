package jp.okiislandsh.oki.schedule.ui.tableall;

import static jp.okiislandsh.library.android.MyUtil.BR;
import static jp.okiislandsh.library.core.MyUtil.isJa;

import androidx.annotation.NonNull;

import jp.okiislandsh.library.core.Pairs;

/** 予約の可否をbool値で管理し、それの文字列化を行う */
public class FerryAttributeBool extends Pairs.Immutable.NonNull._6<Boolean, Boolean, Boolean, Boolean, Boolean, Boolean>{
    public final boolean flgMe;
    public final boolean flgRsvMe;
    public final boolean flgRsvCaseBySeat;
    public final boolean flgCar;
    public final boolean flgRsvCar;
    public final boolean flgPet;

    public FerryAttributeBool(boolean flgMe, boolean flgRsvMe, boolean flgRsvCaseBySeat, boolean flgCar, boolean flgRsvCar, boolean flgPet) {
        super(flgMe, flgRsvMe, flgRsvCaseBySeat, flgCar, flgRsvCar, flgPet);
        this.flgMe = flgMe;
        this.flgRsvMe = flgRsvMe;
        this.flgRsvCaseBySeat = flgRsvCaseBySeat;
        this.flgCar = flgCar;
        this.flgRsvCar = flgRsvCar;
        this.flgPet = flgPet;
    }
    @Override
    public @NonNull String toString() {
        return "FerryAttributeBool{" +
                "flgMe=" + flgMe +
                ", flgRsvMe=" + flgRsvMe +
                ", flgRsvCaseBySeat=" + flgRsvCaseBySeat +
                ", flgCar=" + flgCar +
                ", flgRsvCar=" + flgRsvCar +
                ", flgPet=" + flgPet +
                '}';
    }
    public @NonNull String toDisplayString(){
        final @NonNull StringBuilder buf = new StringBuilder();
        if(isJa()) {
            buf.append("人:").append(flgMe ?"乗船可能":"乗船できない")
                    .append("(予約:").append(flgRsvCaseBySeat ?"座席による":(flgRsvMe ?"推奨":"できない")).append(")").append(BR)
                    .append(flgCar ?"車両積み込み可能":"車両積み込みできない")
                    .append("(予約:").append(flgRsvCar ?"推奨":"できない").append(")").append(BR)
                    .append(flgPet ?"ペット(ケージ)":"ペット不可");
        }else{
            buf.append("Human:").append(flgMe ?"Boardable":"Can't")
                    .append(" (RSV.: ").append(flgRsvCaseBySeat ?"It depends on seat.":(flgRsvMe ?"Recommended":"Can't")).append(" )").append(BR)
                    .append(flgCar ?"Vehicle transportable.":"Vehicle transport not possible.")
                    .append(" (RSV.: ").append(flgRsvCar ?"Recommended":"Can't").append(" )").append(BR)
                    .append(flgPet ?"Pets in cage.":"Pets are not allowed.");
        }
        return buf.toString();
    }
}
