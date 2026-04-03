package com.fitness.app.presentation.auth.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * A Google "G" logo drawn entirely via Canvas — no image assets needed.
 */
@Composable
private fun GoogleGLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.18f
        val radius = w * 0.40f
        val center = Offset(w * 0.48f, h * 0.50f)

        // Blue arc (top-right)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -45f,
            sweepAngle = -75f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )

        // Green arc (bottom-right)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = -90f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )

        // Yellow arc (bottom-left)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 135f,
            sweepAngle = -90f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )

        // Red arc (top-left)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -120f,
            sweepAngle = -75f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )

        // Horizontal bar of the "G" (blue)
        val barY = center.y
        val barStartX = center.x
        val barEndX = center.x + radius + strokeWidth * 0.3f
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(barStartX, barY),
            end = Offset(barEndX, barY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt
        )
    }
}

/**
 * Reusable Google Sign-In button that matches the app's dark theme.
 * - Black/transparent background
 * - Purple (#7B5CF0) border
 * - Rounded pill shape
 * - Google "G" logo + "Continue with Google" text
 */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val purpleAccent = Color(0xFF7B5CF0)

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(25.dp),
        border = BorderStroke(1.dp, purpleAccent.copy(alpha = if (enabled) 0.6f else 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = 0.4f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            GoogleGLogo()
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Continue with Google",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * "or" divider that separates email/password login from social login.
 */
@Composable
fun OrDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.5.dp,
            color = Color.White.copy(alpha = 0.2f)
        )
        Text(
            text = "  or  ",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.5.dp,
            color = Color.White.copy(alpha = 0.2f)
        )
    }
}
