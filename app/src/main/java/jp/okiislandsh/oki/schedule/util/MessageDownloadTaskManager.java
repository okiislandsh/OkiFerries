package jp.okiislandsh.oki.schedule.util;

import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jp.okiislandsh.library.android.live.populate.MutableLivePopulate;
import jp.okiislandsh.library.core.MathUtil;
import jp.okiislandsh.library.core.StopWatch;
import jp.okiislandsh.oki.schedule.MyApp;

/** メッセージのダウンロード処理をシングルトン管理 */
public class MessageDownloadTaskManager {
    private static final @NonNull String LOG_TAG = "MSGTaskManager";
    private static final @NonNull String ASSET_MESSAGE_ROOT = "message";
    private static final @NonNull String INTERNAL_MESSAGE_ROOT = ASSET_MESSAGE_ROOT;

    private static final @NonNull MessageDownloadTaskManager INSTANCE = new MessageDownloadTaskManager(); // シングルトンインスタンス
    private final @NonNull ExecutorService executor = Executors.newSingleThreadExecutor();
    private @Nullable Future<?> currentTask;
    private volatile boolean isRunning = false;

    public final @NonNull MutableLivePopulate<MessageData> livePopMessageData = new MutableLivePopulate<>();

    private MessageDownloadTaskManager() {} // コンストラクタを private にして外部からのインスタンス化を禁止

    public static MessageDownloadTaskManager getInstance() {
        return INSTANCE;
    }

    private static final @NonNull String MESSAGE_FILE_NAME = "message.json";

    private static @Nullable String parseAssetMessageJSON(){
        final @NonNull StopWatch stopWatch = new StopWatch();
        Log.d(LOG_TAG, "Assetのmessage.json読み込み開始。");
        try (InputStream is = MyApp.app.getAssets().open(ASSET_MESSAGE_ROOT +File.separator+ MESSAGE_FILE_NAME);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            final @NonNull byte[] buffer = new byte[1024*8];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }

            final @NonNull String jsonString = bos.toString(StandardCharsets.UTF_8.name());
            Log.d(LOG_TAG, "Assetのmessage.json読み込み完了。" + stopWatch.lap() + "ms");
            return jsonString;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Assetのmessage.json読み込みに失敗。" + stopWatch.lap() + "ms", e);
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

                Log.d(LOG_TAG, "タスク開始");

                //assetとアプリ内ストレージを比較
                final @Nullable MessageData assetMsg = readAssetMsg();
                final @Nullable MessageData inAppMsg = readInAppMsg();
                final @Nullable MessageData postMsg;
                if (assetMsg != null && (inAppMsg == null || inAppMsg.number < assetMsg.number)) {
                    //assetのほうが新しいメッセージのため、アプリ内ストレージへ書込み
                    writeInAppMsg(assetMsg);
                    postMsg = assetMsg;
                }else{
                    //アプリ内ストレージの方が新しいメッセージかも
                    postMsg = inAppMsg;
                }
                if(postMsg==null) Log.e(LOG_TAG, "MessageDataについて、assetもアプリ内ストレージも読み込めなかった。");
                livePopMessageData.postValue(postMsg); //暫定Post

                //サーバから取得を試みる
                Log.d(LOG_TAG, "サーバから取得を開始");
                //適切な頻度でサーバへアクセスする、
                final @NonNull Long lastAccessMillis = P.LAST_SERVER_ACCESS_TIME_MILLIS_FOR_MESSAGE.get(MyApp.app);
                final long subMillis = MathUtil.between(0, System.currentTimeMillis() - lastAccessMillis, Long.MAX_VALUE);
                final int subHours = (int) (subMillis / 60 / 60 / 1000); //整数型の割り算は少数部切り捨て
                final int HOUR_LIMIT = 24;
                if(subHours < HOUR_LIMIT){
                    Log.d(LOG_TAG, "前回から"+HOUR_LIMIT+"時間経過していないためサーバからの取得を中止 経過"+subHours+"hours");
                }else {
                    //サーバアクセス時刻を更新
                    P.LAST_SERVER_ACCESS_TIME_MILLIS_FOR_MESSAGE.set(MyApp.app, System.currentTimeMillis());

                    final @Nullable MessageData serverMsg = readServerMsg();
                    if (serverMsg == null) {
                        Log.d(LOG_TAG, "サーバからMessageDataを取得できなかった");
                    } else {
                        if (postMsg == null || (postMsg.number < serverMsg.number)) { //ローカルの読み込みに失敗しているか、サーバの方が新しいメッセージ
                            writeInAppMsg(serverMsg);
                            Log.d(LOG_TAG, "サーバからMessageData("+serverMsg.number+")を取得し、アプリ内ストレージへコピーが完了した");
                            livePopMessageData.postValue(serverMsg); //確定Post
                        } else {
                            Log.d(LOG_TAG, "サーバからMessageData("+serverMsg.number+")をダウンロードしたが、アプリ内の方("+postMsg.number+")が同じか新しい");
                        }
                    }
                }

                Log.d(LOG_TAG, "タスク完了");
            } finally {
                executor.submit(this::updateTaskStatus); //currentTaskを完了させてからステータス更新、ここでのsubmitなら確定で次に処理されるはず
                isRunning = false;
            }
        });
    }

    /** AssetからMessageDataを読み込む */
    private @Nullable MessageData readAssetMsg() {
        try {
            Log.d(LOG_TAG, "Assetの"+MESSAGE_FILE_NAME+"を読込開始。");
            final @Nullable String messageJsonString = parseAssetMessageJSON();
            if (messageJsonString == null) throw new IOException(ASSET_MESSAGE_ROOT + File.separator + MESSAGE_FILE_NAME + "を読み込めなかった。");
            final @NonNull MessageData messageData = new MessageData(messageJsonString);
            Log.d(LOG_TAG, "Assetの"+MESSAGE_FILE_NAME+"を読込完了。");
            return messageData;
        }catch (Exception e){
            Log.e(LOG_TAG, "Assetの"+MESSAGE_FILE_NAME+"を読込失敗。", e);
            return null;
        }
    }

    /** アプリ内ストレージからMessageDataを読み込む */
    private @Nullable MessageData readInAppMsg() {
        try {
            Log.d(LOG_TAG, "アプリ内ストレージの"+MESSAGE_FILE_NAME+"を読込開始。");
            final @NonNull File internalRootDir = new File(MyApp.app.getFilesDir(), ASSET_MESSAGE_ROOT);
            final @NonNull File messageFile = new File(internalRootDir, MESSAGE_FILE_NAME);
            final @Nullable String messageJsonString = MyApp.readTextFile(messageFile);
            if (messageJsonString == null) throw new IOException(messageFile + "を読み込めなかった。");
            final @NonNull MessageData messageData = new MessageData(messageJsonString);
            Log.d(LOG_TAG, "アプリ内ストレージの"+MESSAGE_FILE_NAME+"を読込完了。");
            return messageData;
        }catch (Exception e){
            Log.e(LOG_TAG, "アプリ内ストレージの"+MESSAGE_FILE_NAME+"を読込失敗。", e);
            return null;
        }
    }

    /** アプリ内ストレージへMessageDataを書き込む */
    private static void writeInAppMsg(@NonNull MessageData newMsg) {
        Log.d(LOG_TAG, "アプリ内ストレージの"+MESSAGE_FILE_NAME+"を書込み開始。");
        final @NonNull File internalRootDir = new File(MyApp.app.getFilesDir(), INTERNAL_MESSAGE_ROOT);
        try {
            if (!internalRootDir.exists() && !internalRootDir.mkdir()) {
                Log.e(LOG_TAG, "アプリ内ストレージの"+internalRootDir+"のmkdirに失敗。");
                return;
            }
        }catch (Exception e){
            Log.e(LOG_TAG, "アプリ内ストレージの"+internalRootDir+"のmkdirに失敗。", e);
            return;
        }

        final @NonNull File messageFile = new File(internalRootDir, MESSAGE_FILE_NAME);
        if(MyApp.writeTextFile(messageFile, newMsg.rawJSONString)){
            Log.d(LOG_TAG, "アプリ内ストレージの"+MESSAGE_FILE_NAME+"を書込み完了。");
        }else{
            Log.e(LOG_TAG, "アプリ内ストレージの"+MESSAGE_FILE_NAME+"へ書込み失敗。");
        }

    }

    /** サーバからMessageDataを読み込む */
    private @Nullable MessageData readServerMsg() {
        try {
            final @NonNull byte[] response = MyApp.readServerAsset(ASSET_MESSAGE_ROOT + "/" + MESSAGE_FILE_NAME);
            final @NonNull String jsonString = new String(response, StandardCharsets.UTF_8);
            return new MessageData(jsonString);
        } catch (Exception e) {
            Log.w(LOG_TAG, "サーバから"+MESSAGE_FILE_NAME+"の読み込みに失敗", e);
            return null;
        }
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
