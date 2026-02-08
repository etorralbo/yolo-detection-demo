# TensorFlow Lite GPU delegate relies on native (JNI) code and references
# internal GPU classes that are not fully resolvable by R8 during analysis.
# Even with the GPU delegate plugin present, R8 may report missing classes.
#
# These rules prevent R8 from stripping GPU delegate classes and suppress
# warnings for optional GPU backends to ensure stable release builds.
-keep class org.tensorflow.lite.gpu.** { *; }
-dontwarn org.tensorflow.lite.gpu.**
