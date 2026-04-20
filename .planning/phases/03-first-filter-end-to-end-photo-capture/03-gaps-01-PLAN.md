---
phase: 03-first-filter-end-to-end-photo-capture
plan: gaps-01
type: execute
wave: null
depends_on: []
autonomous: true
gap_closure: true
files_modified:
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_00.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_01.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_02.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_03.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_04.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_05.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_06.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_07.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_08.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_09.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_10.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_11.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_12.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_13.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_14.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_15.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_16.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_17.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_18.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_19.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_20.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_21.png
  - app/src/main/assets/sprites/spider_on_forehead_v1/frame_22.png
requirements: [REN-07-visual]
threat_refs: []
---

<objective>
Re-extract the spider sprite frames from the reference APK Lottie JSON (`spider_prankfilter.json`) to fix the empty/transparent frames discovered during Phase 3 HANDOFF device verification (2026-04-20). The filter swap pipeline works correctly (logcat confirmed), but the visual output is invisible because `frame_00.png` through `frame_22.png` contain only a faint whitish silhouette (~1-2% non-alpha pixels). Root cause: Plan 03-03 Task 1 extracted the wrong Lottie layer (outline/stroke layer instead of the filled graphics layer).

This is a Phase 4 prerequisite gap. It should be executed before Phase 4 adds spider to the filter catalog picker, otherwise the catalog will ship an invisible filter. It is scoped as a standalone gap plan (not part of the main Phase 4 plan waves) so the Phase 4 planner can either execute it as Wave 0 of Phase 4 or fold it into Phase 4 Task 1 (full sprite extraction for all 15-25 catalog filters).

**Phase 4 planner guidance:** If Phase 4 Task 1 re-extracts ALL reference APK sprites (recommended — fixes spider + adds the remaining catalog sprites in one pass), this gap plan is superseded and can be skipped. File this plan as awareness context, not mandatory pre-work.
</objective>

<context>
@.planning/phases/03-first-filter-end-to-end-photo-capture/03-CONTEXT.md (§D-05 sprite extraction spec, §D-06 asset layout)
@.planning/phases/03-first-filter-end-to-end-photo-capture/03-03-SUMMARY.md (Task 1 extraction method — Node.js base64 decode from Lottie JSON `"p":` fields)
@.planning/phases/03-first-filter-end-to-end-photo-capture/03-05-SUMMARY.md (Soft Gap 1 — evidence of empty frames)
</context>

<gap_root_cause>
## Root Cause Analysis

Plan 03-03 Task 1 used a Node.js script to extract PNG frames from `spider_prankfilter.json` by parsing Lottie JSON `"p":` fields (base64-encoded PNG data). The ant extraction from `home_lottie.json` succeeded because `home_lottie.json` embeds the full-color fill graphics layer. However, `spider_prankfilter.json` contains multiple layers — at minimum a fill/graphics layer and a stroke/outline layer. The extraction script picked up the outline/stroke layer instead of the fill layer.

**Evidence from device verification (2026-04-20):**
- `filter=spider_on_forehead_v1 frame=20..22` logcat confirmed — pipeline executes correctly
- Visual: no visible bug on forehead in screenshot
- Frame content inspection: `frame_00.png` and `frame_11.png` = mostly transparent, faint whitish silhouette (~1-2% non-alpha pixels)
- Ant frames (same extraction method, different source JSON): correct full-color content

**Hypothesis:** `spider_prankfilter.json` Lottie structure has layers like `"nm": "Spider_outline"` (extracted) and `"nm": "Spider_fill"` or `"nm": "Spider_body"` (not extracted). The extraction script selected the first matching layer with `"p":` entries rather than the fill/color layer.
</gap_root_cause>

<tasks>

<task type="auto">
  <name>Task 1: Investigate spider_prankfilter.json layer structure and identify correct layer</name>
  <files>reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk (read-only), app/src/main/assets/sprites/spider_on_forehead_v1/*.png</files>
  <read_first>
    .planning/phases/03-first-filter-end-to-end-photo-capture/03-03-SUMMARY.md (Task 1 extraction script pattern),
    app/src/main/assets/sprites/spider_on_forehead_v1/manifest.json (current manifest — frameCount, frameDurationMs)
  </read_first>
  <action>
    1. Re-unpack reference APK with `apktool` (or `unzip`) to access `assets/` directory.
    2. Read `spider_prankfilter.json` (or locate the spider Lottie JSON — may be at a different path than assumed; search with `find . -name "*spider*"`).
    3. Inspect Lottie JSON layer structure: list all layer `"nm"` (name) fields + check which layers contain `"assets"` with `"p":` (base64 PNG) entries.
    4. Identify the fill/body layer (largest pixel count non-alpha content vs outline layer).
    5. Document: which layer name contains the spider's filled graphics, how many frames it has, and whether it differs from the 23-frame count used in Phase 3.

    If `apktool` output already exists in a temp directory from Plan 03-03, reuse it. If not, re-run:
    ```bash
    apktool d reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk -o /tmp/ref-apk-extract --force-all
    ```
    Then:
    ```bash
    find /tmp/ref-apk-extract -name "*spider*" -o -name "*Spider*" | head -20
    ```
  </action>
  <acceptance_criteria>
    - Layer structure documented (which layer has fill content)
    - Correct asset layer name identified
    - Frame count confirmed (may differ from 23 used in Phase 3)
  </acceptance_criteria>
  <done>Spider Lottie layer structure understood. Correct layer name recorded for Task 2 extraction.</done>
</task>

<task type="auto">
  <name>Task 2: Re-extract spider frames from correct Lottie layer + replace app assets</name>
  <files>
    app/src/main/assets/sprites/spider_on_forehead_v1/frame_00.png .. frame_NN.png,
    app/src/main/assets/sprites/spider_on_forehead_v1/manifest.json
  </files>
  <read_first>
    app/src/main/assets/sprites/spider_on_forehead_v1/manifest.json
  </read_first>
  <action>
    Using the correct layer name identified in Task 1, write a targeted Node.js extraction script that:
    1. Reads `spider_prankfilter.json`
    2. Finds the assets referenced by the CORRECT layer (fill/body graphics layer, not outline)
    3. Base64-decodes each `"p":` value to PNG
    4. Writes `frame_00.png` .. `frame_NN.png` to a staging directory
    5. Validates each frame: pixel inspect to confirm > 10% non-alpha pixels (if still transparent after targeting correct layer, the Lottie source may use vector shapes not raster embeds — see fallback below)

    **Script pattern (from Plan 03-03 Task 1 — adapt layer selection):**
    ```javascript
    const fs = require('fs');
    const data = JSON.parse(fs.readFileSync('spider_prankfilter.json', 'utf8'));
    // Find assets for the FILL layer by layer name
    const fillLayer = data.layers.find(l => l.nm === '<CORRECT_LAYER_NAME_FROM_TASK_1>');
    // ... extract refId -> asset -> "p" (base64) -> decode -> write
    ```

    **If frames are non-raster (vector shapes, no `"p":` in assets for fill layer):**
    Fallback: render the Lottie JSON frames to PNG using `lottie-render` npm package or `lottie-to-apng`. If no raster is available, document the finding and defer to Phase 4 to source an alternative spider sprite asset.

    **After extraction:**
    - Move new frames to `app/src/main/assets/sprites/spider_on_forehead_v1/`
    - Update `manifest.json` if frameCount changed
    - Verify with Android Studio image viewer or `file frame_00.png` + pixel content check

    Commit: `fix(03-gaps-01): re-extract spider sprite frames from correct Lottie fill layer`
  </action>
  <acceptance_criteria>
    - `app/src/main/assets/sprites/spider_on_forehead_v1/frame_00.png` is non-transparent (> 10% non-alpha pixels) — visual confirmation via pixel inspection
    - All frame files exist (frame_00.png through frame_NN.png where N = correct frame count from Task 1)
    - `manifest.json` frameCount matches actual frame count
    - `./gradlew :app:testDebugUnitTest` exits 0 (no regressions from asset file replacement)
    - `./gradlew :app:assembleDebug` exits 0 (APK includes new frames)
  </acceptance_criteria>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest :app:assembleDebug</automated>
  </verify>
  <done>Spider sprite frames contain visible content. Build GREEN. Phase 4 filter catalog can use spider_on_forehead_v1 as a non-empty filter.</done>
</task>

</tasks>

<verification>
- `app/src/main/assets/sprites/spider_on_forehead_v1/frame_00.png` pixel content > 10% non-alpha
- `./gradlew :app:testDebugUnitTest` exits 0
- `./gradlew :app:assembleDebug` exits 0
- Visual on-device (optional): install new APK, switch to spider filter, confirm visible bug on forehead
</verification>

<success_criteria>
- Spider sprite frames re-extracted from correct Lottie layer with visible content
- Asset layout matches D-06 spec (frame_NN.png + manifest.json)
- No unit test regressions
- Phase 4 filter catalog can include `spider_on_forehead_v1` as a visually-functional filter
</success_criteria>

<output>
After completion, create `.planning/phases/03-first-filter-end-to-end-photo-capture/03-gaps-01-SUMMARY.md` documenting the correct Lottie layer name, frame count, extraction method, and before/after pixel content comparison.
</output>
