# Fix FileItem More Menu Auto-Popup Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix the bug where entering the file browser list page automatically pops up every file item's "more menu" (and it cannot be closed) — menus must stay closed until the user taps the more button.

**Architecture:** The `FileItem` composable owns a local `showMenu` state. The more-menu `Popup` should render only when `showMenu == true`. Currently the Popup's render condition ignores `showMenu` entirely (`if (moreMenuContent != null)`), so the menu renders on first composition and dismissal is a no-op. The fix gates both Popup render sites (directory branch and file branch) on `showMenu`. A Compose UI instrumented test locks the behavior in via TDD.

**Tech Stack:** Kotlin, Jetpack Compose (Popup, IconButton), Compose UI Test (`createComposeRule`, AndroidJUnit4), Gradle (AGP 8.2.2).

---

## Context for the Implementer

**Symptom (bug report):** After entering the file browser list page ("文件浏览器"), the file item "more menu" (更多菜单) pops up automatically, and tapping outside does not close it.

**Root cause** (verified in code):

1. `app/src/main/java/com/tdull/webdavviewer/app/ui/browser/FileItem.kt:71` declares `var showMenu by remember { mutableStateOf(false) }`.
2. `FileItem.kt:175` and `FileItem.kt:287` (IconButton onClick): `if (moreMenuContent != null) showMenu = true else onMoreClick()` — the button correctly sets `showMenu = true`.
3. **The bug:** `FileItem.kt:185` and `FileItem.kt:297` render the `Popup` under condition `if (moreMenuContent != null)`. The condition **never reads `showMenu`**, so:
   - The Popup renders immediately when the item composes (auto-popup on page entry).
   - `onDismissRequest = { showMenu = false }` (FileItem.kt:187, 299) changes `showMenu`, but nothing observes it for visibility → menu cannot close.
4. `FileBrowserScreen.kt:381` always passes a non-null `moreMenuContent` lambda, so **every** list item exhibits the bug.

**The fix (one-line, two sites):** change both conditions to `if (showMenu && moreMenuContent != null)`.

**Build environment** (from AGENTS.md, Git Bash on Windows):

```bash
export JAVA_HOME="/d/app/jdk-17.0.14+7"
export ANDROID_HOME="/d/app_data/android/sdk"
export PATH="$JAVA_HOME/bin:$PATH"
```

**Test runner note:** The instrumented tests require a running device or emulator. Verify with `adb devices` (expect a device listed) before running `connectedDebugAndroidTest`.

---

### Task 1: Write the failing UI test

**Files:**
- Create: `app/src/androidTest/java/com/tdull/webdavviewer/app/ui/browser/FileItemTest.kt`

**Step 1: Write the failing test**

Create `app/src/androidTest/java/com/tdull/webdavviewer/app/ui/browser/FileItemTest.kt` with exactly this content:

```kotlin
package com.tdull.webdavviewer.app.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tdull.webdavviewer.app.data.model.ResourceType
import com.tdull.webdavviewer.app.data.model.WebDAVResource
import com.tdull.webdavviewer.app.ui.theme.WebDAVViewerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FileItem 更多菜单（Popup）行为测试
 * 回归背景：进入文件浏览器列表页时，菜单曾自动弹出且无法关闭
 * （Popup 渲染条件未检查 showMenu 导致）
 */
@RunWith(AndroidJUnit4::class)
class FileItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testFile = WebDAVResource(
        path = "/video.mp4",
        name = "video.mp4",
        isDirectory = false,
        size = 1024L,
        lastModified = 0L,
        contentType = "video/mp4",
        resourceType = ResourceType.VIDEO
    )

    private val testDirectory = WebDAVResource(
        path = "/folder",
        name = "folder",
        isDirectory = true,
        size = 0L,
        lastModified = 0L,
        contentType = null,
        resourceType = ResourceType.DIRECTORY
    )

    /**
     * 渲染单个 FileItem，并提供一个最小可用的 moreMenuContent：
     * 菜单内容为一个可点击文本，点击后调用 onDismiss 关闭菜单。
     */
    private fun setFileItemContent(resource: WebDAVResource) {
        composeTestRule.setContent {
            WebDAVViewerTheme {
                FileItem(
                    resource = resource,
                    onClick = {},
                    moreMenuContent = { onDismiss ->
                        Text(
                            text = "MENU_ITEM",
                            modifier = Modifier.clickable { onDismiss() }
                        )
                    }
                )
            }
        }
    }

    @Test
    fun moreMenu_isHiddenInitially_fileItem() {
        // 回归：进入页面时菜单不得自动弹出（非目录分支）
        setFileItemContent(testFile)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MENU_ITEM").assertDoesNotExist()
    }

    @Test
    fun moreMenu_isHiddenInitially_directoryItem() {
        // 回归：进入页面时菜单不得自动弹出（目录分支）
        setFileItemContent(testDirectory)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MENU_ITEM").assertDoesNotExist()
    }

    @Test
    fun moreMenu_showsAfterClick_andMenuItemClickCloses() {
        // 点击"更多操作"按钮后菜单显示；点击菜单项（内部调用 onDismiss）后菜单关闭
        setFileItemContent(testFile)
        composeTestRule.onNodeWithContentDescription("更多操作").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MENU_ITEM").assertIsDisplayed()

        composeTestRule.onNodeWithText("MENU_ITEM").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MENU_ITEM").assertDoesNotExist()
    }
}
```

Design notes for the implementer:
- The two `isHiddenInitially` tests target the bug directly (Popup must not render before the user taps the more button). One covers the non-directory branch (`FileItem.kt:297` Popup), one covers the directory branch (`FileItem.kt:185` Popup) — both sites have the identical bug.
- The third test verifies the full interaction loop: tap more button → menu visible; tap menu item → menu closes (the same `showMenu = false` path that `onDismissRequest` uses).
- `WebDAVViewerTheme` wrapper is required because `FileItem` reads `MaterialTheme.typography` / `MaterialTheme.colorScheme`.

**Step 2: Run the test to verify it fails (RED)**

Ensure a device/emulator is attached, then run:

```bash
export JAVA_HOME="/d/app/jdk-17.0.14+7"
export ANDROID_HOME="/d/app_data/android/sdk"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.tdull.webdavviewer.app.ui.browser.FileItemTest
```

Expected: **BUILD FAILED**. All 3 tests fail:
- `moreMenu_isHiddenInitially_fileItem` — fails because "MENU_ITEM" exists (auto-popup bug).
- `moreMenu_isHiddenInitially_directoryItem` — fails for the same reason.
- `moreMenu_showsAfterClick_andMenuItemClickCloses` — fails at the final `assertDoesNotExist` because dismissing does not hide the Popup.

If the build fails to compile instead, re-check the imports and the `WebDAVResource` constructor signature (`path, name, isDirectory, size, lastModified, contentType, resourceType`).

---

### Task 2: Implement the fix (gate Popup on showMenu)

**Files:**
- Modify: `app/src/main/java/com/tdull/webdavviewer/app/ui/browser/FileItem.kt:185` (directory branch)
- Modify: `app/src/main/java/com/tdull/webdavviewer/app/ui/browser/FileItem.kt:297` (file branch)

**Step 1: Change both Popup render conditions**

In `FileItem.kt` the exact string `if (moreMenuContent != null) {` appears exactly twice (lines ~185 and ~297). Replace **both** occurrences with:

```kotlin
if (showMenu && moreMenuContent != null) {
```

(Either use your editor's replace-all on that exact string, or make two edits — one in the directory branch after the `KeyboardArrowRight` icon block, one in the non-directory branch after the favorite star button block.)

Do NOT change anything else:
- Keep `IconButton` onClick logic as-is (`if (moreMenuContent != null) showMenu = true else onMoreClick()`).
- Keep `onDismissRequest = { showMenu = false }` as-is.
- Keep `PopupProperties(focusable = true)` as-is.

**Step 2: Run the test to verify it passes (GREEN)**

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.tdull.webdavviewer.app.ui.browser.FileItemTest
```

Expected: **BUILD SUCCESSFUL** — all 3 tests pass (`moreMenu_isHiddenInitially_fileItem`, `moreMenu_isHiddenInitially_directoryItem`, `moreMenu_showsAfterClick_andMenuItemClickCloses`).

**Step 3: Regression check**

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Expected: both **BUILD SUCCESSFUL** (no unit test regressions, no compile warnings introduced).

**Step 4: Commit**

```bash
git add app/src/main/java/com/tdull/webdavviewer/app/ui/browser/FileItem.kt app/src/androidTest/java/com/tdull/webdavviewer/app/ui/browser/FileItemTest.kt
git commit -m "fix(browser): keep file item more menu closed until opened" -m "FileItem 的更多菜单 Popup 渲染条件未检查 showMenu，导致进入文件浏览器列表页时菜单自动弹出且无法关闭。将两处渲染条件改为 showMenu && moreMenuContent != null，菜单仅在点击更多按钮后显示，点击菜单项或外部关闭。补充 Compose UI 回归测试覆盖目录/非目录两个分支。"
```

---

### Task 3: Manual smoke verification (optional but recommended)

**Files:** none (verification only)

**Step 1: Install and manually verify on device**

```bash
./gradlew installDebug
```

Manual checks on the device:
1. Open the app → connect → enter the file browser list page. **Expected:** no more-menu popups appear on any list item.
2. Tap the ⋮ (更多操作) button on a video file row. **Expected:** the popup menu (重命名/移动/删除) appears anchored to the button.
3. Tap a menu item or tap outside. **Expected:** the menu closes.
4. Repeat steps 2-3 on a directory row.

---

## Verification Summary

| What | Command | Expected |
|------|---------|----------|
| RED test (before fix) | `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.tdull.webdavviewer.app.ui.browser.FileItemTest` | 3 tests FAIL (menu exists initially / won't close) |
| GREEN test (after fix) | same command | 3 tests PASS |
| Unit regression | `./gradlew testDebugUnitTest` | PASS |
| Compile | `./gradlew assembleDebug` | PASS |

## Notes & Assumptions

- `showMenu` stays as plain `remember` (not `rememberSaveable`): if the menu is open during a configuration change (rotation), closing it is acceptable and matches standard popup behavior. Not part of this fix.
- Downloads page (`DownloadsScreen.kt:260`) uses `FileItem` without `moreMenuContent` (null), so it is unaffected by the condition change.
- Commit only when the user has asked for the commit (project rule); Task 2 Step 4 assumes the user approved committing this fix.
