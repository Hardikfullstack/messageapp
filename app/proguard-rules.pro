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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# kotlinx.serialization â€” without these, R8 can strip/rename the fields of the remote ad-config
# response classes (AppResponse/AppResult) in a release build, silently breaking JSON parsing for
# the ad/maintenance/update config the whole app depends on. Standard rules from kotlinx.serialization's own docs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.message.sms.texting.app.**$$serializer { *; }
-keepclassmembers class com.message.sms.texting.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.message.sms.texting.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Extra insurance for the modern kotlinx.serialization IR codegen path, alongside the rules above.
-keep,allowoptimization class * extends kotlinx.serialization.internal.GeneratedSerializer