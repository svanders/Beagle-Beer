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

object LoggerTaskManager {

  val log = LoggerFactory.getLogger("LoggerTasks")

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

  def destry = {
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
          log.warn("No DS1820s are configured, unable to create Logger Task")
          None
        } else {
          Some(new LoggerTask(10000, devices, List(DebugLogLoggerTaskListener, LatestValueListener)))
        }
    }
  }


}
