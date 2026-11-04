# Known issues

## 0.2.0 (unreleased)

1. **No runtime emulator validation.** The debug and release APKs build and pass unit
   tests, but the terminal has not been booted on an emulator or device in this
   environment. The most likely first-boot issues are proot flag mismatches or
   missing `PROOT_LOADER`/`PROOT_LOADER_32` handling on some kernels.

2. **Extra-keys bar does not implement modifier state.** The `CTRL` and `ALT`
   buttons are placeholders; they do not latch or combine with subsequent keys.
   Cursor keys, ESC, TAB, BKSP, DEL, PGUP, PGDN, HOME, and END work.

3. **Engine binaries are not built in this environment.** The APKs assembled here
   do not contain `libopencode.so` because the engine cross-compile artifacts were
   absent from the working tree. The app will still run, but the AI chat feature
   will show "engine not ready" until the engine binaries are provided.
