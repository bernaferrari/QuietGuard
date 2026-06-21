package com.bernaferari.renetguard.ui.main

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun rememberStatusBadgeShape(progress: Float): Shape {
    val morph = remember { Morph(start = MaterialShapes.Square, end = MaterialShapes.Cookie9Sided) }
    return rememberMorphShape(morph = morph, progress = progress)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun rememberStatusCardShape(progress: Float): Shape {
    val morph = remember { Morph(start = MaterialShapes.Square, end = MaterialShapes.Gem) }
    return rememberMorphShape(morph = morph, progress = progress)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun rememberMorphShape(
    morph: Morph,
    progress: Float,
): Shape =
    remember(morph, progress) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density,
            ): Outline {
                val path = morph.toPath(progress = progress, startAngle = 0)
                val scaleMatrix = Matrix().apply { scale(x = size.width, y = size.height) }
                path.transform(scaleMatrix)

                val bounds = path.getBounds()
                val translateX = (size.width / 2f) - bounds.center.x
                val translateY = (size.height / 2f) - bounds.center.y
                path.transform(
                    Matrix().apply {
                        translate(x = translateX, y = translateY)
                    },
                )

                return Outline.Generic(path)
            }
        }
    }