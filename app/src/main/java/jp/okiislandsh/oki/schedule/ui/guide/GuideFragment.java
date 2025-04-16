package jp.okiislandsh.oki.schedule.ui.guide;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import jp.okiislandsh.library.android.AbsBaseFragment;
import jp.okiislandsh.oki.schedule.R;
import jp.okiislandsh.oki.schedule.databinding.FragmentGuideBinding;

public class GuideFragment extends AbsBaseFragment {

    private GuideViewModel vm;
    private FragmentGuideBinding bind;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        //ViewModelインスタンス化、owner引数=thisはFragmentActivityまたはFragmentを設定する
        vm = new ViewModelProvider(this).get(GuideViewModel.class);

        bind = FragmentGuideBinding.inflate(inflater, container, false);

        //ボタンクリックイベント
        bind.btnMap.setOnClickListener(v -> vm.mButton.postValue(GuideViewModel.BUTTON.MAP));
        bind.btnGuideOkikisen.setOnClickListener(v -> vm.mButton.postValue(GuideViewModel.BUTTON.GUIDE_OKIKISEN));
        bind.btnGuideNaikousen.setOnClickListener(v -> vm.mButton.postValue(GuideViewModel.BUTTON.GUIDE_NAIKOSEN));

        //ボタンに応じてコンテナのViewを入れ替え、ボタンのアクティブ状態を反映
        vm.mButton.observe(getViewLifecycleOwner(), button -> {

            //ボタンの色
            final int colorActiveBack = getResources().getColor(R.color.timeTableButtonActiveBack);
            final int colorActiveText = getResources().getColor(R.color.timeTableButtonActiveText);
            final int colorNormalBack = getResources().getColor(R.color.timeTableButtonBack);
            final int colorNormalText = getResources().getColor(R.color.timeTableButtonText);

            //アクティブボタン色
            final boolean isMap = button == GuideViewModel.BUTTON.MAP;
            final boolean isGuideOkikisen = button == GuideViewModel.BUTTON.GUIDE_OKIKISEN;
            final boolean isGuideNaikosen = button == GuideViewModel.BUTTON.GUIDE_NAIKOSEN;
            bind.btnMap.setBackgroundColor(isMap ? colorActiveBack : colorNormalBack);
            bind.btnMap.setTextColor(isMap ? colorActiveText : colorNormalText);
            bind.btnGuideOkikisen.setBackgroundColor(isGuideOkikisen ? colorActiveBack : colorNormalBack);
            bind.btnGuideOkikisen.setTextColor(isGuideOkikisen ? colorActiveText : colorNormalText);
            bind.btnGuideNaikousen.setBackgroundColor(isGuideNaikosen ? colorActiveBack : colorNormalBack);
            bind.btnGuideNaikousen.setTextColor(isGuideNaikosen ? colorActiveText : colorNormalText);

            //実行
            bind.frameLayout.removeAllViews();
            switch (button){
                case MAP:
                    final @NonNull AllMapView allMapView = new AllMapView(requireContext());
                    allMapView.setLayoutParams(newFrameParamsMM(null));
                    bind.frameLayout.addView(allMapView);
                    break;
                case GUIDE_OKIKISEN:
                    final @NonNull GuideOkikisenView guideOkikisenView = new GuideOkikisenView(requireContext());
                    guideOkikisenView.setLayoutParams(newFrameParamsMM(null));
                    bind.frameLayout.addView(guideOkikisenView);
                    break;
                case GUIDE_NAIKOSEN:
                    final @NonNull GuideNaikousenView guideNaikousenView = new GuideNaikousenView(requireContext());
                    guideNaikousenView.setLayoutParams(newFrameParamsMM(null));
                    bind.frameLayout.addView(guideNaikousenView);
                    break;
            }

        });

        return bind.getRoot();

    }

    @Override
    public void onDestroyView() {
        //ビューモデルのクリア、フラグメントの流儀
        vm = null;
        bind = null;

        super.onDestroyView();
    }
}