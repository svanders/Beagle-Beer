package task

import play.api.db.slick.DB
import models._
import java.util.Date
import org.slf4j.LoggerFactory
import io.DS1820BulkReader
import play.api.Play.current


import scala.annotation.tailrec
import play.api.libs.json.Json
import models.Sample
import scala.Some
import models.Log
import models.DS1820

/**
 * When started, records a new Log in the database, records Measurements every 10 seconds,
 * exits when <code>on</code> is set to false.
 */
class LoggerTask(logIntervalMillis: Int, devices: List[DS1820], listeners: List[LoggerTaskListener]) extends Runnable {
  require(devices != null)
  require(listeners != null)
  require(logIntervalMillis >= 1000)

  val dLog = LoggerFactory.getLogger(this.getClass)

  private var on = false

  def run: Unit = {

    on = true

    // TODO - create the Log somewhere else
    val logRecord =  DB.withTransaction {
      implicit session =>
        LogsDb.insert(Log(None, new Date, None))
    }

    dLog.debug("Started temperature log " + logRecord + " using " + devices.size + " sensors")

    while (on) {
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

    // TODO - end the Log somewhere else
    val endedLogRecord = DB.withTransaction {
      implicit session =>
        LogsDb.update(logRecord.copy(end = Some(new Date)))
    }
    dLog.debug("Ended temperature log " + endedLogRecord)
  }

  def stop = {
    dLog.debug("stopping")
    on = false
  }

  def isRunning = on

  private def createSample(read: (String, Float), logRecord: Log, now: Date): Sample = {
    val device = devices.find(d => d.path == read._1).get // should always find the correct device
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
      val again = new Date().getTime - sleepStart < sleepTimeMillis && on
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


  def isRunning = {
    loggerTask match {
      case None => false
      case Some(task) => task.isRunning
    }
  }

  def start: Boolean = {
    loggerTask match {
      case None => false
      case Some(task) => {
        if (!task.isRunning) {
          new Thread(task).start
        }
        true
      }
    }
  }

  def stop = {
    loggerTask match {
      case Some(task) => {
        if (task.isRunning) {
          task.stop
        }
      }
      case _ => ;
    }
  }

  def destroy = {
    if (isRunning) {
      stop
    }
    task = None
  }


  private def loggerTask: Option[LoggerTask] = {
    task match {
      case None => task = createTask // try and create one
      case _ => ;
    }
    task
  }

  private def createTask: Option[LoggerTask] = {
    DB.withSession {
      implicit session =>
        val devices = DS1820sDb.all
        if (devices isEmpty) {
          dLog.error("No DS1820s are configured, unable to create Logger Task")
          None
        } else {
          Some(new LoggerTask(10000, devices, List(DebugLogLoggerTaskListener, LatestValueListener, SamplePersistingListener)))
        }
    }
  }

}

