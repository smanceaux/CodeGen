package cap.manchou.codegen.strings

import cap.manchou.codegen.matcher.converts
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StringTransformersTest {

  val CAMEL_CASE = "helloWorld"
  val PASCAL_CASE = "HelloWorld"
  val SNAKE_CASE = "hello_world"
  val KEBAB_CASE = "hello-world"
  val UPPER_CASE = "HELLO_WORLD"
  val DOT_CASE = "hello.world"
  val LABEL = "hello world"
  val CAPITALIZED_LABEL = "Hello world"
  val TITLE_CASE = "Hello World"

  @Nested
  inner class SuperSplitTest {

    @Test
    fun `should split camel case into words`() {
      assertEquals(listOf("hello", "World"), superSplit(CAMEL_CASE))
    }

    @Test
    fun `should split snake case into words`() {
      assertEquals(listOf("hello", "world"), superSplit(SNAKE_CASE))
    }

    @Test
    fun `should split uppercase into words`() {
      assertEquals(listOf("HELLO", "WORLD"), superSplit(UPPER_CASE))
    }

    @Test
    fun `should split label into words`() {
      assertEquals(listOf("hello", "world"), superSplit(LABEL))
    }

    @Test
    fun `should split kebab case into words`() {
      assertEquals(listOf("hello", "world"), superSplit(KEBAB_CASE))
    }

    @Test
    fun `should dot separated into words`() {
      assertEquals(listOf("hello", "world"), superSplit(DOT_CASE))
    }

    @Test
    fun `should return empty list for empty string`() {
      assertEquals(emptyList(), superSplit(""))
    }

    @Test
    fun `should not split on numbers`() {
      assertEquals(listOf("hello123", "World"), superSplit("hello123World"))
    }

    @Test
    fun `should split title case into words`() {
      assertEquals(listOf("Hello", "World"), superSplit(TITLE_CASE))
    }

    @Test
    fun `should trim`() {
      assertEquals(listOf("Hello", "World"), superSplit("\t Hello __  World \t "))
    }
  }

  @Nested
  inner class ConvertTest {
    @Test
    fun `should convert any case to label`() {
      assertThat(::label, convertsAnyCaseTo(LABEL))
      assertThat(::label, converts("").to(""))
    }

    @Test
    fun `should convert any case to Capitalized label`() {
      assertThat(::capitalizedLabel, convertsAnyCaseTo(CAPITALIZED_LABEL))
      assertThat(::capitalizedLabel, converts("").to(""))
    }

    @Test
    fun `should convert any case to UPPER_CASE`() {
      assertThat(::upperCase, convertsAnyCaseTo(UPPER_CASE))
      assertThat(::upperCase, converts("").to(""))
    }

    @Test
    fun `should convert any case to camelCase`() {
      assertThat(::camelCase, convertsAnyCaseTo(CAMEL_CASE))
      assertThat(::camelCase, converts("").to(""))
    }

    @Test
    fun `should convert any case to PascalCase`() {
      assertThat(::pascalCase, convertsAnyCaseTo(PASCAL_CASE))
      assertThat(::pascalCase, converts("").to(""))
    }

    @Test
    fun `should convert any case to snake_case`() {
      assertThat(::snakeCase, convertsAnyCaseTo(SNAKE_CASE))
      assertThat(::snakeCase, converts("").to(""))
    }

    @Test
    fun `should convert any case to kebab-case`() {
      assertThat(::kebabCase, convertsAnyCaseTo(KEBAB_CASE))
      assertThat(::kebabCase, converts("").to(""))
    }

    @Test
    fun `should convert any case to dot·case`() {
      assertThat(::dotCase, convertsAnyCaseTo(DOT_CASE))
      assertThat(::dotCase, converts("").to(""))
    }

    @Test
    fun `should convert any case to Title Case`() {
      assertThat(::titleCase, convertsAnyCaseTo(TITLE_CASE))
      assertThat(::titleCase, converts("").to(""))
    }

    fun convertsAnyCaseTo(expected: String) = Matchers.allOf(
      converts(PASCAL_CASE).to(expected),
      converts(SNAKE_CASE).to(expected),
      converts(KEBAB_CASE).to(expected),
      converts(UPPER_CASE).to(expected),
      converts(LABEL).to(expected),
      converts(DOT_CASE).to(expected),
      converts(TITLE_CASE).to(expected),
      converts("\t Hello _ World \t ").to(expected)
    )
  }
}
