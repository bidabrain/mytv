# 套壳 App 暂不混淆（release 也关闭了 minify）。
# 如启用混淆，注入页面的 JS 字符串与 WebView JS 接口需保留。
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
