package com.bernaferrari.quietguard.ui.main

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toPath
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph

/** Hero core: a calm faceted Gem when off that blooms into a 9-sided cookie when on. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun rememberHeroCoreShape(progress: State<Float>): Shape = remember(progress) {
    MorphShape(Morph(MaterialShapes.Gem, MaterialShapes.Cookie9Sided)) { progress.value }
}

/** Inner halo behind the core while protection is on. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun rememberHeroInnerHaloShape(): Shape = MaterialShapes.Sunny.toShape()

/** Outer, wider-scalloped halo; counter-rotates against the inner one. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun rememberHeroOuterHaloShape(): Shape = MaterialShapes.Cookie12Sided.toShape()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private class MorphShape(private val morph: Morph, private val progress: () -> Float) : Shape {
    private val path = Path()
    private val matrix = Matrix()
    private var lastSize = Size.Unspecified
    private var lastProgress = Float.NaN
    private var outline = Outline.Generic(path)

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val fraction = progress().coerceIn(0f, 1f)
        // The layer keeps rotating after the morph settles.
        if (size == lastSize && fraction == lastProgress) return outline
        morph.toPath(fraction, path)
        matrix.reset()
        matrix.scale(size.width, size.height)
        path.transform(matrix)
        path.translate(size.center - path.getBounds().center)
        lastSize = size
        lastProgress = fraction
        outline = Outline.Generic(path)
        return outline
    }
}
