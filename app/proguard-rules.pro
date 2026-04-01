# Add project specific ProGuard rules here.
-keep class com.fluxer.client.data.model.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
