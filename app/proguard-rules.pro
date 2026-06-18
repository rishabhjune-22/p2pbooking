# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Retrofit service definitions are used through dynamic proxies.
-keep interface com.example.roombooking.api.ApiService { *; }

# Gson reads and writes these DTOs reflectively.
-keep class com.example.roombooking.model.** { *; }
-keep class com.example.roombooking.booking.**Request { *; }
-keep class com.example.roombooking.booking.**Response { *; }
-keep class com.example.roombooking.booking.AvailableRoomItem { *; }
-keep class com.example.roombooking.booking.RoomAvailabilityDay { *; }
-keep class com.example.roombooking.booking.RoomAvailabilityGroup { *; }

-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
