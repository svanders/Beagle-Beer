package task

import models.{DS1820, Sample, Log}
import java.util.Date
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

/**
 * A trait for anything interested in new samples to implement.  If the Listener is registered
 * with the LoggerTask (at construction), then it will be called for each new Sample.
 */
trait LoggerTaskListener {

  /**
   * Implementations free to do what they want with the Samples.
   * @param samples
   */
  def receiveRead(samples: List[Sample]): Unit
}

/**
 * A simple Listener that simply logs sampple to the slf4f, useful for debug.
 */
object DebugLogLoggerTaskListener extends LoggerTaskListener {

  val log = LoggerFactory.getLogger("task.DebugLogLoggerTaskListener")

  override def receiveRead(samples: List[Sample]) =
    for (sample <- samples) log.debug("Recorded sample "  + sample)
}

/**
 * A listener that provides the latest samples.
 */
object LatestValueListener extends LoggerTaskListener {

  var latest: List[Sample] = List()

  override def receiveRead(samples: List[Sample]) {
     latest = samples
  }

  def clear: Unit = {
    latest = List()
  }


}



