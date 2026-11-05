# Known issues

## 0.2.0 (unreleased)

1. **Extra-keys bar does not implement modifier state.** The `CTRL` and `ALT`
   buttons are placeholders; they do not latch or combine with subsequent keys.
   Cursor keys, ESC, TAB, BKSP, DEL, PGUP, PGDN, HOME, and END work.

2. **Engine binaries are not built in this environment.** The APKs assembled here
   do not contain `libopencode.so` because the engine cross-compile artifacts were
   absent from the working tree. The app will still run, but the AI chat feature
   will show "engine not ready" until the engine binaries are provided.
