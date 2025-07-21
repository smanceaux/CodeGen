package cap.manchou.codegen.generator.resolver

import cap.manchou.codegen.generator.error.InvalidExpressionException
import cap.manchou.codegen.strings.lowerCase
import cap.manchou.codegen.strings.upperCase
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ExpressionResolverTest {

  lateinit var expressionResolver: ExpressionResolver

  @BeforeEach
  fun setup() {
    expressionResolver = ExpressionResolver(
      mapOf(
        "upperCase" to wrapFunction(::upperCase),
        "lowerCase" to wrapFunction(::lowerCase),
        "buildDummyObject" to { input -> DummyObject(input.toString(), 42) },
      ),
      mapOf(
        "variableString" to "hello world",
        "variableMap" to mapOf("field1" to "toto", "field2" to 42),
        "variableList" to listOf(1, 2, 3),
        "variableNumber" to 123,
        "variableBoolean" to true,
        "variableObject" to DummyObject("hello", 42),
        "variableObject2" to DummyObject("hello", 42),
        "variableNull" to DummyObject(null, 42)
      )
    )
  }

  @Test
  fun `should throw exception if invalid input`() {
    val ex = assertThrows<InvalidExpressionException> {
      expressionResolver.resolveExpressionAsObject("fvfdjn\"dcd\"")
    }

    assertThat(
      ex.message, equalTo("Unresolved expression fvfdjn\"dcd\"")
    )
  }

  @Test
  fun `should handle null input`() {
    assertThat(
      expressionResolver.resolveExpressionAsObject("variableNull.field1"), equalTo(null)
    )
    assertThat(
      expressionResolver.resolveExpression("variableNull.field1"), equalTo("null")
    )
  }

  @Test
  fun `should handle empty input`() {
    assertThat(
      expressionResolver.resolveExpressionAsObject(""), equalTo("")
    )
    assertThat(
      expressionResolver.resolveExpressionAsObject("\"\""), equalTo("")
    )
    assertThat(expressionResolver.resolveExpression(""), equalTo(""))
  }

  @Test
  fun `should resolve literal string`() {
    assertThat(
      expressionResolver.resolveExpression("\"hello world\""), equalTo("hello world")
    )
  }

  @Test
  fun `should resolve literal number`() {
    assertThat(
      expressionResolver.resolveExpressionAsObject("123"), equalTo(123L)
    )
    assertThat(
      expressionResolver.resolveExpressionAsObject("12.3"), equalTo(12.3)
    )
  }

  @Test
  fun `should resolve null`() {
    assertThat(
      expressionResolver.resolveExpressionAsObject("null"), equalTo(null)
    )
    assertThat(
      expressionResolver.resolveExpression("null"), equalTo("null")
    )
  }

  @Test
  fun `should resolve variables`() {
    assertThat(
      expressionResolver.resolveExpressionAsObject("variableString"), equalTo("hello world")
    )
    assertThat(
      expressionResolver.resolveExpressionAsObject("variableMap.field1"), equalTo("toto")
    )
    assertThat(
      expressionResolver.resolveExpressionAsObject("variableMap[\"field1\"]"), equalTo("toto")
    )
    assertThat(
      expressionResolver.resolveExpressionAsObject("variableMap.field2"), equalTo(42)
    )
    assertThat(
      expressionResolver.resolveExpression("variableMap.field2"), equalTo("42")
    )
    assertThat(
      expressionResolver.resolveExpressionAsObject("variableList[1]"), equalTo(1)
    )
    assertThat(
      expressionResolver.resolveExpressionAsObject("variableList[2]"), equalTo(2)
    )
    assertThat(
      expressionResolver.resolveExpressionAsObject("variableList[3]"), equalTo(3)
    )
    assertThat(
      expressionResolver.resolveExpressionAsObject("variableNumber"), equalTo(123)
    )
    assertThat(
      expressionResolver.resolveExpressionAsObject("variableBoolean"), equalTo(true)
    )
    assertThat(
      expressionResolver.resolveExpression("variableBoolean"), equalTo("true")
    )
    assertThat(
      expressionResolver.resolveExpressionAsObject("variableObject"),
      equalTo(DummyObject("hello", 42))
    )
    assertThat(
      expressionResolver.resolveExpression("variableObject"),
      equalTo("DummyObject(field1=hello, field2=42)")
    )
    assertThat(
      expressionResolver.resolveExpressionAsObject("variableObject.field1"), equalTo("hello")
    )
    assertThat(
      expressionResolver.resolveExpressionAsObject("variableObject.field1.length"), equalTo(5)
    )
    assertThat(
      expressionResolver.resolveExpressionAsObject("variableObject.field2"), equalTo(42)
    )
  }

  @Test
  fun `should throw exception if variable not found`() {
    var ex = assertThrows<InvalidExpressionException> {
      expressionResolver.resolveExpressionAsObject("variableNotDefined")
    }
    assertThat(
      ex.message, equalTo("Unresolved expression variableNotDefined")
    )

    ex = assertThrows<InvalidExpressionException> {
      expressionResolver.resolveExpressionAsObject("variableNull.field1.length")
    }
    assertThat(
      ex.message, equalTo("Expression variableNull.field1 is null. Could not resolve field length")
    )

    ex = assertThrows<InvalidExpressionException> {
      expressionResolver.resolveExpressionAsObject("variableObject.toto")
    }
    assertThat(
      ex.message, equalTo("Unknown property toto in variable variableObject")
    )

    ex = assertThrows<InvalidExpressionException> {
      expressionResolver.resolveExpressionAsObject("variableObject[\"toto\"]")
    }
    assertThat(
      ex.message, equalTo("Unknown property \"toto\" in variable variableObject")
    )

    ex = assertThrows<InvalidExpressionException> {
      expressionResolver.resolveExpressionAsObject("variableMap.toto")
    }
    assertThat(
      ex.message, equalTo("Unknown property toto in variable variableMap")
    )

    ex = assertThrows<InvalidExpressionException> {
      expressionResolver.resolveExpressionAsObject("variableMap[\"toto\"]")
    }
    assertThat(
      ex.message, equalTo("Unknown property \"toto\" in variable variableMap")
    )
  }

  @Nested
  inner class Functions {

    @Test
    fun `should resolve functions`() {
      assertThat(
        expressionResolver.resolveExpressionAsObject("upperCase(   \"hello world\")"),
        equalTo("HELLO_WORLD")
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("upperCase(  variableString)"),
        equalTo("HELLO_WORLD")
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("upperCase(variableObject.field1   )"),
        equalTo("HELLO")
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("upperCase(  variableMap[\"field1\"])"),
        equalTo("TOTO")
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("upperCase(variableNumber)"),
        equalTo("123")
      )
    }

    @Test
    fun `should resolve nested functions`() {
      assertThat(
        expressionResolver.resolveExpressionAsObject("lowerCase(upperCase(  variableString))"),
        equalTo("hello_world")
      )
    }

    @Test
    fun `should throw exception if function not found`() {
      val ex = assertThrows<InvalidExpressionException> {
        expressionResolver.resolveExpressionAsObject("functionNotDefined(\"value\")")
      }

      assertThat(
        ex.message,
        equalTo("Unknown function functionNotDefined() in expression functionNotDefined(\"value\")")
      )
    }

    @Test
    fun `should throw exception if param of function is invalid`() {
      val ex = assertThrows<InvalidExpressionException> {
        expressionResolver.resolveExpressionAsObject("upperCase(lrelk)")
      }

      assertThat(
        ex.message, equalTo("Unresolved expression upperCase(lrelk)")
      )

      assertThat(ex.cause?.message, equalTo("Unresolved expression lrelk"))
    }
  }

  @Nested
  inner class Comparisons {

    @Test
    fun `should resolve comparisons of strings`() {
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableString==\"hello world\""),
        equalTo(true)
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableString != \"hello world\""),
        equalTo(false)
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableString<\"aaa\""),
        equalTo(false)
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableString > \"aaa\""),
        equalTo(true)
      )

      assertThat(
        expressionResolver.resolveExpressionAsObject("variableMap.field1==\"toto\""),
        equalTo(true)
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableMap.field1 != \"toto\""),
        equalTo(false)
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableMap.field1 <= \"toto\""),
        equalTo(true)
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableMap.field1 >= \"toto\""),
        equalTo(true)
      )
    }

    @Test
    fun `should resolve comparisons of booleans`() {

      assertThat(
        expressionResolver.resolveExpressionAsObject("variableBoolean==true"),
        equalTo(true)
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableBoolean != true"),
        equalTo(false)
      )
    }

    @Test
    fun `should resolve comparisons of numbers`() {

      assertThat(
        expressionResolver.resolveExpressionAsObject("  variableNumber == 123"),
        equalTo(true)
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableNumber!=123   "),
        equalTo(false)
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableNumber >= 123.5"),
        equalTo(false)
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableNumber >= 123"),
        equalTo(true)
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableNumber < 123"),
        equalTo(false)
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableNumber < 123.5"),
        equalTo(true)
      )
    }

    @Test
    fun `should resolve comparisons of objects`() {

      assertThat(
        expressionResolver.resolveExpressionAsObject("variableObject == buildDummyObject(\"hello\")"),
        equalTo(true)
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableObject == buildDummyObject(\"hello2\")"),
        equalTo(false)
      )

      assertThat(
        expressionResolver.resolveExpressionAsObject("variableObject != buildDummyObject(\"hello\")"),
        equalTo(false)
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableObject != buildDummyObject(\"hello2\")"),
        equalTo(true)
      )
      val ex = assertThrows<InvalidExpressionException> {
        expressionResolver.resolveExpressionAsObject("variableObject >= buildDummyObject(\"hello\")")
      }

      assertThat(
        ex.message,
        equalTo("Unresolved comparison expression variableObject >= buildDummyObject(\"hello\")")
      )
    }

    @Test
    fun `should resolve comparisons with null`() {
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableString == null"),
        equalTo(false)
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableString != null"),
        equalTo(true)
      )
      assertThat(
        expressionResolver.resolveExpressionAsObject("variableNull.field1 == null"),
        equalTo(true)
      )
    }
  }

  companion object {
    fun wrapFunction(function: ((String) -> String)): (Any?) -> Any? {
      return { s -> function.invoke(s?.toString() ?: "") }
    }
  }

  data class DummyObject(val field1: String?, val field2: Int)
}
