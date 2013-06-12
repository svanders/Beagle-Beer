package devices

import scala.io.Source
import java.io.File
import scala.util.matching.Regex

import scala.concurrent._
import ExecutionContext.Implicits.global

/**
 * Reads the temperature from a DS1820 1-Wire device.  Assumes the actual control
 * of the device is provided via the OS, so really this is a specialised
 * file reader.
 * @param deviceLocation The filesystem location of the DS1820 output.
 */
class DS1820Reader(deviceLocation: String) {
   require(deviceLocation != null)

  def read: Float = {
    val lines = Source.fromFile(deviceLocation).getLines()
    val crcLine = lines.next
    val temperatureLine = lines.next
    if (! crcLine.endsWith("YES")) {
      throw new RuntimeException("CRC error on DS1820 read")
    }
    val tempChars = temperatureLine.substring(temperatureLine.indexOf("t=") + 2)
    tempChars.toFloat / 1000
  }

  def readAsync: Future[Float] = {
    future { read }
  }

}

/**
 * Scans a directory (non-recursively) and returns a List of locations of DS1820 devices.
 * @param location The directory to scan
 */
class DS1820Scanner(location: String) {
  require(location != null)

  def readAll(): Map[String, Float] = {
    val readings = for {
      (sensor, path) <- scan
    }  yield (sensor, new DS1820Reader(path).read)
    readings.toMap
  }

  def readAllAsync: Map[String, Future[Float]] = {
    val readings = for {
      (sensor, path) <- scan
    }  yield (sensor, new DS1820Reader(path).readAsync)
    readings.toMap
  }


  def scan: Map[String, String] = {
    val dir: File = new File(location)
    require(dir.isDirectory)
    val devicePaths = for {
      file <- dir.listFiles
      if isDS1820(file.getName)
    } yield (file.getName, file.getAbsolutePath + "/w1_slave")

    devicePaths.toMap
  }


  def extractDeviceId(path: String): Option[String] = {
    val pattern = new Regex("28-\\w{12}")
    pattern.findFirstIn(path)
  }

  def isDS1820(name: String): Boolean = {
    extractDeviceId(name) match {
      case Some(_) => true
      case None => false
    }
  }


}
