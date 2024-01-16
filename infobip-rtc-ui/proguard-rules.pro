#Kotlin
-dontwarn org.jetbrains.annotations.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keepattributes RuntimeVisibleAnnotations

#Java library
-keep public class * {
    public protected *;
}

-printmapping infobip-rtc-ui-out.map
-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Signature,Exceptions,*Annotation*,
                InnerClasses,PermittedSubclasses,EnclosingMethod,
                Deprecated,SourceFile,LineNumberTable

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

#InfobipRtcUi
-keep public class com.infobip.webrtc.** { public protected *;}
-keep public interface com.infobip.webrtc.** { public protected *;}
-keep public enum com.infobip.webrtc.** { public protected *;}

-keep public class com.infobip.webrtc.ui.** { public protected *;}
-keep public interface com.infobip.webrtc.ui.** { public protected *;}
-keep public enum com.infobip.webrtc.ui.** { public protected *;}