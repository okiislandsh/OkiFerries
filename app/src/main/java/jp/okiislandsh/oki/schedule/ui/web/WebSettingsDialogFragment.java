package jp.okiislandsh.oki.schedule.ui.web;

import static jp.okiislandsh.library.android.MyUtil.BR;
import static jp.okiislandsh.library.core.Function.with;
import static jp.okiislandsh.library.core.MyUtil.isJa;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import jp.okiislandsh.library.android.drawable.BorderDrawable;
import jp.okiislandsh.library.android.preference.ButtonPreference;
import jp.okiislandsh.library.android.preference.PreferenceFragmentForCustomDialog;
import jp.okiislandsh.library.android.view.ViewBuilderFunction;
import jp.okiislandsh.library.core.Function;
import jp.okiislandsh.library.core.Pairs;
import jp.okiislandsh.oki.schedule.databinding.WebSettingsDialogFragmentBinding;
import jp.okiislandsh.oki.schedule.util.P;
import jp.okiislandsh.oki.schedule.util.P.URLPreference.TYPE;

/** PreferenceFragmentをダイアログの中身として表示するDialogFragment */
public class WebSettingsDialogFragment extends DialogFragment implements ViewBuilderFunction {
    //いろいろ削ぎ落してonCreateViewでインフレート方式になった
    //試していないが、AlertDialogのsetViewにFragmentContainerViewを突っ込む方法が考えられる

    @Override
    public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final @NonNull WebSettingsDialogFragmentBinding bind = WebSettingsDialogFragmentBinding.inflate(inflater, container, container!=null);

        bind.buttonContainer.setBackground(new BorderDrawable(0x44FFFFFF & getColorFromAttr(android.R.attr.colorPrimary), BorderDrawable.BORDER.TOP));

        //Cancelボタン
        bind.buttonContainer.addView(with(newButton(jp.okiislandsh.library.android.R.string.close, newParams0W(1f), v -> dismiss()), button ->{
            button.setAllCaps(false);
            button.setBackground(newPressColorDrawable());
        }));

        return bind.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        //ダイアログ、表示中だけ取得できるっぽい
        final @Nullable Dialog dialog = getDialog();
        if(dialog!=null) {
            //タイトル、テーマとかで消せそう
            dialog.setTitle("URL Settings");
            /* //余白
            with(dialog.getWindow(), window ->{
                //window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); 全画面にする設定
                //window.setLayout(MATCH_PARENT, MATCH_PARENT); ダイアログの大きさを最大にする設定
            });*/
        }
    }

    public static class MyPreferenceFragment extends PreferenceFragmentForCustomDialog {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            //setPreferencesFromResource(R.xml.preferences, rootKey); XMLからinflateする場合
            //設定画面をインフレート
            setPreferenceScreen(with(
                    getPreferenceManager().createPreferenceScreen(requireContext()),
                    screen->{
                        final @NonNull Function.voidNonNull<TYPE> f = type-> {
                            final @NonNull String title = type.name();
                            screen.addPreference(new ButtonPreference(
                                    requireContext(), title, () -> title, null, ()->"Edit", () -> true,
                                    (that, button) -> showURLSettingEditDialog(requireContext(), title, type)
                            ));
                        };
                        for(@NonNull TYPE type: TYPE.values()) f.run(type);
                    }
            ));
        }

        private void showURLSettingEditDialog(@NonNull Context context, @NonNull String title, @NonNull TYPE type){
            final @NonNull EditText labelEdit = newEdit(P.URL_SETTINGS.getLabel(context, type), newParams0W(1f));
            labelEdit.setHint("Label");
            final @NonNull Button labelSaveButton = newTextStyleButton("Label Save", newParamsWW(0f), v -> {
                try {
                    final @NonNull String input = labelEdit.getText().toString().trim();
                    P.URL_SETTINGS.setLabel(context, type, input.isEmpty() ? null : input);
                    showToastS("Saved");
                } catch (Exception e) {
                    showToastL("Error", e);
                }
            });
            final @Nullable String urlPref = P.URL_SETTINGS.getURL(context, type);
            final @NonNull EditText urlEdit = newEdit(urlPref==null ? getString(type.resID) : urlPref, newParams0W(1f));
            final @NonNull String robotHint = isJa("URLに 翻訳対象URL のプレースホルダとして\"%s\"を含む必要があります。", "URL require \"%s\". That is place holder.");
            urlEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
            urlEdit.setHint(type==TYPE.BUTTON_SMALL_ROBOT ? robotHint : "URL");

            final @NonNull Button urlSaveButton = newTextStyleButton("URL Save", newParamsWW(0f), v -> {
                try {
                    final @NonNull String input = urlEdit.getText().toString().trim();
                    P.URL_SETTINGS.setURL(context, type, input.isEmpty() ? null : input);
                    showToastS("Saved");
                } catch (Exception e) {
                    showToastL("Error", e);
                }
            });
            showDialog(title, isJa("設定変更は、再起動後に有効になります。", "The configuration change takes effect after a reboot.") + (type==TYPE.BUTTON_SMALL_ROBOT ? (BR+robotHint) : ""),
                    newLinearLayout(LinearLayout.VERTICAL, newParamsMM(),
                            newLinearLayout(LinearLayout.HORIZONTAL, newParamsMW(), labelEdit, labelSaveButton),
                            newLinearLayout(LinearLayout.HORIZONTAL, newParamsMW(), urlEdit, urlSaveButton)
                    ),
                    null,
                    new Pairs.Immutable._2<>("Reset", (dialog, which) -> {
                        try {
                            P.URL_SETTINGS.setURLAndLabel(context, type, null);
                            showToastS("Reset");
                        } catch (Exception e) {
                            showToastL("Error", e);
                        }
                        dialog.dismiss();
                    }),
                    new Pairs.Immutable._2<>(getString(jp.okiislandsh.library.android.R.string.close), (dialog, which) -> dialog.dismiss()),
                    null, true, false);
        }

    }

}
