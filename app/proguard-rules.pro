# 基本保留规则
-keep class com.example.stealthrecorder.** { *; }
-keepclassmembers class com.example.stealthrecorder.** { *; }

# 保留自定义日志类
-keep class com.example.stealthrecorder.LogUtil { *; }

# 保留Android组件
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# 保留注解
-keepattributes *Annotation*

# 保留序列化
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保留资源ID
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 优化选项
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification