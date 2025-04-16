package jp.okiislandsh.oki.schedule.util;

import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jp.okiislandsh.library.android.AssetUtil;
import jp.okiislandsh.library.android.live.populate.MutableLivePopulate;
import jp.okiislandsh.library.core.CharsetUtil;
import jp.okiislandsh.library.core.FileUtil;
import jp.okiislandsh.library.core.HMInt;
import jp.okiislandsh.library.core.MathUtil;
import jp.okiislandsh.library.core.MyUtil;
import jp.okiislandsh.library.core.Pairs;
import jp.okiislandsh.library.core.StopWatch;
import jp.okiislandsh.oki.schedule.MyApp;

/** 時刻表のダウンロード処理をシングルトン管理 */
public class TimeTableDownloadTaskManager {
    private static final @NonNull String LOG_TAG = "TTTaskManager";
    private static final @NonNull String ASSET_TIME_TABLE_ROOT = "timetable";
    private static final @NonNull String INTERNAL_TIME_TABLE_ROOT = ASSET_TIME_TABLE_ROOT;

    private static final @NonNull TimeTableDownloadTaskManager INSTANCE = new TimeTableDownloadTaskManager(); // シングルトンインスタンス
    private final @NonNull ExecutorService executor = Executors.newSingleThreadExecutor();
    private @Nullable Future<?> currentTask;
    private volatile boolean isRunning = false;

    public static class TimeTableTuple extends Pairs.Immutable._2<TimeTableLatestData, TimeTableData> {
        public TimeTableTuple(@NonNull TimeTableData ttData) {
            this(null, ttData);
        }
        public TimeTableTuple(@Nullable TimeTableLatestData latest, @NonNull TimeTableData ttData) {
            super(latest, ttData);
        }
        public @Nullable Long number(){
            return f==null ? null : f.number;
        }
        public @NonNull String numberToString(){
            return String.valueOf(number());
        }
        public @Nullable TimeTableLatestData latest(){
            return f;
        }
        public @NonNull TimeTableData ttData(){
            return s;
        }
    }

    public final @NonNull MutableLivePopulate<TimeTableTuple> livePopTimeTableData = new MutableLivePopulate<>();
    public final @NonNull MutableLivePopulate<TimeTableData> livePopNaikouMixData = new MutableLivePopulate<>();

    private static @NonNull TimeTableData convertNaikouMix(@NonNull TimeTableData ttAll){
        final @NonNull StopWatch sw = new StopWatch();
        Log.d(LOG_TAG, "NaikouMix変換開始 ttAll.size="+ttAll.size());
        final @NonNull TimeTableData ret = new TimeTableData();
        for(TimeTableData.Parts parts: ttAll){
            final @NonNull List<TimeTableData.PortTime> naikouPortTime = new ArrayList<>(parts.portTimes);
            //PORT索引
            final @NonNull SparseArray<PORT> portOfArray = new SparseArray<>();
            for (int i = 0; i < naikouPortTime.size(); i++) {
                TimeTableData.PortTime portTime = naikouPortTime.get(i);
                final @Nullable PORT port = PORT.of(portTime.port);
                //重要！ 島前以外の港はnullにしてしまう
                if (MyUtil.matchAny(port, PORT.CHIBU, PORT.AMA, PORT.NISHINOSHIMA)) portOfArray.put(i, port);
            }
            //本土の発着時間、本土へ行く出発時間、本土からくる到着時間をクリアする
            for (int i = 0, size= naikouPortTime.size(); i < size; i++) {
                TimeTableData.PortTime portTime = naikouPortTime.get(i);
                final @Nullable PORT before = (0<i) ? portOfArray.get(i-1) : null;
                final @Nullable PORT current = portOfArray.get(i);
                final @Nullable PORT after = i<(size-1) ? portOfArray.get(i+1) : null;
                if(current==null) { //本土にいるため発着をクリア
                    naikouPortTime.set(i, new TimeTableData.PortTime(parts.portTimes.get(i).port)); //港名は元のテキストを使う
                }else if(before==null || after==null){ //前後が本土
                    final @Nullable HMInt arrive = before==null ? null : portTime.arrive; //本土から来るので到着時間をクリア
                    final @Nullable HMInt departure = after==null ? null : portTime.departure; //本土へ行くので出発時間をクリア
                    naikouPortTime.set(i, new TimeTableData.PortTime(portTime.port, arrive, departure)); //港名は元のテキストを使う
                }else{
                    naikouPortTime.set(i, portTime); //島前便なのでそのまま使う
                }
            }
            ret.add(new TimeTableData.Parts(parts.ship, parts.spans, parts.days, parts.rinji, parts.info, naikouPortTime));
        }
        Log.d(LOG_TAG, "NaikouMix変換完了 NaikouMix.size="+ret.size()+"\t"+sw.now()+"ms"); //sizeは同じになる
        return ret;
    }

    private TimeTableDownloadTaskManager() {} // コンストラクタを private にして外部からのインスタンス化を禁止

    public static TimeTableDownloadTaskManager getInstance() {
        return INSTANCE;
    }

    private static final @NonNull String TIME_TABLE_LATEST_FILE_NAME = "latest.json";

    private static @Nullable String parseAssetTimeTableLatestJSON(){
        final @NonNull StopWatch stopWatch = new StopWatch();
        Log.d(LOG_TAG, "Assetの時刻表latest.json読み込み開始。");
        try (InputStream is = MyApp.app.getAssets().open(ASSET_TIME_TABLE_ROOT+File.separator+TIME_TABLE_LATEST_FILE_NAME);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            final @NonNull byte[] buffer = new byte[1024*8];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }

            final @NonNull String jsonString = bos.toString(StandardCharsets.UTF_8.name());
            Log.d(LOG_TAG, "Assetの時刻表latest.json読み込み完了。" + stopWatch.lap() + "ms");
            return jsonString;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Assetの時刻表latest.json読み込みに失敗。" + stopWatch.lap() + "ms", e);
            return null;
        }
    }

    // タスクの実行
    @AnyThread
    public synchronized void executeTask() {
        if (isRunning) {
            Log.d(LOG_TAG, "タスクはすでに実行中です");
            return;
        }

        currentTask = executor.submit(() -> {
            try {
                isRunning = true;

                final @NonNull StopWatch stopWatch = new StopWatch();
                Log.d(LOG_TAG, "タスク開始");

                //assetをアプリ内ストレージへ同期
                try {
                    Log.d(LOG_TAG, "アセットのアプリ内ストレージへの同期を開始");
                    //latest.jsonを読み込んでアプリ内ストレージのサブディレクトリ名を決定する
                    final @Nullable String latestJsonString = parseAssetTimeTableLatestJSON();
                    if(latestJsonString==null) throw new IOException("assetのlatest.jsonを読み込めなかった。");
                    final @NonNull TimeTableLatestData latest = new TimeTableLatestData(latestJsonString);
                    final @NonNull String INTERNAL_DIR = INTERNAL_TIME_TABLE_ROOT+File.separator+latest.number;
                    Log.d(LOG_TAG, "Assetのlatest.jsonを読込完了。" + stopWatch.lap() + "ms");
                    //assetをアプリ内ストレージへ同期
                    AssetUtil.syncAssetsToInternalStorage(MyApp.app, ASSET_TIME_TABLE_ROOT, INTERNAL_DIR);
                    Log.d(LOG_TAG, "Assetのアプリ内ストレージへの同期完了。" + stopWatch.lap() + "ms");
                }catch (Exception e){
                    Log.e(LOG_TAG, "Assetのアプリ内ストレージへの同期に失敗。Assetの直接読み込み開始。" + stopWatch.lap() + "ms", e);
                    try {
                        @Nullable TimeTableLatestData latest = null; //Assetのlatestを読めないなんてことはさすがにないと思いたい
                        final @NonNull TimeTableData ttData = new TimeTableData();
                        final @NonNull String[] assetFiles = AssetUtil.assetFiles(MyApp.app, ASSET_TIME_TABLE_ROOT);
                        for (@NonNull String file : assetFiles) {
                            try {
                                Log.d(LOG_TAG, "Asset("+file+")チェック。" + stopWatch.lap() + "ms");
                                if (file.endsWith("All.json")) {
                                    final @NonNull String fullPath = ASSET_TIME_TABLE_ROOT + "/" + file;
                                    Log.d(LOG_TAG, "Asset読み込み\t" + fullPath + "\t" + stopWatch.lap() + "ms");
                                    final @NonNull String json = AssetUtil.assetFileToString(MyApp.app, fullPath, CharsetUtil.UTF8, null);
                                    ttData.addAll(new TimeTableData(json));
                                }else if(file.equals(TIME_TABLE_LATEST_FILE_NAME)){
                                    final @NonNull String fullPath = ASSET_TIME_TABLE_ROOT + "/" + file;
                                    Log.d(LOG_TAG, "Asset読み込み\t" + fullPath + "\t" + stopWatch.lap() + "ms");
                                    final @NonNull String json = AssetUtil.assetFileToString(MyApp.app, fullPath, CharsetUtil.UTF8, null);
                                    latest = new TimeTableLatestData(json);
                                }else{
                                    Log.e(LOG_TAG, "Asset("+file+")が不明。" + stopWatch.lap() + "ms");
                                }
                            } catch (Exception e2) { //単一ファイルの読み込みエラーはresume next
                                Log.e(LOG_TAG, "Asset("+file+")読み込み失敗。resume next。\t" + stopWatch.lap() + "ms", e2);
                            }
                        }
                        livePopTimeTableData.postValue(new TimeTableTuple(latest, ttData));
                        livePopNaikouMixData.postValue(convertNaikouMix(ttData));
                    }catch (Exception e3){
                        Log.e(LOG_TAG, "Asset読み込み失敗。\t" + stopWatch.lap() + "ms", e3);
                    }
                }

                //アプリ内ストレージから構築
                // 直前でassetからアプリ内ストレージへコピーされているため、必ず1回は読み込みに成功する
                // 仮に全て失敗し1回も成功しない場合、このブロックではliveDataへpostValueされない ※このケースは直前のブロックの例外処理でasset直接読み込みによりpostValueされているはず
                final @NonNull File internalRootDir = new File(MyApp.app.getFilesDir(), ASSET_TIME_TABLE_ROOT);
                final @Nullable File[] internalSubList = internalRootDir.listFiles(File::isDirectory);
                if(internalSubList!=null && 0<internalSubList.length){
                    Arrays.sort(internalSubList, (o2, o1) -> o1.getName().compareTo(o2.getName())); //降順ソート
                    //新しい順に時刻表が読み込めるか試行 ※不完全なデータがあればスキップする
                    for (File current : internalSubList) {
                        try {
                            Log.d(LOG_TAG, current + "の読み込みを開始。\t" + stopWatch.lap() + "ms");
                            //latest.jsonを読み込む
                            final @NonNull File latestFile = new File(current, TIME_TABLE_LATEST_FILE_NAME);
                            if (!latestFile.isFile() || !latestFile.canRead()) {
                                Log.d(LOG_TAG, latestFile + "が読み込めない。\t" + stopWatch.lap() + "ms");
                                continue;
                            }
                            final @Nullable String latestJsonString = MyApp.readTextFile(latestFile);
                            if (latestJsonString == null)
                                throw new IOException("アプリ内ストレージ" + TIME_TABLE_LATEST_FILE_NAME + "を読み込めなかった。");
                            final @NonNull TimeTableLatestData latest = new TimeTableLatestData(latestJsonString);

                            //latest.jsonのfileListが全て存在すれば完全な時刻表とみなす
                            boolean allFound = true;
                            final @NonNull TimeTableData ttData = new TimeTableData();
                            for (String fileName : latest.fileList) {
                                final @NonNull File file = new File(current, fileName);
                                if (!latestFile.isFile() || !latestFile.canRead()) {
                                    allFound = false;
                                    Log.d(LOG_TAG, current + "にfileNameが存在しなかった。\t" + stopWatch.lap() + "ms");
                                    break;
                                }
                                Log.d(LOG_TAG, "アプリ内ストレージ読み込み\t" + file + "\t" + stopWatch.lap() + "ms");
                                final @Nullable String jsonString = MyApp.readTextFile(file);
                                if (jsonString == null)
                                    throw new IOException(file + "が正しく読み込めなかった。");

                                if (fileName.endsWith("All.json")) {
                                    ttData.addAll(new TimeTableData(jsonString));
                                } else {
                                    Log.e(LOG_TAG, "アプリ内ストレージ(" + file + ")が不明。" + stopWatch.lap() + "ms");
                                }
                            }
                            //構築に失敗
                            if (!allFound) {
                                Log.d(LOG_TAG, current + "に必要なファイルが存在しなかった。 resume next\t" + stopWatch.lap() + "ms");
                                continue;
                            }

                            //時刻表データを読み込みTimeTableDataを構築する
                            Log.d(LOG_TAG, current + "の読み込みが正常に終了したのでTimeTableDataを確定。\t" + stopWatch.lap() + "ms");
                            livePopTimeTableData.postValue(new TimeTableTuple(latest, ttData));
                            livePopNaikouMixData.postValue(convertNaikouMix(ttData));

                            break;
                        } catch (Exception e) {
                            Log.e(LOG_TAG, current + "の読み込みに失敗。resume next。\t" + stopWatch.lap() + "ms", e);
                        }
                    }
                }
                Log.d(LOG_TAG, "サーバとの同期を開始");
                //適切な頻度でサーバへアクセスする
                final @NonNull Long lastAccessMillis = P.LAST_SERVER_ACCESS_TIME_MILLIS_FOR_TIME_TABLE.get(MyApp.app);
                final long subMillis = MathUtil.between(0, System.currentTimeMillis() - lastAccessMillis, Long.MAX_VALUE);
                final int subHours = (int) (subMillis / 60 / 60 / 1000); //整数型の割り算は少数部切り捨て
                final int HOUR_LIMIT = 96;
                if(subHours < HOUR_LIMIT){
                    Log.d(LOG_TAG, "前回から"+HOUR_LIMIT+"時間経過していないため同期を中止 経過"+subHours+"hours");
                }else {
                    //サーバアクセス時刻を更新
                    P.LAST_SERVER_ACCESS_TIME_MILLIS_FOR_TIME_TABLE.set(MyApp.app, System.currentTimeMillis());

                    try {
                        Log.d(LOG_TAG, "サーバからlatest.jsonの読込を開始");
                        final @Nullable TimeTableLatestData serverLatest = readServerLatest();
                        if (serverLatest == null) {
                            Log.w(LOG_TAG, "サーバに接続できないかTimeTableLatestDataの構築に失敗");
                        } else {
                            Log.d(LOG_TAG, "アプリ内ストレージにダウンロード済みかどうか確認を開始");
                            final boolean checkInAppTTDataComplete = checkInAppTTDataComplete(serverLatest);
                            if (checkInAppTTDataComplete) {
                                Log.d(LOG_TAG, "サーバとアプリ内ストレージの中身が一致したためサーバ同期を中止");
                            } else {
                                Log.d(LOG_TAG, "サーバから" + serverLatest.number + " " + Arrays.toString(serverLatest.fileList) + "をダウンロードし、アプリ内ストレージへ保存");
                                final @NonNull TimeTableData serverTTData = syncServerToInApp(serverLatest);

                                Log.d(LOG_TAG, "全件ダウンロード保存が成功したため、LiveDataのTimeTableインスタンスを差し替える");
                                livePopTimeTableData.postValue(new TimeTableTuple(serverLatest, serverTTData));
                                livePopNaikouMixData.postValue(convertNaikouMix(serverTTData));

                                Log.d(LOG_TAG, "サーバとの同期に成功");
                            }

                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "サーバ同期に失敗", e);
                    }

                }

                //TODO: タスクの結果
                // サーバからファイルを読み込んだかどうか
                // 読み込んだファイルの日時や件数
                // スキップ件数
                // assetから読み込んだか
                // タスク実行時間

                Log.d(LOG_TAG, "タスク完了");
            } finally {
                executor.submit(this::updateTaskStatus); //currentTaskを完了させてからステータス更新、ここでのsubmitなら確定で次に処理されるはず
                isRunning = false;
            }
        });
    }

    /** サーバのlatest.jsonを取得する */
    private @Nullable TimeTableLatestData readServerLatest(){
        try {
            final @NonNull byte[] response = MyApp.readServerAsset(ASSET_TIME_TABLE_ROOT + "/" + TIME_TABLE_LATEST_FILE_NAME);
            final @NonNull String jsonString = new String(response, StandardCharsets.UTF_8);
            return new TimeTableLatestData(jsonString);
        } catch (Exception e) {
            Log.w(LOG_TAG, "サーバから"+TIME_TABLE_LATEST_FILE_NAME+"の読み込みに失敗", e);
            return null;
        }
    }

    /** アプリ内ストレージにlatestで示されたファイル群が存在するかチェック */
    private boolean checkInAppTTDataComplete(TimeTableLatestData serverLatest) {

        try {
            final @NonNull File internalRootDir = new File(MyApp.app.getFilesDir(), ASSET_TIME_TABLE_ROOT);
            if(!internalRootDir.exists()){
                return false; //ディレクトリがない
            }

            final @NonNull File internalSameDir = new File(internalRootDir, String.valueOf(serverLatest.number));
            if(!internalSameDir.exists()){
                return false; //ディレクトリがない
            }

            //latest.jsonの一致確認
            final @NonNull File internalSameFile = new File(internalRootDir, TIME_TABLE_LATEST_FILE_NAME);
            if (!internalSameFile.isFile() || !internalSameFile.canRead()) {
                Log.d(LOG_TAG, "アプリ内ストレージ" + internalSameFile + "が存在しないかファイルじゃないか読み込めない。");
                return false;
            }
            final @Nullable String internalLatestJsonString = MyApp.readTextFile(internalSameFile);
            if (internalLatestJsonString == null) {
                Log.w(LOG_TAG, "アプリ内ストレージ" + TIME_TABLE_LATEST_FILE_NAME + "を読み込めなかった。");
                return false;
            }
            final @NonNull TimeTableLatestData internalLatest = new TimeTableLatestData(internalLatestJsonString);
            if(!serverLatest.equals(internalLatest)){
                Log.w(LOG_TAG, "アプリ内ストレージ" + TIME_TABLE_LATEST_FILE_NAME + "とserverLatestが一致しない。"); //保存時に中止されたか、同じnumberで違うファイルが存在
                return false;
            }

            //fileListの存在確認
            for(@NonNull String file: serverLatest.fileList){
                final @NonNull File testFile = new File(internalRootDir, file);
                if (!testFile.isFile() || !testFile.canRead()) {
                    Log.d(LOG_TAG, "アプリ内ストレージ" + testFile + "が存在しないかファイルじゃないか読み込めない。");
                    return false;
                }
            }

            //all green
            return true;

        }catch (Exception e){
            Log.e(LOG_TAG, "アプリ内ストレージの時刻表データセットが完全か確認中に例外", e);
            return false;
        }

    }

    /** 取得済みのlatest.jsonを元にサーバから必要ファイルをダウンロードし、latest.jsonを含めアプリ内ストレージへ書き込む
     * @return fileListから構築された時刻表データ */
    private @NonNull TimeTableData syncServerToInApp(TimeTableLatestData serverLatest) throws Exception {
        //一つでも失敗したら処理は中断される

        final @NonNull TimeTableData ret = new TimeTableData();

        final @NonNull File internalRootDir = FileUtil.mkDir(MyApp.app.getFilesDir(), ASSET_TIME_TABLE_ROOT);
        final @NonNull File internalNumberDir = FileUtil.mkDir(internalRootDir, String.valueOf(serverLatest.number));

        //Latest保存
        final @NonNull File internalLatestFile = new File(internalNumberDir, TIME_TABLE_LATEST_FILE_NAME);
        Log.d(LOG_TAG, serverLatest.number+" "+ TIME_TABLE_LATEST_FILE_NAME + "をアプリ内ストレージへ保存");
        MyApp.writeTextFile(internalLatestFile, serverLatest.rawJSONString);

        for (String file : serverLatest.fileList) {
            Log.d(LOG_TAG, file + "をDL");
            final @NonNull byte[] response = MyApp.readServerAsset(ASSET_TIME_TABLE_ROOT + "/" + file);
            final @NonNull String jsonString = new String(response, StandardCharsets.UTF_8);
            final @NonNull TimeTableData ttData = new TimeTableData(jsonString);

            Log.d(LOG_TAG, file + " "+ttData.size()+"件をTimeTableDataコレクションへ追加");
            ret.addAll(ttData);

            Log.d(LOG_TAG, file + "をアプリ内ストレージへ保存");
            final @NonNull File internalJsonFile = new File(internalNumberDir, file);
            MyApp.writeTextFile(internalJsonFile, jsonString);

        }

        Log.d(LOG_TAG, "全件をアプリ内ストレージへ保存成功 ttAll.size="+ret.size());

        return ret;
    }

    public enum TASK_STATUS {
        NO_TASK, CANCELLED, DONE, RUNNING, EXCEPTION
    }

    public final @NonNull MutableLivePopulate<TASK_STATUS> livePopTaskStatus = new MutableLivePopulate<>();
    protected @NonNull TASK_STATUS livePopTaskStatus_PostValue(@NonNull TASK_STATUS status){
        livePopTaskStatus.postValueIfChange(status);
        return status;
    }

    public final void updateTaskStatus(){
        getTaskStatus();
    }

    // タスクの状態取得
    @AnyThread
    public synchronized @NonNull TASK_STATUS getTaskStatus() {
        if (currentTask == null) return livePopTaskStatus_PostValue(TASK_STATUS.NO_TASK);
        if (currentTask.isCancelled()) return livePopTaskStatus_PostValue(TASK_STATUS.CANCELLED);
        if (currentTask.isDone()) {
            try {
                currentTask.get();
                return livePopTaskStatus_PostValue(TASK_STATUS.DONE);
            } catch (ExecutionException e) {
                return livePopTaskStatus_PostValue(TASK_STATUS.EXCEPTION);
            } catch (InterruptedException e) {
                Log.w(LOG_TAG, "タスクが割り込まれました", e);
            }
        }
        return livePopTaskStatus_PostValue(TASK_STATUS.RUNNING);
    }

    // タスクのキャンセル
    @AnyThread
    public synchronized void cancelTask() {
        if (currentTask != null && !currentTask.isDone()) {
            Log.d(LOG_TAG, "タスクをキャンセルします");
            currentTask.cancel(true);
        } else {
            Log.d(LOG_TAG, "キャンセルするタスクがありません");
        }
        updateTaskStatus();
    }

}
