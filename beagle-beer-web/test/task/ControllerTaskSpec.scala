package task

import org.specs2.mutable._
import io.Gpio
import models.{Log, Sample, DS1820}
import java.util.Date

/**
 * Specifies and tests behaviour for the component that control the heater and cooler
 * based on the incoming samples.
 *
 * @author simonvandersluis
 */
class ControllerTaskSpec extends Specification {

  val hot = new MockGpio("hot")
  val cold = new MockGpio("cold")
  val masterProbe = DS1820(Some(1), "path", "master", true, true)
  val otherProbe = DS1820(Some(2), "path", "other", true, false)
  val log = Log(Some(1), "test", 20, new Date(), None)
  val masterSample: Sample = Sample(None, log.id.get, masterProbe.id.get, new Date, Some(20))
  val otherSample = Sample(None, log.id.get, otherProbe.id.get, new Date, Some(23))

  val control = new GpioControllingListener(masterProbe.id.get, hot, cold)

  // we have a target of 20

  "The controller" should {
    "not control when idling" in {
      hot.off
      cold.on
      val lowMasterSample = masterSample.copy(value = Some(0))
      // if we weren't idling a master sample of 0 would turn the heater on
      control.receiveRead(None, List(lowMasterSample, otherSample))
      hot.isOn must beFalse
      cold.isOn must beTrue
    }

    "control when not idling" in {
      hot.off
      cold.off
      val lowMasterSample = masterSample.copy(value = Some(0))
      // not idling a master sample of 0 will turn the heater on
      control.receiveRead(Some(log), List(lowMasterSample, otherSample))
      hot.isOn must beTrue
      cold.isOn must beFalse
    }

    "not control when not idling and in range (slightly cool)" in {
      hot.off
      cold.on
      val lowMasterSample = masterSample.copy(value = Some(19.0f))
      control.receiveRead(Some(log), List(lowMasterSample, otherSample))
      hot.isOn must beFalse
      cold.isOn must beFalse
    }

    "not control when not idling and in range (slightly warm)" in {
      hot.on
      cold.off
      val highMasterSample = masterSample.copy(value = Some(21.0f))
      control.receiveRead(Some(log), List(highMasterSample, otherSample))
      hot.isOn must beFalse
      cold.isOn must beFalse
    }

    "turn on heater when not idling and temperature is too cool" in {
      hot.off
      cold.on
      val lowMasterSample = masterSample.copy(value = Some(18.9f))
      control.receiveRead(Some(log), List(lowMasterSample, otherSample))
      hot.isOn must beTrue
      cold.isOn must beFalse
    }

    "turn on cooler on when not idling and temperature is too hot" in {
      hot.on
      cold.off
      val lowMasterSample = masterSample.copy(value = Some(21.1f))
      control.receiveRead(Some(log), List(lowMasterSample, otherSample))
      hot.isOn must beFalse
      cold.isOn must beTrue
    }

    "turn off heater and cooler when temperature probes are malfunctioning" in {
      hot.on
      cold.on
      val lowMasterSample = masterSample.copy(value = None)
      control.receiveRead(Some(log), List(lowMasterSample, otherSample))
      hot.isOn must beFalse
      cold.isOn must beFalse
    }

  }


}


class MockGpio(gpio: String) extends Gpio(gpio) {

  var state = false;

  override protected def open = {}
  override def close = {}

  override def on {
    state = true
  }

  override def off {
    state = false
  }

  override def toggle {
    state = !state
  }

  override def isOn(): Boolean = state
}
