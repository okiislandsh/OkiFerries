package jp.okiislandsh.oki.schedule.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public enum PORT {
    CHIBU("知夫", "Chibu", "来居", "Kurii"),
    AMA("海士", "Ama", "菱浦", "Hishiura"),
    NISHINOSHIMA("西ノ島", "Nishinoshima", "別府", "Beppu"),
    SHICHIRUI("七類", "Shichirui", "七類", "Shichirui"),
    SAKAIMINATO("境港", "Sakaiminato", "境港", "Sakaiminato"),
    DOGO("島後", "Dogo", "西郷", "Saigo"),
    ;
    public final @NonNull String city;
    public final @NonNull String cityEn;
    public final @NonNull String port;
    public final @NonNull String portEn;
    PORT(@NonNull String city, @NonNull String cityEn, @NonNull String port, @NonNull String portEn) {
        this.city = city;
        this.cityEn = cityEn;
        this.port = port;
        this.portEn = portEn;
    }
    /** 港名称でENUM変換 */
    public static @Nullable PORT of(@NonNull String s){
        for(PORT port: values()){
            if(port.city.equals(s) || port.cityEn.equals(s) || port.port.equals(s) || port.portEn.equals(s)){
                return port;
            }
        }
        return null;
    }
    public int getBooledPortColor(@NonNull PortsBool portsBool, @NonNull MY_COLORS myColors){
        switch (this){
            default:
            case CHIBU: return portsBool.chibu ? myColors.color_text_chibu : myColors.color_text;
            case AMA:return portsBool.ama ? myColors.color_text_ama : myColors.color_text;
            case NISHINOSHIMA:return portsBool.nishinoshima ? myColors.color_text_nishinoshima : myColors.color_text;
            case SHICHIRUI:return portsBool.shichirui ? myColors.color_text_shichirui : myColors.color_text;
            case SAKAIMINATO:return portsBool.sakaiminato ? myColors.color_text_sakaiminato : myColors.color_text;
            case DOGO:return portsBool.dogo ? myColors.color_text_dogo : myColors.color_text;
        }
    }
}
