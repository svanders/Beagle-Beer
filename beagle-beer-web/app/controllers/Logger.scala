package controllers

import play.api.mvc.{Action, Controller}
import org.slf4j.LoggerFactory
import play.api.db.slick.DB
import models.{Sample, DS1820sDb}

import task.{LoggerTaskManager, LatestValueListener, DebugLogLoggerTaskListener, LoggerTask}
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

  def start = Action {
    if (LoggerTaskManager isRunning) Ok("Already Running")
    else {
      LoggerTaskManager.start
      Ok("Started")
    }
  }

  def stop = Action {
    if (LoggerTaskManager isRunning) {
      LoggerTaskManager.stop
      LatestValueListener.clear
      Ok("Stopped")
    } else {
      Ok("Not Running")
    }
  }

  def latest = Action {
    import models.SamplesJson.sampleWrites
    val latest: List[Sample] = if (LoggerTaskManager.isRunning) LatestValueListener.latest // simply get the latest value from loggerTask
    else {
      // no values to return
      List()
    }
    //log.debug("Latest reading=" + Json.toJson(latest))
    Ok(Json.toJson(latest))
  }


  def isRunning = Action {
    Ok(LoggerTaskManager.isRunning.toString)
  }
}

