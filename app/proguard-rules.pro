# Log関数削除
-assumenosideeffects public class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

#アプリ設定、Enum名をそのままキー名に使用しているため
-keep enum * { *; }

#NavigationのレイアウトにFragmentContainerViewを使用した場合、NavHostFragmentがClassNotFoundException
-keep class * extends androidx.fragment.app.Fragment{}
#noinspection ShrinkerUnresolvedReference
-keep class androidx.navigation.** {*;}
