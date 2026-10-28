package com.bernaferrari.quietguard.ui.theme

import com.materialkolor.PaletteStyle

/**
 * How the chosen color becomes a whole palette: the same four styles as WiFi Weakspot and ChangeDetection.
 * QuietGuard defaults to Tonal, its calm all-teal look.
 */
enum class ColorStyle(val id: String, val palette: PaletteStyle) {
    Expressive("expressive", PaletteStyle.Expressive),
    Vibrant("vibrant", PaletteStyle.Vibrant),
    Tonal("tonal", PaletteStyle.TonalSpot),
    Muted("muted", PaletteStyle.Cmf()),
    ;

    companion object {
        fun from(value: String?): ColorStyle = entries.firstOrNull { it.id == value } ?: Tonal
    }
}
