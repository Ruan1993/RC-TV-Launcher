# Phase D — connected experience

## Cursor editing — 0.5.0
Dedicated cursor-left/right keys in every keyboard layout. Remote OK down/repeats move the insertion point; release does not duplicate movement. D-pad arrows still navigate keys. InputConnection.setSelection and Android selection offsets avoid moving editor focus at empty-field boundaries. A bounded transient read of at most two UTF-16 code units preserves surrogate pairs; no content is logged, persisted or kept as a composing buffer. Selection deletion replaces the selection; ordinary Backspace deletes the code point before the cursor. Unsupported cursor metadata/text access safely yields no movement.

Xiaomi standard native editor checks pass: middle insertion, cursor movement, before-cursor deletion, selected text, Unicode, empty fields and repeated cursor activation. Targeted keyboard layout tests and debug/test-APK compilation pass. Separate instrumentation uses synthetic text only; no IME/default change or preference edits. Working/rollback APKs retained. YouCine's application keyboard is untouched.

## Companion direction
Android phone confirmed. First delivery will use an optional native Android client, local discovery, pinned TLS after explicit fingerprint comparison, short-code pairing and TV approval. Commands are restricted to RC TV's own surfaces. No cloud account or system-wide key injection.
