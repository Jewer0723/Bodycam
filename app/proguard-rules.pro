# MediaPipe 核心規則 (防止 A31 等手機載入失敗)
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# TensorFlow Lite 規則
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# 保持 Assets 原始名稱 (讓模型 .tflite 能被找到)
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# 保持所有含有 Native 方法的類別 (JNI 調用)
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保持 CameraX 相關類別
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# 保持 Kotlin 序列化與標籤
-keepattributes Signature,AnnotationDefault,EnclosingMethod,InnerClasses
