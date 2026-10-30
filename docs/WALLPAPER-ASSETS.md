# Bundled wallpaper collection — first selection increment

Created 30 October 2026 with the built-in imagegen tool, using text-only prompts and no reference images or downloaded stock photographs. The artwork contains no third-party logos or branded imagery. No external image licence is asserted; generated outputs are not proof of exclusive copyright. Review redistribution/ownership requirements before any public release. Repository visibility is unchanged and no public release is made by this increment.

The approved mountain remains unchanged and is still the default; see ASSET-INVENTORY.md for its original generation record.

| Stable ID | Title | Bundled asset | Thumbnail |
| --- | --- | --- | --- |
| mountain | Mountain horizon | `app/src/main/res/drawable-nodpi/cinematic_horizon.webp` | `cinematic_horizon_thumb.webp` |
| coast | Tidal dusk | `app/src/main/res/drawable-nodpi/tidal_dusk.webp` | `tidal_dusk_thumb.webp` |
| desert | Desert afterglow | `app/src/main/res/drawable-nodpi/desert_afterglow.webp` | `desert_afterglow_thumb.webp` |

All full assets are 1672 × 941, opaque RGB WebP quality 88. Small dedicated thumbnails are bounded to 320 × 180, WebP quality 82. Conversion only encodes/resizes the original generated outputs; no creative edits or external image sources. Original PNGs are preserved under the machine's Codex generated_images directory; runtime assets are self-contained in the repository. Installed application icons remain package-manager assets, not bundled art.

Runtime decoding runs on IO, reads image bounds before decode, caps decoded dimensions at 2048 × 1152 via sample size, and precomposites the existing scrims once. One full backdrop is retained in the cache; the display can transiently retain the previous backdrop during a replacement. Published bitmaps are never manually recycled. A mutex serializes full-image decodes; cancelled loads cannot replace the cache. The original mountain's dimensions and scrim calculations are unchanged.

## Exact prompts

### tidal_dusk

Use case: stylized-concept. Asset type: original cinematic wallpaper for an Android TV launcher, landscape 16:9. Create a premium naturalistic seascape at blue hour: dark sculpted basalt cliffs receding along a tranquil coast, distant silver-blue ocean and a subtle amber seam of sunset on the far right horizon, soft marine mist, restrained teal and graphite palette, realistic fine stone and water texture, serene expansive cinematic photography. Composition: wide uninterrupted landscape, left third darker calm negative space for navigation, bottom third dark and uncluttered for glass application cards, visual detail in upper right. Full bleed image only. No UI, text, people, buildings, logos or watermarks. Not a collage. High quality wide 1920x1080 or equivalent 16:9.

### desert_afterglow

Use case: stylized-concept. Asset type: original cinematic wallpaper for an Android TV launcher, landscape 16:9. Create a premium naturalistic desert landscape after sunset: sweeping dark bronze sand dunes in the foreground, distant layered rose sandstone mountains, a narrow warm copper glow near the right horizon fading into a deep indigo night sky, a few subtle stars, delicate atmospheric depth, exquisite realistic sand texture, restrained colour, luxurious calm cinematic photography. Composition: wide uninterrupted landscape, left third darker calm negative space for navigation, bottom third dark and uncluttered for glass application cards, focal landscape in upper right. Full bleed image only. No UI, text, people, buildings, logos or watermarks. Not a collage. High quality wide 1920x1080 or equivalent 16:9.

