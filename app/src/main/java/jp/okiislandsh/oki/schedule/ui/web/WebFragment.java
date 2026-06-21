package jp.okiislandsh.oki.schedule.ui.web;

import static android.widget.LinearLayout.HORIZONTAL;
import static jp.okiislandsh.library.android.MyUtil.BR;
import static jp.okiislandsh.library.core.MyUtil.isJa;
import static jp.okiislandsh.library.core.MyUtil.nvl;
import static jp.okiislandsh.library.core.MyUtil.requireNonNull;
import static jp.okiislandsh.library.core.MyUtil.startsWithAny;
import static jp.okiislandsh.oki.schedule.util.P.isValidAndSecureUrl;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;

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
    /** 証明書エラーが起きた時のペンディングhandler置き場
     *  画面回転で閉じるとき、WebViewがロックしないようにcancelする用 */
    private SslErrorHandler currentHandler;
    /** 証明書エラーペンディングの有無 */
    private boolean isSslProcessed = false;

    @IntDef({View.VISIBLE, View.GONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProgressVisibility {}

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
        bind.btnOkikankou.setOnClickListener(v -> safeLoadUrl(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_LARGE_K), getString(R.string.url_okikankou))));
        bind.btnNaikosen.setOnClickListener(v -> safeLoadUrl(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_LARGE_LEFT), getString(R.string.url_naikosen_status))));
        bind.btnOkikisen.setOnClickListener(v -> safeLoadUrl(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_LARGE_CENTER), getString(R.string.url_okikisen))));
        bind.btnKankokyokai.setOnClickListener(v -> safeLoadUrl(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_LARGE_RIGHT), getString(R.string.url_kankokyokai))));
        bind.btnWeather.setOnClickListener(v -> safeLoadUrl(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_SMALL_WEATHER), getString(R.string.url_weather))));
        bind.btnChibu.setOnClickListener(v -> safeLoadUrl(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_SMALL_CHIBU), getString(R.string.url_chibu))));
        bind.btnAma.setOnClickListener(v -> safeLoadUrl(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_SMALL_AMA), getString(R.string.url_ama))));
        bind.btnNishinoshima.setOnClickListener(v -> safeLoadUrl(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_SMALL_NISHINOSHIMA), getString(R.string.url_nishinoshima))));
        bind.btnDogo.setOnClickListener(v -> safeLoadUrl(nvl(P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_SMALL_DOGO), getString(R.string.url_dogo))));
        bind.btnTranslation.setOnClickListener(v -> {
            final @Nullable String prefURL = P.URL_SETTINGS.getURL(requireContext(), TYPE.BUTTON_SMALL_ROBOT);
            if(prefURL!=null){
                if(prefURL.contains("%s")) {
                    try {
                        final @NonNull String transUrl = String.format(prefURL, bind.webView.getUrl());
                        Log.d("btnTranslation#onClick()\tUser Setting URL\t" + transUrl);
                        safeLoadUrl(transUrl);
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
            safeLoadUrl(transUrl);
        });
        bind.btnBack.setOnClickListener(v -> {
            if (bind.webView.canGoBack()) bind.webView.goBack();
        });
        bind.btnForward.setOnClickListener(v -> {
            if (bind.webView.canGoForward()) bind.webView.goForward();
        });

        //戻る操作をWebViewの戻ると連携
        // callbackのenableで操作する方法もあるが、WebViewのcanGoBackはラグがあって使い物にならないのでcopyBackForwardListを使用する
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) { // 常に有効で受けること
            @Override
            public void handleOnBackPressed() {
                final @NonNull WebBackForwardList list = bind.webView.copyBackForwardList(); //履歴
                int currentIndex = list.getCurrentIndex();

                // currentIndex が 0 より大きい ＝ まだ戻れる過去のページがある
                if (bind.webView.canGoBack() && currentIndex > 0) {
                    bind.webView.goBack();
                } else {
                    // 現在地が 0（最初のページ）か、それ以下なら、これ以上WebView内で戻らせない。
                    // 自分を一時的に無効化して、
                    setEnabled(false);
                    //即座に「本来の（アプリ終了の）戻る挙動」を実行する。
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    // 次回のために有効化し直しておく（Fragmentなどが生存する場合の保険）
                    setEnabled(true);
                }
            }
        });

        //WebView設定
        //noinspection SetJavaScriptEnabled
        bind.webView.getSettings().setJavaScriptEnabled(true);
        bind.webView.getSettings().setBuiltInZoomControls(true);
        bind.webView.setWebViewClient(new MyWebViewClient(requireContext()){
            {
                // 通常はないが、念のためSSLエラーペンディングhandlerを確認
                if (isSslProcessed) {
                    if (currentHandler != null) {
                        currentHandler.cancel(); // 前回の古いハンドラーが生きていれば確実に殺す
                        currentHandler = null;
                    }
                    isSslProcessed = false;
                }
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {//こうしないとリンクをクリックしたとき外部ブラウザが立ち上がる
                final @NonNull String url = request.getUrl().toString();

                Log.d("WebView:shouldOverrideUrlLoading\t" + url);

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
                        return false; //何もなければWebView内遷移
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
                updateWebControlButton(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //戻る・進むボタンの使用可否
                updateWebControlButton(View.GONE);
            }
            /**
             * 404エラーや圏外など、通信エラー発生時の処理
             * (onPageFinishedが呼ばれないケースのUIフリーズ対策)
             */
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) { // 画像や広告などのエラーは無視し、ページ自体のエラーのみ処理する
                    //戻る・進むボタンの使用可否
                    updateWebControlButton(View.GONE);
                }
            }

            void updateWebControlButton(@ProgressVisibility int visibility){
                //戻る・進むボタンの使用可否
                bind.btnBack.setEnabled(bind.webView.canGoBack());
                bind.btnForward.setEnabled(bind.webView.canGoForward());
                //ぐるぐる
                bind.progressBar.setVisibility(visibility);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                currentHandler = handler;
                final @NonNull String url = error.getUrl();

                debugToastS("onReceivedSslError "+url);

                // 1. Spannableで装飾されたメッセージを生成
                final @NonNull CharSequence formattedMessage = createSslErrorMessage(error);

                // 証明書エラーの時、続行するか確認
                new AlertDialog.Builder(requireContext())
                        .setTitle("⚠️ セキュリティ警告")
                        .setMessage(formattedMessage) // Spannableをそのまま渡せる
                        .setPositiveButton(isJa() ? "リスクを承知で続行" : "Proceed despite risks", (dialog, which) -> {
                            isSslProcessed = true;
                            if (currentHandler != null) {
                                //noinspection WebViewClientOnReceivedSslError
                                currentHandler.proceed();
                            }
                        })
                        .setNegativeButton(isJa() ? "アクセスを中止" : "Cancel access", (dialog, which) -> {
                            isSslProcessed = true;
                            if (currentHandler != null) currentHandler.cancel();
                            loadWebViewSSLErrorPage(url);
                        })
                        .setCancelable(false) // 画面外タップで勝手に閉じさせない（必ずどちらか選ばせる）
                        .show();
            }
        });

        //追加のボタンコントロール
        bind.webView.setWebChromeClient(new WebChromeClient() {
            /** ページ内の読み込み進行度が変わったとき
             * @param newProgress 0～100 ただし、0で呼び出されることはめったにないらしい */
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                // 読み込み途中（例：50%段階）でも、内部スタックが確定した瞬間にボタンに反映させる
                if (newProgress > 30) {
                    //戻る・進むボタンの使用可否
                    bind.btnBack.setEnabled(bind.webView.canGoBack());
                    bind.btnForward.setEnabled(bind.webView.canGoForward());
                }
            }
        });

        //画面をタッチしたときに WebView にフォーカス
        bind.webView.setFocusableInTouchMode(true);

        //WebView復元
        if (webViewState != null) {
            bind.webView.restoreState(webViewState);
        }

        //初期URL
        if(bind.webView.getUrl()==null) {
            safeLoadUrl(getString(R.string.url_okikisen));
        }

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
        // 画面回転時・画面離脱時はここで確実に安全にcancel()する
        if (currentHandler != null && !isSslProcessed) {
            isSslProcessed = true;
            currentHandler.cancel();
            currentHandler = null;
        }
        if (bind != null) {
            WebView webView = bind.webView;

            // 1. 通信を強制停止
            webView.stopLoading();

            // 2. ViewBindingのルート（親レイアウト）からWebViewを引きはがす
            // (これにより、WebViewと親ビューの生存関係を完全に断ち切る)
            ViewParent parent = webView.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(webView);
            }

            // 3. WebView自体のメモリ解放
            webView.destroy();
        }

        //ビューモデルのクリア、フラグメントの流儀
        vm = null; //Geminiは削除しなくていいっていう
        bind = null;
        //webViewState = null; 破棄してはいけない

        super.onDestroyView();
    }

    /**
     * SSLエラー情報から、見やすく装飾されたSpannable文字列を生成する
     */
    private CharSequence createSslErrorMessage(SslError error) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        // --- セクション1: 主要なエラー原因（赤色・太字・少し大きめ） ---
        final @NonNull StringBuilder errorTitle = new StringBuilder(
                isJa("【接続の安全性を確認できません】", "[Untrusted Connection]")
        ).append(BR);
        switch (error.getPrimaryError()) {
            case SslError.SSL_EXPIRED:
                errorTitle.append(isJa(
                        "証明書の有効期限が切れています。",
                        "The security certificate has expired."
                ));
                break;
            case SslError.SSL_IDMISMATCH:
                errorTitle.append(isJa(
                        "サイトのURLと証明書のドメインが一致しません。",
                        "The hostname of the site does not match the certificate."
                ));
                break;
            case SslError.SSL_UNTRUSTED:
                errorTitle.append(isJa(
                        "信頼されていない認証局から発行されています。",
                        "The certificate authority is not trusted."
                ));
                break;
            case SslError.SSL_NOTYETVALID:
                errorTitle.append(isJa(
                        "証明書がまだ有効になっていません。",
                        "The certificate is not yet valid."
                ));
                break;
            default:
                errorTitle.append(isJa(
                        "安全ではない暗号化エラーが発生しました。",
                        "An insecure encryption error has occurred."
                ));
                break;
        }
        errorTitle.append(BR).append(BR);
        builder.append(newStyleSpan(errorTitle, Typeface.BOLD, 1.1f, Color.RED, null));

        // --- セクション2: 詳細情報（グレー・標準サイズ） ---
        builder.append(newStyleSpan(
                isJa("📄 証明書の詳細情報:"+BR, "📄 Certificate Details:"+BR),
                Typeface.BOLD, null, null, null
        ));

        final @NonNull StringBuilder infoText = new StringBuilder();
        final @Nullable SslCertificate cert = error.getCertificate();
        if (cert != null) {
            infoText.append(isJa("・対象URL: ", "• URL: ")).append(error.getUrl()).append(BR)
                    .append(isJa("・発行先 (CN): ", "• Issued To (CN): ")).append(cert.getIssuedTo().getCName()).append(BR)
                    .append(isJa("・発行元 (O): ", "• Issued By (O): ")).append(cert.getIssuedBy().getOName()).append(BR)
                    .append(isJa("・有効期限: ", "• Expires on: ")).append(
                            requireNonNull(
                                    cert.getValidNotAfterDate(),
                                    date -> DateFormat.getDateTimeInstance().format(date),
                                    isJa("不明", "Unknown")
                            )
                    ).append(isJa(" まで", "")).append(BR); // 英語は「Expires on: 日付」となるため末尾は空文字
        } else {
            infoText.append(isJa("・証明書情報を取得できませんでした。", "• Could not retrieve certificate information.")).append(BR).append(BR);
        }
        builder.append(infoText);

        // --- セクション3: 注意喚起（警告色・少し小さめ） ---
        builder.append(newStyleSpan(
                isJa(
                        "※閲覧専用のページであっても、通信が傍受されるリスクがあります。",
                        "* Even on view-only pages, there is a risk that your communication may be intercepted."
                ),
                Typeface.BOLD, 0.9f, Color.rgb(230, 81, 0), null
        ));
        return builder;
    }

    /** SSLエラーの時にWebViewへHTMLを流し込んでエラー表示
     * @param failedUrl エラー URL */
    private void loadWebViewSSLErrorPage(@NonNull String failedUrl) {
        if (bind != null) {
            // about:blankの代わりに、カスタムHTMLを表示する
            bind.webView.loadDataWithBaseURL(null, getString(R.string.SSLErrorPageHTML), "text/html", "utf-8", failedUrl); //failedUrlが履歴としてのURLになる
        }
    }

    /**
     * ユーザー入力を安全に検証した上で、WebViewにURLをロードします。
     */
    private void safeLoadUrl(@Nullable String rawInput) {
        // 外出ししたバリデーションメソッドでチェック
        if (!isValidAndSecureUrl(rawInput)) {
            // 失敗したらエラーを表示して終了
            showToastS(isJa("無効なURL形式です。", "Invalid URL format.")+BR+rawInput);
            return;
        }

        // 検証を通過したため、安全に整形してロード
        // (isValidAndSecureUrl を通過した時点で rawInput は null ではないことが確定しています)
        String trimmed = rawInput.trim();
        String finalUrl = (trimmed.startsWith("http://") || trimmed.startsWith("https://"))
                ? trimmed
                : "https://" + trimmed;

        bind.webView.loadUrl(finalUrl);
    }

}