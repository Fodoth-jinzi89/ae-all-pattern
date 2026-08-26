# Thunderbolt Core origin

This directory is a source snapshot of
<https://github.com/ae2lt/Thunderbolt-Core> at commit
`171a6cba1a5d8d62a0019dd8ff74158344928f54`.

It also backports upstream commit
`7738f44cf9a02bf4d5696daf882f53b49aec5e7c` (lazy initialization for injected
runtime state), which is required for safe AE2 19.2.17 grid merges.

It is retained as a separate Gradle source build and package namespace. The
source and generated binary are licensed under GNU LGPL 3.0; see `LICENSE` in
this directory. Local changes should remain clearly attributable and must not
be relicensed as part of AE All Pattern's MIT-owned source.
