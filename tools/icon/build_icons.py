"""Writes every launcher asset both apps need, from the one mark.

Run from the repository root:  python3 tools/icon/build_icons.py

Android expects a legacy bitmap per density for launchers that predate the
adaptive icon (API 24 and 25), an adaptive XML for API 26+, and a monochrome
layer for themed icons on Android 13+. All three are produced here so the two
apps cannot drift apart.
"""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from PIL import Image  # noqa: E402
from mark import GREEN, disc_icon, adaptive_foreground, monochrome  # noqa: E402

# Launcher icon sizes per density, as the Android tools lay them out.
DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# The admin panel keeps the same bag on the app's dark ink, and its slot in the
# panel's cyan accent, so the two icons are related but never confusable.
ADMIN_INK = (18, 21, 26, 255)
ADMIN_ACCENT = (0, 240, 255, 255)

ROOT = pathlib.Path(__file__).resolve().parents[2]


def write(path: pathlib.Path, image: Image.Image) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, "PNG", optimize=True)


def build(module: str, square: Image.Image, foreground: Image.Image, mono: Image.Image) -> None:
    res = ROOT / module / "src" / "main" / "res"
    for density, size in DENSITIES.items():
        # Some launchers mask the square one; a round icon is the same mark with
        # a circular crop, which the system can also do itself, so one drawing
        # serves both names.
        write(res / f"mipmap-{density}" / "ic_launcher.png", square.resize((size, size), Image.LANCZOS))
        write(res / f"mipmap-{density}" / "ic_launcher_round.png", square.resize((size, size), Image.LANCZOS))
    # The adaptive layers are 108dp squares; the system crops them.
    write(res / "drawable-nodpi" / "ic_launcher_foreground.png", foreground)
    write(res / "drawable-nodpi" / "ic_launcher_monochrome.png", mono)
    print(f"{module}: {len(DENSITIES)} densities plus the adaptive layers")


def main() -> None:
    store = disc_icon(GREEN)
    admin = disc_icon(ADMIN_INK, slot=ADMIN_ACCENT)
    build("customer", store, adaptive_foreground(), monochrome())
    build("admin", admin, adaptive_foreground(), monochrome())


if __name__ == "__main__":
    main()
