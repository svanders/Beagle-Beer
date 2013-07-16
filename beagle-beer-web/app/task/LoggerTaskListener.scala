package task

import models.{Sample, Log}
import java.util.Date
import org.slf4j.LoggerFactory

/**
 * A trait for anything interested in new samples to implement.  If the Listener is registered
 * with the LoggerTask (at construction), then it will be called for each new Sample.
 */
trait LoggerTaskListener {

  /**
   * Implementations free to do what they want with the Sample.
   * @param sample
   */
  def receiveRead(sample: Sample)
}

/**
 * A simple Listener that simply logs sampple to the slf4f, useful for debug.
 */
object DebugLogLoggerTaskListener extends LoggerTaskListener {

  val log = LoggerFactory.getLogger("task.DebugLogLoggerTaskListener")

  def receiveRead(sample: Sample) = log.debug("Recorded sample "  + sample)
}








