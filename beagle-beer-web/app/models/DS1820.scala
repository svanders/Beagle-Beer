package models


import scala.slick.driver.H2Driver.simple._
import scala.reflect.runtime.{ universe => ru }


/**
 * The configuration of a DS1820 device in on the beagle bone. Currently consists of:
 * The devices unique id
 * the full path to the device on a linux device (appears as a file) e.g.
 * /sys/devices/w1_bus_master1/28-000003cef061/w1_slave,
 * a label e.g. ambient
 * and a boolean flag indicating if the device is enabled.
 */
case class DS1820(id: Option[Int], path:String, name: String, enabled: Boolean, master: Boolean) extends Persistent {


}

object DS1820sDb extends Table[DS1820]("DS1820") with InsertOrUpdate[DS1820] {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def path = column[String]("path", O.NotNull)
  def name = column[String]("name", O.NotNull)
  def enabled = column[Boolean]("enabled", O.NotNull)
  def master = column[Boolean]("master", O.NotNull)

  def * = id.? ~ path ~ name ~ enabled ~ master <> (DS1820, DS1820.unapply _)
//  def forInsert =  path ~ name ~ enabled ~ master <> ({t => DS1820(None, t._1, t._2, t._3, t._4)},
//                                                      {d: DS1820 => Some((d.path, d.name, d.enabled, d.master))})

  def table = DS1820sDb

  // Use the implicit threadLocalSession
  //import Database.threadLocalSession  --- doesn't always work, bug???

  override def update(ds1820: DS1820)(implicit session: Session) = {
    require(ds1820 != null)
    val query = for ( d <- DS1820sDb if d.id === ds1820.id) yield d
    query.update(ds1820)

    // simply return the DS1820 the update was for
    ds1820
  }

  override def insert(ds1820: DS1820)(implicit session: Session) = {
    val id = DS1820sDb.insertInvoker returning DS1820sDb.id insert(ds1820)
    ds1820.copy(id = Some(id))
  }

  def filterByEnabled(enabled: Boolean)(implicit session: Session) = {
    val query = for (d <- DS1820sDb if d.enabled === enabled) yield d
    query.list
  }

  def all(implicit session: Session) = {
    Query(DS1820sDb).sortBy(_.path).list
  }
}