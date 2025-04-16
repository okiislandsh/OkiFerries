package jp.okiislandsh.oki.schedule.ui.guide;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AllMapView extends FrameLayout
        implements MapScale1View.Listener, MapScale2OkiView.Listener, MapScale2HondoView.Listener, MapScale3View.Listener {

    private final @NonNull Context context;

    private enum MAP_MODE{
        SCALE1, SCALE2_OKI, SCALE2_HONDO, SCALE3_KURII, SCALE3_HISHIURA, SCALE3_BEPPU, SCALE3_SAIGO, SCALE3_SHICHIRUI, SCALE3_SAKAIMINATO
    }

    public AllMapView(@NonNull Context context) {
        super(context);
        this.context = context;
        replaceView(MAP_MODE.SCALE1);
    }
    public AllMapView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        replaceView(MAP_MODE.SCALE1);
    }
    public AllMapView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        replaceView(MAP_MODE.SCALE1);
    }

    private void replaceView(final @NonNull MAP_MODE mapMode){
        removeAllViews();
        switch (mapMode){
            default:
            case SCALE1:
                addView(new MapScale1View(context, this));
                break;
            case SCALE2_OKI:
                addView(new MapScale2OkiView(context, this));
                break;
            case SCALE2_HONDO:
                addView(new MapScale2HondoView(context, this));
                break;
            case SCALE3_KURII:
                addView(new MapScale3View(context, this, MapScale3View.PORT.KURII));
                break;
            case SCALE3_HISHIURA:
                addView(new MapScale3View(context, this, MapScale3View.PORT.HISHIURA));
                break;
            case SCALE3_BEPPU:
                addView(new MapScale3View(context, this, MapScale3View.PORT.BEPPU));
                break;
            case SCALE3_SAIGO:
                addView(new MapScale3View(context, this, MapScale3View.PORT.SAIGO));
                break;
            case SCALE3_SHICHIRUI:
                addView(new MapScale3View(context, this, MapScale3View.PORT.SHICHIRUI));
                break;
            case SCALE3_SAKAIMINATO:
                addView(new MapScale3View(context, this, MapScale3View.PORT.SAKAIMINATO));
                break;
        }
    }

    @Override
    public void onOkiClick() {
        replaceView(MAP_MODE.SCALE2_OKI);
    }

    @Override
    public void onHondoClick() {
        replaceView(MAP_MODE.SCALE2_HONDO);
    }

    @Override
    public void onSeaClick() {
        replaceView(MAP_MODE.SCALE1);
    }

    @Override
    public void onChibuClick() {
        replaceView(MAP_MODE.SCALE3_KURII);
    }

    @Override
    public void onAmaClick() {
        replaceView(MAP_MODE.SCALE3_HISHIURA);
    }

    @Override
    public void onNishinoshimaClick() {
        replaceView(MAP_MODE.SCALE3_BEPPU);
    }

    @Override
    public void onShichiruiClick() {
        replaceView(MAP_MODE.SCALE3_SHICHIRUI);
    }

    @Override
    public void onSakaiminatoClick() {
        replaceView(MAP_MODE.SCALE3_SAKAIMINATO);
    }

    @Override
    public void onDogoClick() {
        replaceView(MAP_MODE.SCALE3_SAIGO);
    }

    @Override
    public void onPortClick(@NonNull MapScale3View.PORT port) {
        switch (port){
            default:
            case KURII:
            case HISHIURA:
            case BEPPU:
            case SAIGO:
                replaceView(MAP_MODE.SCALE2_OKI);
                break;
            case SHICHIRUI:
            case SAKAIMINATO:
                replaceView(MAP_MODE.SCALE2_HONDO);
                break;
        }
    }

}
