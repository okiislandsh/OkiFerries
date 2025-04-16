package jp.okiislandsh.oki.schedule.ui.guide;

import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class SimpleClickableOnTouchListener implements View.OnTouchListener {
    /** ActionUPの座標 */
    private @Nullable Point lastActionDownPosition = null;
    /** タップ誤差閾値 */
    private static final int SUB_RATIO = 5;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final @NonNull Point nowPosition = new Point((int)event.getX(), (int)event.getY());
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                lastActionDownPosition = nowPosition;
                break;
            case MotionEvent.ACTION_UP:
                //タップ誤差
                if(lastActionDownPosition!=null) { //一応
                    final int subX = Math.abs(nowPosition.x - lastActionDownPosition.x);
                    final int subY = Math.abs(nowPosition.y - lastActionDownPosition.y);
                    if (subX < SUB_RATIO && subY < SUB_RATIO) {
                        onSimpleClick();
                    }
                }
                break;
        }
        return true;
    }
    abstract public void onSimpleClick();
}
