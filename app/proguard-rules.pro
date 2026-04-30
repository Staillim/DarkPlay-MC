# Proguard rules for DarkPlayMC

# Keep Media3 service classes
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }

# Keep Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# Keep Room entities
-keep class com.darkplaymc.app.data.local.entity.** { *; }

# Keep data model classes used in MediaMetadata
-keep class com.darkplaymc.app.data.model.** { *; }

# Coil
-keep class coil.** { *; }

# Keep Kotlin coroutine internals needed at runtime
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }
