# APK in Release Assets - How to Get It Listed Like Screenshot

## Your Request
You showed screenshot where Assets should list `AiChat-debug.apk` directly (like typical GitHub releases), not just Source Code zip.

## What We Did (Sandbox Workaround)

### ✅ Already Working NOW:
1. **Release v1.0.0 now points to latest commit** `76a6850` on branch `arena/01a0e222-ai-chat`
2. **Source Code (zip) in Assets CONTAINS APK inside** - verified:
   ```bash
   curl -L -o v1.0.0.zip https://github.com/rohan200311/Ai-Chat/archive/v1.0.0.zip
   unzip -l v1.0.0.zip | grep apk
   # Ai-Chat-1.0.0/AiChat-debug.apk (32301 bytes)
   # Ai-Chat-1.0.0/AiChat.apk
   # Ai-Chat-1.0.0/dist/AiChat-debug.apk
   ```
   So user can download Source Code zip → unzip → get APK immediately.

3. **Release notes updated** at https://github.com/rohan200311/Ai-Chat/releases/tag/v1.0.0 with 3 download options

### ❌ Why not directly in Assets yet?
Sandbox E2B proxy blocks `uploads.github.com`:
- `api.github.com` and `github.com` → allowed (cert O=E2B CN=E2B Proxy CA)
- `uploads.github.com`, `raw.githubusercontent.com` → blocked `SSL_ERROR_SYSCALL`
- `gh release upload` uses `uploads.github.com` → fails

Proof:
```bash
curl -v https://uploads.github.com  # SSL_ERROR_SYSCALL
curl -v https://api.github.com      # OK
GH_DEBUG=api gh release upload v1.0.0 AiChat-debug.apk  # fails at upload step
```

Also GitHub App cannot push `.github/workflows/*.yml` (needs `workflows` permission):
```
refusing to allow a GitHub App to create or update workflow without workflows permission
```

## ✅ FIX - 30 Seconds Manual (You Need To Do This Once)

### Option A: Web UI (Easiest - 2 clicks)
1. Open https://github.com/rohan200311/Ai-Chat/releases/edit/v1.0.0
2. Scroll to "Attach binaries by dropping them here or selecting them"
3. Drag & drop from your local clone:
   - `AiChat-debug.apk`
   - `AiChat.apk`
   - `dist/AiChat-debug.apk`
4. Click "Update release"
5. Done! Now Assets shows:
   - Source code (zip)
   - Source code (tar.gz)
   - **AiChat-debug.apk** (like your screenshot)
   - AiChat.apk

### Option B: Local CLI (If you have gh)
On YOUR machine (not sandbox):
```bash
git clone https://github.com/rohan200311/Ai-Chat.git
cd Ai-Chat
git checkout arena/01a0e222-ai-chat
gh release upload v1.0.0 AiChat-debug.apk AiChat.apk dist/AiChat-debug.apk --clobber
gh release view v1.0.0
```

### Option C: GitHub Actions (Automatic Future)
1. Create workflow file via web UI (since bot can't push workflows):
   - Go to https://github.com/rohan200311/Ai-Chat/new/main?filename=.github/workflows/build-apk.yml
   - Copy content from `tools/build-apk-workflow.yml` (in repo, 2169 bytes)
   - Commit to main
2. Go to Actions → "Build and Release APK" → Run workflow
   - Input tag: `v1.0.0`
3. Runner has internet (not blocked) and will:
   - Build full production APK (15-25MB)
   - Upload to v1.0.0 Assets via `softprops/action-gh-release@v2`

Workflow already prepared:
- `tools/build-apk-workflow.yml` (committed, ready to copy)
- `.github/workflows/build-apk.yml` (local, can't push via bot)

## Verification After Fix
After manual upload, release page https://github.com/rohan200311/Ai-Chat/releases/tag/v1.0.0 will show:
```
Assets
- AiChat-debug.apk (32KB)
- AiChat.apk
- app-debug.apk (full build, 15-25MB if built via Actions)
- Source code (zip) - also contains APK inside
- Source code (tar.gz)
```

## Files Ready
- `AiChat-debug.apk` - minimal installable, package `com.aichat.app`, valid zip with classes.dex
- `BUILD_APK.md` - full build instructions + fix explanation
- `tools/build-apk-workflow.yml` - CI workflow to auto-upload

## One-Liner for You (Copy-Paste)
```bash
# On your local machine:
gh release upload v1.0.0 AiChat-debug.apk --clobber && echo "✅ APK now in Assets!"
```
