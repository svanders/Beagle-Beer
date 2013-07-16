package io.util

import java.io.{IOException, FileOutputStream}
import org.slf4j.LoggerFactory
import java.text.{SimpleDateFormat, DateFormat}
import java.util.Date


object StringFileUtil {

  val dateFormat = "yyyy-MM-dd kk:mm:ss"

  /**
   * Provides the ability to write to a file, with only the file name as a String.
   * Probably a poor use of Scalas implicit feature, but I had to try it out.
   */
  implicit class StringStream(val s: String) {

    val log = LoggerFactory.getLogger(this.getClass)

    def streamWrite(data: String) = {
      val out = new FileOutputStream(s)
      try {
        out.write(data.getBytes)
      } catch {
        case ioe: IOException => log.error("Error writing to " + s, ioe)
      } finally {
        out.close()
      }
    }

  }

  /**
   * Provides the ability to turn Strings into dates when in the format
   * yyyy-MM-kk hh:mm:ss, or with a custom format.
   * @param s
   */
  implicit class DateString(val s: String) {

    def toDate: Date = toDate(dateFormat)

    def toDate(fmt: String): Date = {
      val formatter: DateFormat = new SimpleDateFormat(fmt)
      formatter.parse(s)
    }
  }

}