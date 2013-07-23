package models

import scala.slick.driver.H2Driver.simple._
import scala.reflect.runtime.{universe => ru}
import java.util.Date
import models.typemapping.CustomSlickTypes.dateTypeMapper
import scala.slick.lifted.ColumnOption.DBType

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Represents a temperature reading taken at a point in time.
 */
case class Sample(id: Option[Int], logId: Int, ds1820Id: Int, date: Date, value: Float) extends Persistent
  with Ordered[Sample] {

  def compare(that: Sample) = this.date.getTime compare that.date.getTime

}

object SamplesJson {
  // JSON (de)serilazation
  implicit val sampleReads = Json.reads[Sample]
  implicit val sampleWrites = Json.writes[Sample]
}


object SamplesDb extends Table[Sample]("Sample") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def logId = column[Int]("logId", O.NotNull)
  def ds1820Id = column[Int]("ds1820Id", O.NotNull)
  def date = column[Date]("date", O.NotNull, DBType("TimeStamp"))
  def value = column[Float]("value", O.NotNull)

  def log = foreignKey("Log_FK", logId, LogsDb)(_.id)
  def ds1820 = foreignKey("DS1820_FK", ds1820Id, DS1820sDb)(_.id)

  def * = id.? ~ logId ~ ds1820Id ~ date ~ value <>(Sample, Sample.unapply _)

  def insert(sample: Sample)(implicit session: Session): Sample = {
    val id = SamplesDb.insertInvoker returning SamplesDb.id insert (sample)
    sample.copy(id = Some(id))
  }

  def find(logId: Int)(implicit session: Session): (List[DS1820], List[List[Sample]]) = {
    val query = (for {
      s <- SamplesDb if s.logId === logId
      d <- DS1820sDb if s.ds1820Id === d.id
    } yield (d, s)).sortBy(_._2.date).sortBy(_._1.name)


    val ds1820sAndSamples: (List[DS1820], List[Sample]) = query.list.unzip
    val ds1820s: List[DS1820] = ds1820sAndSamples._1.toSet.toList
    val sampleLists = ds1820sAndSamples._2.groupBy(s => s.date)

    (ds1820s, sampleLists.values.toList)
//    // TODO - make this understandable
//    query.list.groupBy(p => p._1).mapValues(l => l.map(ds => ds._2).sorted)
  }
}




