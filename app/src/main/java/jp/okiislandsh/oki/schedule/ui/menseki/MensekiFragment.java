package jp.okiislandsh.oki.schedule.ui.menseki;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import jp.okiislandsh.library.android.AbsBaseFragment;
import jp.okiislandsh.library.android.preference.IBooleanPreference;
import jp.okiislandsh.oki.schedule.MainActivity;
import jp.okiislandsh.oki.schedule.R;
import jp.okiislandsh.oki.schedule.databinding.FragmentMensekiBinding;
import jp.okiislandsh.oki.schedule.util.P;

public class MensekiFragment extends AbsBaseFragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        final @NonNull MainActivity activity = (MainActivity) requireActivity();
        final @NonNull IBooleanPreference.NonNullBeans<Boolean> menseki = P.MENSEKI.getBeans(activity);

        final @NonNull FragmentMensekiBinding bind = FragmentMensekiBinding.inflate(inflater, container, false);
        bind.btnOK.setOnClickListener(v -> {
            menseki.set(true);
            activity.loadDestination(R.id.nav_web);
        });

        bind.btnCancel.setOnClickListener(v -> {
            menseki.set(false);
            activity.finish(); //アプリ終了
        });

        return bind.getRoot();

    }

}