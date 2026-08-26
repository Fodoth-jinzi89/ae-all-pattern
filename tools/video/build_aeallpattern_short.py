#!/usr/bin/env python3
"""Build the 40-50 second AE All Pattern narrated promo video."""

from __future__ import annotations

import argparse
import json
import subprocess
from pathlib import Path


VOICE = "zh-CN-XiaoxiaoNeural"
RATE = "+25%"
TARGET_VIDEO_DURATION = 46.25
TARGET_TTS_DURATION = 43.8

LINES = [
    "大家好，我是浪柒九九。",
    "自从我的通用动力",
    "模组火了以后，",
    "我又做了这个新模组。",
    "它叫全样板。",
    "专治AE样板地狱。",
    "先用绑定器选择链接器。",
    "再潜行右击目标机器。",
    "一次选择，连续绑定。",
    "紫色框代表连接成功。",
    "机器配方会自动进AE。",
    "不用再手搓处理样板。",
    "直接在终端里下单。",
    "输入先被安全接管。",
    "机器堵塞也不会吞材料。",
    "不管产物来自哪里，",
    "都会自动回到AE。",
    "原版和通用机械都支持。",
    "还能重载和持久保存。",
    "关注浪柒九九，等我发布。",
]

SUBTITLE_OVERRIDES = {
    0: "大家好，我是浪柒99。",
    19: "关注浪柒99，等我发布。",
}


def run(*args: str) -> None:
    subprocess.run(args, check=True)


def probe_duration(path: Path) -> float:
    result = subprocess.run(
        [
            "ffprobe",
            "-v",
            "error",
            "-show_entries",
            "format=duration",
            "-of",
            "default=noprint_wrappers=1:nokey=1",
            str(path),
        ],
        check=True,
        capture_output=True,
        text=True,
    )
    return float(result.stdout.strip())


def is_valid_audio(path: Path) -> bool:
    if not path.exists() or path.stat().st_size < 512:
        return False
    result = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(path)],
        capture_output=True,
        text=True,
    )
    return result.returncode == 0 and bool(result.stdout.strip())


def ass_time(seconds: float) -> str:
    centiseconds = round(seconds * 100)
    hours, centiseconds = divmod(centiseconds, 360000)
    minutes, centiseconds = divmod(centiseconds, 6000)
    secs, centiseconds = divmod(centiseconds, 100)
    return f"{hours}:{minutes:02d}:{secs:02d}.{centiseconds:02d}"


def build_tts(work: Path) -> tuple[Path, Path, Path]:
    tts_dir = work / "tts"
    tts_dir.mkdir(parents=True, exist_ok=True)
    voice_wavs: list[Path] = []
    voice_durations: list[float] = []

    for index, line in enumerate(LINES):
        mp3 = tts_dir / f"{index:02d}.mp3"
        wav = tts_dir / f"{index:02d}.wav"
        if not is_valid_audio(mp3):
            mp3.unlink(missing_ok=True)
            run(
                "uvx",
                "--from",
                "edge-tts",
                "edge-tts",
                "--voice",
                VOICE,
                f"--rate={RATE}",
                "--text",
                line,
                "--write-media",
                str(mp3),
            )
        run(
            "ffmpeg",
            "-y",
            "-loglevel",
            "error",
            "-i",
            str(mp3),
            "-ar",
            "48000",
            "-ac",
            "2",
            "-c:a",
            "pcm_s16le",
            str(wav),
        )
        voice_wavs.append(wav)
        voice_durations.append(probe_duration(wav))

    spoken = sum(voice_durations)
    gap = max(0.10, min(0.55, (TARGET_TTS_DURATION - spoken) / (len(LINES) - 1)))
    padded_wavs: list[Path] = []
    subtitle_events: list[tuple[float, float, str]] = []
    cursor = 0.0

    for index, (line, wav, duration) in enumerate(zip(LINES, voice_wavs, voice_durations, strict=True)):
        padded = tts_dir / f"{index:02d}_padded.wav"
        line_gap = gap if index < len(LINES) - 1 else 0.0
        run(
            "ffmpeg",
            "-y",
            "-loglevel",
            "error",
            "-i",
            str(wav),
            "-af",
            f"apad=pad_dur={line_gap:.4f}",
            "-ar",
            "48000",
            "-ac",
            "2",
            "-c:a",
            "pcm_s16le",
            str(padded),
        )
        padded_wavs.append(padded)
        subtitle_text = SUBTITLE_OVERRIDES.get(index, line)
        subtitle_events.append((cursor, cursor + duration + min(0.12, line_gap), subtitle_text))
        cursor += duration + line_gap

    concat_file = tts_dir / "concat.txt"
    concat_file.write_text(
        "".join(f"file '{path.resolve().as_posix()}'\n" for path in padded_wavs),
        encoding="utf-8",
    )
    tts_wav = work / "tts_xiaoxiao.wav"
    run(
        "ffmpeg",
        "-y",
        "-loglevel",
        "error",
        "-f",
        "concat",
        "-safe",
        "0",
        "-i",
        str(concat_file),
        "-c:a",
        "pcm_s16le",
        str(tts_wav),
    )

    ass = work / "subtitles.ass"
    ass_header = """[Script Info]
ScriptType: v4.00+
PlayResX: 1920
PlayResY: 1080
WrapStyle: 2
ScaledBorderAndShadow: yes

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Main,PingFang SC,72,&H00FFFFFF,&H00FFFFFF,&H00000000,&H80000000,-1,0,0,0,100,100,1,0,1,6,1,2,80,80,72,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
"""
    ass_events = "".join(
        f"Dialogue: 0,{ass_time(start)},{ass_time(end)},Main,,0,0,0,,{line}\n"
        for start, end, line in subtitle_events
    )
    ass.write_text(ass_header + ass_events, encoding="utf-8")

    manifest = work / "tts_manifest.json"
    manifest.write_text(
        json.dumps(
            {
                "voice": VOICE,
                "rate": RATE,
                "gap_seconds": gap,
                "duration": probe_duration(tts_wav),
                "lines": [
                    {"start": start, "end": end, "text": line}
                    for start, end, line in subtitle_events
                ],
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    return tts_wav, ass, manifest


def render_subtitle_images(manifest: Path, work: Path) -> list[Path]:
    subtitle_dir = work / "subtitle_pngs"
    subtitle_dir.mkdir(parents=True, exist_ok=True)
    run(
        "uv",
        "run",
        "--with",
        "pillow",
        "python",
        str(Path(__file__).with_name("render_subtitle_cards.py")),
        "--manifest",
        str(manifest),
        "--output",
        str(subtitle_dir),
    )
    return sorted(subtitle_dir.glob("*.png"))


def build_video(
        source: Path,
        intro: Path,
        bgm: Path,
        output: Path,
        tts: Path,
        manifest: Path,
        subtitle_pngs: list[Path],
        work: Path) -> None:
    crop = "crop=2842:1598:161:126,scale=1920:1080,fps=60,setsar=1"
    subtitle_data = json.loads(manifest.read_text(encoding="utf-8"))["lines"]
    overlays: list[str] = []
    previous = "base"
    for index, event in enumerate(subtitle_data):
        output_label = "vout" if index == len(subtitle_data) - 1 else f"sub{index}"
        overlays.append(
            f"[{previous}][{4 + index}:v]overlay=x=0:y=840:"
            f"enable='between(t,{event['start']:.3f},{event['end']:.3f})'[{output_label}]"
        )
        previous = output_label

    filters = f"""
[1:v]scale=1920:1080:force_original_aspect_ratio=increase,crop=1920:1080,zoompan=z='min(zoom+0.00014,1.045)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=300:s=1920x1080:fps=60,setsar=1[intro];
[0:v]trim=start=0:end=12,setpts=(PTS-STARTPTS)/2.4,{crop}[v1];
[0:v]trim=start=39.2:end=45.5,setpts=(PTS-STARTPTS)/1.2,{crop}[v2];
[0:v]trim=start=46.5:end=58.0,setpts=(PTS-STARTPTS)/2.0,{crop}[v3];
[0:v]trim=start=96:end=109,setpts=(PTS-STARTPTS)/2.0,{crop}[v4];
[0:v]trim=start=58.0:end=71.5,setpts=(PTS-STARTPTS)/2.0,{crop}[v5];
[0:v]trim=start=78:end=90,setpts=(PTS-STARTPTS)/2.4,{crop}[v6];
[0:v]trim=start=149:end=166.5,setpts=(PTS-STARTPTS)/2.5,{crop}[v7];
[intro][v1][v2][v3][v5][v4][v6][v7]concat=n=8:v=1:a=0[base];
{";".join(overlays)};
[2:a]apad=whole_dur={TARGET_VIDEO_DURATION},atrim=duration={TARGET_VIDEO_DURATION},loudnorm=I=-16:LRA=7:TP=-1.5,asplit=2[voice_sc][voice_mix];
[3:a]atrim=start=0:end={TARGET_VIDEO_DURATION},asetpts=PTS-STARTPTS,volume=0.18,afade=t=in:st=0:d=1.0,afade=t=out:st={TARGET_VIDEO_DURATION - 1.2}:d=1.2[bg];
[bg][voice_sc]sidechaincompress=threshold=0.018:ratio=9:attack=18:release=320[ducked];
[ducked][voice_mix]amix=inputs=2:duration=first:dropout_transition=0:normalize=0,alimiter=limit=0.95,aresample=48000[aout]
""".strip()
    filter_script = work / "filter_complex.txt"
    filter_script.write_text(filters + "\n", encoding="utf-8")
    output.parent.mkdir(parents=True, exist_ok=True)
    command = [
        "ffmpeg",
        "-y",
        "-i",
        str(source),
        "-i",
        str(intro),
        "-i",
        str(tts),
        "-i",
        str(bgm),
    ]
    for subtitle_png in subtitle_pngs:
        command.extend(["-loop", "1", "-t", str(TARGET_VIDEO_DURATION), "-i", str(subtitle_png)])
    command.extend([
        "-filter_complex_script",
        str(filter_script),
        "-map",
        "[vout]",
        "-map",
        "[aout]",
        "-c:v",
        "libx264",
        "-preset",
        "medium",
        "-crf",
        "18",
        "-pix_fmt",
        "yuv420p",
        "-r",
        "60",
        "-c:a",
        "aac",
        "-b:a",
        "192k",
        "-movflags",
        "+faststart",
        "-t",
        str(TARGET_VIDEO_DURATION),
        str(output),
    ])
    run(*command)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--intro", type=Path, required=True)
    parser.add_argument("--bgm", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--work", type=Path, required=True)
    args = parser.parse_args()
    args.work.mkdir(parents=True, exist_ok=True)
    tts, _ass, manifest = build_tts(args.work)
    subtitle_pngs = render_subtitle_images(manifest, args.work)
    build_video(args.source, args.intro, args.bgm, args.output, tts, manifest, subtitle_pngs, args.work)


if __name__ == "__main__":
    main()
