package jp.okiislandsh.oki.schedule.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

/** timetable/latest.json */
public class TimeTableLatestData {
    public final @NonNull String rawJSONString;
    public final @NonNull Long number;
    public final @NonNull String descriptionJa;
    public final @NonNull String descriptionEn;
    public final @NonNull String[] fileList;

    public TimeTableLatestData(@NonNull String jsonString) throws JSONException {
        this.rawJSONString = jsonString;

        final @NonNull JSONObject json = new JSONObject(jsonString);

        this.number = json.getLong("number");
        this.descriptionJa = json.getString("descriptionJa");
        this.descriptionEn = json.getString("descriptionEn");
        this.fileList = getStringArray(json.getJSONArray("fileList"));
    }

    private static @NonNull String[] getStringArray(@NonNull JSONArray json) throws JSONException {
        final @NonNull ArrayList<String> ret = new ArrayList<>();
        for (int i = 0, size = json.length(); i < size; i++) ret.add(json.getString(i));
        return ret.toArray(new String[0]);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof TimeTableLatestData &&
                number.equals(((TimeTableLatestData) obj).number) &&
                descriptionJa.equals(((TimeTableLatestData) obj).descriptionJa) &&
                descriptionEn.equals(((TimeTableLatestData) obj).descriptionEn) &&
                Arrays.equals(fileList, ((TimeTableLatestData) obj).fileList);
    }

    @Override
    public int hashCode() {
        return number.hashCode();
    }

    @Override
    public @NonNull String toString() {
        return rawJSONString;
    }
}
