package io

import scala.io.Source
import java.io.File
import scala.util.matching.Regex

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import org.slf4j.LoggerFactory

/**
 * Reads the temperature from a DS1820 1-Wire device.  Assumes the actual control
 * of the device is provided via the OS, so really this is a specialised
 * file reader.
 * @param deviceLocation The filesystem location of the DS1820 output.
 */
class DS1820Reader(deviceLocation: String) {
  require(deviceLocation != null)
  require(DS1820NameParser.isDS1820(deviceLocation))

  val log = LoggerFactory.getLogger(this.getClass)
  val deviceId: String = DS1820NameParser.extractDeviceId(deviceLocation)

  /**
   * Reads this device using the current thread.  Reading a device takes
   * ~700ms, so it's recommended that readAsync is used in preference to
   * this method. 
   */
  def read: (String, Option[Float]) = {
    try {
      val lines = Source.fromFile(deviceLocation).getLines()
      val crcLine = lines.next()
      val temperatureLine = lines.next()
      if (!crcLine.endsWith("YES")) {
        throw new RuntimeException("CRC error on DS1820 read")
      }
      val tempChars = temperatureLine.substring(temperatureLine.indexOf("t=") + 2)
      (deviceLocation, Some(tempChars.toFloat / 1000))
    } catch {
      case e: Exception =>
        log.error("Unable to read DS1820 - " + deviceLocation, e)
        (deviceLocation, None)
    }
  }

  /**
   * Reads this device, returning a Future containing the result.  The Future
   * should take ~700ms to complete.
   */
  def readAsync: Future[(String, Option[Float])] = {
    future {
      blocking {
        read
      }
    }
  }

}

/**
 * Scans a directory (non-recursively) and returns a List of locations of DS1820 devices.
 * @param location The directory to scan
 */
class DS1820Scanner(location: String) {
  require(location != null)

  def readAll: List[(String, Option[Float])] = {
    DS1820BulkReader.readAll(scan)
  }

  /**
   * Scans the location this was constructed with for DS1820 probes, returning a List
   * of the probe locations.
   */
  def scan: List[String] = {
    val dir: File = new File(location)
    require(dir.isDirectory)
    val devicePaths = for {
      file <- dir.listFiles
      if DS1820NameParser.isDS1820(file.getName)
    } yield file.getAbsolutePath + "/w1_slave"

    devicePaths.toList.sorted
  }

}

/**
 * Use to read many DS1820 probes in parallel.
 */
object DS1820BulkReader {

  /**
   * Reads all the DS1820 specified by paths in parallel.  It will suspend the current thread
   * while waiting for the reads to complete, usually ~ 700 ms.
   * @param paths The locations of the sensors.
   * @return
   */
  def readAll(paths: List[String]): List[(String, Option[Float])] = {
    val readCount = paths.size
    if (readCount == 0) {
      Nil
    } else {
      // read all of the probes in parallel, resulting in a single Future
      // that will complete with all of it's Futures are complete.
      val futureReads = Future.sequence(readAllAsync(paths))

      // wait for the future containing all the reads in ready.
      Await.result(futureReads, readCount seconds)
    }
  }

  private def readAllAsync(paths: List[String]): List[Future[(String, Option[Float])]] = {
    paths.map {
      path => new DS1820Reader(path).readAsync
    }
  }
}

object DS1820NameParser {

  def extractDeviceId(path: String): String = {
    extractDeviceIdOption(path) match {
      case Some(id) => id
      case None => throw new RuntimeException("Path '" + path + "' does not represent a DS1820")
    }
  }

  def isDS1820(name: String): Boolean = {
    extractDeviceIdOption(name) match {
      case Some(_) => true
      case None => false
    }
  }

  def extractDeviceIdOption(path: String): Option[String] = {
    val pattern = new Regex("28-\\w{12}")
    pattern.findFirstIn(path)
  }
}
