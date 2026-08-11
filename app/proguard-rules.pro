# ProGuard / R8 rules for Network Speed Monitor (com.netspeed.monitor)

# Keep model & data classes for serialization/state persistence
-keep class com.netspeed.monitor.data.** { *; }

# Keep service and receiver entry points
-keep class com.netspeed.monitor.service.** { *; }
-keep class com.netspeed.monitor.receiver.** { *; }

# Keep Samsung reflection utilities for SEM_PLATFORM_INT / One UI detection
-keep class com.netspeed.monitor.utils.SamsungDeviceUtils { *; }
-keepclassmembers class com.netspeed.monitor.utils.SamsungDeviceUtils { *; }

# Keep general annotations and keep rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
