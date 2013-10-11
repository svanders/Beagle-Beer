package task

import models._
import org.slf4j.LoggerFactory
import play.api.db.slick.DB
import models.Sample
import io.{Gpio, GpioRegistry}

/**
 * A trait for anything interested in new samples to implement.  If the Listener is registered
 * with the LoggerTask (at construction), then it will be called for each new Sample.
 */
trait LoggerTaskListener {

  /**
   * Implementations free to do what they want with the Samples.
   * @param samples
   */
  def receiveRead(log: Option[Log], samples: List[Sample]): Unit
}

/**
 * A simple Listener that simply logs sampple to the slf4f, useful for debug.
 */
object DebugLogLoggerTaskListener extends LoggerTaskListener {

  val dLog = LoggerFactory.getLogger("task.DebugLogLoggerTaskListener")

  override def receiveRead(log: Option[Log], samples: List[Sample]) =
    for (sample <- samples) dLog.debug("Recorded sample " + sample)
}

/**
 * A listener that provides the latest samples.
 */
object LatestValueListener extends LoggerTaskListener {

  var latest: List[Sample] = List()

  override def receiveRead(log: Option[Log], samples: List[Sample]) {
    latest = samples
  }

  def clear: Unit = {
    latest = List()
  }
}

/**
 * Saves the Samples to the DB
 */
object SamplePersistingListener extends LoggerTaskListener {

  import play.api.Play.current

  override def receiveRead(log: Option[Log], samples: List[Sample]) {
    if (LoggerTaskManager.isRunning) {
      DB.withTransaction {
        implicit session =>
          samples.foreach(s => SamplesDb.insert(s))
      }
    }
  }
}

class GpioControllingListener(masterProbeId: Int, hot: Gpio, cold: Gpio) extends LoggerTaskListener {

  val dLog = LoggerFactory.getLogger(this.getClass)

  override def receiveRead(logOption: Option[Log], samples: List[Sample]) {
    logOption match {
      case None => dLog.debug("idling");
      case Some(log) => {
        val masterSample = samples.find(s => s.ds1820Id == masterProbeId)
        masterSample match {
          case None => dLog.error("No sample provided for master probe (id=" + masterProbeId + ")")
          case Some(ms) => {
            control(ms.value, log.targetTemperature)
          }
        }
      }
    }
  }

  private def control(actualTemp: Option[Float], targetTemp: Float) = {
    actualTemp match {
      case None => {
        dLog.error("Master probe not reading correctly, switch all GPIO temperature controls off")
        hot.off
        cold.off
      }
      case Some(temp) => {
        if (temp < targetTemp - 1) {
          dLog.debug("Temperature low, turning on heater")
          hot.on
          cold.off
        } else if (temp > targetTemp + 1) {
          dLog.debug("Temperature high, turning on cooler")
          hot.off
          cold.on
        } else {
          dLog.debug("Temperature in range")
        }
      }
    }
  }


}

