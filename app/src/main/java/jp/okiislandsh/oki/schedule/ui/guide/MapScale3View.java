package jp.okiislandsh.oki.schedule.ui.guide;

import static jp.okiislandsh.library.core.MyUtil.isJa;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import jp.okiislandsh.library.android.BitmapUtil;
import jp.okiislandsh.library.android.MimeUtil;
import jp.okiislandsh.library.android.view.DragAndPinchLayout;
import jp.okiislandsh.oki.schedule.R;

public class MapScale3View extends DragAndPinchLayout {

    private final @NonNull Context context;

    private @Nullable Listener listener = null;

    private final @NonNull Listener helper = (port) -> {
        if(listener!=null)listener.onPortClick(port);
    };

    public interface Listener{
        void onPortClick(@NonNull PORT port);
    }

    public enum PORT{
        KURII, HISHIURA, BEPPU, SHICHIRUI, SAKAIMINATO, SAIGO
    }

    public MapScale3View(@NonNull Context context) {
        super(context);
        this.context = context;
        init(PORT.KURII);
    }

    public MapScale3View(@NonNull Context context, @NonNull Listener listener, @NonNull PORT port) {
        super(context);
        this.context = context;
        this.listener = listener;
        init(port);
    }

    public MapScale3View(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(PORT.KURII);
    }

    public MapScale3View(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        init(PORT.KURII);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(final @NonNull PORT port) {
        final int resID;
        switch (port){
            default:
            case KURII:
                resID = isJa() ? R.drawable.map_scale3_kurii_ja : R.drawable.map_scale3_kurii;
                break;
            case HISHIURA:
                resID = isJa() ? R.drawable.map_scale3_hishiura_ja : R.drawable.map_scale3_hishiura;
                break;
            case BEPPU:
                resID = isJa() ? R.drawable.map_scale3_beppu_ja : R.drawable.map_scale3_beppu;
                break;
            case SHICHIRUI:
                resID = isJa() ? R.drawable.map_scale3_shichirui_ja : R.drawable.map_scale3_shichirui;
                break;
            case SAKAIMINATO:
                resID = isJa() ? R.drawable.map_scale3_sakaiminato_ja : R.drawable.map_scale3_sakaiminato;
                break;
            case SAIGO:
                resID = isJa() ? R.drawable.map_scale3_saigo_ja : R.drawable.map_scale3_saigo;
                break;
        }

        @Nullable Bitmap bitmap;
        try {
            bitmap = BitmapUtil.getBitmap(new BitmapUtil.MyResource(getResources(), resID, MimeUtil.SUPPORTED_IMAGE.PNG), 1600, 1600, Bitmap.Config.RGB_565);
        } catch (IOException e) {
            bitmap = null;
            Log.w("港地図読み込みに失敗", e);
        }

        final @NonNull ImageView imageView = new ImageView(context);
        imageView.setImageBitmap(bitmap);
        /* Bitmap縮小読み込みに変更のため
        final @NonNull LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL);
        final @Nullable Drawable drawable = imageView.getDrawable();
        if(drawable!=null) {
            Log.d("MapScale3View", "drawable size="+drawable.getIntrinsicWidth()+", "+ drawable.getIntrinsicHeight());
            params.width = drawable.getIntrinsicWidth()/3;
            params.height = drawable.getIntrinsicHeight()/3;
        }
        imageView.setLayoutParams(params);
        */
        //簡易クリック検出、setOnClickListenerを使うとうまく処理されない。ドラッグにより自身の座標もぴったり連動するため必ずクリック判定される
        imageView.setOnTouchListener(new SimpleClickableOnTouchListener(){
            @Override
            public void onSimpleClick() {
                helper.onPortClick(port);
            }
        });

        addView(imageView);

    }

    public void setListener(@Nullable Listener listener){
        this.listener = listener;
    }

}
