package task

import models.{Sample, Log, DS1820}
import java.util.Date
import org.slf4j.LoggerFactory
import io.DS1820BulkReader

import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.annotation.tailrec

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

    val logRecord = new Log(None, new Date, None)
    log.debug("Started temperature log " + logRecord + " using " + devices.size + " sensors")
    // TODO save logRecord
    while (on) {
      val now = new Date
      val reads = DS1820BulkReader.readAll(devices.map(d => d.path))
      log.debug("read " + reads.size + " devices")

      // convert the reads into samples
      val samples = for {read <- reads} yield createSample(read, logRecord, now)

      // send each sample through each LoggerTaskListener
      for {
        listener <- listeners
        sample <- samples
      } listener.receiveRead(sample)

      sleepWithFastWake(logIntervalMillis, 200)
    }

    log.debug("done")
    val endedLogRecord = new Log(logRecord.id, logRecord.start, Some(new Date))
    log.debug("Ended temperature log " + endedLogRecord)
    // TODO update logRecord in DB


  }

  def stop = {
    log.debug("stopping")
    on = false
  }

  def isRunning = on

  private def createSample(read: (String, Float), logRecord: Log, now: Date): Sample = {
    val device = devices.find(d => d.path == read._1).get // should always fins the correct device
    val sample = new Sample(logRecord, device, now, read._2)
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