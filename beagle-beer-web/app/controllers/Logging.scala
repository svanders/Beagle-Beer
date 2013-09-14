package controllers

import play.api.mvc.{Action, Controller}
import org.slf4j.LoggerFactory
import play.api.db.slick.DB
import models.{SamplesDb, LogsDb, Sample, DS1820sDb}

import controllers.util.FlashScope
import FlashScope.emptyFlash
import task.{LoggerTaskManager, LatestValueListener, DebugLogLoggerTaskListener, LoggerTask}
import play.api.Play.current
import play.api.libs.json.Json

/**
 * Web controller for all Logging actions.
 * Starting/Stopping the logger
 * Getting the current logging/not logging status of the logger
 * Getting the latest value
 * Getting a list of all saved logs
 * Getting the contents of a log in several formats
 */
object Logging extends Controller {

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
    val latest: List[Sample] = if (LoggerTaskManager.isInitialised) LatestValueListener.latest // simply get the latest value from loggerTask
    else {
      // no values to return
      List()
    }
    println("Latest reading=" + Json.toJson(latest))
    Ok(Json.toJson(latest))
  }


  def isRunning = Action {
    Ok(LoggerTaskManager.isRunning.toString)
  }

  def logHistory = Action {
    DB.withSession {
      implicit session =>
        Ok(views.html.logHistory(LogsDb.all))
    }
  }

  def logData(logId: Int) = Action {
    DB.withSession {
      implicit session =>
        val result = SamplesDb.find(logId)
        Ok(views.html.logData(result._1, result._2))
    }
  }

  def logDataJson(logId: Int) = Action {
    import models.SamplesJson.sampleWrites
    DB.withSession {
      implicit session =>
        val result = SamplesDb.find(logId)
        Ok(Json.toJson(result._2))
    }
  }

  def logPlot(logId: Int) = Action {
    Ok(views.html.logPlot(logId))
  }
}

