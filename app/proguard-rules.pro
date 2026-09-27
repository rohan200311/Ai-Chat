# Keep models
-keep class com.aichat.app.domain.model.** { *; }
-keep class com.aichat.app.data.local.db.** { *; }
-keep class com.tom_roush.pdfbox.** { *; }
-keep class org.apache.poi.** { *; }

# Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers,allowobfuscation @kotlinx.serialization.Serializable class * { *; }
