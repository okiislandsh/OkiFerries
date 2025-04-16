package jp.okiislandsh.oki.schedule;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import jp.okiislandsh.library.android.live.LiveClock;
import jp.okiislandsh.library.android.live.NonNullLiveConverter;
import jp.okiislandsh.library.core.Function;
import jp.okiislandsh.library.core.HMInt;
import jp.okiislandsh.library.core.Pairs;
import jp.okiislandsh.library.core.YMDInt;
import jp.okiislandsh.oki.schedule.util.MessageData;
import jp.okiislandsh.oki.schedule.util.MessageDownloadTaskManager;
import jp.okiislandsh.oki.schedule.util.TimeTableData;
import jp.okiislandsh.oki.schedule.util.TimeTableDownloadTaskManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MyApp extends Application {

    private static final @NonNull TimeTableDownloadTaskManager timeTableDownloadTaskManager = TimeTableDownloadTaskManager.getInstance();

    public static @NonNull LiveData<TimeTableDownloadTaskManager.TimeTableTuple> newLiveTimeTableData(){
        return timeTableDownloadTaskManager.livePopTimeTableData.newLive();
    }
    public static @Nullable TimeTableDownloadTaskManager.TimeTableTuple getTableDataNow(){
        return timeTableDownloadTaskManager.livePopTimeTableData.newLive().getValue();
    }

    public static @NonNull LiveData<TimeTableData> newLiveNaikouMixData(){
        return timeTableDownloadTaskManager.livePopNaikouMixData.newLive();
    }

    private static final @NonNull MessageDownloadTaskManager messageDownloadTaskManager = MessageDownloadTaskManager.getInstance();

    public static @NonNull LiveData<MessageData> newLiveMessageData(){
        return messageDownloadTaskManager.livePopMessageData.newLive();
    }
    public static @Nullable MessageData getMessageDataNow(){
        return messageDownloadTaskManager.livePopMessageData.newLive().getValue();
    }

    public static MyApp app;

    public MyApp() {
        super();

        app = this;

        timeTableDownloadTaskManager.executeTask();
        messageDownloadTaskManager.executeTask();
    }

    /** LiveClockから点滅用のLiveData生成 */
    public static @NonNull LiveData<Pairs.Immutable.NonNull._3<YMDInt, HMInt, Integer>> newLiveClockToYMDHM(@NonNull LiveClock liveClock){
        final @NonNull Calendar defaultValue = GregorianCalendar.getInstance(TimeZone.getTimeZone("Japan")); //適当
        final @NonNull NonNullLiveConverter<Function.nonnullVoid<Calendar>, Pairs.Immutable.NonNull._3<YMDInt, HMInt, Integer>> liveConverter =
                NonNullLiveConverter.newInstance(new Pairs.Immutable.NonNull._3<>(new YMDInt(defaultValue), new HMInt(defaultValue), 0), fn->{
                    final @NonNull Calendar cal = fn.run();
                    return new Pairs.Immutable.NonNull._3<>(new YMDInt(cal), new HMInt(cal), cal.get(Calendar.SECOND));
                });
        liveConverter.setLiveData(liveClock);
        return liveConverter;
    }

    /** @return 読み込みに失敗した場合、null */
    public static @Nullable String readTextFile(@NonNull File file){
        //final @NonNull StopWatch stopWatch = new StopWatch();
        //Log.d(LOG_TAG, "テキストファイル一括読み込み開始。"+file);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) { //FileReaderのコンストラクタでCharsetを指定できる
            final @NonNull StringBuilder content = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
            final @NonNull String ret = content.toString();
            //Log.d(LOG_TAG, "テキストファイル一括読み込み完了。"+ret.length()+"文字\t" + stopWatch.lap() + "ms");
            return ret;
        } catch (Exception e) {
            //Log.e(LOG_TAG, "テキストファイル一括読み込みに失敗。"+file+"\t" + stopWatch.lap() + "ms", e);
            return null;
        }
    }

    /** @return 書込みに失敗した場合、false */
    public static boolean writeTextFile(@NonNull File file, @NonNull String text){
        try (BufferedWriter br = new BufferedWriter(new FileWriter(file))) { //FileWriterのコンストラクタでCharsetを指定できる
            br.write(text);
            return true;
        } catch (Exception e) {
            Log.e("writeTextFile", "書込み失敗", e);
            return false;
        }
    }

    /** サーバアセット以下のファイルを取得する */
    public static @NonNull byte[]  readServerAsset(@NonNull String pathInAsset) throws Exception{
        final @NonNull String url = app.getString(R.string.url_github_asset, pathInAsset);
        return downloadByteArray(url);
    }

    /** Okhttpを使用したダウンロード */
    public static @NonNull byte[] downloadByteArray(String url) throws Exception {
        Log.d("downloadByteArray", "download "+url);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            try (@Nullable ResponseBody body = response.body()) {

                if (body == null) throw new IOException("ResponseBody is null. HTTPエラー？");

                try (InputStream inputStream = body.byteStream();
                     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                    byte[] buffer = new byte[1024 * 64];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    final @NonNull byte[] ret = outputStream.toByteArray();

                    Log.d("downloadByteArray", "finish "+url);

                    return ret;
                }
            }
        } catch (Exception e) {
            Log.d("downloadByteArray", "finish "+url, e);
            throw e;
        }

    }
}
