package jp.okiislandsh.oki.schedule.ui.guide;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import jp.okiislandsh.library.android.MyUtil;
import jp.okiislandsh.library.android.live.MutableNonNullLiveData;

public class GuideViewModel extends ViewModel {

    /** どのボタンが選択されたか(ラジオボタン) */
    public enum BUTTON implements Parcelable {
        MAP, GUIDE_OKIKISEN, GUIDE_NAIKOSEN;

        @Override
        public int describeContents() {
            return 0;
        }
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(name());
        }
        public static final Creator<BUTTON> CREATOR = new Creator<BUTTON>() {
            @Override
            public BUTTON createFromParcel(Parcel source) {
                return BUTTON.valueOf(source.readString());
            }
            @Override
            public BUTTON[] newArray(int size) {
                return new BUTTON[size];
            }
        };

    }

    public final @NonNull MutableNonNullLiveData<BUTTON> mButton;

    public GuideViewModel(@NonNull SavedStateHandle state) {
        mButton = new MutableNonNullLiveData<>(MyUtil.nvl(state.get("mButton"), BUTTON.MAP));
    }

}