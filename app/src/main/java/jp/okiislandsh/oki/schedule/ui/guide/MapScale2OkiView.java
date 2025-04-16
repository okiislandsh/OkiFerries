package jp.okiislandsh.oki.schedule.ui.guide;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jp.okiislandsh.library.android.view.DragAndPinchLayout;
import jp.okiislandsh.oki.schedule.databinding.MapScale2OkiViewBinding;

public class MapScale2OkiView extends DragAndPinchLayout {

    private final @NonNull Context context;

    private @Nullable Listener listener = null;

    private final @NonNull Listener helper = new Listener() {
        @Override
        public void onSeaClick() {
            if(listener!=null)listener.onSeaClick();
        }
        @Override
        public void onChibuClick() {
            if(listener!=null)listener.onChibuClick();
        }
        @Override
        public void onAmaClick() {
            if(listener!=null)listener.onAmaClick();
        }
        @Override
        public void onNishinoshimaClick() {
            if(listener!=null)listener.onNishinoshimaClick();
        }
        @Override
        public void onDogoClick() {
            if(listener!=null)listener.onDogoClick();
        }
    };

    public interface Listener{
        void onSeaClick();
        void onChibuClick();
        void onAmaClick();
        void onNishinoshimaClick();
        void onDogoClick();
    }

    public MapScale2OkiView(@NonNull Context context) {
        super(context);
        this.context = context;
        init();
    }

    public MapScale2OkiView(@NonNull Context context, @NonNull Listener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
        init();
    }

    public MapScale2OkiView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public MapScale2OkiView(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        init();
    }


    private void init() {
        final @NonNull MapScale2OkiViewBinding bind = MapScale2OkiViewBinding.inflate(LayoutInflater.from(context), this, true);

        //簡易クリック検出、setOnClickListenerを使うとうまく処理されない。ドラッグにより自身の座標もぴったり連動するため必ずクリック判定される
        bind.eventAreaSea.setOnTouchListener(new SimpleClickableOnTouchListener(){
            @Override
            public void onSimpleClick() {
                helper.onSeaClick();
            }
        });
        bind.textChibu.setOnTouchListener(new SimpleClickableOnTouchListener(){
            @Override
            public void onSimpleClick() {
                helper.onChibuClick();
            }
        });
        bind.textAma.setOnTouchListener(new SimpleClickableOnTouchListener(){
            @Override
            public void onSimpleClick() {
                helper.onAmaClick();
            }
        });
        bind.textNishinoshima.setOnTouchListener(new SimpleClickableOnTouchListener(){
            @Override
            public void onSimpleClick() {
                helper.onNishinoshimaClick();
            }
        });
        bind.textDogo.setOnTouchListener(new SimpleClickableOnTouchListener(){
            @Override
            public void onSimpleClick() {
                helper.onDogoClick();
            }
        });

    }

    public void setListener(@Nullable Listener listener){
        this.listener = listener;
    }

}
