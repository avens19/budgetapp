# The default proguard-android-optimize.txt already keeps Activity methods used
# as android:onClick handlers, and AGP generates keep rules for the custom views
# referenced from layout XML (SquarePieChart, NonScrollableListView).

# MPAndroidChart resolves a handful of animation properties by name through
# ObjectAnimator, which R8 cannot see.
-keep class com.github.mikephil.charting.animation.** { *; }

# Keep the line numbers so Play Console crash reports stay readable, but strip
# the original source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
