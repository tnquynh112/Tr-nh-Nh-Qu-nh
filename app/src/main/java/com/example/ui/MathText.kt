package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// MATHEMATICAL FRACTION AND TEXT RENDERER
// ==========================================

sealed class MathTextElement {
    data class NormalText(val text: String) : MathTextElement()
    data class Fraction(val numerator: String, val denominator: String) : MathTextElement()
    data class Power(val base: String, val exponent: String) : MathTextElement()
    data class Subscript(val base: String, val sub: String) : MathTextElement()
    data class SubSup(val base: String, val sub: String, val sup: String) : MathTextElement()
    data class Integral(val lower: String, val upper: String) : MathTextElement()
    data class NewLine(val text: String) : MathTextElement()
}

data class FractionMatch(
    val startIdx: Int,
    val endIdx: Int,
    val numerator: String,
    val denominator: String
)

fun findMatchingOpenBracket(s: String, endIdx: Int, openChar: Char, closeChar: Char): Int? {
    var balance = 0
    for (i in endIdx downTo 0) {
        val c = s[i]
        if (c == closeChar) {
            balance++
        } else if (c == openChar) {
            balance--
            if (balance == 0) {
                return i
            }
        }
    }
    return null
}

fun findMatchingCloseBracket(s: String, startIdx: Int, openChar: Char, closeChar: Char): Int? {
    var balance = 0
    for (i in startIdx until s.length) {
        val c = s[i]
        if (c == openChar) {
            balance++
        } else if (c == closeChar) {
            balance--
            if (balance == 0) {
                return i
            }
        }
    }
    return null
}

fun isSimpleMathChar(c: Char): Boolean {
    return c.isLetterOrDigit() || c == '_' || c == 'π' || c == '√' || c == '²' || c == '³' || c == '^' || c == '∞' || c == 'θ' || c == 'α' || c == 'β' || c == 'γ' || c == 'Δ' || c == '±' || c == '°' || c == '≥' || c == '≤' || (c in '₀'..'₉') || (c in '⁰'..'⁹') || c == '\'' || c == '.' || c == ','
}

fun findFirstFracLatex(s: String): FractionMatch? {
    var idx = 0
    while (true) {
        val fracIdx = s.indexOf("\\frac", idx)
        if (fracIdx == -1) break
        
        var firstOpen = fracIdx + 5
        while (firstOpen < s.length && s[firstOpen].isWhitespace()) {
            firstOpen++
        }
        if (firstOpen < s.length && s[firstOpen] == '{') {
            val firstClose = findMatchingCloseBracket(s, firstOpen, '{', '}')
            if (firstClose != null) {
                var secondOpen = firstClose + 1
                while (secondOpen < s.length && s[secondOpen].isWhitespace()) {
                    secondOpen++
                }
                if (secondOpen < s.length && s[secondOpen] == '{') {
                    val secondClose = findMatchingCloseBracket(s, secondOpen, '{', '}')
                    if (secondClose != null) {
                        return FractionMatch(
                            startIdx = fracIdx,
                            endIdx = secondClose + 1,
                            numerator = s.substring(firstOpen + 1, firstClose),
                            denominator = s.substring(secondOpen + 1, secondClose)
                        )
                    }
                }
            }
        }
        idx = fracIdx + 1
    }
    return null
}

fun hasBalancedParentheses(s: String): Boolean {
    var balance = 0
    for (i in s.indices) {
        val c = s[i]
        if (c == '(' || c == '[' || c == '{') {
            balance++
        } else if (c == ')' || c == ']' || c == '}') {
            balance--
            if (balance < 0) return false
        }
    }
    return balance == 0
}

fun findFirstFraction(s: String): FractionMatch? {
    var bestMatch: FractionMatch? = null
    
    for (i in s.indices) {
        if (s[i] == '/') {
            val slashIdx = i
            
            // 1. Scan backwards for numerator
            var numEnd = slashIdx - 1
            while (numEnd >= 0 && s[numEnd].isWhitespace()) {
                numEnd--
            }
            if (numEnd < 0) continue
            
            var bracketBalance = 0
            var tempNum = numEnd
            while (tempNum >= 0) {
                val c = s[tempNum]
                if (c == ')' || c == ']' || c == '}') {
                    bracketBalance++
                    tempNum--
                } else if (c == '(' || c == '[' || c == '{') {
                    bracketBalance--
                    if (bracketBalance < 0) {
                        break
                    }
                    tempNum--
                } else {
                    if (bracketBalance == 0) {
                        if (c == '+' || c == '-' || c == '=' || c == '<' || c == '>' || c == '*' || c == ',' || c == ';' || c.isWhitespace()) {
                            break
                        }
                        if (!isSimpleMathChar(c)) {
                            break
                        }
                    }
                    tempNum--
                }
            }
            val numStart = tempNum + 1
            if (numStart > numEnd) continue
            
            var numeratorText = s.substring(numStart, numEnd + 1)
            
            while (numeratorText.length >= 2) {
                val first = numeratorText.first()
                val last = numeratorText.last()
                val match = when {
                    first == '(' && last == ')' -> true
                    first == '[' && last == ']' -> true
                    first == '{' && last == '}' -> true
                    else -> false
                }
                if (match) {
                    val inner = numeratorText.substring(1, numeratorText.length - 1)
                    if (hasBalancedParentheses(inner)) {
                        numeratorText = inner
                    } else {
                        break
                    }
                } else {
                    break
                }
            }
            
            // 2. Scan forwards for denominator
            var denStart = slashIdx + 1
            while (denStart < s.length && s[denStart].isWhitespace()) {
                denStart++
            }
            if (denStart >= s.length) continue
            
            bracketBalance = 0
            var tempDen = denStart
            while (tempDen < s.length) {
                val c = s[tempDen]
                if (c == '(' || c == '[' || c == '{') {
                    bracketBalance++
                    tempDen++
                } else if (c == ')' || c == ']' || c == '}') {
                    bracketBalance--
                    if (bracketBalance < 0) {
                        break
                    }
                    tempDen++
                } else {
                    if (bracketBalance == 0) {
                        if (c == '+' || c == '-' || c == '=' || c == '<' || c == '>' || c == '*' || c == ',' || c == ';' || c.isWhitespace()) {
                            break
                        }
                        if (!isSimpleMathChar(c)) {
                            break
                        }
                    }
                    tempDen++
                }
            }
            val denEnd = tempDen - 1
            if (denStart > denEnd) continue
            
            var denominatorText = s.substring(denStart, denEnd + 1)
            
            while (denominatorText.length >= 2) {
                val first = denominatorText.first()
                val last = denominatorText.last()
                val match = when {
                    first == '(' && last == ')' -> true
                    first == '[' && last == ']' -> true
                    first == '{' && last == '}' -> true
                    else -> false
                }
                if (match) {
                    val inner = denominatorText.substring(1, denominatorText.length - 1)
                    if (hasBalancedParentheses(inner)) {
                        denominatorText = inner
                    } else {
                        break
                    }
                } else {
                    break
                }
            }
            
            val match = FractionMatch(
                startIdx = numStart,
                endIdx = denEnd + 1,
                numerator = numeratorText,
                denominator = denominatorText
            )
            
            if (bestMatch == null || match.startIdx < bestMatch.startIdx) {
                bestMatch = match
            }
        }
    }
    
    return bestMatch
}

data class PowerMatch(
    val startIdx: Int,
    val endIdx: Int,
    val base: String,
    val exponent: String
)

fun findFirstPower(s: String): PowerMatch? {
    var idx = 0
    while (true) {
        val caretIdx = s.indexOf('^', idx)
        if (caretIdx == -1) break
        
        // Find base
        var baseEnd = caretIdx - 1
        while (baseEnd >= 0 && s[baseEnd].isWhitespace()) {
            baseEnd--
        }
        if (baseEnd < 0) {
            idx = caretIdx + 1
            continue
        }
        
        var baseStart = baseEnd
        if (s[baseEnd] == ')') {
            val open = findMatchingOpenBracket(s, baseEnd, '(', ')')
            if (open != null) {
                baseStart = open
            }
        } else if (s[baseEnd] == '}') {
            val open = findMatchingOpenBracket(s, baseEnd, '{', '}')
            if (open != null) {
                baseStart = open
            }
        } else if (s[baseEnd] == ']') {
            val open = findMatchingOpenBracket(s, baseEnd, '[', ']')
            if (open != null) {
                baseStart = open
            }
        } else {
            while (baseStart >= 0 && isSimpleMathChar(s[baseStart]) && s[baseStart] != '^') {
                baseStart--
            }
            baseStart++
        }
        
        if (baseStart > baseEnd) {
            idx = caretIdx + 1
            continue
        }
        
        var baseText = s.substring(baseStart, baseEnd + 1)
        if ((baseText.startsWith("{") && baseText.endsWith("}")) ||
            (baseText.startsWith("(") && baseText.endsWith(")"))) {
            baseText = baseText.substring(1, baseText.length - 1)
        }
        
        // Find exponent
        var expStart = caretIdx + 1
        while (expStart < s.length && s[expStart].isWhitespace()) {
            expStart++
        }
        if (expStart >= s.length) {
            idx = caretIdx + 1
            continue
        }
        
        var expEnd = expStart
        if (s[expStart] == '{') {
            val close = findMatchingCloseBracket(s, expStart, '{', '}')
            if (close != null) {
                expEnd = close
            }
        } else if (s[expStart] == '(') {
            val close = findMatchingCloseBracket(s, expStart, '(', ')')
            if (close != null) {
                expEnd = close
            }
        } else {
            while (expEnd < s.length && (isSimpleMathChar(s[expEnd]) || (expEnd == expStart && (s[expEnd] == '-' || s[expEnd] == '+'))) && s[expEnd] != '^' && s[expEnd] != '_') {
                if (expEnd > expStart && (s[expEnd] == '+' || s[expEnd] == '-' || s[expEnd] == '=' || s[expEnd] == '<' || s[expEnd] == '>')) {
                    break
                }
                expEnd++
            }
            expEnd--
        }
        
        if (expStart > expEnd) {
            idx = caretIdx + 1
            continue
        }
        
        var exponentText = s.substring(expStart, expEnd + 1).trim()
        if ((exponentText.startsWith("{") && exponentText.endsWith("}")) ||
            (exponentText.startsWith("(") && exponentText.endsWith(")"))) {
            exponentText = exponentText.substring(1, exponentText.length - 1).trim()
        }
        
        return PowerMatch(
            startIdx = baseStart,
            endIdx = expEnd + 1,
            base = baseText,
            exponent = exponentText
        )
    }
    return null
}

data class SubscriptMatch(
    val startIdx: Int,
    val endIdx: Int,
    val base: String,
    val sub: String
)

fun findFirstSubscript(s: String): SubscriptMatch? {
    var idx = 0
    while (true) {
        val subIdx = s.indexOf('_', idx)
        if (subIdx == -1) break
        
        var baseEnd = subIdx - 1
        while (baseEnd >= 0 && s[baseEnd].isWhitespace()) {
            baseEnd--
        }
        if (baseEnd < 0) {
            idx = subIdx + 1
            continue
        }
        
        var baseStart = baseEnd
        if (s[baseEnd] == ')') {
            val open = findMatchingOpenBracket(s, baseEnd, '(', ')')
            if (open != null) baseStart = open
        } else if (s[baseEnd] == '}') {
            val open = findMatchingOpenBracket(s, baseEnd, '{', '}')
            if (open != null) baseStart = open
        } else {
            while (baseStart >= 0 && isSimpleMathChar(s[baseStart]) && s[baseStart] != '_' && s[baseStart] != '^') {
                baseStart--
            }
            baseStart++
        }
        
        if (baseStart > baseEnd) {
            idx = subIdx + 1
            continue
        }
        
        var baseText = s.substring(baseStart, baseEnd + 1)
        if ((baseText.startsWith("{") && baseText.endsWith("}")) ||
            (baseText.startsWith("(") && baseText.endsWith(")"))) {
            baseText = baseText.substring(1, baseText.length - 1)
        }
        
        var subStart = subIdx + 1
        while (subStart < s.length && s[subStart].isWhitespace()) {
            subStart++
        }
        if (subStart >= s.length) {
            idx = subIdx + 1
            continue
        }
        
        var subEnd = subStart
        if (s[subStart] == '{') {
            val close = findMatchingCloseBracket(s, subStart, '{', '}')
            if (close != null) subEnd = close
        } else if (s[subStart] == '(') {
            val close = findMatchingCloseBracket(s, subStart, '(', ')')
            if (close != null) subEnd = close
        } else {
            while (subEnd < s.length && (isSimpleMathChar(s[subEnd]) || (subEnd == subStart && (s[subEnd] == '-' || s[subEnd] == '+'))) && s[subEnd] != '_' && s[subEnd] != '^') {
                if (subEnd > subStart && (s[subEnd] == '+' || s[subEnd] == '-' || s[subEnd] == '=' || s[subEnd] == '<' || s[subEnd] == '>')) {
                    break
                }
                subEnd++
            }
            subEnd--
        }
        
        if (subStart > subEnd) {
            idx = subIdx + 1
            continue
        }
        
        var subText = s.substring(subStart, subEnd + 1).trim()
        if ((subText.startsWith("{") && subText.endsWith("}")) ||
            (subText.startsWith("(") && subText.endsWith(")"))) {
            subText = subText.substring(1, subText.length - 1).trim()
        }
        
        return SubscriptMatch(
            startIdx = baseStart,
            endIdx = subEnd + 1,
            base = baseText,
            sub = subText
        )
    }
    return null
}

data class IntegralMatch(
    val startIdx: Int,
    val endIdx: Int,
    val lower: String,
    val upper: String
)

fun findFirstIntegral(s: String): IntegralMatch? {
    val intIdx = s.indexOf("\\int")
    if (intIdx == -1) return null
    
    var lower = ""
    var upper = ""
    var currentIdx = intIdx + 4
    var endIdx = currentIdx
    
    var foundLower = false
    var foundUpper = false
    
    while (currentIdx < s.length) {
        while (currentIdx < s.length && s[currentIdx].isWhitespace()) {
            currentIdx++
        }
        if (currentIdx >= s.length) break
        
        if (s[currentIdx] == '_') {
            if (!foundLower) {
                currentIdx++
                while (currentIdx < s.length && s[currentIdx].isWhitespace()) {
                    currentIdx++
                }
                if (currentIdx < s.length) {
                    if (s[currentIdx] == '{') {
                        val close = findMatchingCloseBracket(s, currentIdx, '{', '}')
                        if (close != null) {
                            lower = s.substring(currentIdx + 1, close)
                            currentIdx = close + 1
                            endIdx = currentIdx
                            foundLower = true
                            continue
                        }
                    } else if (s[currentIdx] == '(') {
                        val close = findMatchingCloseBracket(s, currentIdx, '(', ')')
                        if (close != null) {
                            lower = s.substring(currentIdx + 1, close)
                            currentIdx = close + 1
                            endIdx = currentIdx
                            foundLower = true
                            continue
                        }
                    } else {
                        lower = s[currentIdx].toString()
                        currentIdx++
                        endIdx = currentIdx
                        foundLower = true
                        continue
                    }
                }
            }
        } else if (s[currentIdx] == '^') {
            if (!foundUpper) {
                currentIdx++
                while (currentIdx < s.length && s[currentIdx].isWhitespace()) {
                    currentIdx++
                }
                if (currentIdx < s.length) {
                    if (s[currentIdx] == '{') {
                        val close = findMatchingCloseBracket(s, currentIdx, '{', '}')
                        if (close != null) {
                            upper = s.substring(currentIdx + 1, close)
                            currentIdx = close + 1
                            endIdx = currentIdx
                            foundUpper = true
                            continue
                        }
                    } else if (s[currentIdx] == '(') {
                        val close = findMatchingCloseBracket(s, currentIdx, '(', ')')
                        if (close != null) {
                            upper = s.substring(currentIdx + 1, close)
                            currentIdx = close + 1
                            endIdx = currentIdx
                            foundUpper = true
                            continue
                        }
                    } else {
                        upper = s[currentIdx].toString()
                        currentIdx++
                        endIdx = currentIdx
                        foundUpper = true
                        continue
                    }
                }
            }
        }
        break
    }
    
    return IntegralMatch(
        startIdx = intIdx,
        endIdx = endIdx,
        lower = lower,
        upper = upper
    )
}

data class SubSupMatch(
    val startIdx: Int,
    val endIdx: Int,
    val base: String,
    val sub: String,
    val sup: String
)

fun findFirstSubSup(s: String): SubSupMatch? {
    val subMatch = findFirstSubscript(s)
    val powerMatch = findFirstPower(s)
    
    val candidates = mutableListOf<SubSupMatch>()
    
    if (subMatch != null) {
        var expIdx = subMatch.endIdx
        while (expIdx < s.length && s[expIdx].isWhitespace()) expIdx++
        if (expIdx < s.length && s[expIdx] == '^') {
            var expStart = expIdx + 1
            while (expStart < s.length && s[expStart].isWhitespace()) expStart++
            if (expStart < s.length) {
                var expEnd = expStart
                if (s[expStart] == '{') {
                    val close = findMatchingCloseBracket(s, expStart, '{', '}')
                    if (close != null) expEnd = close
                } else if (s[expStart] == '(') {
                    val close = findMatchingCloseBracket(s, expStart, '(', ')')
                    if (close != null) expEnd = close
                } else {
                    while (expEnd < s.length && (isSimpleMathChar(s[expEnd]) || (expEnd == expStart && (s[expEnd] == '-' || s[expEnd] == '+'))) && s[expEnd] != '^' && s[expEnd] != '_') {
                        if (expEnd > expStart && (s[expEnd] == '+' || s[expEnd] == '-' || s[expEnd] == '=' || s[expEnd] == '<' || s[expEnd] == '>')) break
                        expEnd++
                    }
                    expEnd--
                }
                if (expStart <= expEnd) {
                    var expText = s.substring(expStart, expEnd + 1).trim()
                    if ((expText.startsWith("{") && expText.endsWith("}")) || (expText.startsWith("(") && expText.endsWith(")"))) {
                        expText = expText.substring(1, expText.length - 1).trim()
                    }
                    candidates.add(
                        SubSupMatch(
                            startIdx = subMatch.startIdx,
                            endIdx = expEnd + 1,
                            base = subMatch.base,
                            sub = subMatch.sub,
                            sup = expText
                        )
                    )
                }
            }
        }
    }
    
    if (powerMatch != null) {
        var subIdx = powerMatch.endIdx
        while (subIdx < s.length && s[subIdx].isWhitespace()) subIdx++
        if (subIdx < s.length && s[subIdx] == '_') {
            var subStart = subIdx + 1
            while (subStart < s.length && s[subStart].isWhitespace()) subStart++
            if (subStart < s.length) {
                var subEnd = subStart
                if (s[subStart] == '{') {
                    val close = findMatchingCloseBracket(s, subStart, '{', '}')
                    if (close != null) subEnd = close
                } else if (s[subStart] == '(') {
                    val close = findMatchingCloseBracket(s, subStart, '(', ')')
                    if (close != null) subEnd = close
                } else {
                    while (subEnd < s.length && (isSimpleMathChar(s[subEnd]) || (subEnd == subStart && (s[subEnd] == '-' || s[subEnd] == '+'))) && s[subEnd] != '^' && s[subEnd] != '_') {
                        if (subEnd > subStart && (s[subEnd] == '+' || s[subEnd] == '-' || s[subEnd] == '=' || s[subEnd] == '<' || s[subEnd] == '>')) break
                        subEnd++
                    }
                    subEnd--
                }
                if (subStart <= subEnd) {
                    var subText = s.substring(subStart, subEnd + 1).trim()
                    if ((subText.startsWith("{") && subText.endsWith("}")) || (subText.startsWith("(") && subText.endsWith(")"))) {
                        subText = subText.substring(1, subText.length - 1).trim()
                    }
                    candidates.add(
                        SubSupMatch(
                            startIdx = powerMatch.startIdx,
                            endIdx = subEnd + 1,
                            base = powerMatch.base,
                            sub = subText,
                            sup = powerMatch.exponent
                        )
                    )
                }
            }
        }
    }
    
    if (candidates.isEmpty()) return null
    return candidates.minByOrNull { it.startIdx }
}

fun parseMathText(text: String): List<MathTextElement> {
    val elements = mutableListOf<MathTextElement>()
    var remaining = text
    
    while (remaining.isNotEmpty()) {
        val subSupMatch = findFirstSubSup(remaining)
        val integralMatch = findFirstIntegral(remaining)
        val latexMatch = findFirstFracLatex(remaining)
        val slashMatch = findFirstFraction(remaining)
        val powerMatch = findFirstPower(remaining)
        val subscriptMatch = findFirstSubscript(remaining)
        
        val candidates = mutableListOf<Triple<Int, Int, MathTextElement>>()
        
        if (subSupMatch != null) {
            candidates.add(Triple(subSupMatch.startIdx, subSupMatch.endIdx, MathTextElement.SubSup(subSupMatch.base, subSupMatch.sub, subSupMatch.sup)))
        }
        if (integralMatch != null) {
            candidates.add(Triple(integralMatch.startIdx, integralMatch.endIdx, MathTextElement.Integral(integralMatch.lower, integralMatch.upper)))
        }
        if (latexMatch != null) {
            candidates.add(Triple(latexMatch.startIdx, latexMatch.endIdx, MathTextElement.Fraction(latexMatch.numerator, latexMatch.denominator)))
        }
        if (slashMatch != null) {
            candidates.add(Triple(slashMatch.startIdx, slashMatch.endIdx, MathTextElement.Fraction(slashMatch.numerator, slashMatch.denominator)))
        }
        if (powerMatch != null) {
            candidates.add(Triple(powerMatch.startIdx, powerMatch.endIdx, MathTextElement.Power(powerMatch.base, powerMatch.exponent)))
        }
        if (subscriptMatch != null) {
            candidates.add(Triple(subscriptMatch.startIdx, subscriptMatch.endIdx, MathTextElement.Subscript(subscriptMatch.base, subscriptMatch.sub)))
        }
        
        if (candidates.isEmpty()) {
            elements.add(MathTextElement.NormalText(remaining))
            break
        }
        
        candidates.sortWith { a, b ->
            val firstCompare = a.first.compareTo(b.first)
            if (firstCompare != 0) firstCompare else b.second.compareTo(a.second)
        }
        val firstMatch = candidates.first()
        val startIdx = firstMatch.first
        val endIdx = firstMatch.second
        val element = firstMatch.third
        
        if (startIdx > 0) {
            elements.add(MathTextElement.NormalText(remaining.substring(0, startIdx)))
        }
        
        elements.add(element)
        remaining = remaining.substring(endIdx)
    }
    
    return elements
}

@Composable
fun InlineMathText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    val elements = parseMathText(text)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
    ) {
        elements.forEach { element ->
            when (element) {
                is MathTextElement.NormalText -> {
                    Text(text = element.text, style = style)
                }
                is MathTextElement.Fraction -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .width(IntrinsicSize.Min)
                            .padding(horizontal = 2.dp)
                    ) {
                        InlineMathText(text = element.numerator, style = style.copy(fontSize = (style.fontSize.value * 0.8f).sp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(style.color)
                        )
                        InlineMathText(text = element.denominator, style = style.copy(fontSize = (style.fontSize.value * 0.8f).sp))
                    }
                }
                is MathTextElement.Power -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InlineMathText(text = element.base, style = style)
                        Text(
                            text = element.exponent,
                            style = style.copy(
                                fontSize = (style.fontSize.value * 0.7f).sp,
                                baselineShift = BaselineShift.Superscript,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                is MathTextElement.Subscript -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InlineMathText(text = element.base, style = style)
                        Text(
                            text = element.sub,
                            style = style.copy(
                                fontSize = (style.fontSize.value * 0.7f).sp,
                                baselineShift = BaselineShift.Subscript,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                is MathTextElement.SubSup -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InlineMathText(text = element.base, style = style)
                        Column(
                            verticalArrangement = Arrangement.spacedBy((-1.5).dp),
                            modifier = Modifier.padding(start = 1.dp)
                        ) {
                            InlineMathText(text = element.sup, style = style.copy(fontSize = (style.fontSize.value * 0.68f).sp, fontWeight = FontWeight.Bold))
                            InlineMathText(text = element.sub, style = style.copy(fontSize = (style.fontSize.value * 0.68f).sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }
                is MathTextElement.Integral -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "∫",
                            style = style.copy(fontSize = (style.fontSize.value * 1.4f).sp)
                        )
                        if (element.lower.isNotEmpty() || element.upper.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy((-1.5).dp),
                                modifier = Modifier
                                    .wrapContentHeight()
                                    .align(Alignment.CenterVertically)
                                    .padding(start = 1.dp)
                            ) {
                                InlineMathText(text = element.upper, style = style.copy(fontSize = (style.fontSize.value * 0.62f).sp))
                                InlineMathText(text = element.lower, style = style.copy(fontSize = (style.fontSize.value * 0.62f).sp))
                            }
                        }
                    }
                }
                is MathTextElement.NewLine -> {}
            }
        }
    }
}

fun parseBoldAndNormal(text: String): List<Pair<String, Boolean>> {
    val result = mutableListOf<Pair<String, Boolean>>()
    var current = ""
    var isBold = false
    var i = 0
    while (i < text.length) {
        if (text.startsWith("**", i)) {
            if (current.isNotEmpty()) {
                result.add(Pair(current, isBold))
                current = ""
            }
            isBold = !isBold
            i += 2
        } else if (text[i] == '*' && (i == 0 || text[i-1] != '*') && (i + 1 == text.length || text[i+1] != '*')) {
            if (current.isNotEmpty()) {
                result.add(Pair(current, isBold))
                current = ""
            }
            isBold = !isBold
            i += 1
        } else {
            current += text[i]
            i++
        }
    }
    if (current.isNotEmpty()) {
        result.add(Pair(current, isBold))
    }
    return result
}

@Composable
fun appendMathElements(
    builder: AnnotatedString.Builder,
    elements: List<MathTextElement>,
    style: androidx.compose.ui.text.TextStyle,
    inlineContentMap: MutableMap<String, InlineTextContent>,
    lineIdx: Int,
    getNextElementIdx: () -> Int
) {
    var prevWasMathSymbol = false
    elements.forEach { element ->
        when (element) {
            is MathTextElement.NormalText -> {
                var txt = element.text
                if (prevWasMathSymbol && txt.startsWith(" ")) {
                    txt = txt.replaceFirst(" ", "\u00A0")
                }
                prevWasMathSymbol = false
                val segments = parseBoldAndNormal(txt)
                segments.forEach { (segmentText, isBoldSegment) ->
                    if (isBoldSegment) {
                        builder.withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            builder.append(segmentText)
                        }
                    } else {
                        builder.append(segmentText)
                    }
                }
            }
            is MathTextElement.Fraction -> {
                prevWasMathSymbol = true
                val elementIdx = getNextElementIdx()
                val inlineId = "fraction_${lineIdx}_${elementIdx}"
                builder.appendInlineContent(inlineId, "[Phân số]")
                
                val baseFontSize = style.fontSize
                val baseSize = if (baseFontSize == androidx.compose.ui.unit.TextUnit.Unspecified) 14f else baseFontSize.value
                val numLen = element.numerator.length
                val denLen = element.denominator.length
                val maxLen = maxOf(numLen, denLen)
                val widthSp = maxOf(22f, maxLen * baseSize * 0.45f + 8f)
                
                inlineContentMap[inlineId] = InlineTextContent(
                    Placeholder(
                        width = widthSp.sp,
                        height = (baseSize * 2.1f).sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight().padding(horizontal = 1.dp)
                    ) {
                        InlineMathText(
                            text = element.numerator,
                            style = style.copy(
                                fontSize = (baseSize * 0.82f).sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        InlineMathText(
                            text = element.denominator,
                            style = style.copy(
                                fontSize = (baseSize * 0.82f).sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
            is MathTextElement.Power -> {
                prevWasMathSymbol = true
                val baseElements = parseMathText(element.base)
                appendMathElements(builder, baseElements, style, inlineContentMap, lineIdx, getNextElementIdx)
                
                val baseFontSize = style.fontSize
                val baseSize = if (baseFontSize == androidx.compose.ui.unit.TextUnit.Unspecified) 14f else baseFontSize.value
                builder.withStyle(
                    SpanStyle(
                        baselineShift = BaselineShift.Superscript,
                        fontSize = (baseSize * 0.72f).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                ) {
                    val expClean = cleanMathText(element.exponent)
                    builder.append(expClean)
                }
            }
            is MathTextElement.Subscript -> {
                prevWasMathSymbol = true
                val baseElements = parseMathText(element.base)
                appendMathElements(builder, baseElements, style, inlineContentMap, lineIdx, getNextElementIdx)
                
                val baseFontSize = style.fontSize
                val baseSize = if (baseFontSize == androidx.compose.ui.unit.TextUnit.Unspecified) 14f else baseFontSize.value
                builder.withStyle(
                    SpanStyle(
                        baselineShift = BaselineShift.Subscript,
                        fontSize = (baseSize * 0.72f).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                ) {
                    val subClean = cleanMathText(element.sub)
                    builder.append(subClean)
                }
            }
            is MathTextElement.SubSup -> {
                prevWasMathSymbol = true
                val elementIdx = getNextElementIdx()
                val inlineId = "subsup_${lineIdx}_${elementIdx}"
                builder.appendInlineContent(inlineId, "[Mũ-ChỉSố]")
                
                val baseFontSize = style.fontSize
                val baseSize = if (baseFontSize == androidx.compose.ui.unit.TextUnit.Unspecified) 14f else baseFontSize.value
                val cleanSub = cleanMathText(element.sub)
                val cleanSup = cleanMathText(element.sup)
                val cleanBase = cleanMathText(element.base)
                val baseLen = maxOf(1, cleanBase.length)
                val scriptLen = maxOf(cleanSub.length, cleanSup.length)
                
                val widthSp = (baseLen * baseSize * 0.45f) + (scriptLen * baseSize * 0.35f) + 4f
                
                inlineContentMap[inlineId] = InlineTextContent(
                    Placeholder(
                        width = widthSp.sp,
                        height = (baseSize * 1.8f).sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        InlineMathText(
                            text = element.base,
                            style = style
                        )
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxHeight(0.85f)
                                .padding(start = 1.dp)
                        ) {
                            InlineMathText(
                                text = element.sup,
                                style = style.copy(
                                    fontSize = (baseSize * 0.68f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    lineHeight = (baseSize * 0.7f).sp
                                )
                            )
                            InlineMathText(
                                text = element.sub,
                                style = style.copy(
                                    fontSize = (baseSize * 0.68f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    lineHeight = (baseSize * 0.7f).sp
                                )
                            )
                        }
                    }
                }
            }
            is MathTextElement.Integral -> {
                prevWasMathSymbol = true
                val elementIdx = getNextElementIdx()
                val inlineId = "integral_${lineIdx}_${elementIdx}"
                builder.appendInlineContent(inlineId, "[Tích phân]")
                
                val baseFontSize = style.fontSize
                val baseSize = if (baseFontSize == androidx.compose.ui.unit.TextUnit.Unspecified) 14f else baseFontSize.value
                val cleanLower = cleanMathText(element.lower)
                val cleanUpper = cleanMathText(element.upper)
                val limitLen = maxOf(cleanLower.length, cleanUpper.length)
                
                val widthSp = if (limitLen == 0) (baseSize * 0.7f) else (baseSize * 0.7f + limitLen * baseSize * 0.4f)
                
                inlineContentMap[inlineId] = InlineTextContent(
                    Placeholder(
                        width = widthSp.sp,
                        height = (baseSize * 2.1f).sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(
                            text = "∫",
                            style = style.copy(
                                fontSize = (baseSize * 1.6f).sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        if (element.lower.isNotEmpty() || element.upper.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxHeight(0.85f)
                                    .align(Alignment.CenterVertically)
                                    .padding(start = 1.dp)
                            ) {
                                InlineMathText(
                                    text = element.upper,
                                    style = style.copy(
                                        fontSize = (baseSize * 0.68f).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        lineHeight = (baseSize * 0.7f).sp
                                    )
                                )
                                InlineMathText(
                                    text = element.lower,
                                    style = style.copy(
                                        fontSize = (baseSize * 0.68f).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        lineHeight = (baseSize * 0.7f).sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
            is MathTextElement.NewLine -> {
                prevWasMathSymbol = false
                builder.append("\n")
            }
        }
    }
}

fun normalizeMathSymbols(text: String): String {
    val sb = java.lang.StringBuilder()
    var i = 0
    val len = text.length
    while (i < len) {
        val cp = text.codePointAt(i)
        val charCount = java.lang.Character.charCount(cp)
        
        var mapped: String? = null
        if (cp in 0x1D400..0x1D7FF) {
            val offset = cp - 0x1D400
            if (offset in 0..25) {
                mapped = ( 'A' + offset ).toString()
            } else if (offset in 26..51) {
                mapped = ( 'a' + (offset - 26) ).toString()
            } else if (offset in 52..77) {
                mapped = ( 'A' + (offset - 52) ).toString()
            } else if (offset in 78..103) {
                mapped = ( 'a' + (offset - 78) ).toString()
            } else if (offset in 104..129) {
                mapped = ( 'A' + (offset - 104) ).toString()
            } else if (offset in 130..155) {
                mapped = ( 'a' + (offset - 130) ).toString()
            } else if (offset in 156..181) {
                mapped = ( 'A' + (offset - 156) ).toString()
            } else if (offset in 182..207) {
                mapped = ( 'a' + (offset - 182) ).toString()
            } else if (offset in 208..233) {
                mapped = ( 'A' + (offset - 208) ).toString()
            } else if (offset in 234..259) {
                mapped = ( 'a' + (offset - 234) ).toString()
            } else if (offset in 260..285) {
                mapped = ( 'A' + (offset - 260) ).toString()
            } else if (offset in 286..311) {
                mapped = ( 'a' + (offset - 286) ).toString()
            } else if (offset in 312..337) {
                mapped = ( 'A' + (offset - 312) ).toString()
            } else if (offset in 338..363) {
                mapped = ( 'a' + (offset - 338) ).toString()
            } else if (offset in 364..389) {
                mapped = ( 'A' + (offset - 364) ).toString()
            } else if (offset in 390..415) {
                mapped = ( 'a' + (offset - 390) ).toString()
            } else if (offset in 416..441) {
                mapped = ( 'A' + (offset - 416) ).toString()
            } else if (offset in 442..467) {
                mapped = ( 'a' + (offset - 442) ).toString()
            } else if (offset in 468..493) {
                mapped = ( 'A' + (offset - 468) ).toString()
            } else if (offset in 494..519) {
                mapped = ( 'a' + (offset - 494) ).toString()
            } else if (offset in 520..545) {
                mapped = ( 'A' + (offset - 520) ).toString()
            } else if (offset in 546..571) {
                mapped = ( 'a' + (offset - 546) ).toString()
            } else if (offset in 572..597) {
                mapped = ( 'A' + (offset - 572) ).toString()
            } else if (offset in 598..623) {
                mapped = ( 'a' + (offset - 598) ).toString()
            } else if (offset in 624..649) {
                mapped = ( 'A' + (offset - 624) ).toString()
            } else if (offset in 650..675) {
                mapped = ( 'a' + (offset - 650) ).toString()
            }
            
            // Map mathematical Greek Capitals to standard Greek Capitals (0x0391 - Α)
            else if (cp in 0x1D6A8..0x1D6C0) {
                mapped = java.lang.Character.toString((0x0391 + (cp - 0x1D6A8)))
            } else if (cp in 0x1D6E2..0x1D6FA) {
                mapped = java.lang.Character.toString((0x0391 + (cp - 0x1D6E2)))
            } else if (cp in 0x1D71C..0x1D734) {
                mapped = java.lang.Character.toString((0x0391 + (cp - 0x1D71C)))
            } else if (cp in 0x1D756..0x1D76E) {
                mapped = java.lang.Character.toString((0x0391 + (cp - 0x1D756)))
            } else if (cp in 0x1D790..0x1D7A8) {
                mapped = java.lang.Character.toString((0x0391 + (cp - 0x1D790)))
            }
            
            // Map mathematical Greek Small to standard Greek Small (0x03B1 - α)
            else if (cp in 0x1D6C2..0x1D6DA) {
                mapped = java.lang.Character.toString((0x03B1 + (cp - 0x1D6C2)))
            } else if (cp in 0x1D6FC..0x1D714) {
                mapped = java.lang.Character.toString((0x03B1 + (cp - 0x1D6FC)))
            } else if (cp in 0x1D736..0x1D74E) {
                mapped = java.lang.Character.toString((0x03B1 + (cp - 0x1D736)))
            } else if (cp in 0x1D770..0x1D788) {
                mapped = java.lang.Character.toString((0x03B1 + (cp - 0x1D770)))
            } else if (cp in 0x1D7AA..0x1D7C2) {
                mapped = java.lang.Character.toString((0x03B1 + (cp - 0x1D7AA)))
            }
        }
        
        if (mapped != null) {
            sb.append(mapped)
        } else {
            val cleanedLetter = when (cp) {
                0x212C -> "B"
                0x2130 -> "E"
                0x2131 -> "F"
                0x210B -> "H"
                0x2110 -> "I"
                0x2112 -> "L"
                0x2133 -> "M"
                0x211B -> "R"
                0x210A -> "g"
                0x2134 -> "o"
                0x2102 -> "C"
                0x210D -> "H"
                0x2115 -> "N"
                0x2119 -> "P"
                0x211A -> "Q"
                0x211D -> "R"
                0x2124 -> "Z"
                else -> text.substring(i, i + charCount)
            }
            sb.append(cleanedLetter)
        }
        i += charCount
    }
    return sb.toString()
}

fun cleanMathText(text: String): String {
    if (text.isBlank()) return ""
    val normalized = normalizeMathSymbols(text)
    return normalized
        // Remove LaTeX math block/inline delimiters $ and $$ to eliminate leftover dollar signs
        .replace(Regex("\\$\\$+"), " ")
        .replace("$", "")
        
        // Remove LaTeX block-diagram packages (like tikzpicture, pgfplots, etc.) that cause rendering mess
        .replace(Regex("\\\\begin\\{tikzpicture\\}[\\s\\S]*?\\\\end\\{tikzpicture\\}"), " [Hình vẽ minh họa] ")
        .replace(Regex("\\\\begin\\{(array|matrix|tabular|pmatrix|bmatrix)\\}\\{?[^\\}]*\\}?"), "\n")
        .replace(Regex("\\\\end\\{(array|matrix|tabular|pmatrix|bmatrix)\\}"), "\n")
        .replace("\\\\", "\n")
        .replace("&", " | ")
        .replace("\\hline", "")
        .replace(Regex("\\\\begin\\{aligned\\}"), " ")
        .replace(Regex("\\\\end\\{aligned\\}"), " ")
        
        // Clean up superscript and subscript spacing artifacts
        .replace(Regex("\\^\\s+\\{"), "^{")
        .replace(Regex("_\\s+\\{"), "_{")
        .replace(Regex("\\^\\s+"), "^")
        .replace(Regex("_\\s+"), "_")
        .replace("^{}", "")
        .replace("_{}", "")
        
        // Format cases equation system elegantly for display
        .replace("\\begin{cases}", " { ")
        .replace("\\end{cases}", " ")
        
        // Clean vectors and long arrows to plain text to avoid broken symbols
        .replace(Regex("\\\\vec\\{([^\\}]+)\\}"), "$1")
        .replace(Regex("\\\\overrightarrow\\{([^\\}]+)\\}"), "$1")
        .replace(Regex("\\\\overline\\{([^\\}]+)\\}"), "$1")
        .replace(Regex("\\\\bar\\{([^\\}]+)\\}"), "$1")
        .replace(Regex("\\\\widehat\\{([^\\}]+)\\}"), "góc $1")
        .replace(Regex("\\\\hat\\{([^\\}]+)\\}"), "$1")
        .replace(Regex("\\\\tilde\\{([^\\}]+)\\}"), "$1")
        
        // Clean LaTeX text formats and fonts so standard words render beautifully
        .replace(Regex("\\\\text\\{([^\\}]+)\\}"), "$1")
        .replace(Regex("\\\\mathrm\\{([^\\}]+)\\}"), "$1")
        .replace(Regex("\\\\mathbf\\{([^\\}]+)\\}"), "$1")
        .replace(Regex("\\\\mathit\\{([^\\}]+)\\}"), "$1")
        .replace(Regex("\\\\mathsf\\{([^\\}]+)\\}"), "$1")
        .replace(Regex("\\\\mathtt\\{([^\\}]+)\\}"), "$1")
        .replace(Regex("\\\\mathscr\\{([^\\}]+)\\}"), "$1")
        .replace(Regex("\\\\math(cal|bb|rm|bf|it|sf|tt|scr)\\{([^\\}]+)\\}"), "$2")
        .replace(Regex("\\\\math(cal|bb|rm|bf|it|sf|tt|scr)\\s+([A-Za-z0-9])"), "$2")
        
        // Clean double-struck capitals R, C, Z, N, Q that fail to render on many devices
        .replace("\\mathbb{R}", "R")
        .replace("\\mathbb R", "R")
        .replace("ℝ", "R")
        .replace("\\mathbb{C}", "C")
        .replace("\\mathbb C", "C")
        .replace("ℂ", "C")
        .replace("\\mathbb{Z}", "Z")
        .replace("\\mathbb Z", "Z")
        .replace("ℤ", "Z")
        .replace("\\mathbb{N}", "N")
        .replace("\\mathbb N", "N")
        .replace("ℕ", "N")
        .replace("\\mathbb{Q}", "Q")
        .replace("\\mathbb Q", "Q")
        .replace("ℚ", "Q")
        
        // Clean LaTeX calligraphic symbols (\mathcal) and envelope glyphs that render incorrectly on many Android devices
        .replace("✉", "")
        .replace("\u2709", "")
        .replace("\u2700", "")
        .replace("\u2701", "")
        .replace("\\mathcal{C}", "C")
        .replace("\\mathcal C", "C")
        .replace("𝒞", "C")
        .replace("\\mathcal{D}", "D")
        .replace("\\mathcal D", "D")
        .replace("𝒟", "D")
        .replace("\\mathcal{P}", "P")
        .replace("\\mathcal P", "P")
        .replace("𝒫", "P")
        .replace("\\mathcal{O}", "O")
        .replace("\\mathcal O", "O")
        .replace("𝒪", "O")
        .replace("\\mathcal{S}", "S")
        .replace("\\mathcal S", "S")
        .replace("𝒮", "S")
        .replace("\\mathcal{M}", "M")
        .replace("\\mathcal M", "M")
        .replace("ℳ", "M")
        .replace("\\mathcal{N}", "N")
        .replace("\\mathcal N", "N")
        .replace("𝒩", "N")
        
        // Clean Unicode subscript characters to standard LaTeX subscripts so the app's parser draws them correctly
        .replace("₀", "_0")
        .replace("₁", "_1")
        .replace("₂", "_2")
        .replace("₃", "_3")
        .replace("₄", "_4")
        .replace("₅", "_5")
        .replace("₆", "_6")
        .replace("₇", "_7")
        .replace("₈", "_8")
        .replace("₉", "_9")
        .replace("₊", "_+")
        .replace("₋", "_-")
        
        // Clean Unicode superscript characters to standard LaTeX powers/superscripts so the app's parser draws them correctly
        .replace("⁰", "^0")
        .replace("¹", "^1")
        .replace("²", "^2")
        .replace("³", "^3")
        .replace("⁴", "^4")
        .replace("⁵", "^5")
        .replace("⁶", "^6")
        .replace("⁷", "^7")
        .replace("⁸", "^8")
        .replace("⁹", "^9")
        .replace("ⁿ", "^n")
        .replace("⁺", "^+")
        .replace("⁻", "^-")
        
        // Clean standard LaTeX layout controls and spacing
        .replace("\\left(", "(")
        .replace("\\right)", ")")
        .replace("\\left[", "[")
        .replace("\\right]", "]")
        .replace("\\left\\{", "{")
        .replace("\\right\\}", "}")
        .replace("\\left", "")
        .replace("\\right", "")
        .replace("\\{", "{")
        .replace("\\}", "}")
        .replace("\\,", " ")
        .replace("\\;", " ")
        .replace("\\!", "")
        
        // Remove LaTeX function backslashes to make them standard text
        .replace("\\log", "log")
        .replace("\\ln", "ln")
        .replace("\\sin", "sin")
        .replace("\\cos", "cos")
        .replace("\\tan", "tan")
        .replace("\\cot", "cot")
        .replace("\\lim", "lim")
        
        // Clean standard LaTeX symbols with compatible equivalents
        .replace("\\le", "≤")
        .replace("\\ge", "≥")
        .replace("\\leq", "≤")
        .replace("\\geq", "≥")
        .replace("\\neq", "≠")
        .replace("\\in", "∈")
        .replace("\\notin", "∉")
        .replace("\\subset", "⊂")
        .replace("\\supset", "⊃")
        .replace("\\cup", "∪")
        .replace("\\cap", "∩")
        .replace("\\varnothing", "∅")
        .replace("\\emptyset", "∅")
        .replace("\\infty", "∞")
        .replace("\\pi", "π")
        .replace("\\theta", "θ")
        .replace("\\alpha", "α")
        .replace("\\beta", "β")
        .replace("\\gamma", "γ")
        .replace("\\Delta", "Δ")
        .replace("\\pm", "±")
        .replace("\\times", " × ")
        .replace("\\cdot", " · ")
        .replace("\\approx", "≈")
        .replace("\\to", "→")
        .replace("\\rightarrow", "→")
        .replace("\\Rightarrow", "⇒")
        .replace("\\Leftrightarrow", "⇔")
        .replace("\\parallel", "//")
        .replace("\\perp", "⊥")
        .replace("\\degree", "°")
        .replace("\\circ", "°")
        .replace("\\equiv", "≡")
        .replace("\\sim", "~")
        .replace("\\nearrow", "↗")
        .replace("\\searrow", "↘")
        .replace("\\uparrow", "↑")
        .replace("\\downarrow", "↓")
        .replace("\\updownarrow", "↕")
        .replace("\\angle", "góc")
        .replace("\\triangle", "Δ")
        .replace("\\forall", "với mọi")
        .replace("\\exists", "tồn tại")
        
        // Strip other raw double escape characters
        .replace("\\\\", "\\")
}

@Composable
fun MathTable(
    rows: List<List<String>>,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    if (rows.isEmpty()) return
    Card(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(4.dp)
        ) {
            Column {
                rows.forEachIndexed { rowIndex, row ->
                    val isHeader = rowIndex == 0
                    val rowBg = if (isHeader) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else if (rowIndex % 2 == 1) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }

                    Row(
                        modifier = Modifier
                            .background(rowBg, shape = RoundedCornerShape(4.dp))
                    ) {
                        row.forEachIndexed { colIndex, cellText ->
                            val cleanCell = cellText.trim()
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                InlineMathText(
                                    text = cleanCell,
                                    style = style.copy(
                                        fontWeight = if (isHeader || colIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = (style.fontSize.value * 0.92f).sp,
                                        color = if (isHeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormattedMathText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium
) {
    val cleanedText = cleanMathText(text)
    val lines = cleanedText.split("\n")
    
    val blocks = mutableListOf<Any>()
    var currentTableRows = mutableListOf<List<String>>()
    
    lines.forEach { line ->
        val trimmed = line.trim()
        if (trimmed.contains("|") && trimmed.split("|").filter { it.isNotBlank() }.size >= 2) {
            val isSeparatorRow = trimmed.replace("|", "").replace("-", "").replace(":", "").isBlank()
            if (!isSeparatorRow) {
                val cells = trimmed.split("|")
                    .map { it.trim() }
                    .filterIndexed { idx, cell ->
                        !(idx == 0 && cell.isEmpty()) && !(idx == trimmed.split("|").lastIndex && cell.isEmpty())
                    }
                if (cells.isNotEmpty()) {
                    currentTableRows.add(cells)
                }
            }
        } else {
            if (currentTableRows.isNotEmpty()) {
                blocks.add(currentTableRows.toList())
                currentTableRows = mutableListOf()
            }
            if (trimmed.isNotEmpty()) {
                blocks.add(trimmed)
            }
        }
    }
    if (currentTableRows.isNotEmpty()) {
        blocks.add(currentTableRows.toList())
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.Start
    ) {
        blocks.forEachIndexed { blockIdx, block ->
            if (block is List<*>) {
                @Suppress("UNCHECKED_CAST")
                val tableRows = block as List<List<String>>
                MathTable(rows = tableRows, style = style)
            } else if (block is String) {
                var trimmedLine = block
                if (trimmedLine.startsWith("* ") || trimmedLine.startsWith("- ")) {
                    trimmedLine = "• " + trimmedLine.substring(2)
                }
                
                val elements = parseMathText(trimmedLine)
                var elementIdx = 0
                val inlineContentMap = mutableMapOf<String, InlineTextContent>()
                val annotatedString = buildAnnotatedString {
                    appendMathElements(
                        builder = this,
                        elements = elements,
                        style = style,
                        inlineContentMap = inlineContentMap,
                        lineIdx = blockIdx,
                        getNextElementIdx = { elementIdx++ }
                    )
                }
                
                Text(
                    text = annotatedString,
                    style = style,
                    inlineContent = inlineContentMap,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
