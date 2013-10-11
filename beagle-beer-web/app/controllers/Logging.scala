package controllers

import play.api.mvc.{Action, Controller}
import org.slf4j.LoggerFactory
import play.api.db.slick.DB
import models._

import controllers.util.{FormExtension, FlashScope}

import task.{LoggerTaskManager, LatestValueListener, DebugLogLoggerTaskListener, LoggerTask}
import play.api.Play.current
import play.api.libs.json.Json
import play.api.data.Form
import play.api.data.Forms._
import models.Sample

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
    implicit request =>
      DB.withSession {
        implicit session =>
          startForm.bindFromRequest.fold(
            formWithErrors => BadRequest(views.html.index(formWithErrors, DS1820sDb.filterByEnabled(true))),
            values => {
              LoggerTaskManager.start(values._1, values._2, 10000)
              Redirect(routes.Application.index).flashing(FlashScope.success -> "Logging started")
            })
      }
  }

  val startForm = Form(
    tuple(
      "Name / Label" -> nonEmptyText,
      "Target Temperature" -> number(min = 0, max = 100)
    ) verifying("Device is not setup ", form => LoggerTaskManager.isInitialised)
      verifying("Log already in progress", form => !(LoggerTaskManager.isRunning))
  )

  def stop() = Action {
    implicit request =>
      DB.withTransaction {
        implicit session =>
          if (LoggerTaskManager.isRunning) {
            LoggerTaskManager.stop
            Redirect(routes.Application.index).flashing(FlashScope.success -> "Logging stopped")
          } else {
            Redirect(routes.Application.index).flashing(FlashScope.success -> "Logging not running")
          }

      }
  }



  def latest = Action {
    import models.SamplesJson.sampleWrites
    // simply get the latest value from loggerTask
    val latest: List[Sample] = if (LoggerTaskManager.isInitialised) LatestValueListener.latest
    else {
      // no values to return
      List()
    }
    Ok(Json.toJson(latest))
  }


  def isRunning = Action {
    Ok(LoggerTaskManager.isRunning.toString)
  }

  def logHistory = Action {
    import FlashScope.emptyFlash
    DB.withSession {
      implicit session =>
        Ok(views.html.logHistory(LogsDb.all))
    }
  }

  def logData(logId: Int) = Action {
    import FlashScope.emptyFlash
    DB.withSession {
      implicit session =>
        val log = LogsDb.byId(logId)
        val result = SamplesDb.find(logId)
        Ok(views.html.logData(log, result._1, result._2))
    }
  }

  def logDataJson(logId: Int) = Action {
    import FlashScope.emptyFlash
    import models.SamplesJson.sampleWrites
    DB.withSession {
      implicit session =>
        val result = SamplesDb.find(logId)
        Ok(Json.toJson(result._2))
    }
  }

  def logPlot(logId: Int) = Action {
    import FlashScope.emptyFlash
    Ok(views.html.logPlot(logId))
  }
}

