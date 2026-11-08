# The shared module is consumed by two applications that shrink the release
# builds. The generated code of Dagger and Hilt refers to the classes of the
# module by name, so the classes, the constructors and the members that the
# generated code uses have to survive the shrinking step.
-keep class com.zakeleo877.saeedcore.** { *; }
-keepclassmembers class com.zakeleo877.saeedcore.** { *; }

# The annotations that the frameworks read at runtime.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# The name of the class is kept so that the report of a failure names it.
-keepnames class com.zakeleo877.saeedcore.**
