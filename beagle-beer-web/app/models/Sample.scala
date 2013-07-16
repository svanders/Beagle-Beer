package models

import java.util.Date
import scala.slick.driver.H2Driver.simple._
import scala.reflect.runtime.{ universe => ru }

/**
 * Represents a temperature reading taken at a point in time.
 */
case class Sample(log: Log, device: DS1820, date: Date, value: Float) {

}


