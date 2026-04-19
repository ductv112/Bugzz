# ADR 02-01: ML Kit Tracking + Contour Mode are Mutually Exclusive

**Status:** Accepted (2026-04-19)
**Phase:** 02 (Camera Preview + Face Detection)
**Gap reference:** GAP-02-A (02-VERIFICATION.md)
**Authors:** gsd-planner (gap closure wave 1), Claude (gsd-verifier evidence)

## Context

Phase 2 plan 02-03 wired `FaceDetectorClient.buildOptions()` per CONTEXT.md D-15 with
both `.setContourMode(CONTOUR_MODE_ALL)` and `.enableTracking()`. Research
`.planning/research/PITFALLS.md §3 (line ~110)` recommended `.enableTracking()` for
stable per-face identity without noting that Google ML Kit silently ignores the call
when contour mode is active.

Device verification on Xiaomi 13T Pro / HyperOS (2026-04-19) produced **459/459
FaceTracker log frames with `face.trackingId == null`** over a 20-second continuous
face hold. `FaceDetectorOptions.isTrackingEnabled` still reflects `true` (which is why
the unit test passed pre-gap-closure), but the detector runtime emits faces with null
trackingId.

This is documented Google ML Kit behavior — contour-mode detection and per-face
tracking use disjoint runtime paths.

## Decision

1. **Remove `.enableTracking()` from `FaceDetectorClient.buildOptions()`.** Do not
   re-add it as long as `setContourMode(CONTOUR_MODE_ALL)` is present.
2. **Keep `CONTOUR_MODE_ALL`** — contour points are load-bearing for Phase 3+
   (filter anchoring to nose bridge, cheeks, jawline, lips). Dropping contour to
   regain tracking would rewrite Phase 4 CRAWL behavior.
3. **Defer full bbox-IoU face-identity tracking to Phase 3.** Phase 2 accepts
   `face.trackingId == null` and keys the 1€ filter on the `-1` sentinel (all faces
   share one smoother bucket). Phase 2's device runbook uses a single-face scenario,
   so cross-face state contamination is not observable in this phase.
4. **Phase 3 prerequisite:** implement a `BboxIouTracker` utility that assigns
   stable integer IDs based on boundingBox centroid-IoU frame-over-frame. Feed the
   assigned ID into `LandmarkSmoother` as the state key, replacing the `-1` sentinel
   path.

## Consequences

- `LandmarkSmoother` state in Phase 2 is recomputed every frame (keyed on `-1`);
  1€ smoothing degrades to per-frame initialization. On a still head in Phase 2's
  runbook this still reduces jitter versus raw landmarks, because 1€ internally
  maintains per-"key" state and reinitialization on the first call is cheap.
- `CAM-08` acceptance relaxes to "boundingBox centerX/centerY persists across
  consecutive frames when face is held still"; the original "trackingId remains
  stable across 60+ frames" is downgraded to "trackingId may be null — documented
  ML Kit limitation in contour mode."
- Multi-face scenarios cross-contaminate smoother state in Phase 2. Phase 3 must
  land `BboxIouTracker` before any production filter ships, else two faces confuse
  the 1€ smoother's cached per-landmark state.

## Follow-ups (Phase 3 planner must action)

- [ ] Implement `BboxIouTracker` (spatial-proximity tracker, cost: ~100 LOC +
  unit tests with contrived frame sequences).
- [ ] Re-key `LandmarkSmoother` on `BboxIouTracker` ID, not on `face.trackingId`.
- [ ] Update `FaceDetectorClient.createAnalyzer()` to pass the tracker handle into
  the MlKitAnalyzer callback.
- [ ] Update `02-VERIFICATION.md` CAM-08 row on next re-verification (Phase 3 exit)
  to reference the new tracker.

## Alternatives Considered

| Alternative | Rejected because |
|-------------|------------------|
| Switch to `LANDMARK_MODE_ALL + .enableTracking()` | Loses contour points; Phase 4 CRAWL needs them. Research SUMMARY.md Resolution #2 already locked this. |
| Run two detectors (contour + landmark+tracking, merged by bbox) | 2× CPU cost; PITFALLS #4 says we're already at the backpressure edge on mid-tier devices. Explicit non-goal. |
| Wait for ML Kit fix | No timeline. Google documentation explicitly says the two are exclusive; not a bug. |
| Implement bbox-IoU tracker in Phase 2 | Out of scope — would bloat Phase 2 past the risk-front-loaded boundary. Gap-closure charter is to close the current gap, not introduce net-new production code. |
