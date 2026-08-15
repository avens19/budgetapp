# AGP generates keep rules for views referenced from layout XML, which now
# means the Material and AndroidX widgets plus MPAndroidChart's PieChart. The
# app's own custom views are gone: the screens bind their listeners in code
# rather than through android:onClick, so nothing is reached by name any more
# except the chart animation below.

# MPAndroidChart resolves a handful of animation properties by name through
# ObjectAnimator, which R8 cannot see.
-keep class com.github.mikephil.charting.animation.** { *; }

# Keep the line numbers so Play Console crash reports stay readable, but strip
# the original source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
