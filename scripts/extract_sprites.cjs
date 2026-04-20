// scripts/extract_sprites.cjs
// Wave 0 sprite extraction: Lottie JSON base64-PNG frames → app/src/main/assets/sprites/
// Run from project root: node scripts/extract_sprites.cjs
//
// Sources:
//   - reference/raw_extract/res/raw/spider_prankfilter.json  → sprite_spider (23 frames, image_0..image_22)
//   - reference/raw_extract/res/raw/home_lottie.json         → sprite_bugA (7 frames, imgSeq_0..6)
//                                                            → sprite_bugB (12 frames, imgSeq_14..25)
//                                                            → sprite_bugC (16 frames, imgSeq_38..53)
//
// Per 04-CONTEXT D-03/D-04/D-05/D-30 and 04-RESEARCH §Lottie Extraction Script.
// Manifest schema matches SpriteManifest.kt (kotlinx.serialization @Serializable).
// anchorType/behavior/scaleFactor are placeholder defaults — FilterDefinition (Plan 04-04) overrides.

'use strict';

const fs = require('fs');
const path = require('path');

// 4 output groups per 04-CONTEXT D-02 amended roster.
// assetIds null = take all assets with non-empty .p (spider_prankfilter.json flat structure).
// Explicit assetId arrays for home_lottie.json three-group split (confirmed by pre-run dump).
const GROUPS = [
  {
    file: 'reference/raw_extract/res/raw/spider_prankfilter.json',
    outputDir: 'sprite_spider',
    assetIds: null,  // flat: image_0..image_22, all 23 with base64 PNG data
    displayName: 'Spider',
    anchorType: 'FOREHEAD',
    behavior: 'STATIC',
    scaleFactor: 0.30,
  },
  {
    file: 'reference/raw_extract/res/raw/home_lottie.json',
    outputDir: 'sprite_bugA',
    assetIds: ['imgSeq_0','imgSeq_1','imgSeq_2','imgSeq_3','imgSeq_4','imgSeq_5','imgSeq_6'],
    displayName: 'Bug A',
    anchorType: 'NOSE_TIP',
    behavior: 'CRAWL',
    scaleFactor: 0.22,
  },
  {
    file: 'reference/raw_extract/res/raw/home_lottie.json',
    outputDir: 'sprite_bugB',
    assetIds: [
      'imgSeq_14','imgSeq_15','imgSeq_16','imgSeq_17','imgSeq_18','imgSeq_19',
      'imgSeq_20','imgSeq_21','imgSeq_22','imgSeq_23','imgSeq_24','imgSeq_25',
    ],
    displayName: 'Bug B',
    anchorType: 'NOSE_TIP',
    behavior: 'CRAWL',
    scaleFactor: 0.22,
  },
  {
    file: 'reference/raw_extract/res/raw/home_lottie.json',
    outputDir: 'sprite_bugC',
    assetIds: [
      'imgSeq_38','imgSeq_39','imgSeq_40','imgSeq_41','imgSeq_42','imgSeq_43',
      'imgSeq_44','imgSeq_45','imgSeq_46','imgSeq_47','imgSeq_48','imgSeq_49',
      'imgSeq_50','imgSeq_51','imgSeq_52','imgSeq_53',
    ],
    displayName: 'Bug C',
    anchorType: 'NOSE_TIP',
    behavior: 'CRAWL',
    scaleFactor: 0.22,
  },
];

const BASE = 'app/src/main/assets/sprites';

// D-05 validation: minimum PNG byte size to reject truly-empty/blank frames.
//
// NOTE: The research formula (buf.length / (w * h * 4) > 0.10) is WRONG for PNG.
// PNG compression is so efficient that even colorful sprites on transparent backgrounds
// compress to <1% of their uncompressed RGBA size (e.g., a 1500x1500 spider sprite
// with real artwork compresses to ~6-8KB, giving density 0.0007 — well below 0.10).
//
// Correct approach: compare absolute file size against a minimum threshold.
//   - A fully-transparent all-alpha PNG (any size) compresses to ~200–500 bytes.
//   - Any real sprite content (even thin outlines) produces >=2KB of PNG data.
// Threshold: 2000 bytes — safely rejects only fully-blank/corrupt frames.
//
// Rule 1 deviation: research formula assumed uncompressed RGBA bitmaps, not PNG files.
const MIN_BYTES = 2000;

let totalExtracted = 0;
let totalSkipped = 0;

for (const group of GROUPS) {
  const { file, outputDir, assetIds, displayName, anchorType, behavior, scaleFactor } = group;

  console.log(`\n=== ${outputDir} (${displayName}) ===`);
  console.log(`Source: ${file}`);
  console.log(`Asset IDs filter: ${assetIds === null ? 'ALL (flat file)' : assetIds.join(', ')}`);

  let data;
  try {
    data = JSON.parse(fs.readFileSync(file, 'utf8'));
  } catch (e) {
    console.error(`ERROR: Cannot read/parse ${file}: ${e.message}`);
    process.exit(1);
  }

  const fr = data.fr || 24;
  const frameDurationMs = Math.round(1000 / fr);
  console.log(`Frame rate: ${fr} fps → ${frameDurationMs}ms/frame`);

  // Filter assets: must have non-empty .p (base64 PNG data) and match assetIds if specified.
  const allImgAssets = data.assets.filter(a => a.p && a.p.length > 50);
  const chosen = assetIds === null
    ? allImgAssets
    : allImgAssets.filter(a => assetIds.includes(a.id));

  console.log(`Matched ${chosen.length} assets (${allImgAssets.length} total with base64 data)`);

  // Print all matched IDs for sanity check.
  if (chosen.length > 0) {
    console.log(`Asset IDs found: ${chosen.map(a => a.id).join(', ')}`);
  }

  const outDir = path.join(BASE, outputDir);
  fs.mkdirSync(outDir, { recursive: true });

  let extracted = 0;
  let skipped = 0;

  chosen.forEach((asset, idx) => {
    // Strip data-URI prefix if present (some Lottie tools embed it, some don't).
    const b64 = asset.p.replace(/^data:image\/[a-z]+;base64,/, '');
    const buf = Buffer.from(b64, 'base64');

    const w = asset.w || 360;
    const h = asset.h || 360;
    const fname = `frame_${String(idx).padStart(2, '0')}.png`;
    const outPath = path.join(outDir, fname);

    // D-05 validation: reject frames smaller than MIN_BYTES (likely blank/transparent-only).
    if (buf.length < MIN_BYTES) {
      console.warn(
        `  SKIP ${outputDir}/${fname} (id=${asset.id}, ${buf.length}B < ${MIN_BYTES}B threshold, ${w}x${h})`
      );
      skipped++;
      return;
    }

    // Verify valid PNG magic bytes (89 50 4E 47 0D 0A 1A 0A).
    if (buf[0] !== 0x89 || buf[1] !== 0x50 || buf[2] !== 0x4E || buf[3] !== 0x47) {
      console.warn(
        `  SKIP ${outputDir}/${fname} (id=${asset.id}, invalid PNG header: ${buf.slice(0,4).toString('hex')})`
      );
      skipped++;
      return;
    }

    fs.writeFileSync(outPath, buf);
    extracted++;
    console.log(
      `  OK   ${outputDir}/${fname}  id=${asset.id}  ${buf.length}B  ${w}x${h}`
    );
  });

  totalExtracted += extracted;
  totalSkipped += skipped;

  // Manifest — SpriteManifest.kt schema (kotlinx.serialization @Serializable compatible).
  // behaviorConfig (Plan 04-04 extension D-29) omitted here — backwards-compatible null default.
  const manifest = {
    id: outputDir,
    displayName,
    frameCount: extracted,
    frameDurationMs,
    anchorType,    // placeholder — overridden by FilterDefinition.anchorType per D-30
    behavior,      // placeholder — overridden by FilterDefinition.behavior per D-30
    scaleFactor,
    mirrorable: true,
  };

  const manifestPath = path.join(outDir, 'manifest.json');
  fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));

  const statusMsg = skipped > 0
    ? `WARNING: ${skipped} frames skipped (below ${MIN_BYTES}B threshold)`
    : 'OK';
  console.log(`→ ${extracted} frames extracted, ${skipped} skipped. manifest.json written. [${statusMsg}]`);
}

console.log(`\n=== DONE: ${totalExtracted} frames total, ${totalSkipped} skipped ===`);

// Exit non-zero if any group produced 0 frames (D-05 collapse rule).
for (const group of GROUPS) {
  const manifestPath = path.join(BASE, group.outputDir, 'manifest.json');
  if (!fs.existsSync(manifestPath)) {
    console.error(`FAIL: ${group.outputDir}/manifest.json not created.`);
    process.exit(1);
  }
  const m = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
  if (m.frameCount < 1) {
    console.error(`FAIL: ${group.outputDir} produced 0 frames — all frames rejected by D-05. Check Lottie source.`);
    process.exit(1);
  }
  if (m.frameCount < 5) {
    console.warn(`WARN: ${group.outputDir} has only ${m.frameCount} frames — below recommended minimum 5.`);
  }
}

console.log('All groups passed minimum-frame check. Extraction complete.');
