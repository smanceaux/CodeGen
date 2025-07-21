package cap.manchou.codegen.matcher

import org.hamcrest.Description
import org.hamcrest.TypeSafeDiagnosingMatcher

class ConvertsToMatcher(val input: String, val expectedOutput: String? = UNKNOWN) :
  TypeSafeDiagnosingMatcher<((String) -> String)>() {

  fun to(expectedOutput: String?) = ConvertsToMatcher(input, expectedOutput)

  override fun describeTo(description: Description) {
    description.appendText("converts ").appendValue(input).appendText(" to ")
      .appendValue(expectedOutput)
  }

  override fun matchesSafely(
    function: ((String) -> String),
    description: Description,
  ): Boolean {
    if (expectedOutput == UNKNOWN) {
      throw AssertionError(
        """
        expectedOutput is not initialized. 
        You should use 'to' method following this example:
        assertThat({ s: String -> anyStringFunction(s) }, converts("input").to("expectedOutput")
        """.trimIndent()
      )
    }
    val result = function(input)
    description.appendText("converted ").appendValue(input).appendText(" to ")
      .appendValue(result)
    return result == this.expectedOutput
  }
}

const val UNKNOWN = "unknown-invalid-expectedOutput"

fun converts(input: String): ConvertsToMatcher = ConvertsToMatcher(input)
