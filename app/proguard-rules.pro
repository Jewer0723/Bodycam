# 保持所有含有 Native 方法的類別 (JNI 調用)
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保持 CameraX 相關類別
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# 保持 Kotlin 序列化與標籤
-keepattributes Signature,AnnotationDefault,EnclosingMethod,InnerClasses
