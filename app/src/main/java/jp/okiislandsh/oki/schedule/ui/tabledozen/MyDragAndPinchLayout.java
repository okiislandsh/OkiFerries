package jp.okiislandsh.oki.schedule.ui.tabledozen;

import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import jp.okiislandsh.library.android.view.DragAndPinchLayout;

public class MyDragAndPinchLayout extends DragAndPinchLayout {

    public MyDragAndPinchLayout(@NonNull Context context) {
        super(context);
    }

    public MyDragAndPinchLayout(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyDragAndPinchLayout(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public @NonNull RectF getOffsetLimit(@NonNull Calculate value){
        return calcDragOffsetLimit_Box(value);
    }

}
