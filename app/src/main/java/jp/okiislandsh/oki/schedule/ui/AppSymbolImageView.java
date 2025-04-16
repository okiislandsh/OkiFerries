package jp.okiislandsh.oki.schedule.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import jp.okiislandsh.library.android.view.ViewBuilderFunction;
import jp.okiislandsh.library.core.Pairs;
import jp.okiislandsh.oki.schedule.util.P;

/** ドロワーのヘッダにアプリアイコンを表示したい */
public class AppSymbolImageView extends AppCompatImageView implements ViewBuilderFunction.OnView {

    public AppSymbolImageView(Context context) {
        super(context);
    }

    public AppSymbolImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AppSymbolImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    {
        //デバッグ切り替えの隠しリンク
        setOnClickListener(new View.OnClickListener() {
            final int ARRAY_COUNT = 2; //クリック回数
            final int TIME_LIMIT = ARRAY_COUNT*130; //総クリック時間
            int index = 0;
            final @NonNull long[] millisArray = new long[ARRAY_COUNT];
            @Override
            public void onClick(View v) {
                final long now = millisArray[index] = System.currentTimeMillis();
                index = (index +1) % ARRAY_COUNT;
                //クリック時間を判定
                // 最新のクリック時刻から全て閾値以下であること
                // ただし、時間差が負の場合は無効 ※longのオーバーフローを考慮
                for (long millis: millisArray) {
                    final long sub = now - millis;
                    if (sub < 0 || sub >= TIME_LIMIT) {
                        return;
                    }
                }
                //成立、デバッグ切り替えダイアログを表示
                showDebugDialogDialog();
            }
        });
    }

    /** デバッグ切り替えダイアログを表示 */
    private void showDebugDialogDialog(){
        final @NonNull String debugMode = "デバッグモード";
        final @NonNull String normalMode = "通常モード";
        showDialog("デバッグ？", "現在:"+(P.DEBUG_MODE.get(requireContext()) ? debugMode : normalMode), null,
                new Pairs.Immutable._2<>(debugMode, (dialog, which) -> {
                    P.DEBUG_MODE.set(requireContext(), true);
                    showToastS(debugMode);
                }),
                new Pairs.Immutable._2<>(normalMode, (dialog, which) -> {
                    P.DEBUG_MODE.set(requireContext(), false);
                    showToastS(normalMode);
                }),
                null, null, true, false);
    }

}
