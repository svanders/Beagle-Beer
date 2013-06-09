package devices

import scala.io.Source
import java.io.File
import scala.util.matching.Regex

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
    val tempChars = temperatureLine.takeRight(5)
    tempChars.toFloat / 1000
  }

}

/**
 * Scans a directory (non-recursively) and returns a List of locations of DS1820 devices.
 * @param location The directory to scan
 */
class DS1820Scanner(location: String) {
  require(location != null)

  def readAll(): Map[String, Float] = {
    val pattern = new Regex("28-\\w{12}")
    val readings = for {
      sensor <- scan
      name <- pattern.findFirstIn(sensor)
    }  yield (name, new DS1820Reader(sensor).read)
    readings.toMap
  }


  def scan: List[String] = {
    val dir: File = new File(location)
    require(dir.isDirectory)
    val devices = (dir.listFiles().map(f => f.getAbsolutePath)).filter(isDS1820).toList
    devices.map(s => s + "/w1-slave")
  }

  def isDS1820(name: String): Boolean = {
     val pattern = new Regex("28-\\w{12}")
    pattern.findFirstIn(name) match {
      case Some(_) => true
      case None => false
    }
  }


}
