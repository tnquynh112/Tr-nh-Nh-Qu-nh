package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuestionVisual(part: Int, questionId: Int, modifier: Modifier = Modifier) {
    val hasVisual = when {
        part == 1 && questionId == 1 -> true
        part == 1 && questionId == 2 -> true
        part == 2 && questionId == 1 -> true
        part == 2 && questionId == 3 -> true
        part == 2 && questionId == 5 -> true
        part == 2 && questionId == 6 -> true
        part == 3 && questionId == 4 -> true
        else -> false
    }

    if (!hasVisual) {
        return
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hình vẽ / Bảng số liệu minh họa:",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    part == 1 && questionId == 1 -> CựcTrịVariationTable()
                    part == 1 && questionId == 2 -> TiệmCậnGraph()
                    part == 2 && questionId == 1 -> HàmNhấtBiếnTable()
                    part == 2 && questionId == 3 -> TrùngPhươngTable()
                    part == 2 && questionId == 5 -> HộpChữNhật3D()
                    part == 2 && questionId == 6 -> XétDấuĐạoHàmTable()
                    part == 3 && questionId == 4 -> MặtCầuNgoạiTiếpHộp()
                }
            }
        }
    }
}

@Composable
fun DefaultPlaceholderDiagram(part: Int, questionId: Int) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // Draw grid lines
        for (i in 1..4) {
            drawLine(
                color = outlineVariant.copy(alpha = 0.3f),
                start = Offset(0f, h * i / 5),
                end = Offset(w, h * i / 5),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = outlineVariant.copy(alpha = 0.3f),
                start = Offset(w * i / 5, 0f),
                end = Offset(w * i / 5, h),
                strokeWidth = 1.dp.toPx()
            )
        }
        // Nice abstract math design
        drawCircle(
            color = primaryColor.copy(alpha = 0.1f),
            radius = h * 0.35f,
            center = Offset(w / 2, h / 2)
        )
        drawCircle(
            color = primaryColor.copy(alpha = 0.4f),
            radius = h * 0.35f,
            center = Offset(w / 2, h / 2),
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
        )
    }
    Text(
        text = "Sơ đồ hỗ trợ tính toán\n(Phần $part - Câu $questionId)",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            lineHeight = 16.sp
        )
    )
}

@Composable
fun CựcTrịVariationTable() {
    // Variation table for y = x³ - 3x² + 2
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val colHeader = 1.5f
        val col1 = 1.0f  // -∞
        val col2 = 1.5f  // interval
        val col3 = 1.0f  // 0
        val col4 = 1.5f  // interval
        val col5 = 1.0f  // 2
        val col6 = 1.5f  // interval
        val col7 = 1.0f  // +∞
        val totalColsWeight = col1 + col2 + col3 + col4 + col5 + col6 + col7 // 8.5f

        // Row x
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TableCell("x", weight = colHeader, isHeader = true)
            TableCell("-∞", weight = col1)
            TableCell("", weight = col2)
            TableCell("0", weight = col3, isHeader = true)
            TableCell("", weight = col4)
            TableCell("2", weight = col5, isHeader = true)
            TableCell("", weight = col6)
            TableCell("+∞", weight = col7)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Row y'
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TableCell("y'", weight = colHeader, isHeader = true)
            TableCell("", weight = col1)
            TableCell("+", weight = col2)
            TableCell("0", weight = col3, isHeader = true)
            TableCell("-", weight = col4)
            TableCell("0", weight = col5, isHeader = true)
            TableCell("+", weight = col6)
            TableCell("", weight = col7)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Row y
        Row(modifier = Modifier.fillMaxWidth().height(100.dp), verticalAlignment = Alignment.CenterVertically) {
            TableCell("y", weight = colHeader, isHeader = true)
            
            Box(modifier = Modifier.weight(totalColsWeight).fillMaxHeight()) {
                // Background Canvas for arrows
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val p1X = w * (0.5f / totalColsWeight)
                    val p2X = w * (3.0f / totalColsWeight)
                    val p3X = w * (5.5f / totalColsWeight)
                    val p4X = w * (8.0f / totalColsWeight)

                    val pMaxY = h * 0.25f
                    val pMinY = h * 0.75f

                    // Draw arrows
                    drawLine(color = Color(0xFF1E88E5), start = Offset(p1X + 5f, pMinY - 5f), end = Offset(p2X - 5f, pMaxY + 5f), strokeWidth = 2.dp.toPx())
                    drawArrow(this, Offset(p1X + 5f, pMinY - 5f), Offset(p2X - 5f, pMaxY + 5f), Color(0xFF1E88E5))

                    drawLine(color = Color(0xFF1E88E5), start = Offset(p2X + 5f, pMaxY + 5f), end = Offset(p3X - 5f, pMinY - 5f), strokeWidth = 2.dp.toPx())
                    drawArrow(this, Offset(p2X + 5f, pMaxY + 5f), Offset(p3X - 5f, pMinY - 5f), Color(0xFF1E88E5))

                    drawLine(color = Color(0xFF1E88E5), start = Offset(p3X + 5f, pMinY - 5f), end = Offset(p4X - 5f, pMaxY + 5f), strokeWidth = 2.dp.toPx())
                    drawArrow(this, Offset(p3X + 5f, pMinY - 5f), Offset(p4X - 5f, pMaxY + 5f), Color(0xFF1E88E5))
                }

                // Foreground layout for text labels
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(col1).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        Text("-∞", fontSize = 11.sp, maxLines = 1, softWrap = false, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Spacer(modifier = Modifier.weight(col2))
                    Box(modifier = Modifier.weight(col3).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                        Text("2", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), maxLines = 1, softWrap = false, modifier = Modifier.padding(top = 8.dp))
                    }
                    Spacer(modifier = Modifier.weight(col4))
                    Box(modifier = Modifier.weight(col5).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        Text("-2", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828), maxLines = 1, softWrap = false, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Spacer(modifier = Modifier.weight(col6))
                    Box(modifier = Modifier.weight(col7).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                        Text("+∞", fontSize = 11.sp, maxLines = 1, softWrap = false, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TiệmCậnGraph() {
    // Coordinate graph showing y = (2x - 1)/(x - 1)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val outline = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        val density = LocalDensity.current
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        
        // Origin shifted slightly to the bottom-left to show first quadrant clearly
        val ox = w / 2f - with(density) { 20.dp.toPx() }
        val oy = h / 2f + with(density) { 30.dp.toPx() }
        
        // Scale unit: 36dp is safe and large enough on all screen densities
        val u = with(density) { 36.dp.toPx() }
        
        // Vertical Asymptote x = 1 (dashed red)
        val asympX = ox + 1f * u
        // Horizontal Asymptote y = 2 (dashed blue)
        val asympY = oy - 2f * u
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw coordinate axes with arrows at positive ends
            drawLine(color = outline.copy(alpha = 0.5f), start = Offset(0f, oy), end = Offset(w, oy), strokeWidth = 1.25.dp.toPx())
            drawLine(color = outline.copy(alpha = 0.5f), start = Offset(ox, 0f), end = Offset(ox, h), strokeWidth = 1.25.dp.toPx())
            
            // Draw x-axis arrow
            val arrowSize = 6.dp.toPx()
            val pathX = Path().apply {
                moveTo(w, oy)
                lineTo(w - arrowSize * 1.5f, oy - arrowSize)
                lineTo(w - arrowSize * 1.5f, oy + arrowSize)
                close()
            }
            drawPath(pathX, color = outline.copy(alpha = 0.6f))
            
            // Draw y-axis arrow
            val pathY = Path().apply {
                moveTo(ox, 0f)
                lineTo(ox - arrowSize, arrowSize * 1.5f)
                lineTo(ox + arrowSize, arrowSize * 1.5f)
                close()
            }
            drawPath(pathY, color = outline.copy(alpha = 0.6f))
            
            // Vertical Asymptote x = 1 (dashed red)
            drawLine(
                color = Color(0xFFE53935),
                start = Offset(asympX, 0f),
                end = Offset(asympX, h),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )
            
            // Horizontal Asymptote y = 2 (dashed blue)
            drawLine(
                color = Color(0xFF1E88E5),
                start = Offset(0f, asympY),
                end = Offset(w, asympY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )
            
            // Draw hyperbola y = 2 + 1/(x-1)
            val path1 = Path()
            val path2 = Path()
            
            var first1 = true
            var first2 = true
            
            // Draw curve point by point
            for (px in 0 until w.toInt() step 2) {
                val rx = (px - ox) / u
                if (Math.abs(rx - 1f) < 0.12f) continue // Near vertical asymptote
                
                val ry = 2f + 1f / (rx - 1f)
                val py = oy - ry * u
                
                if (py < 0 || py > h) continue
                
                if (rx > 1f) {
                    if (first1) {
                        path1.moveTo(px.toFloat(), py)
                        first1 = false
                    } else {
                        path1.lineTo(px.toFloat(), py)
                    }
                } else {
                    if (first2) {
                        path2.moveTo(px.toFloat(), py)
                        first2 = false
                    } else {
                        path2.lineTo(px.toFloat(), py)
                    }
                }
            }
            
            drawPath(path1, color = primary, style = Stroke(width = 2.dp.toPx()))
            drawPath(path2, color = primary, style = Stroke(width = 2.dp.toPx()))
            
            // Draw key intersection dots
            drawCircle(color = secondary, radius = 4.dp.toPx(), center = Offset(ox, oy - 1f * u)) // (0, 1) intersection
            drawCircle(color = secondary, radius = 4.dp.toPx(), center = Offset(ox + 0.5f * u, oy)) // (0.5, 0) x-intercept
        }
        
        // Precise Labels for Coordinate Axes and Asymptotes
        // Origin label
        Text(
            text = "O",
            modifier = Modifier.offset(
                x = with(density) { (ox - 14f).toDp() },
                y = with(density) { (oy + 2f).toDp() }
            ),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = onSurface.copy(alpha = 0.7f)
        )
        
        // x-axis label
        Text(
            text = "x",
            modifier = Modifier.offset(
                x = with(density) { (w - 16f).toDp() },
                y = with(density) { (oy + 6f).toDp() }
            ),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = onSurface.copy(alpha = 0.7f)
        )
        
        // y-axis label
        Text(
            text = "y",
            modifier = Modifier.offset(
                x = with(density) { (ox - 16f).toDp() },
                y = with(density) { 2.dp }
            ),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = onSurface.copy(alpha = 0.7f)
        )
        
        // y-intercept (0, 1) label "1"
        Text(
            text = "1",
            modifier = Modifier.offset(
                x = with(density) { (ox - 12f).toDp() },
                y = with(density) { (oy - 1f * u - 8f).toDp() }
            ),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = secondary
        )
        
        // x-intercept (0.5, 0) label "1/2"
        Text(
            text = "1/2",
            modifier = Modifier.offset(
                x = with(density) { (ox + 0.5f * u - 10f).toDp() },
                y = with(density) { (oy + 4f).toDp() }
            ),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = secondary
        )
        
        // Vertical Asymptote "x = 1" label (placed near the top of the dashed red line)
        Text(
            text = "x = 1 (TCĐ)",
            modifier = Modifier.offset(
                x = with(density) { (asympX + 6f).toDp() },
                y = with(density) { 16.dp }
            ),
            fontSize = 10.sp,
            color = Color(0xFFE53935),
            fontWeight = FontWeight.Bold
        )
        
        // Horizontal Asymptote "y = 2" label (placed near the right of the dashed blue line)
        Text(
            text = "y = 2 (TCN)",
            modifier = Modifier.offset(
                x = with(density) { (w - 74f).toDp() },
                y = with(density) { (asympY - 16f).toDp() }
            ),
            fontSize = 10.sp,
            color = Color(0xFF1E88E5),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HàmNhấtBiếnTable() {
    // Variation table for y = (x + 1)/(x - 1)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val colHeader = 1.5f
        val col1 = 1.0f  // -∞
        val col2 = 1.5f  // interval
        val col3 = 1.0f  // 1 (tiệm cận đứng)
        val col4 = 1.5f  // interval
        val col5 = 1.0f  // +∞
        val totalColsWeight = col1 + col2 + col3 + col4 + col5 // 6.0f

        // Row x
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TableCell("x", weight = colHeader, isHeader = true)
            TableCell("-∞", weight = col1)
            TableCell("", weight = col2)
            TableCell("1", weight = col3, isHeader = true, color = Color.Red)
            TableCell("", weight = col4)
            TableCell("+∞", weight = col5)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Row y'
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TableCell("y'", weight = colHeader, isHeader = true)
            TableCell("", weight = col1)
            TableCell("-", weight = col2)
            TableCell("||", weight = col3, isHeader = true, color = Color.Red)
            TableCell("-", weight = col4)
            TableCell("", weight = col5)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Row y
        Row(modifier = Modifier.fillMaxWidth().height(100.dp), verticalAlignment = Alignment.CenterVertically) {
            TableCell("y", weight = colHeader, isHeader = true)
            
            Box(modifier = Modifier.weight(totalColsWeight).fillMaxHeight()) {
                // Background Canvas for asymptotes and arrows
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val p1X = w * (0.5f / totalColsWeight)
                    val p3X = w * (3.0f / totalColsWeight) // Center asymptote (Col 3)
                    val p5X = w * (5.5f / totalColsWeight)

                    val pTopY = h * 0.22f
                    val pBottomY = h * 0.78f

                    // Draw red double bar at center
                    val barOffset = 3.dp.toPx()
                    drawLine(color = Color.Red, start = Offset(p3X - barOffset, 0f), end = Offset(p3X - barOffset, h), strokeWidth = 1.5.dp.toPx())
                    drawLine(color = Color.Red, start = Offset(p3X + barOffset, 0f), end = Offset(p3X + barOffset, h), strokeWidth = 1.5.dp.toPx())

                    // Left branch going down: starts at p1X (top) to pLeftTextX (bottom)
                    val pLeftTextX = w * (2.5f / totalColsWeight) - 16.dp.toPx()
                    val leftArrowStart = Offset(p1X + 10.dp.toPx(), pTopY + 4.dp.toPx())
                    val leftArrowEnd = Offset(pLeftTextX - 16.dp.toPx(), pBottomY - 4.dp.toPx())
                    drawLine(color = Color(0xFF1E88E5), start = leftArrowStart, end = leftArrowEnd, strokeWidth = 2.dp.toPx())
                    drawArrow(this, leftArrowStart, leftArrowEnd, Color(0xFF1E88E5))

                    // Right branch going down: starts at pRightTextX (top) to p5X (bottom)
                    val pRightTextX = w * (3.5f / totalColsWeight) + 16.dp.toPx()
                    val rightArrowStart = Offset(pRightTextX + 16.dp.toPx(), pTopY + 4.dp.toPx())
                    val rightArrowEnd = Offset(p5X - 12.dp.toPx(), pBottomY - 4.dp.toPx())
                    drawLine(color = Color(0xFF1E88E5), start = rightArrowStart, end = rightArrowEnd, strokeWidth = 2.dp.toPx())
                    drawArrow(this, rightArrowStart, rightArrowEnd, Color(0xFF1E88E5))
                }

                // Foreground layout for text labels
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(col1).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                        Text("1", fontSize = 11.sp, maxLines = 1, softWrap = false, modifier = Modifier.padding(top = 8.dp))
                    }
                    Box(modifier = Modifier.weight(col2).fillMaxHeight(), contentAlignment = Alignment.BottomEnd) {
                        Text("-∞", fontSize = 11.sp, maxLines = 1, softWrap = false, modifier = Modifier.padding(bottom = 8.dp, end = 4.dp))
                    }
                    Spacer(modifier = Modifier.weight(col3))
                    Box(modifier = Modifier.weight(col4).fillMaxHeight(), contentAlignment = Alignment.TopStart) {
                        Text("+∞", fontSize = 11.sp, maxLines = 1, softWrap = false, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
                    }
                    Box(modifier = Modifier.weight(col5).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        Text("1", fontSize = 11.sp, maxLines = 1, softWrap = false, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TrùngPhươngTable() {
    // Variation table for y = x⁴ - 2x² + 3
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val colHeader = 1.2f
        val col1 = 1.0f  // -∞
        val col2 = 1.2f  // interval
        val col3 = 1.0f  // -1
        val col4 = 1.2f  // interval
        val col5 = 1.0f  // 0
        val col6 = 1.2f  // interval
        val col7 = 1.0f  // 1
        val col8 = 1.2f  // interval
        val col9 = 1.0f  // +∞
        val totalColsWeight = col1 + col2 + col3 + col4 + col5 + col6 + col7 + col8 + col9 // 9.8f

        // Row x
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TableCell("x", weight = colHeader, isHeader = true)
            TableCell("-∞", weight = col1)
            TableCell("", weight = col2)
            TableCell("-1", weight = col3, isHeader = true)
            TableCell("", weight = col4)
            TableCell("0", weight = col5, isHeader = true)
            TableCell("", weight = col6)
            TableCell("1", weight = col7, isHeader = true)
            TableCell("", weight = col8)
            TableCell("+∞", weight = col9)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Row y'
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TableCell("y'", weight = colHeader, isHeader = true)
            TableCell("", weight = col1)
            TableCell("-", weight = col2)
            TableCell("0", weight = col3, isHeader = true)
            TableCell("+", weight = col4)
            TableCell("0", weight = col5, isHeader = true)
            TableCell("-", weight = col6)
            TableCell("0", weight = col7, isHeader = true)
            TableCell("+", weight = col8)
            TableCell("", weight = col9)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Row y
        Row(modifier = Modifier.fillMaxWidth().height(100.dp), verticalAlignment = Alignment.CenterVertically) {
            TableCell("y", weight = colHeader, isHeader = true)
            
            Box(modifier = Modifier.weight(totalColsWeight).fillMaxHeight()) {
                // Background Canvas for arrows
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val p1X = w * (0.5f / totalColsWeight)
                    val p2X = w * (2.7f / totalColsWeight)
                    val p3X = w * (4.9f / totalColsWeight)
                    val p4X = w * (7.1f / totalColsWeight)
                    val p5X = w * (9.3f / totalColsWeight)

                    val pMaxY = h * 0.25f
                    val pMinY = h * 0.75f

                    // Path: -∞ (top) -> -1 (min) -> 0 (max) -> 1 (min) -> +∞ (top)
                    drawLine(color = Color(0xFF1E88E5), start = Offset(p1X + 5f, pMaxY + 5f), end = Offset(p2X - 5f, pMinY - 5f), strokeWidth = 2.dp.toPx())
                    drawArrow(this, Offset(p1X + 5f, pMaxY + 5f), Offset(p2X - 5f, pMinY - 5f), Color(0xFF1E88E5))

                    drawLine(color = Color(0xFF1E88E5), start = Offset(p2X + 5f, pMinY - 5f), end = Offset(p3X - 5f, pMaxY + 5f), strokeWidth = 2.dp.toPx())
                    drawArrow(this, Offset(p2X + 5f, pMinY - 5f), Offset(p3X - 5f, pMaxY + 5f), Color(0xFF1E88E5))

                    drawLine(color = Color(0xFF1E88E5), start = Offset(p3X + 5f, pMaxY + 5f), end = Offset(p4X - 5f, pMinY - 5f), strokeWidth = 2.dp.toPx())
                    drawArrow(this, Offset(p3X + 5f, pMaxY + 5f), Offset(p4X - 5f, pMinY - 5f), Color(0xFF1E88E5))

                    drawLine(color = Color(0xFF1E88E5), start = Offset(p4X + 5f, pMinY - 5f), end = Offset(p5X - 5f, pMaxY + 5f), strokeWidth = 2.dp.toPx())
                    drawArrow(this, Offset(p4X + 5f, pMinY - 5f), Offset(p5X - 5f, pMaxY + 5f), Color(0xFF1E88E5))
                }

                // Foreground layout for text labels
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(col1).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                        Text("+∞", fontSize = 10.sp, maxLines = 1, softWrap = false, modifier = Modifier.padding(top = 8.dp))
                    }
                    Spacer(modifier = Modifier.weight(col2))
                    Box(modifier = Modifier.weight(col3).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        Text("2 (CT)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), maxLines = 1, softWrap = false, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Spacer(modifier = Modifier.weight(col4))
                    Box(modifier = Modifier.weight(col5).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                        Text("3 (CĐ)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5), maxLines = 1, softWrap = false, modifier = Modifier.padding(top = 8.dp))
                    }
                    Spacer(modifier = Modifier.weight(col6))
                    Box(modifier = Modifier.weight(col7).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        Text("2 (CT)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), maxLines = 1, softWrap = false, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Spacer(modifier = Modifier.weight(col8))
                    Box(modifier = Modifier.weight(col9).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                        Text("+∞", fontSize = 10.sp, maxLines = 1, softWrap = false, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HộpChữNhật3D() {
    // 3D wireframe box with labels ABCD and dimensions
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.secondary
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        
        // Precise physical proportions matching AB = 1, AD = 2, AA' = 3
        // Use container min size for responsive scaling (prevents cutting off on narrow screens)
        val sizePx = minOf(widthPx, heightPx)
        val unitPx = sizePx * 0.14f
        val dx = unitPx // AB = 1
        val dy = unitPx * 3f // AA' = 3
        val depth = unitPx * 2f // AD = 2
        
        // Oblique projection angles: 30 degrees, shortening factor 0.5
        val ox = (Math.cos(Math.toRadians(30.0)) * depth * 0.5).toFloat()
        val oy = (-Math.sin(Math.toRadians(30.0)) * depth * 0.5).toFloat()
        
        // Center the 3D bounding box perfectly inside the container
        val offsetX = (widthPx - (dx + ox)) / 2f
        val offsetY = (heightPx - (dy - oy)) / 2f - oy
        
        // Vertices bottom face
        val A = Offset(offsetX, offsetY + dy)
        val B = Offset(offsetX + dx, offsetY + dy)
        val C = Offset(offsetX + dx + ox, offsetY + dy + oy)
        val D = Offset(offsetX + ox, offsetY + dy + oy)
        
        // Vertices top face
        val Ap = Offset(offsetX, offsetY)
        val Bp = Offset(offsetX + dx, offsetY)
        val Cp = Offset(offsetX + dx + ox, offsetY + oy)
        val Dp = Offset(offsetX + ox, offsetY + oy)
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw hidden back edges dashed
            val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            drawLine(color = outlineVariant.copy(alpha = 0.5f), start = A, end = D, strokeWidth = 1.dp.toPx(), pathEffect = dashedEffect)
            drawLine(color = outlineVariant.copy(alpha = 0.5f), start = D, end = C, strokeWidth = 1.dp.toPx(), pathEffect = dashedEffect)
            drawLine(color = outlineVariant.copy(alpha = 0.5f), start = D, end = Dp, strokeWidth = 1.dp.toPx(), pathEffect = dashedEffect)
            
            // Draw solid visible edges
            val drawSolid = { p1: Offset, p2: Offset ->
                drawLine(color = primary, start = p1, end = p2, strokeWidth = 2.dp.toPx())
            }
            
            drawSolid(A, B)
            drawSolid(B, C)
            drawSolid(A, Ap)
            drawSolid(B, Bp)
            drawSolid(C, Cp)
            
            drawSolid(Ap, Bp)
            drawSolid(Bp, Cp)
            drawSolid(Cp, Dp)
            drawSolid(Dp, Ap)
            
            // Draw simple annotation circles
            val markerRadius = 3.dp.toPx()
            drawCircle(color = primary, radius = markerRadius, center = A)
            drawCircle(color = primary, radius = markerRadius, center = B)
            drawCircle(color = primary, radius = markerRadius, center = C)
            drawCircle(color = outlineVariant, radius = markerRadius, center = D)
            drawCircle(color = primary, radius = markerRadius, center = Ap)
            drawCircle(color = primary, radius = markerRadius, center = Bp)
            drawCircle(color = primary, radius = markerRadius, center = Cp)
            drawCircle(color = primary, radius = markerRadius, center = Dp)
        }
        
        // Labels aligned EXACTLY at the vertices!
        val centerX = offsetX + (dx + ox) / 2f
        val centerY = offsetY + (dy + oy) / 2f
        
        val vertices = listOf(
            "A" to A,
            "B" to B,
            "C" to C,
            "D" to D,
            "A'" to Ap,
            "B'" to Bp,
            "C'" to Cp,
            "D'" to Dp
        )
        
        vertices.forEach { (name, offset) ->
            val labelX = with(density) { offset.x.toDp() }
            val labelY = with(density) { offset.y.toDp() }
            
            val extraX = if (offset.x < centerX) (-14).dp else 4.dp
            val extraY = if (offset.y < centerY) (-16).dp else 2.dp
            
            Text(
                text = name,
                modifier = Modifier.offset(x = labelX + extraX, y = labelY + extraY),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (name == "D") onSurface.copy(alpha = 0.5f) else onSurface
            )
        }
        
        // Dimension annotations aligned with the edges!
        val midAB = Offset((A.x + B.x)/2, (A.y + B.y)/2)
        val midAD = Offset((A.x + D.x)/2, (A.y + D.y)/2)
        val midAAp = Offset((A.x + Ap.x)/2, (A.y + Ap.y)/2)
        
        val abX = with(density) { midAB.x.toDp() }
        val abY = with(density) { midAB.y.toDp() }
        Text(
            text = "AB = 1",
            modifier = Modifier.offset(x = abX - 20.dp, y = abY + 4.dp),
            fontSize = 10.sp,
            color = secondary,
            fontWeight = FontWeight.SemiBold
        )
        
        val adX = with(density) { midAD.x.toDp() }
        val adY = with(density) { midAD.y.toDp() }
        Text(
            text = "AD = 2",
            modifier = Modifier.offset(x = adX - 52.dp, y = adY - 10.dp),
            fontSize = 10.sp,
            color = secondary,
            fontWeight = FontWeight.SemiBold
        )
        
        val aaX = with(density) { midAAp.x.toDp() }
        val aaY = with(density) { midAAp.y.toDp() }
        Text(
            text = "AA' = 3",
            modifier = Modifier.offset(x = aaX - 52.dp, y = aaY - 8.dp),
            fontSize = 10.sp,
            color = secondary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun XétDấuĐạoHàmTable() {
    // Sign chart for f'(x)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val colHeader = 1.5f
        val col1 = 1.0f  // -∞
        val col2 = 1.5f  // interval
        val col3 = 1.0f  // -1
        val col4 = 1.5f  // interval
        val col5 = 1.0f  // 2
        val col6 = 1.5f  // interval
        val col7 = 1.0f  // +∞

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TableCell("x", weight = colHeader, isHeader = true)
            TableCell("-∞", weight = col1)
            TableCell("", weight = col2)
            TableCell("-1", weight = col3, isHeader = true)
            TableCell("", weight = col4)
            TableCell("2", weight = col5, isHeader = true)
            TableCell("", weight = col6)
            TableCell("+∞", weight = col7)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TableCell("f'(x)", weight = colHeader, isHeader = true)
            TableCell("", weight = col1)
            TableCell("-", weight = col2)
            TableCell("0", weight = col3, isHeader = true)
            TableCell("+", weight = col4)
            TableCell("0", weight = col5, isHeader = true)
            TableCell("-", weight = col6)
            TableCell("", weight = col7)
        }
    }
}

@Composable
fun MặtCầuNgoạiTiếpHộp() {
    // Wireframe box inside sphere
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.secondary
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        
        val cx = widthPx / 2f
        val cy = heightPx / 2f
        
        // Use container min size for responsive scaling (prevents cutting off on narrow screens)
        val sizePx = minOf(widthPx, heightPx)
        // Let the height of the box AA' represent a safe height in the container.
        // We will make half_H = sizePx * 0.18f, so AA' = sizePx * 0.36f.
        val half_H = sizePx * 0.18f
        // Since AB = 3, AD = 4, AA' = 5, the proportions of our physical box are:
        val half_W = half_H * (1.5f / 2.5f) // AB = 3, AA' = 5 -> half_W = half_H * 3/5
        val half_D = half_H * (2.0f / 2.5f) // AD = 4, AA' = 5 -> half_D = half_H * 4/5
        
        // Oblique projection: 30 degrees, shortening factor 0.5
        val cos30 = 0.866025f
        val sin30 = 0.5f
        val sf = 0.5f // shortening factor
        
        val projX = half_D * cos30 * sf
        val projY = half_D * sin30 * sf
        
        // Vertices bottom face
        val A = Offset(cx - half_W + projX, cy + half_H - projY)
        val B = Offset(cx + half_W + projX, cy + half_H - projY)
        val C = Offset(cx + half_W - projX, cy + half_H + projY)
        val D = Offset(cx - half_W - projX, cy + half_H + projY)
        
        // Vertices top face
        val Ap = Offset(cx - half_W + projX, cy - half_H - projY)
        val Bp = Offset(cx + half_W + projX, cy - half_H - projY)
        val Cp = Offset(cx + half_W - projX, cy - half_H + projY)
        val Dp = Offset(cx - half_W - projX, cy - half_H + projY)
        
        // Calculate the exact radius of the circle representing the sphere so that
        // the furthest projected vertices (Bp, Dp, D, Ap) lie EXACTLY on the circle.
        val r = kotlin.math.sqrt((half_W + projX) * (half_W + projX) + (half_H + projY) * (half_H + projY))
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Draw sphere outline
            drawCircle(
                color = primary.copy(alpha = 0.12f),
                radius = r,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = primary.copy(alpha = 0.8f),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
            
            // 2. Draw sphere horizontal dashed ellipse (projection)
            val pathEllipse = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(cx - r, cy - r * 0.3f, cx + r, cy + r * 0.3f))
            }
            drawPath(
                path = pathEllipse,
                color = primary.copy(alpha = 0.4f),
                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
            )
            
            // 3. Draw hidden back edges dashed
            val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            drawLine(color = outlineVariant.copy(alpha = 0.5f), start = A, end = D, strokeWidth = 1.dp.toPx(), pathEffect = dashedEffect)
            drawLine(color = outlineVariant.copy(alpha = 0.5f), start = D, end = C, strokeWidth = 1.dp.toPx(), pathEffect = dashedEffect)
            drawLine(color = outlineVariant.copy(alpha = 0.5f), start = D, end = Dp, strokeWidth = 1.dp.toPx(), pathEffect = dashedEffect)
            
            // 4. Draw solid visible edges
            val drawSolid = { st: Offset, en: Offset ->
                drawLine(color = primary, start = st, end = en, strokeWidth = 1.5.dp.toPx())
            }
            drawSolid(A, B)
            drawSolid(B, C)
            drawSolid(A, Ap)
            drawSolid(B, Bp)
            drawSolid(C, Cp)
            
            drawSolid(Ap, Bp)
            drawSolid(Bp, Cp)
            drawSolid(Cp, Dp)
            drawSolid(Dp, Ap)
            
            // 5. Draw major diagonal (diameter of sphere)
            drawLine(
                color = Color(0xFFE53935),
                start = A,
                end = Cp,
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
            )
            
            // Marker dots
            val markerRadius = 2.5.dp.toPx()
            drawCircle(color = primary, radius = markerRadius, center = A)
            drawCircle(color = primary, radius = markerRadius, center = B)
            drawCircle(color = primary, radius = markerRadius, center = C)
            drawCircle(color = outlineVariant, radius = markerRadius, center = D)
            drawCircle(color = primary, radius = markerRadius, center = Ap)
            drawCircle(color = primary, radius = markerRadius, center = Bp)
            drawCircle(color = primary, radius = markerRadius, center = Cp)
            drawCircle(color = primary, radius = markerRadius, center = Dp)
        }
        
        // Labels aligned EXACTLY at the vertices relative to sphere center
        val vertices = listOf(
            "A" to A,
            "B" to B,
            "C" to C,
            "D" to D,
            "A'" to Ap,
            "B'" to Bp,
            "C'" to Cp,
            "D'" to Dp
        )
        
        vertices.forEach { (name, offset) ->
            val labelX = with(density) { offset.x.toDp() }
            val labelY = with(density) { offset.y.toDp() }
            
            val extraX = if (offset.x < cx) (-14).dp else 4.dp
            val extraY = if (offset.y < cy) (-16).dp else 2.dp
            
            Text(
                text = name,
                modifier = Modifier.offset(x = labelX + extraX, y = labelY + extraY),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (name == "D") onSurface.copy(alpha = 0.5f) else onSurface
            )
        }
        
        // Dimensions annotations: AB = 3, AD = 4, AA' = 5
        val midAB = Offset((A.x + B.x)/2, (A.y + B.y)/2)
        val midAD = Offset((A.x + D.x)/2, (A.y + D.y)/2)
        val midAAp = Offset((A.x + Ap.x)/2, (A.y + Ap.y)/2)
        
        val abX = with(density) { midAB.x.toDp() }
        val abY = with(density) { midAB.y.toDp() }
        Text(
            text = "AB = 3",
            modifier = Modifier.offset(x = abX - 20.dp, y = abY + 4.dp),
            fontSize = 10.sp,
            color = secondary,
            fontWeight = FontWeight.SemiBold
        )
        
        val adX = with(density) { midAD.x.toDp() }
        val adY = with(density) { midAD.y.toDp() }
        Text(
            text = "AD = 4",
            modifier = Modifier.offset(x = adX - 52.dp, y = adY - 10.dp),
            fontSize = 10.sp,
            color = secondary,
            fontWeight = FontWeight.SemiBold
        )
        
        val aaX = with(density) { midAAp.x.toDp() }
        val aaY = with(density) { midAAp.y.toDp() }
        Text(
            text = "AA' = 5",
            modifier = Modifier.offset(x = aaX - 52.dp, y = aaY - 8.dp),
            fontSize = 10.sp,
            color = secondary,
            fontWeight = FontWeight.SemiBold
        )
        
        // Bán kính mặt cầu ngoại tiếp label
        Text(
            text = "Bán kính R = d / 2",
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 4.dp),
            fontSize = 11.sp,
            color = Color(0xFFE53935),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 2.dp, vertical = 6.dp),
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        fontSize = 11.sp,
        maxLines = 1,
        softWrap = false,
        color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
    )
}

private fun drawArrow(canvas: androidx.compose.ui.graphics.drawscope.DrawScope, start: Offset, end: Offset, color: Color) {
    val angle = Math.atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val arrowLength = 15f
    val arrowAngle = Math.PI / 6
    
    val x1 = end.x - arrowLength * Math.cos(angle - arrowAngle)
    val y1 = end.y - arrowLength * Math.sin(angle - arrowAngle)
    
    val x2 = end.x - arrowLength * Math.cos(angle + arrowAngle)
    val y2 = end.y - arrowLength * Math.sin(angle + arrowAngle)
    
    canvas.drawLine(color = color, start = end, end = Offset(x1.toFloat(), y1.toFloat()), strokeWidth = 4f)
    canvas.drawLine(color = color, start = end, end = Offset(x2.toFloat(), y2.toFloat()), strokeWidth = 4f)
}
