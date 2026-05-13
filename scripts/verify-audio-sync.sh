#!/usr/bin/env bash
# Phase 7 PRF-03 verification — run on dev PC against a pulled MP4.
# Usage: ./scripts/verify-audio-sync.sh /path/to/Bugzz_*.mp4
#
# PASS criteria (D-10/11):
#   |start_drift| < 0.050  (audio.start_time - video.start_time, in seconds)
#   |dur_drift|   < 0.050  (audio.duration   - video.duration,   in seconds)
#   1795 <= frames <= 1805 (for a 60s @ 30fps recording; informational for shorter clips)
#
# Exit codes:
#   0 — PASS (both drifts under threshold)
#   1 — FAIL (one or both drifts exceed threshold)
#   2 — usage error (missing/invalid mp4 arg)
#   3 — ffprobe not on PATH
#
# Windows note: invoke through git-bash / MSYS / WSL. The shebang routes to bash;
# no chmod needed on Windows since the bash interpreter handles exec.

set -eu

mp4="${1:-}"
if [ -z "$mp4" ] || [ ! -f "$mp4" ]; then
    echo "Usage: $0 <path/to/video.mp4>" >&2
    exit 2
fi

if ! command -v ffprobe >/dev/null 2>&1; then
    echo "ERROR: ffprobe (from FFmpeg) not found on PATH" >&2
    echo "  Install: winget install ffmpeg (Windows) | apt install ffmpeg (Linux) | brew install ffmpeg (Mac)" >&2
    exit 3
fi

audio_start=$(ffprobe -v 0 -select_streams a:0 -show_entries stream=start_time -of csv=p=0 "$mp4")
audio_dur=$(ffprobe -v 0   -select_streams a:0 -show_entries stream=duration   -of csv=p=0 "$mp4")
video_start=$(ffprobe -v 0 -select_streams v:0 -show_entries stream=start_time -of csv=p=0 "$mp4")
video_dur=$(ffprobe -v 0   -select_streams v:0 -show_entries stream=duration   -of csv=p=0 "$mp4")
nb_frames=$(ffprobe -v 0   -select_streams v:0 -count_frames -show_entries stream=nb_read_frames -of csv=p=0 "$mp4")

# Drift math via awk (bc not always available on Windows git-bash; awk is in coreutils).
start_drift=$(awk -v a="$audio_start" -v v="$video_start" 'BEGIN { printf "%.6f", a - v }')
dur_drift=$(awk   -v a="$audio_dur"   -v v="$video_dur"   'BEGIN { printf "%.6f", a - v }')

# Absolute values + threshold comparison via awk; mirrors |x| < 0.050 in pure POSIX.
abs_start=$(awk -v d="$start_drift" 'BEGIN { if (d < 0) d = -d; printf "%.6f", d }')
abs_dur=$(awk   -v d="$dur_drift"   'BEGIN { if (d < 0) d = -d; printf "%.6f", d }')

start_pass=$(awk -v d="$abs_start" 'BEGIN { print (d < 0.050) ? "PASS" : "FAIL" }')
dur_pass=$(awk   -v d="$abs_dur"   'BEGIN { print (d < 0.050) ? "PASS" : "FAIL" }')

# Frame-count band check (informational — only meaningful at the 60s @ 30fps target).
if [ -n "$nb_frames" ] && [ "$nb_frames" -ge 1795 ] && [ "$nb_frames" -le 1805 ] 2>/dev/null; then
    frames_pass="PASS"
else
    frames_pass="INFO (60s@30fps target band is 1795-1805; shorter/longer clips report INFO)"
fi

echo "================================================================"
echo "Phase 7 PRF-03 verification — $mp4"
echo "================================================================"
echo "audio_start: $audio_start s"
echo "video_start: $video_start s"
echo "   start_drift: $start_drift s (|$abs_start| < 0.050 → $start_pass)"
echo ""
echo "audio_dur:   $audio_dur s"
echo "video_dur:   $video_dur s"
echo "   dur_drift:   $dur_drift s (|$abs_dur| < 0.050 → $dur_pass)"
echo ""
echo "video frames counted: $nb_frames ($frames_pass)"
echo "================================================================"

# Exit 0 only if BOTH start_drift AND dur_drift pass the <50ms threshold.
# Frame-count is informational only (a 3.5s smoke-test clip should not fail the script).
if [ "$start_pass" = "PASS" ] && [ "$dur_pass" = "PASS" ]; then
    echo "RESULT: PASS (both drifts under 50ms)"
    exit 0
else
    echo "RESULT: FAIL (drift threshold exceeded)"
    exit 1
fi
