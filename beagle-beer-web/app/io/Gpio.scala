package io


import org.slf4j.LoggerFactory
import io.util.StringExtras.StringStream
import java.io.File

/**
 * Basic GPIO controls for a beagle board.  Should only be access by a single thread
 * or subjected to external synchronisation.
 */
class Gpio(gpio: String) extends AutoCloseable {

  // Dev note: Strangely using the scala Path class to write to the io devices
  // doesn't work so using output streams via the StringStream class


  val log = LoggerFactory.getLogger(this.getClass)

  /** Write the GPIO number to export open it. */
  private val export = "/sys/class/gpio/export"

  /** Write the GPIO number to unexport close it. */
  private val unexport = "/sys/class/gpio/unexport"

  private val path = "/sys/class/gpio/gpio" + gpio + "/direction"

  private var opened = false

  private var state = false

  /** Open the GPIO */
  this.close   // close it first to steal it from other processes
  this.open

  protected def open = {
    log.debug("opening GPIO " + gpio)
    opened = true
    export.streamWrite(gpio)
  }

  override def close = {
    if (new File(unexport + "/" + gpio).exists())  {
      unexport.streamWrite(gpio)
    }
    opened = false
  }


  def on = {
    require(opened, "GPIO " + gpio + " must be opened before it can be written to")
    path.streamWrite("high")
    state = true
  }

  def off = {
    require(opened, "GPIO " + gpio + " must be opened before it can be written to")
    path.streamWrite("low")
    state = true
  }

  def toggle = {
    if (isOn) off else on
  }

  def isOn() = state
}

object GpioRegistry {

  val hot = new Gpio("23")
  val cold = new Gpio("26")

}