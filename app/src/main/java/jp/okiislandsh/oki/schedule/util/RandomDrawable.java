package jp.okiislandsh.oki.schedule.util;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import jp.okiislandsh.oki.schedule.R;

/** 時刻表背景のランダム画像 */
public class RandomDrawable {
    private static final int[] photoArray = new int[]{R.drawable.photo03, R.drawable.photo04, R.drawable.photo05, R.drawable.photo10, R.drawable.photo12, R.drawable.photo21};
    public static @Nullable Drawable getRandomDrawable(@NonNull Resources resources){
        return ResourcesCompat.getDrawable(resources,photoArray[(int)(Math.random()* photoArray.length)], null);
    }
}
