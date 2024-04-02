# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/mstipanov/Programs/android-sdk-macosx/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-printmapping mobile-messaging-out.map
-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,EnclosingMethod

# Preserve all annotations.

-keepattributes *Annotation*

# Preserve all public classes, and their public and protected fields and
# methods.

-keep public class org.infobip.mobile.messaging.** {
    public protected *;
}

# Preserve all .class method names.

-keepclassmembernames class org.infobip.mobile.messaging.** {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

-keepclassmembernames class org.infobip.mobile.messaging.mobileapi.common.ResultWrapper { *; }

# Preserve all native method names and the names of their classes.

-keepclasseswithmembernames class org.infobip.mobile.messaging.** {
    native <methods>;
}

# Preserve the special static methods that are required in all enumeration
# classes.

-keepclassmembers class org.infobip.mobile.messaging.** extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep public enum org.infobip.mobile.messaging.** { *; }

# Explicitly preserve all serialization members. The Serializable interface
# is only a marker interface, so it wouldn't save them.
# You can comment this out if your library doesn't use serialization.
# If your code contains serializable classes that have to be backward
# compatible, please refer to the manual.

-keepclassmembers class org.infobip.mobile.messaging.** implements java.io.Serializable {
    static final long serialVersionUID;
    static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Suppress lombok warnings
-dontwarn lombok.**

# Mobile Messaging API classes
-keep class org.infobip.mobile.messaging.api.** { *; }
-keep public interface org.infobip.mobile.messaging.api.** { public *; }
-keep public enum org.infobip.mobile.messaging.api.** { public *; }

# Fix for JobIntentService
-keep class org.infobip.mobile.messaging.platform.JobIntentService$* { *; }

# GSON-related

# Gson specific classes
-keep class sun.misc.Unsafe.** { *; }
#-dontwarn sun.misc.Unsafe
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class org.infobip.mobile.messaging.dal.json.** { *; }
-keep class org.infobip.mobile.messaging.Installation { *; }
-keep class org.infobip.mobile.messaging.Message { *; }
-keep class org.infobip.mobile.messaging.User { *; }
-keep class org.infobip.mobile.messaging.CustomAttributeValue { *; }
-keep class org.infobip.mobile.messaging.interactive.NotificationAction { *; }
-keep class org.infobip.mobile.messaging.interactive.NotificationAction$* { *; }
-keep class org.infobip.mobile.messaging.interactive.NotificationCategory { *; }
-keep class org.infobip.mobile.messaging.cloud.firebase.FirebaseMessageMapper { *; }
-keep class org.infobip.mobile.messaging.cloud.firebase.FirebaseMessageMapper$* { *; }
-keep class org.infobip.mobile.messaging.mobileapi.** { *; }

# Prevent proguard from stripping interface information from TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Gson rules for AGP 7.1.0 and higher
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type
# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
