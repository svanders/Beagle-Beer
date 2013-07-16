package models

import scala.slick.driver.H2Driver.simple._
import scala.reflect.runtime.{ universe => ru }
import java.util.Date

/**
 * Created with IntelliJ IDEA.
 * User: simonvandersluis
 * Date: 14/07/13
 * Time: 3:51 PM
 * To change this template use File | Settings | File Templates.
 */
case class Log(id: Option[Int], start: Date, end: Option[Date]) extends Persistent {



}


//object Logs extends Table[Log]("Log") {
//
//  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
//  def start = column[Date]("start", O.NotNull)
//  def end = column[Date]("name")
//
//  def * = id.? ~ start ~ end.? <> (Log.apply _, Log.unapply _)
//
//
//
//}