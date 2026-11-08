"""The shop mark, drawn once and used for both launchers.

The store bag from the design's icon, with the Saeed green as ground. Drawn at
1080px and downsampled so the curves stay clean at small sizes; the shop and the
admin panel differ only in the disc behind the bag.
"""
from PIL import Image, ImageDraw

GREEN = (1, 125, 32, 255)        # SaeedGreen
GREEN_DARK = (1, 96, 22, 255)    # SaeedGreenDark
WHITE = (255, 255, 255, 255)
S = 1080                          # master size, drawn at 4x a 270px icon


def draw_bag(draw, cx, cy, scale, body=WHITE, slot=None, handle=None, width=None):
    """A shopping bag centred on (cx, cy).

    The body is a plain rounded rectangle; the meaning lives in the handle: a
    *narrow, tall* arch whose feet sit on the top edge, half the bag's width
    across. A wide shallow arch reads as a suitcase, so the proportions matter
    more than the shapes.
    """
    w = scale
    h = scale * 0.86
    top, bottom = cy - h * 0.42, cy + h * 1.12
    r = w * 0.20
    draw.rounded_rectangle([cx - w, top, cx + w, bottom], radius=r, fill=body)

    # The slot: a bar in the ground colour, so the body reads as a bag rather
    # than as a blank card even at 24dp.
    slot_color = slot if slot is not None else GREEN
    if slot_color[3] != 0:
        sw, sh = w * 0.40, h * 0.10
        draw.rounded_rectangle(
            [cx - sw, cy + h * 0.30, cx + sw, cy + h * 0.30 + sh],
            radius=sh / 2, fill=slot_color,
        )

    # The handle: narrower than half the body and taller than a semicircle, so
    # the silhouette is a shopping bag. Its feet sit on the top edge and are
    # rounded, so the arch grows out of the body instead of floating above it.
    stroke = int(width or w * 0.145)
    hr = w * 0.33
    rise = h * 0.62
    colour = handle if handle is not None else body
    draw.arc(
        [cx - hr, top - rise, cx + hr, top + rise * 0.42],
        start=182, end=358, fill=colour, width=stroke,
    )
    for x in (cx - hr, cx + hr):
        draw.ellipse(
            [x - stroke * 0.55, top - stroke * 0.55, x + stroke * 0.55, top + stroke * 0.55],
            fill=colour,
        )


def disc_icon(background, bag_body=WHITE, slot=GREEN, ring=None):
    """Full-bleed square icon: a rounded disc on a ground, bag centred."""
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S, S], fill=background)
    if ring is not None:
        m = S * 0.30
        d.ellipse([m, m, S - m, S - m], outline=ring, width=int(S * 0.035))
    draw_bag(d, S / 2, S / 2, S * 0.27, body=bag_body, slot=slot)
    return img


def adaptive_foreground(bag_body=WHITE, slot=GREEN):
    """Transparent 108dp-layer foreground; the mark stays in the safe zone."""
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # 66/108 is the diameter the launcher can never crop away.
    draw_bag(d, S / 2, S / 2, S * 0.235, body=bag_body, slot=slot)
    return img


def monochrome():
    """One colour on transparent, which the system tints itself.

    Android 13+ draws themed icons by recolouring this layer, so the colour must
    be a single opaque tone - not black, which the system would read as "no
    tint", and not the ground, which would vanish into the mask.
    """
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    draw_bag(d, S / 2, S / 2, S * 0.235, body=WHITE, slot=(0, 0, 0, 0), handle=WHITE)
    return img
