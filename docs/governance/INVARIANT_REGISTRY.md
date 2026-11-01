# INVARIANT REGISTRY — Hermes Voice Console Android

INV-01 The repository `Paolo83Ita/hermes-voice-console-android` remains PRIVATE.
INV-02 Only `Paolo83Ita/hermes-voice-console-android` is authorized by this Graph; no other
       repository is accessed, inspected or modified.
INV-03 No authentication token enters Git history or persistent project files.
INV-04 No production signing key is created or used (debug builds use the ephemeral debug key).
INV-05 No public GitHub release is created.
INV-06 No public package or public build artifact is created (artifacts live in private Actions).
INV-07 No automatic merge into `main`.
INV-08 No force-push.
INV-09 No history rewrite.
INV-10 No remote branch deletion without Owner approval.
INV-11 No modification of unrelated repositories. (Graph brief INV-11; wording completed from
       the authorization clause "The token is authorized only for
       Paolo83Ita/hermes-voice-console-android".)

Extensions installed by this Graph (marked as Graph extensions):

INV-12 No physical-device identifiers (IP, MAC, IMEI, Android serial, Android ID) are copied
       into the repository.
INV-13 No bootloader unlock, root, custom recovery or custom ROM is performed or required;
       OEM unlocking stays out of scope.
INV-14 ADB is never exposed through Tailscale or any remote channel.
INV-15 Hermes Tower remains the sole authority; the local model never becomes a second one.
INV-16 The UI renders only real console state; the 15-state machine may not display invented
       runtime data.
INV-17 Evidence is typed; generic PASS is forbidden.
INV-18 No public artifact of any kind; build outputs leave GitHub only as private Actions
       artifacts or Owner-local files.
INV-19 Every APK claim carries its SHA-256 and the commit it was built from.
