package org.pixelrush.moneyiq.ui.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Exact clone of 1Money's toolbar_right.xml vector.
val DoubleChevronRight: ImageVector by lazy {
    ImageVector.Builder(
        name           = "DoubleChevronRight",
        defaultWidth   = 24.dp,
        defaultHeight  = 24.dp,
        viewportWidth  = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill            = SolidColor(Color.Transparent),
            stroke          = SolidColor(Color.Black),
            strokeLineWidth = 1.25f,
            strokeLineCap   = StrokeCap.Round,
            strokeLineJoin  = StrokeJoin.Round
        ) {
            moveTo(13.346f, 1.688f)
            lineTo(20.52f, 12.001f)
            lineTo(13.346f, 22.314f)
        }
        path(
            fill            = SolidColor(Color.Transparent),
            stroke          = SolidColor(Color.Black),
            strokeLineWidth = 1.25f,
            strokeLineCap   = StrokeCap.Round,
            strokeLineJoin  = StrokeJoin.Round
        ) {
            moveTo(4f, 1.688f)
            curveTo(3.507f, 1.688f, 3.334f, 2.018f, 3.615f, 2.421f)
            lineTo(9.765f, 11.261f)
            curveTo(10.047f, 11.712f, 10.047f, 12.283f, 9.765f, 12.733f)
            lineTo(3.617f, 21.576f)
            curveTo(3.336f, 21.982f, 3.509f, 22.31f, 4.002f, 22.31f)
            lineTo(7.027f, 22.31f)
            curveTo(7.58f, 22.279f, 8.094f, 22.012f, 8.436f, 21.576f)
            lineTo(14.587f, 12.736f)
            curveTo(14.869f, 12.286f, 14.869f, 11.714f, 14.587f, 11.264f)
            lineTo(8.438f, 2.424f)
            curveTo(8.095f, 1.988f, 7.582f, 1.721f, 7.029f, 1.69f)
            lineTo(4f, 1.688f)
        }
    }.build()
}

// Exact clone of 1Money's toolbar_left.xml vector.
val DoubleChevronLeft: ImageVector by lazy {
    ImageVector.Builder(
        name           = "DoubleChevronLeft",
        defaultWidth   = 24.dp,
        defaultHeight  = 24.dp,
        viewportWidth  = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill            = SolidColor(Color.Transparent),
            stroke          = SolidColor(Color.Black),
            strokeLineWidth = 1.25f,
            strokeLineCap   = StrokeCap.Round,
            strokeLineJoin  = StrokeJoin.Round
        ) {
            moveTo(10.654f, 1.688f)
            lineTo(3.48f, 12.001f)
            lineTo(10.654f, 22.314f)
        }
        path(
            fill            = SolidColor(Color.Transparent),
            stroke          = SolidColor(Color.Black),
            strokeLineWidth = 1.25f,
            strokeLineCap   = StrokeCap.Round,
            strokeLineJoin  = StrokeJoin.Round
        ) {
            moveTo(20f, 1.688f)
            curveTo(20.493f, 1.688f, 20.666f, 2.018f, 20.384f, 2.421f)
            lineTo(14.235f, 11.261f)
            curveTo(13.953f, 11.711f, 13.953f, 12.283f, 14.235f, 12.733f)
            lineTo(20.384f, 21.574f)
            curveTo(20.666f, 21.979f, 20.493f, 22.307f, 20f, 22.307f)
            lineTo(16.975f, 22.307f)
            curveTo(16.422f, 22.277f, 15.908f, 22.01f, 15.566f, 21.574f)
            lineTo(9.412f, 12.736f)
            curveTo(9.131f, 12.286f, 9.131f, 11.714f, 9.412f, 11.264f)
            lineTo(15.562f, 2.424f)
            curveTo(15.904f, 1.988f, 16.418f, 1.721f, 16.971f, 1.69f)
            lineTo(20f, 1.688f)
        }
    }.build()
}

// 1Money's toolbar_profile.xml vector with lighter stroke for the top bar.
val ToolbarProfileIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ToolbarProfileIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(7.078f, 9.891f)
            arcToRelative(4.922f, 4.922f, 0f, true, false, 9.844f, 0f)
            arcToRelative(4.922f, 4.922f, 0f, true, false, -9.844f, 0f)
        }
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(18.481f, 20.321f)
            arcToRelative(9.137f, 9.137f, 0f, false, false, -12.962f, 0f)
        }
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(1.453f, 12f)
            arcToRelative(10.547f, 10.547f, 0f, true, false, 21.094f, 0f)
            arcToRelative(10.547f, 10.547f, 0f, true, false, -21.094f, 0f)
        }
    }.build()
}

// Exact clone of 1Money's toolbar_edit.xml vector with lighter stroke for the top bar.
val ToolbarEditIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ToolbarEditIcon",
        defaultWidth = 25.dp,
        defaultHeight = 25.dp,
        viewportWidth = 25f,
        viewportHeight = 25f
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4.168f, 23.172f)
            lineTo(4.168f, 12.927f)
            lineTo(9.069f, 15.571f)
            lineTo(12.501f, 13.119f)
            lineTo(15.933f, 15.571f)
            lineTo(20.833f, 12.927f)
            lineTo(20.833f, 23.172f)
        }
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4.177f, 12.926f)
            lineTo(11.703f, 2.244f)
            curveTo(12.094f, 1.69f, 12.916f, 1.69f, 13.306f, 2.244f)
            lineTo(20.832f, 12.926f)
        }
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(9.068f, 15.571f)
            lineTo(9.068f, 23.172f)
        }
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(15.933f, 15.571f)
            lineTo(15.933f, 23.172f)
        }
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(8.187f, 7.236f)
            lineTo(16.814f, 7.236f)
        }
    }.build()
}

// 1Money's nav_settings.xml vector with lighter stroke for the top bar.
val ToolbarSettingsIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ToolbarSettingsIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(10.637f, 3.036f)
            arcToRelative(1.835f, 1.835f, 0f, false, false, 2.726f, 0f)
            lineTo(14.25f, 2.063f)
            arcToRelative(1.837f, 1.837f, 0f, false, true, 3.197f, 1.325f)
            lineToRelative(-0.067f, 1.313f)
            arcToRelative(1.836f, 1.836f, 0f, false, false, 1.923f, 1.926f)
            lineToRelative(1.313f, -0.067f)
            arcToRelative(1.837f, 1.837f, 0f, false, true, 1.322f, 3.197f)
            lineToRelative(-0.977f, 0.881f)
            arcToRelative(1.837f, 1.837f, 0f, false, false, 0f, 2.727f)
            lineToRelative(0.977f, 0.881f)
            arcToRelative(1.837f, 1.837f, 0f, false, true, -1.325f, 3.197f)
            lineToRelative(-1.313f, -0.067f)
            arcToRelative(1.836f, 1.836f, 0f, false, false, -1.928f, 1.928f)
            lineToRelative(0.067f, 1.313f)
            arcToRelative(1.837f, 1.837f, 0f, false, true, -3.189f, 1.321f)
            lineToRelative(-0.882f, -0.976f)
            arcToRelative(1.837f, 1.837f, 0f, false, false, -2.726f, 0f)
            lineToRelative(-0.886f, 0.976f)
            arcToRelative(1.837f, 1.837f, 0f, false, true, -3.193f, -1.32f)
            lineToRelative(0.068f, -1.313f)
            arcToRelative(1.836f, 1.836f, 0f, false, false, -1.928f, -1.928f)
            lineToRelative(-1.313f, 0.067f)
            arcToRelative(1.836f, 1.836f, 0f, false, true, -1.327f, -3.194f)
            lineToRelative(0.976f, -0.881f)
            arcToRelative(1.837f, 1.837f, 0f, false, false, 0f, -2.727f)
            lineToRelative(-0.976f, -0.886f)
            arcToRelative(1.836f, 1.836f, 0f, false, true, 1.32f, -3.193f)
            lineToRelative(1.313f, 0.067f)
            arcToRelative(1.836f, 1.836f, 0f, false, false, 1.929f, -1.931f)
            lineToRelative(-0.062f, -1.315f)
            arcToRelative(1.837f, 1.837f, 0f, false, true, 3.193f, -1.321f)
            close()
        }
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(7.781f, 12.001f)
            arcToRelative(4.219f, 4.219f, 0f, true, false, 8.438f, 0f)
            arcToRelative(4.219f, 4.219f, 0f, true, false, -8.438f, 0f)
        }
    }.build()
}
