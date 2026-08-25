# Room
-keep class androidx.room.Room { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Hilt & Dagger
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.app.Activity { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.TestSingletonComponent { *; }
-dontwarn com.google.errorprone.annotations.**
-dontwarn dagger.hilt.android.internal.lifecycle.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    <init>(...);
}

# NotificationListenerService
-keep class com.quietinbox.service.QuietListenerService { *; }
-keep class com.quietinbox.service.BootReceiver { *; }

# Biometrics
-keep class androidx.biometric.** { *; }
