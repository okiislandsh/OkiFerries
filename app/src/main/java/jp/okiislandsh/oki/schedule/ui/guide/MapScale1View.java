package jp.okiislandsh.oki.schedule.ui.guide;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jp.okiislandsh.library.android.view.DragAndPinchLayout;
import jp.okiislandsh.oki.schedule.databinding.MapScale1ViewBinding;

public class MapScale1View extends DragAndPinchLayout {

    private final @NonNull Context context;

    private @Nullable Listener listener = null;

    private final @NonNull Listener helper = new Listener() {
        @Override
        public void onOkiClick() {
            if(listener!=null)listener.onOkiClick();
        }
        @Override
        public void onHondoClick() {
            if(listener!=null)listener.onHondoClick();
        }
    };

    public interface Listener{
        void onOkiClick();
        void onHondoClick();
    }

    public MapScale1View(@NonNull Context context) {
        super(context);
        this.context = context;
        init();
    }

    public MapScale1View(@NonNull Context context, @NonNull Listener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
        init();
    }

    public MapScale1View(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public MapScale1View(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        init();
    }

    private void init() {
        final @NonNull MapScale1ViewBinding bind = MapScale1ViewBinding.inflate(LayoutInflater.from(context), this, true);

        //簡易クリック検出、setOnClickListenerを使うとうまく処理されない。ドラッグにより自身の座標もぴったり連動するため必ずクリック判定される
        bind.eventAreaOkiIslands.setOnTouchListener(new SimpleClickableOnTouchListener(){
            @Override
            public void onSimpleClick() {
                helper.onOkiClick();
            }
        });

        bind.eventAreaHondo.setOnTouchListener(new SimpleClickableOnTouchListener(){
            @Override
            public void onSimpleClick() {
                helper.onHondoClick();
            }
        });

    }

    public void setListener(@Nullable Listener listener){
        this.listener = listener;
    }

}
