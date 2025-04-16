package jp.okiislandsh.oki.schedule.util;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.Serializable;

import jp.okiislandsh.library.android.LogDB;

public class PortsBool implements Parcelable, Serializable {
    private static final @NonNull LogDB.ILog<CharSequence> Log = LogDB.getStringInstance();

    public boolean chibu = false;
    public boolean ama = false;
    public boolean nishinoshima = false;
    public boolean shichirui = false;
    public boolean sakaiminato = false;
    public boolean dogo = false;

    private static final short GET_SERIAL_RANGE = 4096;
    /** parse復元不可能なクラス構造の変更がある場合カウントアップすること */
    private static final long serialVersionUID = 1;

    public int toSerialize(){
        return (chibu?1:0) + (ama?2:0) + (nishinoshima?4:0) + (shichirui?8:0) + (sakaiminato?16:0) + (dogo?32:0) + ((short)serialVersionUID*GET_SERIAL_RANGE);
    }
    public static @NonNull PortsBool parse(int serialValue){
        final @NonNull PortsBool ret = new PortsBool();
        if(serialValue/ GET_SERIAL_RANGE == serialVersionUID){
            ret.chibu = (serialValue&1)==1;
            ret.ama = (serialValue&2)==2;
            ret.nishinoshima = (serialValue&4)==4;
            ret.shichirui = (serialValue&8)==8;
            ret.sakaiminato = (serialValue&16)==16;
            ret.dogo = (serialValue&32)==32;
        }else{
            Log.w("バージョンコードが違う。static="+serialVersionUID+", serialValue="+serialValue+", parseID="+(serialValue/GET_SERIAL_RANGE));
        }
        return ret;
    }

    public static @NonNull PortsBool getAllFalseInstance(){
        return new PortsBool();
    }

    @Override
    public @NonNull String toString() {
        return "PortsBool{" +
                "chibu=" + chibu +
                ", ama=" + ama +
                ", nishinoshima=" + nishinoshima +
                ", shichirui=" + shichirui +
                ", sakaiminato=" + sakaiminato +
                ", dogo=" + dogo +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(toSerialize());
    }

    public static final Creator<PortsBool> CREATOR = new Creator<PortsBool>() {
        @Override
        public PortsBool createFromParcel(Parcel source) {
            return parse(source.readInt());
        }
        @Override
        public PortsBool[] newArray(int size) {
            return new PortsBool[size];
        }
    };

}
