package jp.okiislandsh.oki.schedule;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static jp.okiislandsh.library.android.MyUtil.BR;
import static jp.okiislandsh.library.core.MyUtil.isJa;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import java.util.concurrent.atomic.AtomicBoolean;

import jp.okiislandsh.library.android.DividerSpan;
import jp.okiislandsh.library.android.MyAppCompatActivity;
import jp.okiislandsh.library.android.MyUtil;
import jp.okiislandsh.library.android.live.NonNullLiveData;
import jp.okiislandsh.library.core.Pairs;
import jp.okiislandsh.oki.schedule.util.MessageData;
import jp.okiislandsh.oki.schedule.util.P;

public class MainActivity extends MyAppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    @SuppressWarnings("FieldCanBeLocal") //メンバ変数にしないと消滅する
    private NonNullLiveData<Boolean> liveMenseki;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        liveMenseki = P.MENSEKI.getLive(this);

        final @NonNull Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //横向きの時ツールバーを隠す
        final @Nullable ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null) {
            if(!MyUtil.getNowDeviceOrientationIsPortrait(this)) {
                actionBar.hide();
            }else {
                actionBar.show(); //一応戻す、リソース関連はよくキャッシュから取り出される
            }
        }

        final @NonNull DrawerLayout drawer = findViewById(R.id.drawer_layout);
        final @NonNull NavigationView navigationView = findViewById(R.id.nav_view);
        liveMenseki.observe(this, bool->{ //免責同意済みなら免責メニューを表示しない
            MyUtil.requireNonNull(navigationView.getMenu().findItem(R.id.nav_menseki), menuItem->menuItem.setVisible(!bool));
        });
        //ナビゲーション.xmlを変更した場合、合わせてここも修正すること
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_web, R.id.nav_table_dozen, R.id.nav_table_all,
                R.id.nav_guide, R.id.nav_setting, R.id.nav_menseki) //アプリバーコンフィグの免責は残っていても正常に動作する
                .setOpenableLayout(drawer)
                .build();
        final @NonNull NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        //免責同意ステータス
        if(!liveMenseki.getValue()){ //未同意なら免責画面へ飛ばす
            loadDestination(R.id.nav_menseki);
        }

        //メニューはFragmentで単一管理するのが良いらしい
        //addMenuProvider(new MenuProvider()

    }

    //TODO:これは必須？
    @Override
    public boolean onSupportNavigateUp() {
        final @NonNull NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    /** アプリからのお知らせをダイアログ表示する */
    public void showMessagesDialog(@NonNull MessageData msgData){
        final @NonNull SpannableStringBuilder buf = new SpannableStringBuilder();
        for(@NonNull MessageData.Parts m: msgData.messages){
            //MessageDataクラスのソートをそのまま表示に適用する
            if(0<buf.length()) buf.append(DividerSpan.newDividerSpan()); //メッセージごとに境界線
            buf.append(newSizeSpan(m.ymd+BR, .8f))
                    .append(isJa(m.ja, m.en));
        }
        //日付ごとに複数のメッセージがある
        final @NonNull TextView text = newText(buf, newParamsMW(), P.TT_TEXT_SIZE.get(this).px(this));
        setPadding(text, 12);
        text.setAutoLinkMask(Linkify.WEB_URLS);
        text.setTextIsSelectable(true);

        final @NonNull AlertDialog dialog = showDialog(getString(R.string.option_menu_notification), null, newVScroll(newParamsMW(), text),
                null, null, new Pairs.Immutable._2<>(isJa("既読", "I read it."), (d, which)-> {
                    //表示を保存
                    P.LAST_READ_MESSAGE_NUMBER.set(this, msgData.number);
                    //閉じる
                    d.dismiss();
                }),
                null, true, false);
        //デバッグ用message.json表示
        final @Nullable Button button = dialog.getButton(BUTTON_NEGATIVE);
        if(button!=null){
            final @NonNull AtomicBoolean isJSON = new AtomicBoolean(false);
            button.setOnLongClickListener(v->{
                isJSON.set(!isJSON.get());
                text.setText(isJSON.get() ? msgData.rawJSONString : buf);
                text.invalidate();
                return true;
            });
        }
    }

    /** ナビゲーションによって表示されている画面をリロードする */
    public void loadDestination(int navID){
        final @NonNull NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment); //コントローラ
        final @NonNull NavigationView navigationView = findViewById(R.id.nav_view); //メニュー
        final @Nullable MenuItem item = navigationView.getMenu().findItem(navID); //MenuItem逆引き
        if (item != null) { //逆引き成功
            NavigationUI.onNavDestinationSelected(item, navController);
            //navController.navigate(resId); 直接navigateを呼び出すとバグる、たぶんフラグメント増殖
        }
    }

}
