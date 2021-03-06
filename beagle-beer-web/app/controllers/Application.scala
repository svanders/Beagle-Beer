package controllers

import play.api.mvc._
import java.lang.management.ManagementFactory
import play.api.db.slick.DB
import play.api.Play.current
import controllers.util.FlashScope
import FlashScope.emptyFlash
import models.{SamplesDb, DS1820sDb, LogsDb}
import play.api.libs.json.Json
import task.LoggerTaskManager
import play.api.data.Form
import play.api.data.Forms._

object Application extends Controller {


  def index = Action {

    DB.withSession {
      implicit session =>
        Ok(views.html.index(Logging.startForm, DS1820sDb.filterByEnabled(true)))
    }
  }



  def status = Action {
    DB.withSession {
      implicit session =>

        val runtime = ManagementFactory.getRuntimeMXBean()
        val threads = ManagementFactory.getThreadMXBean()
        val os = ManagementFactory.getOperatingSystemMXBean()
        val classes = ManagementFactory.getClassLoadingMXBean()
        val compile = ManagementFactory.getCompilationMXBean()
        val memory = ManagementFactory.getMemoryMXBean()

        val vmStats = List(
          ("Operating System:", os.getName + os.getVersion),
          ("Architecture:", os.getArch),
          ("Number of processors:", os.getAvailableProcessors),
          ("Virtual Machine:", runtime.getVmName),
          ("Vendor:", runtime.getVmVendor),
          ("Version", runtime.getVmVersion),
          ("Up time:", runtime.getUptime().toString() + "ms"),
          ("Current heap size:", memory.getHeapMemoryUsage().getUsed),
          ("Committed memory:", memory.getHeapMemoryUsage.getCommitted),
          ("Maximum heap size:", memory.getHeapMemoryUsage().getMax),
          ("JIT compiler:", compile.getName),
          ("Total compile time:", compile.getTotalCompilationTime().toString() + "ms"),
          ("Live threads:", threads.getThreadCount),
          ("Daemon Threads:", threads.getDaemonThreadCount),
          ("Total threads started:", threads.getTotalStartedThreadCount),
          ("Current classes loaded:", classes.getLoadedClassCount),
          ("Peak:", threads.getPeakThreadCount),
          ("Total classes loaded:", classes.getTotalLoadedClassCount),
          ("Total classes unloaded:", classes.getUnloadedClassCount),
          ("Pending finalisation:", memory.getObjectPendingFinalizationCount),
          ("Database URL", session.conn.getMetaData.getURL),
          ("Connection class", session.conn.getClass.getName)
        )
        Ok(views.html.status(vmStats))
    }
  }

}