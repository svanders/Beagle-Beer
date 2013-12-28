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

  "When idling The controller" should {
    "not control" in {
      hot.off
      cold.on
      val lowMasterSample = masterSample.copy(value = Some(0))
      // if we weren't idling a master sample of 0 would turn the heater on
      control.receiveRead(None, List(lowMasterSample, otherSample))
      hot.isOn must beFalse
      cold.isOn must beTrue
    }
  }

  "When not not idling and aiming for 20 +/- 1" should {
    
    "turn on heater below 19" in {
      hot.off
      cold.off
      val lowMasterSample = masterSample.copy(value = Some(18.9f))
      control.receiveRead(Some(log), List(lowMasterSample, otherSample))
      hot.isOn must beTrue
      cold.isOn must beFalse
    }

    "leave heater on at 19.9 when already heating" in {
      hot.on
      cold.off
      val lowMasterSample = masterSample.copy(value = Some(19.9f))
      control.receiveRead(Some(log), List(lowMasterSample, otherSample))
      hot.isOn must beTrue
      cold.isOn must beFalse
    }
    
    "turn heater off at 20 when heating" in {
      hot.on
      cold.off
      val lowMasterSample =masterSample.copy(value = Some(20f))
      control.receiveRead(Some(log), List(lowMasterSample, otherSample))
      hot.isOn must beFalse
      cold.isOn must beFalse
    } 
    
    "leave cooler off at 20.9 when not cooling" in {
      hot.off
      cold.off
      val lowMasterSample = masterSample.copy(value = Some(20.9f))
      control.receiveRead(Some(log), List(lowMasterSample, otherSample))
      hot.isOn must beFalse
      cold.isOn must beFalse
    }
    
    "turn cooler on at 21.1" in {
      hot.off
      cold.off
      val lowMasterSample = masterSample.copy(value = Some(21.1f))
      control.receiveRead(Some(log), List(lowMasterSample, otherSample))
      hot.isOn must beFalse
      cold.isOn must beTrue
    }
    
    "leave cooler on at 20.5 when not cooling" in {
      hot.off
      cold.on
      val lowMasterSample = masterSample.copy(value = Some(20.5f))
      control.receiveRead(Some(log), List(lowMasterSample, otherSample))
      hot.isOn must beFalse
      cold.isOn must beTrue
    }
    
    "turn cooler off at 20 when cooling" in  {
      hot.off
      cold.on
      val lowMasterSample = masterSample.copy(value = Some(20f))
      control.receiveRead(Some(log), List(lowMasterSample, otherSample))
      hot.isOn must beFalse
      cold.isOn must beFalse
    }
    
    "leave heater off at 19.5 when not heating" in {
      hot.off
      cold.off
      val lowMasterSample = masterSample.copy(value = Some(19.5f))
      control.receiveRead(Some(log), List(lowMasterSample, otherSample))
      hot.isOn must beFalse
      cold.isOn must beFalse
    }
  
  }
  

  "When probes are malfunctioning" should { 
    "turn off heater and cooler" in {
      hot.on
      cold.on
      val lowMasterSample = masterSample.copy(value = None)
      control.receiveRead(Some(log), List(lowMasterSample, otherSample))
      hot.isOn must beFalse
      cold.isOn must beFalse
    }
  }
  
  "When in an unexpected state" should {
  
     "turn heater and cooler off at 20 when cooling and heating" in  {
      hot.on
      cold.on
      val lowMasterSample = masterSample.copy(value = Some(20f))
      control.receiveRead(Some(log), List(lowMasterSample, otherSample))
      hot.isOn must beFalse
      cold.isOn must beFalse
    }
  }
  
}

/**
 * A mock Gpio implementation for use in tests.
 */
class MockGpio(gpio: String) extends Gpio(gpio) {

  var state = false;

  override protected def open: Unit = {}
  override def close: Unit = {}

  override def on: Unit = {
    state = true
  }

  override def off: Unit = {
    state = false
  }

  override def toggle: Unit = {
    state = !state
  }

  override def isOn(): Boolean = state
  
 // override def isOff(): Boolean = !state
}
