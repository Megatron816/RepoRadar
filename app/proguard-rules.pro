# Gson reflects over these model classes; keep their fields and constructors.
-keep class com.example.githubprofileexplorer.data.model.** { *; }

# Keep generic signatures and annotations used by Retrofit/Gson.
-keepattributes Signature, RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
