# Phase 7 (Plan 07-02): release minification enabled.
# Phase 1-6 strategy: NO pre-emptive keep rules. Hilt 2.57 + Compose Compiler ship
# consumer-proguard-rules in their AARs that handle their own reflection needs.
# Add hand-written keep rules HERE only when empirical R8 failure proves they're necessary
# (Phase 4/5 inline-gap-fix protocol; RESEARCH §Anti-pattern bullet 1).

# Generic safety nets for Timber + crash diagnostics (always safe to keep):
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Reactive section — populate as R8 failures surface during device verification:
# (add narrow -keep rules here, one per observed failure; document each with
# PR reference + the grep-assert it was needed to preserve)

# Plan 07-02 device CHECKPOINT inline fix (2026-05-13):
# R8 obfuscated CameraMode enum + Routes data classes that kotlinx.serialization +
# navigation-compose look up by FQN at runtime. Symptom:
#   IllegalArgumentException: Cannot find class with name
#   "com.bugzz.filter.camera.ui.home.CameraMode"
# Keep classnames + @Serializable companion serializers for all Bugzz nav route +
# enum types. This is the standard navigation-compose + kotlinx.serialization R8
# fix from https://developer.android.com/guide/navigation/navcontroller#type-safe.
-keepnames class com.bugzz.filter.camera.ui.home.CameraMode { *; }
-keepnames class com.bugzz.filter.camera.ui.nav.** { *; }
-if @kotlinx.serialization.Serializable class com.bugzz.filter.camera.**
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    static kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.bugzz.filter.camera.**$$serializer { *; }
