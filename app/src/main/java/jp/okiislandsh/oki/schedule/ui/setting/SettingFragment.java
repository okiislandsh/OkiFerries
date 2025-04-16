package jp.okiislandsh.oki.schedule.ui.setting;

import android.os.Bundle;

import jp.okiislandsh.library.android.preference.PreferenceFragmentForCustomDialog;
import jp.okiislandsh.oki.schedule.util.P;

public class SettingFragment extends PreferenceFragmentForCustomDialog {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        //setPreferencesFromResource(R.xml.preferences, rootKey); XMLからinflateする場合
        setPreferenceScreen(
                P.inflatePreference(this, requireContext(),
                        getPreferenceManager().createPreferenceScreen(requireContext())
                )
        );
    }

}