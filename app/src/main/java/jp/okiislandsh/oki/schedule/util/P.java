package jp.okiislandsh.oki.schedule.util;

import static jp.okiislandsh.library.android.MyUtil.BR;
import static jp.okiislandsh.library.core.Function.with;
import static jp.okiislandsh.library.core.MyUtil.isJa;
import static jp.okiislandsh.library.core.MyUtil.requireNonNull;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import org.json.JSONException;
import org.json.JSONObject;

import jp.okiislandsh.library.android.IContextAccess;
import jp.okiislandsh.library.android.MyUtil;
import jp.okiislandsh.library.android.live.NonNullLiveData;
import jp.okiislandsh.library.android.preference.ButtonPreference;
import jp.okiislandsh.library.android.preference.IBooleanPreference;
import jp.okiislandsh.library.android.preference.IEnumPreference;
import jp.okiislandsh.library.android.preference.IIntPreference;
import jp.okiislandsh.library.android.preference.IJSONPreference;
import jp.okiislandsh.library.android.preference.ILongPreference;
import jp.okiislandsh.library.android.preference.LifecyclePreference;
import jp.okiislandsh.library.android.preference.PreferenceBuilderFunction;
import jp.okiislandsh.library.core.DateUtil;
import jp.okiislandsh.library.core.Function;
import jp.okiislandsh.oki.schedule.BuildConfig;
import jp.okiislandsh.oki.schedule.MyApp;
import jp.okiislandsh.oki.schedule.R;

/** SharedPreferenceを使用したExif情報のキャッシュクラス */
public class P {
    private P(){}

    /** 免責同意状態 */
    public static final @NonNull IBooleanPreference.INonNull MENSEKI = IBooleanPreference.newNonNull("MENSEKI", false, (c, p) -> "免責同意", (c, p) -> p.get(c) ? "同意済み": "未同意");

    /** デバッグモード */
    public static final @NonNull IBooleanPreference.INonNull DEBUG_MODE = IBooleanPreference.newNonNull("DEBUG_MODE",
            false, (c, p) -> "デバッグモード", (c, p) -> p.get(c) ? "ON": "OFF");

    /** TimeTableDownloadTaskManagerのサーバアクセス実行時刻 */
    public static final @NonNull ILongPreference.INonNull LAST_SERVER_ACCESS_TIME_MILLIS_FOR_TIME_TABLE = ILongPreference.newNonNull("LAST_SERVER_ACCESS_TIME_MILLIS_FOR_TIME_TABLE", Long.MIN_VALUE, 0, Long.MAX_VALUE, (c, p) -> "時刻表データ 更新確認", (c, p) -> DateUtil.toEasyString(p.get(c)));

    /** MessageDownloadTaskManagerのサーバアクセス実行時刻 */
    public static final @NonNull ILongPreference.INonNull LAST_SERVER_ACCESS_TIME_MILLIS_FOR_MESSAGE = ILongPreference.newNonNull("LAST_SERVER_ACCESS_TIME_MILLIS_FOR_MESSAGE", Long.MIN_VALUE, 0, Long.MAX_VALUE, (c, p) -> "お知らせ 更新確認", (c, p) -> DateUtil.toEasyString(p.get(c)));

    /** お知らせの表示済みタイムスタンプ */
    public static final @NonNull ILongPreference.INonNull LAST_READ_MESSAGE_NUMBER = ILongPreference.newNonNull("LAST_READ_MESSAGE_NUMBER", Long.MIN_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, (c, p) -> "お知らせの表示済みnumber", (c, p) -> "number:"+p.get(c));

    /** 時刻表色選択 */
    public static final @NonNull IIntPreference.INonNull PORTS_BOOL = IIntPreference.newNonNull("PORTS_BOOL", PortsBool.getAllFalseInstance().toSerialize(), Integer.MIN_VALUE, Integer.MAX_VALUE, (c, p) -> "時刻表色選択", (c, p) -> PortsBool.parse(p.get(c)).toString());

    /** 島前乗り換え車指定 */
    public static final @NonNull IBooleanPreference.INonNull CAR_ONLY = IBooleanPreference.newNonNull("CAR_ONLY", false, (c, p) -> "島前乗り換えの車指定", (c, p) -> p.get(c) ? "ON" : "OFF");

    /** 到着港 */
    public static final @NonNull IIntPreference.INonNull PORTS_BOOL_DOZEN_ARRIVE = IIntPreference.newNonNull("PORTS_BOOL_DOZEN_ARRIVE", PORT_DOZEN.CHIBU.toSerialize(), Integer.MIN_VALUE, Integer.MAX_VALUE, (c, p) -> "ハイライト(到着港)", (c, p) -> PORT_DOZEN.parse(p.get(c)).name());
    /** 出発港 */
    public static final @NonNull IIntPreference.INonNull PORTS_BOOL_DOZEN_DEPARTURE = IIntPreference.newNonNull("PORTS_BOOL_DOZEN_DEPARTURE", PORT_DOZEN.AMA.toSerialize(), Integer.MIN_VALUE, Integer.MAX_VALUE, (c, p) -> "ハイライト(出発港)", (c, p) -> PORT_DOZEN.parse(p.get(c)).name());

    /** カラーテーマ */
    public static final @NonNull IEnumPreference.INonNull<MY_COLORS> COLOR_THEME = IEnumPreference.newNonNull(MY_COLORS.class, "theme", MY_COLORS.NORMAL, (c, p) -> c.getString(R.string.preference_theme_title));
    /** 時刻表文字サイズ */
    public static final @NonNull IEnumPreference.INonNull<TEXT_SIZE> TT_TEXT_SIZE = IEnumPreference.newNonNull(TEXT_SIZE.class, "text_size", TEXT_SIZE.SP16, (c, p) -> c.getString(R.string.preference_text_size_title));

    /** Web画面の各ボタンの設定 */
    public static final @NonNull URLPreference URL_SETTINGS = new URLPreference("URL_SETTINGS");
    public static class URLPreference implements IJSONPreference.INonNull {
        public enum TYPE {
            BUTTON_LARGE_K(R.string.url_okikankou),
            BUTTON_LARGE_LEFT(R.string.url_naikosen_status),
            BUTTON_LARGE_CENTER(R.string.url_okikisen),
            BUTTON_LARGE_RIGHT(R.string.url_kankokyokai),
            BUTTON_SMALL_ROBOT(R.string.url_translation),
            BUTTON_SMALL_WEATHER(R.string.url_weather),
            BUTTON_SMALL_CHIBU(R.string.url_chibu), BUTTON_SMALL_AMA(R.string.url_ama), BUTTON_SMALL_NISHINOSHIMA(R.string.url_nishinoshima), BUTTON_SMALL_DOGO(R.string.url_dogo),
            ;

            public final @StringRes int resID;

            TYPE(int resID) {
                this.resID = resID;
            }
        }
        private static final @NonNull String KEY_URL = "url";
        private static final @NonNull String KEY_LABEL = "label";
        private final @NonNull String preferenceKey;
        URLPreference(@NonNull String preferenceKey){
            this.preferenceKey = preferenceKey;
        }
        /** 戻り値がnullの時、アプリ初期設定を使うこと */
        public @Nullable String getURL(@NonNull Context context, @NonNull TYPE type){
            return getString(context, type, KEY_URL);
        }
        /** 戻り値がnullの時、アプリ初期設定を使うこと */
        public @Nullable String getLabel(@NonNull Context context, @NonNull TYPE type){
            return getString(context, type, KEY_LABEL);
        }
        /** JSONを部部編集して保存。値がnullの時 削除する。 */
        public void setURLAndLabel(@NonNull Context context, @NonNull TYPE type, @Nullable JSONObject jsonObject) throws Exception {
            final @NonNull JSONObject root = get(context); //保存に必要
            if(jsonObject==null){ //削除
                root.remove(type.name());
            }else{ //追加
                root.put(type.name(), jsonObject);
            }
            set(context, root);
        }
        /** JSONを部部編集して保存。値がnullの時 削除する。 */
        public void setURL(@NonNull Context context, @NonNull TYPE type, @Nullable String url) throws Exception {
            edit(context, type, KEY_URL, url);
        }
        /** JSONを部部編集して保存。値がnullの時 削除する。 */
        public void setLabel(@NonNull Context context, @NonNull TYPE type, @Nullable String label) throws Exception {
            edit(context, type, KEY_LABEL, label);
        }
        /** JSONを部部編集して保存。値がnullの時 削除する。 */
        public void edit(@NonNull Context context, @NonNull TYPE type, @NonNull String key, @Nullable String value) throws Exception {
            final @NonNull JSONObject root = get(context); //保存に必要
            final @NonNull JSONObject editObject;
            if(root.has(type.name())) {
                editObject = root.getJSONObject(type.name());
            }else{
                editObject = new JSONObject();
                root.put(type.name(), editObject);
            }
            if(value==null) { //削除
                editObject.remove(key);
            }else{ //追加
                editObject.put(key, value);
            }
            set(context, root);
        }
        /** 読み取り専用、更新系では使わない事 */
        public @Nullable String getString(@NonNull Context context, @NonNull TYPE type, @NonNull String jsonKey){
            final @Nullable JSONObject jsonObject = getJSONObject(context, type);
            try {
                return jsonObject==null ? null : jsonObject.getString(jsonKey);
            } catch (JSONException e) {
                return null;
            }
        }
        /** 読み取り専用、更新系では使わない事 */
        public @Nullable JSONObject getJSONObject(@NonNull Context context, @NonNull TYPE type){
            try {
                return get(context).getJSONObject(type.name());
            } catch (JSONException e) {
                return null;
            }
        }
        @Override
        public @NonNull JSONObject getDefaultValue() {
            return new JSONObject();
        }
        @Override
        public @NonNull String getKey() {
            return preferenceKey;
        }
        @Override
        public @NonNull String getTitle(@NonNull Context context) {
            return preferenceKey;
        }
        @Override
        public @NonNull String getText(@NonNull Context context) {
            try{
                return get(context).toString(2);
            }catch (Exception e){
                ((IContextAccess) context).showToastL("JSON.toString() Error", e);
                return "null";
            }
        }
    }

    /** rootに設定をinflateして、rootを返す */
    public static @NonNull PreferenceScreen inflatePreference(@NonNull PreferenceFragmentCompat fragment, @NonNull Context context, @NonNull PreferenceScreen root){
        PreferenceBuilderFunction.buildAndRun(new PreferenceBuilderFunction.BuildAndRun(context) {
            @Override
            public void run() {
                addPreference(root, buildLabel(isJa("ビルドバージョン", "Build Version"), getBuildVersionName()));
                final boolean isDebug = DEBUG_MODE.get(context);
                if(isDebug){
                    addPreference(root, buildLabel("version code, name and applicationID", "Code="+BuildConfig.VERSION_CODE + ", Name=" + BuildConfig.VERSION_NAME + BR + context.getPackageName()),
                            build(DEBUG_MODE),
                            build(MENSEKI),
                            buildLabel(LAST_READ_MESSAGE_NUMBER),
                            buildLabel(PORTS_BOOL.getKey(), PORTS_BOOL.getTitle(context), p->getPortsBool(context).toString()),
                            buildLabel(CAR_ONLY),
                            buildLabel(PORTS_BOOL_DOZEN_DEPARTURE), buildLabel(PORTS_BOOL_DOZEN_ARRIVE));
                }
                addPreference(root,
                        build(COLOR_THEME),
                        build(TT_TEXT_SIZE)
                );
                with(LAST_SERVER_ACCESS_TIME_MILLIS_FOR_TIME_TABLE, TimeTableDownloadTaskManager.getInstance(), isJa("停止", "STOP"), isJa("更新確認", "Check Updates"),
                        (p, manager, buttonLabel_stop, buttonLabel_start)-> {
                            addPreference(root,
                                    buildButton(p.getKey(),
                                            p.getTitle(context),
                                            buttonPreference ->
                                                    isJa("バージョン：", "Version: ") + requireNonNull(MyApp.getTableDataNow(), TimeTableDownloadTaskManager.TimeTableTuple::numberToString, "No found.") + BR +
                                                            isJa("タスクの状態：", "Task Status: ") + manager.getTaskStatus().name() + BR +
                                                            isJa("サーバ接続時刻：", "Server Access: ") + p.getText(context),
                                            buttonPreference -> manager.getTaskStatus() == TimeTableDownloadTaskManager.TASK_STATUS.RUNNING ? buttonLabel_stop : buttonLabel_start,
                                            (that, button) -> { //ボタンクリック処理
                                                final @Nullable CharSequence nowLabel = that.getButtonLabel();
                                                if(nowLabel==null) {
                                                    showToastS("IllegalState. Label is null.");
                                                }else {
                                                    //ボタンラベル設定時と現在のタスク状態が一致するときのみ処理を続行する
                                                    final @Nullable CharSequence nowLabelString = nowLabel.toString();
                                                    if(nowLabelString.equals(buttonLabel_stop)) {
                                                        if (manager.getTaskStatus() == TimeTableDownloadTaskManager.TASK_STATUS.RUNNING) { //現在タスク実行中
                                                            manager.cancelTask();
                                                            showToastS("Cancel");
                                                        } else { //ボタンラベルと実際のタスクの状態が変化した
                                                            that.notifyChanged(); //ラベル更新
                                                            showToastS("Status changed.");
                                                        }
                                                    }else if(nowLabelString.equals(buttonLabel_start)) {
                                                        if (manager.getTaskStatus() != TimeTableDownloadTaskManager.TASK_STATUS.RUNNING) { //現在タスク停止中
                                                            p.remove(context);
                                                            manager.executeTask();
                                                            showToastS("Execute");
                                                        } else { //ボタンラベルと実際のタスクの状態が変化した
                                                            that.notifyChanged(); //ラベル更新
                                                            showToastS("Status changed.");
                                                        }
                                                    }else{
                                                        showToastS("IllegalState. Label is unknown. "+nowLabelString);
                                                    }
                                                }
                                            },
                                            (that) -> {
                                                //ボタンのラベルをPreferenceLiveDataによって動的に設定したい
                                                final @NonNull MutableLiveData<Void> liveNotifier = new MutableLiveData<>();
                                                //PreferenceLiveDataなどの変更により、画面へnotifyChangedして更新する
                                                //memo:LifecyclePreferenceをownerにPreferenceLiveDataをobserveし自身へnotifyChangedすると、onBindViewHolderの呼び出しでPreferenceLiveDataのsetValueが発生して無限ループになる
                                                //     LiveDataのobserveはPreferenceFragmentを使用しましょう。
                                                liveNotifier.observe(fragment, unused -> {
                                                    //Observerでthatを参照すると万が一 リークする可能性
                                                    //そもそもfragmentにLiveDataが多数登録されるリスクがある、LiveData自体の管理をFragmentへ任せないかぎり
                                                    //PreferenceのitemViewにLifecycleを実装して、自身のnotifyChangedを呼ぶ？
                                                    final @Nullable Preference displayPreference = fragment.findPreference(p.getKey());
                                                    if(displayPreference instanceof ButtonPreference) ((ButtonPreference) displayPreference).notifyChanged();
                                                }); //ObserverによるnotifyChangedの呼び出しを一つにまとめる
                                                p.getLive(context).observe(fragment, accessTimeMillis -> liveNotifier.postValue(null));
                                                manager.livePopTaskStatus.newLive().observe(fragment, accessTimeMillis -> liveNotifier.postValue(null));
                                            }
                                    )
                            );
                        }
                );
                with(LAST_SERVER_ACCESS_TIME_MILLIS_FOR_MESSAGE, MessageDownloadTaskManager.getInstance(), isJa("停止", "STOP"), isJa("更新確認", "Check Updates"),
                        (p, manager, buttonLabel_stop, buttonLabel_start)-> {
                            addPreference(root,
                                    buildButton(p.getKey(),
                                            p.getTitle(context),
                                            buttonPreference ->
                                                    isJa("バージョン：", "Version: ") +  requireNonNull(MyApp.getMessageDataNow(), messageData -> messageData.number.toString(), "No found.") + BR +
                                                            isJa("タスクの状態：", "Task Status: ") + manager.getTaskStatus().name() + BR +
                                                            isJa("サーバ接続時刻：", "Server Access: ") + p.getText(context),
                                            buttonPreference -> manager.getTaskStatus() == MessageDownloadTaskManager.TASK_STATUS.RUNNING ? buttonLabel_stop : buttonLabel_start,
                                            (that, button) -> { //ボタンクリック処理
                                                final @Nullable CharSequence nowLabel = that.getButtonLabel();
                                                if(nowLabel==null) {
                                                    showToastS("IllegalState. Label is null.");
                                                }else {
                                                    //ボタンラベル設定時と現在のタスク状態が一致するときのみ処理を続行する
                                                    final @Nullable CharSequence nowLabelString = nowLabel.toString();
                                                    if(nowLabelString.equals(buttonLabel_stop)) {
                                                        if (manager.getTaskStatus() == MessageDownloadTaskManager.TASK_STATUS.RUNNING) { //現在タスク実行中
                                                            manager.cancelTask();
                                                            showToastS("Cancel");
                                                        } else { //ボタンラベルと実際のタスクの状態が変化した
                                                            that.notifyChanged(); //ラベル更新
                                                            showToastS("Status changed.");
                                                        }
                                                    }else if(nowLabelString.equals(buttonLabel_start)) {
                                                        if (manager.getTaskStatus() != MessageDownloadTaskManager.TASK_STATUS.RUNNING) { //現在タスク停止中
                                                            p.remove(context);
                                                            manager.executeTask();
                                                            showToastS("Execute");
                                                        } else { //ボタンラベルと実際のタスクの状態が変化した
                                                            that.notifyChanged(); //ラベル更新
                                                            showToastS("Status changed.");
                                                        }
                                                    }else{
                                                        showToastS("IllegalState. Label is unknown. "+nowLabelString);
                                                    }
                                                }
                                            },
                                            (that) -> {
                                                //ボタンのラベルをPreferenceLiveDataによって動的に設定したい
                                                final @NonNull MutableLiveData<Void> liveNotifier = new MutableLiveData<>();
                                                liveNotifier.observe(fragment, unused -> {
                                                    final @Nullable Preference displayPreference = fragment.findPreference(p.getKey());
                                                    if(displayPreference instanceof ButtonPreference) ((ButtonPreference) displayPreference).notifyChanged();
                                                });
                                                p.getLive(context).observe(fragment, accessTimeMillis -> liveNotifier.postValue(null));
                                                manager.livePopTaskStatus.newLive().observe(fragment, accessTimeMillis -> liveNotifier.postValue(null));
                                            }
                                    )
                            );
                        }
                );
            }
        });
        return root;
    }

    public static @NonNull String getBuildVersionName(){
        final @NonNull String packageName = MyApp.app.getPackageName();
        switch(packageName){
            case "jp.okiislandsh.oki.schedule.amzn":
                return isJa("Amazonアプリストア向け", "for Amazon appstore");
            case "jp.okiislandsh.oki.schedule.github":
                return isJa("Github向け", "for Github");
            case "jp.okiislandsh.oki.schedule":
                return isJa("Google PlayStore向け", "for Google PlayStore");
            case "jp.okiislandsh.oki.schedule.debug":
                return "Debugビルド";
            default:
                return "Unknownビルド "+packageName;
        }
    }

    /** テキストサイズ+
     * @return 成功 true / false 失敗 */
    public static boolean setTimeTableFontSizePlus(@NonNull Context context){
        final @NonNull TEXT_SIZE currentSize = P.TT_TEXT_SIZE.get(context);
        final int nextOrdinal = currentSize.ordinal()+1;
        final @NonNull TEXT_SIZE[] sizeArray = TEXT_SIZE.values();
        if(nextOrdinal < sizeArray.length){
            P.TT_TEXT_SIZE.set(context, sizeArray[nextOrdinal]);
            return true;
        }
        return false;
    }

    /** テキストサイズ-
     * @return 成功 true / false 失敗 */
    public static boolean setTimeTableFontSizeMinus(@NonNull Context context){
        final @NonNull TEXT_SIZE currentSize = P.TT_TEXT_SIZE.get(context);
        final int nextOrdinal = currentSize.ordinal()-1;
        final @NonNull TEXT_SIZE[] sizeArray = TEXT_SIZE.values();
        if(0 <= nextOrdinal){
            P.TT_TEXT_SIZE.set(context, sizeArray[nextOrdinal]);
            return true;
        }
        return false;
    }

    /** 時刻表の文字サイズ */
    public static int getTimeTableFontSizePx(@NonNull Context context){
        return TT_TEXT_SIZE.get(context).px(context);
    }

    /** 時刻表の色設定 */
    public static @NonNull MY_COLORS getMyColors(@NonNull Context context){
        return COLOR_THEME.get(context);
    }

    /** 時刻表の強調表示設定 */
    public static @NonNull PortsBool getPortsBool(@NonNull Context context){
        return PortsBool.parse(PORTS_BOOL.get(context));
    }

    /** 時刻表の強調表示設定 */
    public static void setPortsBool(@NonNull Context context, @NonNull PortsBool portsBool){
        PORTS_BOOL.set(context, portsBool.toSerialize());
    }

    /** 島前時刻表の出発港設定 */
    public static void setPortsBoolDozenDeparture(@NonNull Context context, @NonNull PORT_DOZEN portDozen){
        PORTS_BOOL_DOZEN_DEPARTURE.set(context, portDozen.toSerialize());
    }

    /** 島前時刻表の到着港設定 */
    public static void setPortsBoolDozenArrive(@NonNull Context context, @NonNull PORT_DOZEN portDozen){
        PORTS_BOOL_DOZEN_ARRIVE.set(context, portDozen.toSerialize());
    }

    /** 島前時刻表の車両設定を反転させる */
    public static void setReverseCarOnly(@NonNull Context context){
        CAR_ONLY.set(context, !CAR_ONLY.get(context));
    }

}
