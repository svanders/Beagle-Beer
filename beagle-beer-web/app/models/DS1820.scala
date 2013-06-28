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

object DS1820s extends Table[DS1820]("DS1820") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def path = column[String]("path", O.NotNull)
  def name = column[String]("name", O.NotNull)
  def enabled = column[Boolean]("enabled", O.NotNull)
  def master = column[Boolean]("master", O.NotNull)

  def * = id.? ~ path ~ name ~ enabled ~ master <> (DS1820.apply _, DS1820.unapply _)
//  def forInsert =  path ~ name ~ enabled ~ master <> ({t => DS1820(None, t._1, t._2, t._3, t._4)},
//                                                      {d: DS1820 => Some((d.path, d.name, d.enabled, d.master))})


  // Use the implicit threadLocalSession
  //import Database.threadLocalSession  --- doesn't always work, bug???



  def insertOrUpdate(ds1820: DS1820)(implicit session: Session) = {
    require(ds1820 != null)
    if (ds1820.isPersisted) {
      update(ds1820)
    } else {
      this.insert(ds1820)
    }
  }

  def update(ds1820: DS1820)(implicit session: Session) = {
    require(ds1820 != null)
    val query = for ( d <- DS1820s if d.id === ds1820.id) yield d
    query.update(ds1820)
  }

  def filterByEnabled(enabled: Boolean)(implicit session: Session) = {
    val query = for (d <- DS1820s if d.enabled === enabled) yield d
    query.list
  }

  def all(implicit session: Session) = {
    Query(DS1820s).sortBy(_.path).list
  }
}