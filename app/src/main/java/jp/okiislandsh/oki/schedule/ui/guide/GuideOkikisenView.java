package jp.okiislandsh.oki.schedule.ui.guide;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.widget.NestedScrollView;

import java.util.ArrayList;

import jp.okiislandsh.library.android.view.ViewBuilderFunction;
import jp.okiislandsh.oki.schedule.R;

public class GuideOkikisenView extends NestedScrollView implements ViewBuilderFunction.OnView {

    private final @NonNull Context context;

    public GuideOkikisenView(@NonNull Context context) {
        super(context);
        this.context = context;
        init();
    }

    public GuideOkikisenView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public GuideOkikisenView(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        init();
    }

    private static class Page{
        final @DrawableRes int imageResID;
        final @StringRes int stringResID;
        private Page(@DrawableRes int imageResID, @StringRes int stringResID) {
            this.imageResID = imageResID;
            this.stringResID = stringResID;
        }
    }

    private class HeaderPage extends Page{
        private HeaderPage(@DrawableRes int imageResID, @StringRes int stringResID) {
            super(imageResID, stringResID);
        }
    }

    final @NonNull ArrayList<Page> pageList = new ArrayList<>();
    {
        pageList.add(new HeaderPage(R.drawable.icon_rainbow_okikisen, R.string.guide_okikisen_title));
        pageList.add(new Page(R.drawable.icon_rainbow_smartphone, R.string.guide_okikisen_1));
        pageList.add(new Page(R.drawable.icon_rainbow_wind, R.string.guide_okikisen_2));
        pageList.add(new Page(R.drawable.icon_rainbow_bus, R.string.guide_okikisen_3));
        pageList.add(new Page(R.drawable.icon_rainbow_pen, R.string.guide_okikisen_4));
        pageList.add(new Page(R.drawable.icon_rainbow_pet, R.string.guide_okikisen_5));
        pageList.add(new Page(R.drawable.icon_rainbow_sleep, R.string.guide_okikisen_6));
        pageList.add(new Page(R.drawable.icon_rainbow_dolphin, R.string.guide_okikisen_7));
        pageList.add(new Page(R.drawable.icon_rainbow_islands, R.string.guide_okikisen_8));
    }

    private void init() {
        final @NonNull LinearLayout verticalContainer = newLinearLayout(LinearLayout.VERTICAL, newFrameParamsMW(null));

        for(@NonNull Page page: pageList){
            final @NonNull LinearLayout pageContainer = newLinearLayout(LinearLayout.HORIZONTAL, setMarginRight(newParamsMW(0f), 20));

            final @NonNull ImageView imageView = new ImageView(context);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(192, LinearLayout.LayoutParams.MATCH_PARENT));
            imageView.setPadding(10, 10, 10, 10);
            imageView.setImageResource(page.imageResID);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setColorFilter(Color.BLACK);
            pageContainer.addView(imageView);

            final @NonNull TextView textView = newText(page.stringResID,
                    setMarginTop(newParams0W(1f), 20));
            textView.setTextColor(Color.BLACK);
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            textView.setMinHeight(192);
            pageContainer.addView(textView);

            if(page instanceof HeaderPage){
                textView.setGravity(Gravity.CENTER);
            }

            verticalContainer.addView(pageContainer);

        }

        addView(verticalContainer);

    }

}
