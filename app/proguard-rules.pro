# Phase 7 (Plan 07-02): release minification enabled.
# Phase 1-6 strategy: NO pre-emptive keep rules. Hilt 2.57 + Compose Compiler ship
# consumer-proguard-rules in their AARs that handle their own reflection needs.
# Add hand-written keep rules HERE only when empirical R8 failure proves they're necessary
# (Phase 4/5 inline-gap-fix protocol; RESEARCH §Anti-pattern bullet 1).

# Generic safety nets for Timber + crash diagnostics (always safe to keep):
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Reactive section — populate as R8 failures surface during device verification:
# (add narrow -keep rules here, one per observed failure; document each with
# PR reference + the grep-assert it was needed to preserve)
