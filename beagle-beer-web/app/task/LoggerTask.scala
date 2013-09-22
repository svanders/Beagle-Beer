package task

import play.api.db.slick.DB
import models._
import java.util.Date
import org.slf4j.LoggerFactory
import io.DS1820BulkReader
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
class LoggerTask(logIntervalMillis: Int, devices: List[DS1820], listeners: List[LoggerTaskListener]) extends Runnable {
  require(devices != null)
  require(listeners != null)
  require(logIntervalMillis >= 1000)

  val dLog = LoggerFactory.getLogger(this.getClass)

  var logRecord = LogsDb.ildeLog
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
      } listener.receiveRead(samples)

      sleepWithFastWake(logIntervalMillis, 200)
    }


  }

  def destroy = {
    dLog.debug("destroying")
    endThread = true
  }

  def isRunning: Boolean = !logRecord.equals(LogsDb.ildeLog)

  private def createSample(read: (String, Option[Float]), logRecord: Log, now: Date): Sample = {
    val device = devices.find(d => d.path == read._1).get // should always find the correct device
    // bug here logRecord.id is None when idling
    val sample = new Sample(None, logRecord.id.get, device.id.get, now, read._2)
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

  private var task: Option[LoggerTask] = None


  def isInitialised: Boolean = {
    task match {
      case None => false
      case Some(t) => true
    }
  }


  def isRunning: Boolean = {
    task match {
      case None => false
      case Some(t) => t.isRunning
    }
  }

  def start(name: String, targetTemperature: Int)(implicit session: Session): Unit = {
    task match {
      case None => ;
      case Some(t) => {
        if (!t.isRunning) {
          val log = LogsDb.insert(Log(None, name, targetTemperature, new Date, None))
          t.logRecord = log
          dLog.debug("Started temperature log " + log)
        }
      }
    }
  }

  def stop(implicit session: Session) = {
    task match {
      case Some(t) => {
        if (t.isRunning) {
          val endedLogRecord = LogsDb.update(t.logRecord.copy(end = Some(new Date)))
          dLog.debug("Ended temperature log " + endedLogRecord)
        }
        t.logRecord = LogsDb.ildeLog
      }
      case None => ;
    }

  }

  def destroy(implicit session: Session) = {
    task match {
      case Some(t) => {
        if (t.isRunning) {
          stop
        }
        t.destroy
      }
      case _ => ;
    }
  }


  //  private def loggerTask: Option[LoggerTask] = {
  //    task match {
  //      case None => task = initialise // try and create one
  //      case _ => ;
  //    }
  //    task
  //  }

  def initialise: Unit = {
    DB.withSession {
      implicit session =>
        val devices = DS1820sDb.filterByEnabled(true)
        if (devices isEmpty) {
          dLog.error("No DS1820s are configured, unable to create Logger Task")
          task = None
        } else {
          val newTask = new LoggerTask(10000, devices, List(DebugLogLoggerTaskListener, LatestValueListener, SamplePersistingListener))
          new Thread(newTask).start
          task = Some(newTask)
        }
    }
  }

}

