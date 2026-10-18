# Compact heading banners

The current banners use Minecraft's actual bitmap letter shapes, the supplied mod icon, and a vanilla paper item sprite. They are laid out as simple SVG graphics and exported to transparent PNGs, without newly generated illustration.

Each image is 800 x 96 pixels. The icon is 80 x 80 pixels, the letters use a regular eight-pixel grid, and the only text shadow is a two-pixel flat offset. There is no background panel, texture, bevel or black padding.

- `modern-screen.png`: diamond advancement icon
- `requirements.png`: paper advancement icon
- `old-screen.png`: book advancement icon
- `settings.png`: pickaxe advancement icon

The matching SVG files are editable source assets. `build-banners.cjs` rebuilds both formats with Node.js and the `sharp` package. `source/glyphs.json` stores the used glyph pixels from `source/ascii.png`, so rebuilding does not need a Minecraft installation.

The mod logo was supplied by the project authors. The font and paper texture are original Minecraft 26.3 assets, copyright Mojang Studios. The full mod logo is embedded unchanged and clipped to its existing icon frames in SVG.

`PROMPTS.md` documents the superseded generated designs. Those are not used by the current description.
