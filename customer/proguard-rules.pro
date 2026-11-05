# The release build is shrunk. The generated code of Dagger and Hilt names the
# application classes, the modules and the view models, so they have to keep
# their names and their members for the framework to find them.
-keep class com.zakeleo877.saeedstore.** { *; }
-keep class dagger.hilt.** { *; }
-keep class * extends androidx.fragment.app.Fragment { *; }

# The rule above keeps the application code. The rest of the rules protect the
# parts of the libraries that are reached by reflection.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# The name of the class is kept so that the report of a failure names it.
-keepnames class com.zakeleo877.saeedstore.**

# The injection constructors and the methods that the generated code calls.
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}
-keepclassmembers class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-keep class dagger.hilt.internal.aggregatedroot.codegen.** { *; }
-keep class dagger.hilt.android.internal.** { *; }
-keep class dagger.internal.** { *; }
