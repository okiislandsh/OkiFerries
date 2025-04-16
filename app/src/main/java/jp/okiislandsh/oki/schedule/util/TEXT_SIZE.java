package jp.okiislandsh.oki.schedule.util;

import android.content.Context;

import androidx.annotation.NonNull;

import jp.okiislandsh.library.android.SizeUtil;
import jp.okiislandsh.library.android.preference.IEnumPreference;

/** 時刻表の文字サイズ */
public enum TEXT_SIZE implements IEnumPreference.IAltText {
    SP10(10),
    SP12(12),
    SP14(14),
    SP15(15),
    SP16(16),
    SP18(18),
    SP20(20),
    SP22(22),
    SP24(24),
    SP26(26),
    SP30(30),
    SP34(34),
    ;
    public final int sp;
    TEXT_SIZE(int sp) {
        this.sp = sp;
    }

    public int px(@NonNull Context context){
        return (int) SizeUtil.sp2px(sp, context);
    }
    @Override
    public @NonNull String getAltText(@NonNull Context context) {
        if(sp<24) {
            return String.valueOf(sp);
        }else{
            return sp +"(for tablet)";
        }
    }
    @Override
    public @NonNull String getListText(@NonNull Context context) {
        return getAltText(context);
    }
}
