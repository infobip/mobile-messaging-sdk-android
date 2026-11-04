# Preserve public RTC UI types and their public/protected API for minified consumers.
# React Native and Flutter also access parts of this API by reflection.
# The root single-star wildcard excludes com.infobip.webrtc.ui.internal.*.
# The other rules include nested types and subpackages of the named API packages.
# Kotlin internal types compiled as public bytecode and generated R/BuildConfig
# classes in these packages are also kept.

-keep public class com.infobip.webrtc.ui.* { public protected *; }
-keep public class com.infobip.webrtc.ui.logging.** { public protected *; }
-keep public class com.infobip.webrtc.ui.model.** { public protected *; }
-keep public class com.infobip.webrtc.ui.service.** { public protected *; }
-keep public class com.infobip.webrtc.ui.view.** { public protected *; }
