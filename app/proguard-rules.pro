# Keep Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }

# Keep WebView JavaScript interface
-keepclassmembers class com.yarnl.app.** {
    @android.webkit.JavascriptInterface <methods>;
}
