# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/sslavin/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in the build script.
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

# Runtime metadata required by Kotlin, Gson, nested classes and annotations.
-keepattributes Exceptions,InnerClasses,EnclosingMethod,Signature,SourceFile,LineNumberTable,*Annotation*

# Keep Kotlin metadata.
-keep class kotlin.Metadata { *; }

# Preserve all annotations.
-keepattributes *Annotation*

# ---------------------------------------------------------------------------
# Public/protected SDK API
# ---------------------------------------------------------------------------

# Public classes, including abstract and sealed classes.
# Kotlin sealed classes have no separate ProGuard selector.
-keep public class org.infobip.mobile.messaging.chat.** {
    public protected *;
}

-keep public class org.infobip.mobile.messaging.chat.**$* {
    public protected *;
}

# Public interfaces and nested interfaces.
-keep public interface org.infobip.mobile.messaging.chat.** {
    public protected *;
}

-keep public interface org.infobip.mobile.messaging.chat.**$* {
    public protected *;
}

# Public enums, their constants and generated members.
-keep public enum org.infobip.mobile.messaging.chat.** {
    *;
}

-keep public enum org.infobip.mobile.messaging.chat.**$* {
    *;
}


# ---------------------------------------------------------------------------
# Internal runtime boundaries
# ---------------------------------------------------------------------------

# Loaded from AndroidManifest metadata through Class.forName() and newInstance().
-keep class org.infobip.mobile.messaging.chat.InAppChatImpl {
    public <init>();
}

# Methods called from JavaScript by name.
-keepclassmembers class org.infobip.mobile.messaging.chat.** {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve enum names used by JSON, JavaScript, broadcasts and toString().
# Includes LivechatWidgetMethod, LivechatWidgetView,
# LivechatWidgetMessageType, LivechatWidgetThreadStatus and
# MultithreadStrategy.
-keepclassmembers enum org.infobip.mobile.messaging.chat.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve all .class method names.
-keepclassmembernames class org.infobip.mobile.messaging.chat.** {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

# Preserve all native method names and the names of their classes.
-keepclasseswithmembernames class org.infobip.mobile.messaging.chat.** {
    native <methods>;
}


# ---------------------------------------------------------------------------
# Gson reflective deserialization
# ---------------------------------------------------------------------------

# Used directly and as Array<LivechatWidgetThread>.
-keep class org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThread {
    <fields>;
    <init>(...);
}

# LivechatWidgetException stores its JSON values in inherited private fields.
-keep class org.infobip.mobile.messaging.chat.core.InAppChatException {
    <fields>;
}

-keep class org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetException {
    <fields>;
    <init>(...);
}

# Plugin customization and nested toolbar models.
-keep class org.infobip.mobile.messaging.chat.view.styles.PluginChatCustomization {
    <fields>;
    <init>(...);
}

-keep class org.infobip.mobile.messaging.chat.view.styles.PluginChatToolbarCustomization {
    <fields>;
    <init>(...);
}

# Explicitly preserve all serialization members. The Serializable interface
# is only a marker interface, so it wouldn't save them.
# You can comment this out if your library doesn't use serialization.
# If your code contains serializable classes that have to be backward
# compatible, please refer to the manual.
-keepclassmembers class org.infobip.mobile.messaging.chat.** implements java.io.Serializable {
    static final long serialVersionUID;
    static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
