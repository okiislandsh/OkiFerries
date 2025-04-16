package jp.okiislandsh.oki.schedule.ui.web;

import static android.widget.LinearLayout.HORIZONTAL;
import static jp.okiislandsh.library.android.MyUtil.BR;
import static jp.okiislandsh.library.core.MyUtil.isJa;
import static jp.okiislandsh.library.core.MyUtil.nvl;
import static jp.okiislandsh.library.core.MyUtil.requireNonNull;
import static jp.okiislandsh.library.core.MyUtil.startsWithAny;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import java.net.MalformedURLException;
import java.net.URL;

import jp.okiislandsh.library.android.AbsBaseFragment;
import jp.okiislandsh.library.android.IntentUtil;
import jp.okiislandsh.library.android.SizeUtil;
import jp.okiislandsh.library.android.drawable.TextDrawable;
import jp.okiislandsh.library.core.RawString;
import jp.okiislandsh.oki.schedule.MainActivity;
import jp.okiislandsh.oki.schedule.R;
import jp.okiislandsh.oki.schedule.databinding.FragmentWebBinding;
import jp.okiislandsh.oki.schedule.ui.MyWebViewClient;
import jp.okiislandsh.oki.schedule.util.MessageData;
import jp.okiislandsh.oki.schedule.util.P;
import jp.okiislandsh.oki.schedule.util.P.URLPreference.TYPE;

public class WebFragment extends AbsBaseFragment {

    private WebViewModel vm;
    private FragmentWebBinding bind;
    /** WebViewの状態の維持に使う、Fragment*/
    private Bundle webViewState;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        //ViewModelインスタンス化、owner引数=thisはFragmentActivityまたはFragmentを設定する
        vm = new ViewModelProvider(this).get(WebViewModel.class);

        bind = FragmentWebBinding.inflate(inflater, container, false);

        return bind.getRoot();

    }

    /**
     * @param type Preference値がnull以外ならStringRes IDを上書きして使用する
     * @param nvlID 基本のラベル
     */
    private @NonNull StateListDrawable newButtonDrawable(@NonNull TYPE type, @StringRes int nvlID){
        final @Nullable String overrideLabel = P.URL_SETTINGS.getLabel(requireContext(), type);
        return newButtonDrawable(overrideLabel==null ? getString(nvlID) : overrideLabel);
    }
    private @NonNull StateListDrawable newButtonDrawable(@StringRes int id){
        return newButtonDrawable(getString(id));
    }
    private @NonNull StateListDrawable newButtonDrawable(@NonNull String label){
        final @NonNull StateListDrawable ret = new StateListDrawable();
        ret.addState(new int[]{-android.R.attr.state_enabled}, newTextDrawable(label, Color.LTGRAY));
        ret.addState(new int[]{}, newTextDrawable(label, Color.BLACK)); //normal
        return ret;
    }
    private @NonNull TextDrawable newTextDrawable(@NonNull String label, @ColorInt int color){
        final @NonNull TextDrawable ret = new TextDrawable(label, 30); //文字サイズ適当、2以下でバグった
        ret.setTextAlign(TextDrawable.Align.CENTER, TextDrawable.VAlign.MIDDLE);
        ret.setColor(color);
        return ret;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //ボタンラベルユーザ設定
        bind.btnOkikankou.setImageDrawable(newButtonDrawable(TYPE.BUTTON_LARGE_K, R.string.button_okikankou));
        bind.btnNaikosen.setImageDrawable(newButtonDrawable(TYPE.BUTTON_LARGE_LEFT, R.string.button_naikosen));
        bind.btnOkikisen.setImageDrawable(newButtonDrawable(TYPE.BUTTON_LARGE_CENTER, R.string.button_okikisen));
        bind.btnKankokyokai.setImageDrawable(newButtonDrawable(TYPE.BUTTON_LARGE_RIGHT, R.string.button_kankokyokai));
        requireNonNull(P.URL_SETTINGS.getLabel(requireContext(), TYPE.BUTTON_SMALL_WEATHER), title->bind.btnWeather.setImageDrawable(new TextDrawable(title, bind.btnChibu.getTextSize())));
        requireNonNull(P.URL_SETTINGS.getLabel(requireContext(), TYPE.BUTTON_SMALL_CHIBU), title->bind.btnChibu.setText(title));
        requireNonNull(P.URL_SETTINGS.getLabel(requireContext(), TYPE.BUTTON_SMALL_AMA), title->bind.btnAma.setText(title));
        requireNonNull(P.URL_SETTINGS.getLabel(requireContext(), TYPE.BUTTON_SMALL_NISHINOSHIMA), title->bind.btnNishinoshima.setText(title));
        requireNonNull(P.URL_SETTINGS.getLabel(requireContext(), TYPE.BUTTON_SMALL_DOGO), title->bind.btnDogo.setText(title));
        requireNonNull(P.URL_SETTINGS.getLabel(requireContext(), TYPE.BUTTON_SMALL_ROBOT), title->bind.btnTranslation.setImageDrawable(new TextDrawable(title, bind.btnChibu.getTextSize())));

        //ボタンクリックイベント
        bind.btnOkikankou.setOnClickListener(v -> vm.mUrl.postValue(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_LARGE_K), getString(R.string.url_okikankou))));
        bind.btnNaikosen.setOnClickListener(v -> vm.mUrl.postValue(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_LARGE_LEFT), getString(R.string.url_naikosen_status))));
        bind.btnOkikisen.setOnClickListener(v -> vm.mUrl.postValue(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_LARGE_CENTER), getString(R.string.url_okikisen))));
        bind.btnKankokyokai.setOnClickListener(v -> vm.mUrl.postValue(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_LARGE_RIGHT), getString(R.string.url_kankokyokai))));
        bind.btnWeather.setOnClickListener(v -> vm.mUrl.postValue(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_SMALL_WEATHER), getString(R.string.url_weather))));
        bind.btnChibu.setOnClickListener(v -> vm.mUrl.postValue(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_SMALL_CHIBU), getString(R.string.url_chibu))));
        bind.btnAma.setOnClickListener(v -> vm.mUrl.postValue(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_SMALL_AMA), getString(R.string.url_ama))));
        bind.btnNishinoshima.setOnClickListener(v -> vm.mUrl.postValue(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_SMALL_NISHINOSHIMA), getString(R.string.url_nishinoshima))));
        bind.btnDogo.setOnClickListener(v -> vm.mUrl.postValue(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_SMALL_DOGO), getString(R.string.url_dogo))));
        bind.btnTranslation.setOnClickListener(v -> {
            final @Nullable String prefURL = P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_SMALL_ROBOT);
            if(prefURL!=null){
                if(prefURL.contains("%s")) {
                    try {
                        final @NonNull String transUrl = String.format(prefURL, bind.webView.getUrl());
                        Log.d("btnTranslation#onClick()\tUser Setting URL\t" + transUrl);
                        vm.mUrl.postValue(transUrl);
                        return;
                    } catch (Exception e) {
                        showToastL("Trans URL is bad." + BR + prefURL, e);
                    }
                    //エラーの時は、通常処理を行う。
                }else{
                    showToastL(isJa("翻訳URLに%sが含まれていません。", "Transfer URL require \"%s\"")+BR+prefURL);
                }
            }
            final @NonNull String transUrl = String.format(getString(R.string.url_translation), bind.webView.getUrl());
            Log.d("btnTranslation#onClick()\t" + transUrl);
            vm.mUrl.postValue(transUrl);
        });
        bind.btnBack.setOnClickListener(v -> {
            if (bind.webView.canGoBack()) bind.webView.goBack();
        });
        bind.btnForward.setOnClickListener(v -> {
            if (bind.webView.canGoForward()) bind.webView.goForward();
        });

        //WebView設定
        //noinspection SetJavaScriptEnabled
        bind.webView.getSettings().setJavaScriptEnabled(true);
        bind.webView.getSettings().setBuiltInZoomControls(true);
        bind.webView.setWebViewClient(new MyWebViewClient(requireContext()){
            @Override
            public boolean shouldOverrideUrlLoading(@NonNull WebView view, @NonNull String url) { //こうしないとリンクをクリックしたとき外部ブラウザが立ち上がる
                Log.d("WebView:shouldOverrideUrlLoading\t" + url);

                //戻る・進むボタンの使用可否
                updateWebControlButton();

                try{
                    if("about:blank".equals(url)){
                        return true;
                    }
                    if (!startsWithAny(url, true, "http", "https")) {
                        Log.d(new RawString("怪しげなURL\t", url));
                        //return false;
                    }

                    new URL(url); //エラーが起きるかどうか実験

                    if(!url.endsWith("pdf")) {
                        vm.mUrl.setValue(url); //何もなければWebView内遷移
                        return false;
                    }
                } catch (MalformedURLException e) {
                    Log.w(new RawString("URLオブジェクト生成に失敗\t", url));
                }

                //URLっぽくないのでIntentで外部アプリ起動試行
                try {
                    final @NonNull Uri uri = Uri.parse(url);
                    final @NonNull Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    if(!IntentUtil.startActivity(requireContext(), intent, url)){
                        showToastL("Can't open."+BR+uri);
                    }
                } catch (Exception e){
                    showToastL("外部アプリ起動に失敗"+BR+url, e);
                }
                return true;

            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                //戻る・進むボタンの使用可否
                updateWebControlButton();
                //ぐるぐる
                bind.progressBar.setVisibility(View.VISIBLE);
                bind.progressBar.setProgress(0);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //戻る・進むボタンの使用可否
                updateWebControlButton();
                //ぐるぐる
                bind.progressBar.setVisibility(View.INVISIBLE);
            }

            void updateWebControlButton(){
                //戻る・進むボタンの使用可否
                bind.btnBack.setEnabled(bind.webView.canGoBack());
                bind.btnForward.setEnabled(bind.webView.canGoForward());
            }

        });

        //キーイベント処理
        bind.webView.setFocusableInTouchMode(true);
        bind.webView.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                if(bind.webView.canGoBack()){
                    bind.webView.goBack();
                    return true;
                }
            }
            return false; //バケット表示中ならシステムにより自動終了
        });

        //WebView復元
        if (webViewState != null) {
            bind.webView.restoreState(webViewState);
            final @Nullable String url = bind.webView.getUrl();
            if(url!=null) vm.mUrl.setValue(bind.webView.getUrl());
        }

        //Url変更処理
        vm.mUrl.observe(getViewLifecycleOwner(), s -> bind.webView.loadUrl(s)); //NonNullLiveData

        //メッセージ変更処理
        vm.mMessage.observe(getViewLifecycleOwner(), msg-> vm.mInvalidateOptionMenuNotifier.postValue(null));
        vm.mLastReadMessageNumber.observe(getViewLifecycleOwner(), timestamp-> vm.mInvalidateOptionMenuNotifier.postValue(null));
        vm.mInvalidateOptionMenuNotifier.observe(getViewLifecycleOwner(), unused-> requireActivity().invalidateOptionsMenu());

        //オプションメニュー
        requireActivity().addMenuProvider(new MenuProvider() {
            //memo: onCreateでmenu.clearし全体のメニューをinflateし、onPrepareでitemを変更するのが基本
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            }
            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                menu.clear();
                MenuProvider.super.onPrepareMenu(menu); //一応
                {
                    //Github移行
                    final @Nullable MessageData msg = vm.mMessage.getValue();
                    if (msg!=null && !msg.messages.isEmpty()) {
                        final @NonNull MenuItem item = menu.add(0, R.string.option_menu_notification, 0, R.string.option_menu_notification);
                        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                        //Menuに必要に応じてバッジをつける
                        final @Nullable Long readNumber = vm.mLastReadMessageNumber.getValue();
                        final boolean newMark = (readNumber==null || readNumber < msg.number);
                        final @NonNull LinearLayout container = newLinearLayout(HORIZONTAL, newParamsWW());
                        container.setOnClickListener(v->onMenuItemSelected(item));
                        final @NonNull ImageView imgIcon = new ImageView(requireContext());
                        final int dp48ToPx = (int)SizeUtil.dp2px(48, requireContext());
                        imgIcon.setLayoutParams(new LinearLayout.LayoutParams(dp48ToPx, dp48ToPx));
                        imgIcon.setImageResource(newMark ? R.drawable.icon_email_new : R.drawable.icon_email);
                        setPadding(imgIcon, (int)SizeUtil.dp2px(8, requireContext()));
                        container.addView(imgIcon);
                        item.setActionView(container);
                    }

                }
                //URL編集
                menu.add(1, R.string.option_menu_url, 1, R.string.option_menu_url);
                //URL開く
                menu.add(1, R.string.option_menu_share_to_browser, 2, R.string.option_menu_share_to_browser);
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                final int itemId = menuItem.getItemId();
                if (itemId == R.string.option_menu_notification) { //Github移行
                    final @Nullable MessageData msg = vm.mMessage.getValue();
                    if(msg==null){
                        showToastS("Missing MessageData.");
                    }else {
                        ((MainActivity) requireActivity()).showMessagesDialog(msg);
                    }
                }else if (itemId == R.string.option_menu_url) { //URL編集
                    new WebSettingsDialogFragment()
                            .show(requireActivity().getSupportFragmentManager(), null);
                    return true;
                }else if (itemId == R.string.option_menu_share_to_browser) { //URL開く
                    final @Nullable String url = bind.webView.getUrl();
                    if(url!=null) {
                        if (!IntentUtil.web(requireContext(), url, null)) {
                            showToastS(isJa("URLを開くアプリが見つかりません。", "No found app."));
                        }
                    }
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED); //FragmentでaddMenuProviderする時の作法

    }

    @Override
    public void onResume() {
        super.onResume();

        //表示する時に未読メッセージがあれば表示する
        try {
            final @Nullable MessageData msg = vm.mMessage.getValue();
            if(msg!=null) {
                final @Nullable Long readNumber = vm.mLastReadMessageNumber.getValue();
                if (readNumber == null || readNumber < msg.number) { //未読
                    ((MainActivity) requireActivity()).showMessagesDialog(msg);
                }
            }
        } catch (Exception e) {
            showToastS("Failed to display a message dialog.", e);
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        webViewState = new Bundle();
        bind.webView.saveState(webViewState);

    }

    @Override
    public void onDestroyView() {
        bind.webView.loadUrl("about:blank"); // ページ読み込みを停止
        bind.webView.stopLoading();
        //bind.webView.setWebViewClient(null); // NullPointerExceptionが発生する可能性
        bind.webView.setWebViewClient(new WebViewClient(){}); //ダミー
        bind.webView.removeAllViews();
        bind.webView.destroy(); //非同期でコールされたりするため、画面回転中だとgetContext=nullで死ぬ

        //ビューモデルのクリア、フラグメントの流儀
        vm = null;
        bind = null;
        webViewState = null;

        super.onDestroyView();
    }

}