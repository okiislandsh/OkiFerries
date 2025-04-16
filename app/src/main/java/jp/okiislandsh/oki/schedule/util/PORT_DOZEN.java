package jp.okiislandsh.oki.schedule.util;

import androidx.annotation.NonNull;

/** 島前3港択一 */
public enum PORT_DOZEN {
    CHIBU, AMA, NISHINOSHIMA;
    public int toSerialize(){
        switch (this){
            default:
            case CHIBU: return 1;
            case AMA: return 2;
            case NISHINOSHIMA: return 3;
        }
    }
    public static @NonNull PORT_DOZEN parse(int serialValue){
        switch (serialValue){
            default:
            case 1: return CHIBU;
            case 2: return AMA;
            case 3: return NISHINOSHIMA;
        }
    }
    public @NonNull PORT toPort(){
        switch (this){
            default:
            case CHIBU: return PORT.CHIBU;
            case AMA: return PORT.AMA;
            case NISHINOSHIMA: return PORT.NISHINOSHIMA;
        }
    }

}
