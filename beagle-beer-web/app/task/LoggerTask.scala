package task

import play.api.db.slick.DB
import models._
import java.util.Date
import org.slf4j.LoggerFactory
import io.{GpioRegistry, DS1820BulkReader}
import play.api.Play.current


import scala.annotation.tailrec
import models.Sample
import scala.Some
import models.Log
import models.DS1820
import scala.slick.driver.H2Driver.simple._


/**
 * When started, records a new Log in the database, records Measurements every 10 seconds,
 * exits when <code>on</code> is set to false.
 */
class LoggerTask(var logIntervalMillis: Int, devices: List[DS1820], var listeners: List[LoggerTaskListener]) extends Runnable {
  require(devices != null)

  def this(devices: List[DS1820]) = this(10000, devices, LoggerTaskManager.idleListeners)


  val dLog = LoggerFactory.getLogger(this.getClass)

  var logRecord: Option[Log] = None
  var endThread = false

  def run: Unit = {


    while (!endThread) {
      val now = new Date
      val reads = DS1820BulkReader.readAll(devices.map(d => d.path))
      dLog.debug("read " + reads.size + " devices")

      // convert the reads into samples
      val samples = for {read <- reads} yield createSample(read, logRecord, now)

      // send each sample through each LoggerTaskListener
      for {
        listener <- listeners
      } listener.receiveRead(logRecord, samples)

      sleepWithFastWake(logIntervalMillis, 200)
    }


  }

  def destroy = {
    dLog.debug("destroying")
    endThread = true
  }

  def isRunning: Boolean = logRecord match {
    case None => false
    case Some(_) => true
  }

  private def createSample(read: (String, Option[Float]), logRecord: Option[Log], now: Date): Sample = {
    val device = devices.find(d => d.path == read._1).get // should always find the correct device
    val logRecordId = logRecord match {
        case None => -1
        case Some(lRcd) => lRcd.id.getOrElse(-1)
      }
    val sample = new Sample(None, logRecordId, device.id.get, now, read._2)
    return sample

  }

  /**
   * Sleeps the current thread for a specified time, but checks for the on flag == false
   * at the specified poll time out.  Wakes the thread after the specified time or
   * when on == false, whichever comes first.
   */
  def sleepWithFastWake(sleepTimeMillis: Int, pollForWakeTimeMillis: Int): Unit = {
    require(sleepTimeMillis >= pollForWakeTimeMillis)

    val sleepStart = new Date().getTime
    sleep

    @tailrec
    def sleep: Unit = {
      Thread.sleep(pollForWakeTimeMillis)
      val again = new Date().getTime - sleepStart < sleepTimeMillis && !endThread
      if (again) sleep
    }
  }
}

/**
 * A helper class to mange LoggerTask threads. 
 */
object LoggerTaskManager {

  val dLog = LoggerFactory.getLogger("LoggerTasks")
  val idleListeners = List(LatestValueListener)


  private var taskOption: Option[LoggerTask] = None


  def isInitialised: Boolean = {
    taskOption match {
      case None => false
      case Some(t) => true
    }
  }


  def isRunning: Boolean = {
    taskOption match {
      case None => false
      case Some(t) => t.isRunning
    }
  }

  def start(name: String, targetTemperature: Float, logIntervalMillis: Int)(implicit session: Session): Unit = {
    taskOption match {
      case None => ;
      case Some(t) => {
        if (!t.isRunning) {
          val log = LogsDb.insert(Log(None, name, targetTemperature, new Date, None))
          t.logRecord = Some(log)

          val masterProbe = DS1820sDb.getMaster
          val runningListeners = List(LatestValueListener,
                                      SamplePersistingListener,
                                      new GpioControllingListener(masterProbe.id.get, GpioRegistry.hot, GpioRegistry.cold))

          t.listeners = runningListeners
          t.logIntervalMillis = logIntervalMillis
          dLog.debug("Started temperature log " + log)
        }
      }
    }
  }

  def stop(implicit session: Session) = {
    taskOption match {
      case Some(task) => {
        if (task.isRunning) {
          task.logIntervalMillis = 10000
          task.listeners = idleListeners
          val endedLogRecord = LogsDb.update(task.logRecord.get.copy(end = Some(new Date)))
          dLog.debug("Ended temperature log " + endedLogRecord)
        }
        task.logRecord = None
      }
      case None => ;
    }

  }

  def destroy(implicit session: Session) = {
    taskOption match {
      case Some(t) => {
        if (t.isRunning) {
          stop
        }
        t.destroy
      }
      case _ => ;
    }
  }


  def loggerTask: Option[LoggerTask] = {
    taskOption
  }

  def initialise(implicit session: Session): Unit = {
    val devices = DS1820sDb.filterByEnabled(true)
    if (devices isEmpty) {
      dLog.error("No DS1820s are configured, unable to create Logger Task")
      taskOption = None
    } else {
      val newTask = new LoggerTask(devices);
      new Thread(newTask).start
      taskOption = Some(newTask)
    }
  }

}

