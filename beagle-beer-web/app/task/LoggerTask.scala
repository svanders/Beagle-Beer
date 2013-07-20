package task

import play.api.db.slick.DB
import models.{LogsDb, Sample, Log, DS1820}
import java.util.Date
import org.slf4j.LoggerFactory
import io.DS1820BulkReader
import play.api.Play.current


import scala.annotation.tailrec
import play.api.libs.json.Json

/**
 * When started, records a new Log in the database, records Measurements every 10 seconds,
 * exits when <code>on</code> is set to false.
 */
class LoggerTask(logIntervalMillis: Int, devices: List[DS1820], listeners: List[LoggerTaskListener]) extends Runnable {
  require(devices != null)
  require(listeners != null)
  require(logIntervalMillis >= 1000)

  val log = LoggerFactory.getLogger(this.getClass)

  private var on = false

  def run: Unit = {

    on = true

    val logRecord =  DB.withTransaction {
      implicit session =>
        LogsDb.insert(Log(None, new Date, None))
    }

    log.debug("Started temperature log " + logRecord + " using " + devices.size + " sensors")

    while (on) {
      val now = new Date
      val reads = DS1820BulkReader.readAll(devices.map(d => d.path))
      log.debug("read " + reads.size + " devices")

      // convert the reads into samples
      val samples = for {read <- reads} yield createSample(read, logRecord, now)

      // send each sample through each LoggerTaskListener
      for {
        listener <- listeners
      } listener.receiveRead(samples)

      sleepWithFastWake(logIntervalMillis, 200)
    }

    val endedLogRecord = DB.withTransaction {
      implicit session =>
        LogsDb.update(logRecord.copy(end = Some(new Date)))
    }
    log.debug("Ended temperature log " + endedLogRecord)
  }

  def stop = {
    log.debug("stopping")
    on = false
  }

  def isRunning = on

  private def createSample(read: (String, Float), logRecord: Log, now: Date): Sample = {
    val device = devices.find(d => d.path == read._1).get // should always fins the correct device
    val sample = new Sample(None, logRecord.id.get, device.id.get, now, read._2)
    return sample
  }

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

