package controllers

import play.api.mvc._
import io.Gpio
import org.slf4j.LoggerFactory
import io.GpioRegistry.{cold, hot}
/**
 * Turns the elements on and off.
 */
object Element extends Controller {

  val dLog = LoggerFactory.getLogger("Element")

  def switch(element: String, command: Boolean) = Action {

    dLog.debug("switch " + element + " " + command)
    val gpio: Gpio = element match {
      case "hot" => hot
      case "cold" => cold
      case x: String => throw new IllegalArgumentException("unknown element " + x)
    }

      if (command) gpio.on
      else gpio.off

      Ok("")
  }

}


