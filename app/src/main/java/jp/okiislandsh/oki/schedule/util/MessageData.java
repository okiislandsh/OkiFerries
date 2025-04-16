package jp.okiislandsh.oki.schedule.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jp.okiislandsh.library.android.LogDB;
import jp.okiislandsh.library.core.YMDInt;

/** message/message.json */
public class MessageData {

    private static final @NonNull LogDB.ILog<CharSequence> Log = LogDB.getStringInstance();

    private static final @NonNull String  KEY_NUMBER = "number";
    private static final @NonNull String  KEY_MESSAGES = "messages";

    private static final @NonNull String  KEY_YMD = "ymd";
    private static final @NonNull String  KEY_JA = "ja";
    private static final @NonNull String  KEY_EN = "en";

    /** 日付、メッセージのセット */
    public static class Parts implements Comparable<Parts> {
        public @NonNull final YMDInt ymd;
        public @NonNull final String ja;
        public @NonNull final String en;
        public Parts(@NonNull String ymd, @NonNull String ja, @NonNull String en) {
            this(new YMDInt(ymd), ja, en);
        }
        public Parts(@NonNull YMDInt ymd, @NonNull String ja, @NonNull String en) {
            this.ymd = ymd;
            this.ja = ja;
            this.en = en;
        }
        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof Parts &&
                    Objects.equals(this.ymd, ((Parts) obj).ymd) &&
                    Objects.equals(this.ja, ((Parts) obj).ja) &&
                    Objects.equals(this.en, ((Parts) obj).en);
        }
        @Override
        public int hashCode() {
            return ymd.hashCode() | ja.hashCode() | en.hashCode();
        }
        @Override
        public @NonNull String toString() {
            return ymd + "\t" + ja + "\t" + en;
        }

        @Override
        public int compareTo(@NonNull Parts that) {
            return ymd.compareTo(that.ymd);
        }

    }

    public final @NonNull String rawJSONString;
    public final @NonNull Long number;
    public final @NonNull List<Parts> messages;

    public MessageData(@NonNull String jsonString) throws Exception {
        rawJSONString = jsonString;

        final @NonNull JSONObject json = new JSONObject(jsonString);

        number = json.getLong(KEY_NUMBER);
        messages = new ArrayList<>();

        final @NonNull JSONArray messagesJSON = json.getJSONArray(KEY_MESSAGES);
        for (int i = 0; i < messagesJSON.length(); i++) {
            try {
                final @NonNull JSONObject partsJSON = messagesJSON.getJSONObject(i);
                final @NonNull Parts parts = new Parts(partsJSON.getString(KEY_YMD), partsJSON.getString(KEY_JA), partsJSON.getString(KEY_EN));
                messages.add(parts);
            }catch (Exception e){
                Log.e("on error resume. index="+i, e);
            }
        }
        Collections.sort(messages, Collections.reverseOrder()); //逆順ソート
    }

}
