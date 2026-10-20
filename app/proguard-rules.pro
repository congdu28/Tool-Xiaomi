# Keep native & basic attributes
-dontwarn **
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep application classes
-keep class app.cd.mitool.** { *; }

# Keep Material Components and Support classes used in XML
-keep class com.google.android.material.** { *; }
-keep class androidx.cardview.widget.** { *; }
-keep class androidx.coordinatorlayout.widget.** { *; }
-keep class androidx.core.widget.** { *; }
