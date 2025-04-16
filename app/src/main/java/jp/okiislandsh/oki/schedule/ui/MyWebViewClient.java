package jp.okiislandsh.oki.schedule.ui;

import static jp.okiislandsh.library.android.MyUtil.BR;
import static jp.okiislandsh.library.core.MyUtil.isJa;
import static jp.okiislandsh.library.core.MyUtil.requireNonNullNullable;

import android.app.AlertDialog;
import android.content.Context;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import jp.okiislandsh.library.android.view.ViewBuilderFunction;
import jp.okiislandsh.library.core.DateUtil;
import jp.okiislandsh.library.core.Pairs;
import jp.okiislandsh.library.core.RawString;

/** SSLエラー時にユーザ操作で読込を継続可能にしたWebViewClient */
public class MyWebViewClient extends WebViewClient implements ViewBuilderFunction {
    private final @NonNull Context context;
    public MyWebViewClient(@NonNull Context context){
        this.context = context;
    }
    @Override
    public @NonNull Context getContext() {
        return context;
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        try {
            final @NonNull RawString.Builder message = new RawString.Builder(error.getUrl(), BR, isJa("SSL証明書に関するエラー。", "SSL Certificate error."), BR);
            switch (error.getPrimaryError()) {
                case SslError.SSL_UNTRUSTED:
                    message.append(isJa("「証明機関が信頼できない」", "The certificate authority is not trusted."));
                    break;
                case SslError.SSL_EXPIRED:
                    message.append(isJa("「証明書が有効期限切れ」", "The certificate has expired."));
                    break;
                case SslError.SSL_IDMISMATCH:
                    message.append(isJa("「ホスト名が不一致」", "The certificate Hostname mismatch."));
                    break;
                case SslError.SSL_NOTYETVALID:
                    message.append(isJa("「証明書がまだ有効ではない」", "The certificate is not yet valid."));
                    break;
            }
            message.append(isJa("続行しますか？", " Do you want to continue anyway?"));

            final @NonNull String subDialogTitle = isJa("詳細", "Detail");
            new AlertDialog.Builder(requireContext())
                    .setTitle(isJa("SSL証明書エラー", "SSL Certificate Error"))
                    .setMessage(message)
                    .setView(newTextStyleButton(subDialogTitle, newParamsWW(), v -> {
                        final @NonNull SslCertificate sslCertificate = error.getCertificate();
                        showDialog(subDialogTitle,
                                sslCertificate.toString() + BR + BR +
                                        "Valid Not Before Date " + requireNonNullNullable(sslCertificate.getValidNotBeforeDate(), d -> DateUtil.formatFullDateTime(d.getTime())) + BR + BR +
                                        "Valid Not After Date " + requireNonNullNullable(sslCertificate.getValidNotAfterDate(), d -> DateUtil.formatFullDateTime(d.getTime())),
                                null, null, null,
                                new Pairs.Immutable._2<>(getString(jp.okiislandsh.library.android.R.string.close), (dialog, which) -> dialog.dismiss()), null, null, null);
                    }))
                    .setPositiveButton(isJa("続行", "continue"), (dialog, which) -> invoke_SslErrorProceed(handler))
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> invoke_SslErrorCancel(handler))
                    .setOnCancelListener(dialog -> invoke_SslErrorCancel(handler))
                    .show();
        }catch (Exception e){
            //念のため、例外で落ちないようにする。証明書エラー如きでアプリが落ちるなんてだめ。
            showToastS("WebView Received SSL Error."+BR+"error="+error, e);
        }
    }

    private void invoke_SslErrorProceed(@NonNull SslErrorHandler handler){
        try {
            handler.proceed();
        }catch (Exception e){
            //念のため、例外で落ちないようにする。証明書エラー如きでアプリが落ちるなんてだめ。
            showToastS("SSLError proceed fail.", e);
        }
    }

    private void invoke_SslErrorCancel(@NonNull SslErrorHandler handler){
        try {
            handler.cancel();
        }catch (Exception e){
            //念のため、例外で落ちないようにする。証明書エラー如きでアプリが落ちるなんてだめ。
            showToastS("SSLError cancel fail.", e);
        }
    }

}
