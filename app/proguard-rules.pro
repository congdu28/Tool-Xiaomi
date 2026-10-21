# Keep native & basic attributes
-dontwarn **
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep application classes
-keep class app.cd.mitool.** { *; }

# Material Components and Support classes used in XML
-keep class com.google.android.material.** { *; }
-keep class androidx.cardview.widget.** { *; }
-keep class androidx.coordinatorlayout.widget.** { *; }
-keep class androidx.core.widget.** { *; }

# Shizuku
-keep class rikka.shizuku.** { *; }
-keepclassmembers class rikka.shizuku.Shizuku {
    private static rikka.shizuku.ShizukuRemoteProcess newProcess(java.lang.String[], java.lang.String[], java.lang.String);
}
