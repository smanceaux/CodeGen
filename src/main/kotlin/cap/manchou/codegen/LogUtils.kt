package cap.manchou.codegen

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.LoggerFactory

object LogUtils {

  private val log = LoggerFactory.getLogger(LogUtils.javaClass)
  val env = "local"

  private val writer = ObjectMapper().writer().withoutRootName()

  var lineSeparator = lineSeparator()

  var tab = tab()


  fun lineSeparator(): String {
    try {
      log.debug("Environment: $env")
      if (arrayOf("local", "qa", "dev", "recette").contains(env.toLowerCase())) {
        return System.lineSeparator()
      }
    } catch (ex: IllegalArgumentException) {
      log.error("Missing 'env' environment variable")
    }
    return "|"
  }

  fun tab(): String {
    try {
      log.debug("Environment: $env")
      if (arrayOf("local", "qa", "dev", "recette").contains(env.toLowerCase())) {
        return "\t"
      }
    } catch (ex: IllegalArgumentException) {
      log.error("Missing 'env' environment variable")
    }
    return "  "
  }

  fun toJson(o: Any): String {
    try {
      return writer.writeValueAsString(o)
    } catch (e: JsonProcessingException) {
      return "ERROR when processing Json: " + e.message
    }
  }

  /**
   * Get the stack trace of [ex] and factorise the lines when a bunch of similar lines are repeated.
   * The [lineSeparator] is used as separator between lines instead of a classical "\n".
   *
   * @return the collapsed stack as String
   */
  fun collapseStack(ex: Exception): String {
    try {
      return collapseStack(ExceptionUtils.getStackFrames(ex))
    } catch (e: Throwable) {
      return ExceptionUtils.getStackTrace(ex)
    }
  }

  /**
   * Take the given the [stackFrames] and factorise them when a bunch of similar lines are repeated.
   * The [lineSeparator] is used as separator between lines instead of a classical "\n".
   *
   * @return the collapsed stack as String
   */
  internal fun collapseStack(stackFrames: Array<String>): String {

    var stackElements = mutableListOf<StackElement>()

    for (frame in stackFrames) {
      val line = frame.trim();
      val last = stackElements.lastOrNull()
      if (!(last is StackElementMultiple) && line.equals(last?.frame)) {
        last!!.plusOne()
      } else {
        stackElements.add(StackElement(line, 1))
      }

      for (collapseSize in 2..9) {
        stackElements = collapseMultipleRepetition(stackElements, collapseSize)
      }
    }
    return stackElements.joinToString(separator = lineSeparator + tab, transform = StackElement::toLog)
  }

  /**
   * Search in the [stackElements] if the last [collapseSize] elements are equal to the [collapseSize] elements before.
   * If so, creates a StackElementMultiple to factorise them.
   *
   * @return the modified StackElements
   */
  private fun collapseMultipleRepetition(
    stackElements: MutableList<StackElement>,
    collapseSize: Int): MutableList<StackElement> {

    if (stackElements.size > collapseSize * 2) {
      if (stackElements.get(stackElements.size - collapseSize - 1) is StackElementMultiple) {
        val multiple = stackElements.get(stackElements.size - collapseSize - 1) as StackElementMultiple
        if (stackElements.subList(stackElements.size - collapseSize, stackElements.size) == multiple.subs) {
          multiple.plusOne()
          return stackElements.subList(0, stackElements.size - collapseSize);
        }
      } else if (stackElements.subList(stackElements.size - collapseSize, stackElements.size)
        == stackElements.subList(stackElements.size - collapseSize * 2, stackElements.size - collapseSize)) {
        val newStack = stackElements.subList(0, stackElements.size - collapseSize * 2)
        newStack.add(StackElementMultiple(stackElements.subList(stackElements.size - collapseSize, stackElements.size), 2))
        return newStack;
      }
    }
    return stackElements
  }

  private open class StackElement(val frame: String, var nb: Int) {

    fun plusOne(): StackElement {
      nb++
      return this
    }

    open fun toLog(): String {
      return if (nb == 1) frame else "$frame  ($nb times)"
    }

    override fun toString(): String {
      return "$frame ($nb times)"
    }

    override fun equals(other: Any?): Boolean {
      return other is StackElement
          && frame == other.frame
          && nb == other.nb
    }

    override fun hashCode(): Int {
      return frame.hashCode() * 31 + nb
    }
  }

  private class StackElementMultiple : StackElement {

    val subs : List<StackElement>
    val start = "{" + if (tab.length > 1) tab.substring(1) else tab

    constructor(subs: MutableList<StackElement>, nb: Int) : super("", nb) {
      this.subs = subs.toMutableList()
    }

    override fun toLog(): String {
      return start + subs.joinToString("$lineSeparator$tab$tab", transform = StackElement::toLog) + " }  ($nb times)"
    }

    override fun toString(): String {
      return "{ " + subs.joinToString(", ", transform = Any::toString) + " } ($nb times)"
    }

    override fun equals(other: Any?): Boolean {
      return other is StackElementMultiple
          && subs == other.subs
          && nb == other.nb
    }

    override fun hashCode(): Int {
      return subs.hashCode() * 31 + nb
    }
  }
}