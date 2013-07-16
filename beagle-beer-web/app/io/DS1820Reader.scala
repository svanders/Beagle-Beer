package io

import scala.io.Source
import java.io.File
import scala.util.matching.Regex

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Reads the temperature from a DS1820 1-Wire device.  Assumes the actual control
 * of the device is provided via the OS, so really this is a specialised
 * file reader.
 * @param deviceLocation The filesystem location of the DS1820 output.
 */
class DS1820Reader(deviceLocation: String) {
   require(deviceLocation != null)
   require(DS1820NameParser.isDS1820(deviceLocation))

  val deviceId: String = DS1820NameParser.extractDeviceId(deviceLocation)

  def read: (String, Float) = {
    val lines = Source.fromFile(deviceLocation).getLines()
    val crcLine = lines.next
    val temperatureLine = lines.next
    if (! crcLine.endsWith("YES")) {
      throw new RuntimeException("CRC error on DS1820 read")
    }
    val tempChars = temperatureLine.substring(temperatureLine.indexOf("t=") + 2)
    (deviceLocation, tempChars.toFloat / 1000)
  }

  def readAsync: Future[(String, Float)] = {
    future { blocking { read } }
  }

}

/**
 * Scans a directory (non-recursively) and returns a List of locations of DS1820 devices.
 * @param location The directory to scan
 */
class DS1820Scanner(location: String) {
  require(location != null)

  def readAll: List[(String, Float)] = {
    DS1820BulkReader.readAll(scan)
  }

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

object DS1820BulkReader {

  /**
   * Reads all the DS1820 specified by paths in parallel.  It will suspend the current thread
   * while waiting for the reads to complete, usually ~ 700 ms.
   * @param paths
   * @return
   */
  def readAll(paths: List[String]): List[(String, Float)] = {
    val readCount = paths.size
    if (readCount == 0) {
      Nil
    } else {
      val futureReads = Future.sequence(readAllAsync(paths))
      Await.result(futureReads, readCount seconds)
    }
  }

  private def readAllAsync(paths: List[String]): List[Future[(String, Float)]] = {
    paths.map {
      path =>   new DS1820Reader(path).readAsync
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
