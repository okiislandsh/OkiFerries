package jp.okiislandsh.oki.schedule.util;

import android.content.Context;

import androidx.annotation.NonNull;

import jp.okiislandsh.library.android.preference.IEnumPreference;

public enum MY_COLORS implements IEnumPreference.IAltText {
    NORMAL("Normal", 0x99ccccff, 0xff222222, 0xffff0000,
            0xff8800ff, 0xff0000ff, 0xff0088cc,
            0xffbb4400, 0xffbb4400, 0xff008800),
    SAKURA("Sakura", 0xccff6666, 0xffffffff, 0xffff0000,
            0xff8800ff, 0xff0000ff, 0xff2266cc,
            0xffbb0000, 0xffbb0000, 0xff00aa00),
    ELECTRIC("Electric", 0xcc000000, 0xff00ff00, 0xffff0000,
            0xffff4444, 0xffff00bb, 0xff8888ff,
            0xffdddddd, 0xffdddddd, 0xff00bbdd),
    DARK_RED("DarkRed", 0xcc440000, 0xffffffff, 0xffff0000,
            0xffff8888, 0xff88ff88, 0xff6666ff,
            0xffffff44, 0xffffff44, 0xff88ffff),
    DARK_BLUE("DarkBlue", 0xcc000033, 0xffffffff, 0xffff0000,
            0xffff8888, 0xff88ff88, 0xff6666ff,
            0xffffff44, 0xffffff44, 0xff88ffff),
    DARK_GREEN("DarkGreen", 0xcc003300, 0xffffffff, 0xffff0000,
            0xffff8888, 0xff88ff88, 0xff6666ff,
            0xffffff44, 0xffffff44, 0xff88ffff),
    ;
    public final @NonNull String name;
    public final int color_back;
    public final int color_text;
    public final int color_text_attention;
    public final int color_text_chibu;
    public final int color_text_ama;
    public final int color_text_nishinoshima;
    public final int color_text_shichirui;
    public final int color_text_sakaiminato;
    public final int color_text_dogo;
    MY_COLORS(@NonNull String name, int color_back, int color_text, int color_text_attention, int color_text_chibu, int color_text_ama, int color_text_nishinoshima, int color_text_shichirui, int color_text_sakaiminato, int color_text_dogo) {
        this.name = name;
        this.color_back = color_back;
        this.color_text = color_text;
        this.color_text_attention = color_text_attention;
        this.color_text_chibu = color_text_chibu;
        this.color_text_ama = color_text_ama;
        this.color_text_nishinoshima = color_text_nishinoshima;
        this.color_text_shichirui = color_text_shichirui;
        this.color_text_sakaiminato = color_text_sakaiminato;
        this.color_text_dogo = color_text_dogo;
    }

    @Override
    public @NonNull String getAltText(@NonNull Context context) {
        return name;
    }

    @Override
    public @NonNull String getListText(@NonNull Context context) {
        return name;
    }
}
