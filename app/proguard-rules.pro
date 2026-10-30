# Hermes Voice Console proguard rules.
# V0.1 ships a debug-oriented prototype; release minification is a V1.0 topic.
# Keep kotlinx-serialization serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.hermestower.voiceconsole.** { kotlinx.serialization.KSerializer serializer(...); }
