package jp.okiislandsh.oki.schedule.ui.guide;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jp.okiislandsh.library.android.view.DragAndPinchLayout;
import jp.okiislandsh.oki.schedule.databinding.MapScale2HondoViewBinding;

public class MapScale2HondoView extends DragAndPinchLayout {

    private final @NonNull Context context;

    private @Nullable Listener listener = null;

    private final @NonNull Listener helper = new Listener() {
        @Override
        public void onSeaClick() {
            if(listener!=null)listener.onSeaClick();
        }
        @Override
        public void onShichiruiClick() {
            if(listener!=null)listener.onShichiruiClick();
        }
        @Override
        public void onSakaiminatoClick() {
            if(listener!=null)listener.onSakaiminatoClick();
        }
    };

    public interface Listener{
        void onSeaClick();
        void onShichiruiClick();
        void onSakaiminatoClick();
    }

    public MapScale2HondoView(@NonNull Context context) {
        super(context);
        this.context = context;
        init();
    }

    public MapScale2HondoView(@NonNull Context context, @NonNull Listener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
        init();
    }

    public MapScale2HondoView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public MapScale2HondoView(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        init();
    }

    private void init() {
        final @NonNull MapScale2HondoViewBinding bind = MapScale2HondoViewBinding.inflate(LayoutInflater.from(context), this, true);

        //簡易クリック検出、setOnClickListenerを使うとうまく処理されない。ドラッグにより自身の座標もぴったり連動するため必ずクリック判定される
        bind.eventAreaSea.setOnTouchListener(new SimpleClickableOnTouchListener(){
            @Override
            public void onSimpleClick() {
                helper.onSeaClick();
            }
        });
        bind.textShichirui.setOnTouchListener(new SimpleClickableOnTouchListener(){
            @Override
            public void onSimpleClick() {
                helper.onShichiruiClick();
            }
        });
        bind.textSakaiminato.setOnTouchListener(new SimpleClickableOnTouchListener(){
            @Override
            public void onSimpleClick() {
                helper.onSakaiminatoClick();
            }
        });

    }

    public void setListener(@Nullable Listener listener){
        this.listener = listener;
    }

}
