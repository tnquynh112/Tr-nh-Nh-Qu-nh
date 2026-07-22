package com.example

import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testMathParsing_FractionWithPowerInNumerator() {
    val mathElements = parseMathText("(x-1)^2/25")
    assertEquals(1, mathElements.size)
    val first = mathElements[0]
    assertTrue(first is MathTextElement.Fraction)
    val fraction = first as MathTextElement.Fraction
    assertEquals("(x-1)^2", fraction.numerator)
    assertEquals("25", fraction.denominator)
  }

  @Test
  fun testMathParsing_FractionWithOuterParentheses() {
    val mathElements = parseMathText("(x+1)/(x-1)")
    assertEquals(1, mathElements.size)
    val first = mathElements[0]
    assertTrue(first is MathTextElement.Fraction)
    val fraction = first as MathTextElement.Fraction
    assertEquals("x+1", fraction.numerator)
    assertEquals("x-1", fraction.denominator)
  }

  @Test
  fun testMathParsing_CompoundExpression() {
    val mathElements = parseMathText("y = (2x - 1) / (x - 1)")
    // Expect: NormalText("y = "), Fraction("2x - 1", "x - 1")
    assertEquals(2, mathElements.size)
    assertTrue(mathElements[0] is MathTextElement.NormalText)
    assertEquals("y = ", (mathElements[0] as MathTextElement.NormalText).text)
    
    assertTrue(mathElements[1] is MathTextElement.Fraction)
    val fraction = mathElements[1] as MathTextElement.Fraction
    assertEquals("2x - 1", fraction.numerator)
    assertEquals("x - 1", fraction.denominator)
  }
}
