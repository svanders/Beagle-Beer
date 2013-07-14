package io.util

import java.io.{IOException, FileOutputStream}
import org.slf4j.LoggerFactory

/**
 * Provides the ability to write to a file, with only the file name as a String.
 * Probably a poor use of Scalas implicit feature, but I had to try it out.
 */
object StringFileUtil {

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

}