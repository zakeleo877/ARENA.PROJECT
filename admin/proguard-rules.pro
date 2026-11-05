# The release build is shrunk. The generated code of Dagger and Hilt names the
# application classes, the modules and the view models, so they have to keep
# their names and their members for the framework to find them.
-keep class com.zakeleo877.saeedadmin.** { *; }
-keep class dagger.hilt.** { *; }
-keep class * extends androidx.fragment.app.Fragment { *; }

# The rule above keeps the application code. The rest of the rules protect the
# parts of the libraries that are reached by reflection.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit

# The name of the class is kept so that the report of a failure names it.
-keepnames class com.zakeleo877.saeedadmin.**

# The injection constructors and the methods that the generated code calls.
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}
-keepclassmembers class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-keep class dagger.hilt.internal.aggregatedroot.codegen.** { *; }
-keep class dagger.hilt.android.internal.** { *; }
-keep class dagger.internal.** { *; }

# The framework creates the view models with the reflection, so the class and
# the constructor of every view model have to survive the shrinking step.
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class * extends androidx.lifecycle.AndroidViewModel { <init>(...); }
-keep class * extends androidx.lifecycle.SavedStateViewModel { <init>(...); }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { <init>(...); }
