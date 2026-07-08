
# Tink (via androidx.security-crypto) references error-prone annotations
# that are compile-time-only and safe to omit from the shrunk release build.
-dontwarn com.google.errorprone.annotations.**
