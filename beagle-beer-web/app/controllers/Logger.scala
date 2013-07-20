package controllers

import play.api.mvc.{Action, Controller}
import org.slf4j.LoggerFactory
import play.api.db.slick.DB
import models.{Sample, DS1820sDb}

import task.{LatestValueListener, DebugLogLoggerTaskListener, LoggerTask}
import play.api.Play.current
import play.api.libs.json.Json

/**
 * Created with IntelliJ IDEA.
 * User: simonvandersluis
 * Date: 14/07/13
 * Time: 5:03 PM
 * To change this template use File | Settings | File Templates.
 */
object Logger extends Controller {

  val log = LoggerFactory.getLogger(this.getClass)

  val loggerTask = {
    DB.withSession {
      implicit session =>
        val devices = DS1820sDb.all
        if (devices isEmpty) {
          throw new RuntimeException("No DS1820s found, please configure device first")
        }
        new LoggerTask(10000, devices, List(DebugLogLoggerTaskListener, LatestValueListener))
    }
  }


  def start = Action {
    if (loggerTask isRunning) Ok("Already Running")
    else {
      new Thread(loggerTask).start
      Ok("Started")
    }
  }

  def stop = Action {
    if (loggerTask isRunning) {
      loggerTask.stop
      LatestValueListener.clear
      Ok("Stopped")
    } else {
      Ok("Not Running")
    }
  }

  def latest = Action {
    import models.SamplesJson.sampleWrites
    val latest: List[Sample] = if (loggerTask.isRunning) LatestValueListener.latest  // simply get the latest value from loggerTask
    else {
       // need to read the sensors
       List()
    }
    log.debug("Latest reading=" + Json.toJson(latest))
    Ok(Json.toJson(latest))
  }


  def isRunning = Action {
    Ok(loggerTask.isRunning.toString)
  }
}
