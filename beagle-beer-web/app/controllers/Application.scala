package controllers

import play.api._
import play.api.mvc._
import java.lang.management.ManagementFactory

object Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def status = Action {

    val runtime = ManagementFactory.getRuntimeMXBean()
    val threads = ManagementFactory.getThreadMXBean()
    val os = ManagementFactory.getOperatingSystemMXBean()
    val classes = ManagementFactory.getClassLoadingMXBean()
    val compile = ManagementFactory.getCompilationMXBean()
    val memory = ManagementFactory.getMemoryMXBean()

    val vmStats = List(
      ("Virtual Machine:", runtime.getVmName),
      ("Vendor:", runtime.getVmVendor),
      ("version", runtime.getVmVersion),
      ("Up time:", runtime.getUptime().toString() + "ms"),
      ("JIT compiler:", compile.getName),
      ("Total compile time:", compile.getTotalCompilationTime().toString() + "ms"),
      ("Live threads:", threads.getThreadCount),
      ("Current classes loaded:", classes.getLoadedClassCount),
      ("Peak:", threads.getPeakThreadCount),
      ("Total classes loaded:", classes.getTotalLoadedClassCount),
      ("Daemon Threads:", threads.getDaemonThreadCount),
      ("Total classes unloaded:", classes.getUnloadedClassCount),
      ("Total threads started:", threads.getTotalStartedThreadCount),
      ("Current heap size:", memory.getHeapMemoryUsage().getUsed),
      ("Committed memory:", memory.getHeapMemoryUsage.getCommitted),
      ("Maximum heap size:",memory.getHeapMemoryUsage().getMax),
      ("Pending finalisation:", memory.getObjectPendingFinalizationCount),
      ("Operating System:", os.getName +  os.getVersion),
//      ("Total physical memory:", os.getTotalPhysicalMemorySize),
      ("Architecture:", os.getArch),
//      ("Free physical memory:", os.getFreePhysicalMemorySize),
      ("Number of processors:", os.getAvailableProcessors)
//      ("Total swap space:", os.getTotalSwapSpaceSize)),
//      ("Committed virtual memory:", os.getCommittedVirtualMemorySize),
//       ("Free swap space:", os.getFreeSwapSpaceSize),
//        ("Process CPU time:", os.get),
    )

    Ok(views.html.status(vmStats))
  }
}