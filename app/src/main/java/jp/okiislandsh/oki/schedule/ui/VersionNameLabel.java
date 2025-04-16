package jp.okiislandsh.oki.schedule.ui;

import static jp.okiislandsh.library.android.MyUtil.BR;
import static jp.okiislandsh.library.core.MyUtil.isJa;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import jp.okiislandsh.library.android.view.live.LifecycleTextViewBase;
import jp.okiislandsh.oki.schedule.BuildConfig;
import jp.okiislandsh.oki.schedule.util.TimeTableData;
import jp.okiislandsh.oki.schedule.util.TimeTableDownloadTaskManager;

/** ドロワーのヘッダにバージョン名を出したい */
public class VersionNameLabel extends LifecycleTextViewBase {

    public VersionNameLabel(Context context) {
        super(context);
    }

    public VersionNameLabel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VersionNameLabel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private final @NonNull LiveData<TimeTableDownloadTaskManager.TimeTableTuple> liveTTData = TimeTableDownloadTaskManager.getInstance().livePopTimeTableData.newLive();
    private final @NonNull MutableLiveData<Void> liveUpdateTextNotifier = new MutableLiveData<>();

    {
        //初期テキスト
        updateText();

        //時刻表データのバージョン
        liveTTData.observe(this, unused->liveUpdateTextNotifier.postValue(null));

        //テキスト更新notifier
        liveUpdateTextNotifier.observe(this, unused->updateText());
    }

    private void updateText(){
        final @NonNull StringBuilder buf = new StringBuilder();
        //apkバージョン名とサフィックス
        buf.append("App: ").append(BuildConfig.VERSION_NAME.replace(". ", "."+ BR));
        //時刻表バージョン
        final @Nullable TimeTableDownloadTaskManager.TimeTableTuple tuple = liveTTData.getValue();
        buf.append(BR).append(tuple==null ? isJa("時刻表読み込み中...", "TimeTable loading...") :
                ("TimeTable: "+tuple.number())
        );
        setText(buf);
    }

}
