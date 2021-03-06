package models

import scala.slick.driver.H2Driver.simple._
import scala.reflect.runtime.{ universe => ru }
import java.util.Date
import models.typemapping.CustomSlickTypes.dateTypeMapper
import scala.slick.lifted.ColumnOption.DBType

/**
 * Created with IntelliJ IDEA.
 * User: simonvandersluis
 * Date: 14/07/13
 * Time: 3:51 PM
 * To change this template use File | Settings | File Templates.
 */
case class Log(id: Option[Int], name: String, targetTemperature: Float, start: Date, end: Option[Date]) extends Persistent {

}



object LogsDb extends Table[Log]("Log") with InsertOrUpdate[Log] {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def targetTemperature = column[Float]("targetTemperature", O.NotNull)
  def start = column[Date]("start", O.NotNull)
  def end = column[Date]("end", O.Nullable, DBType("TimeStamp"))

  def * = id.? ~ name ~ targetTemperature ~ start ~ end.? <> (Log, Log.unapply _)



  override def update(log: Log)(implicit session: Session): Log = {
    require(log != null)
    val query = for ( l <- LogsDb if l.id === log.id) yield l
    query.update(log)

    // simply return the Log the update was for
    log
  }

  override def insert(log: Log)(implicit session: Session): Log = {
    val id = LogsDb.insertInvoker returning LogsDb.id  insert(log)
    log.copy(id = Some(id))
  }

  def byId(id: Int)(implicit session: Session): Log = {
    val query = for (log <- LogsDb if log.id === id) yield log
    query.first
  }

  def all(implicit session: Session): List[Log] = {
    Query(LogsDb).sortBy(_.start).list.reverse
  }

}