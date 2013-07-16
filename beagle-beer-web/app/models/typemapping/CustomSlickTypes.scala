package models.typemapping

import scala.slick.lifted.MappedTypeMapper
import java.util.Date
import java.sql.Timestamp


/**
 * Custom Slick type mappings (hopefulyl not too many of these ;) )
 */
object CustomSlickTypes {

  /**
   * Maps java.util.Date to and from java.sql.TimeStamp.
   */
  implicit val dateTypeMapper = MappedTypeMapper.base[Date, Timestamp] (
  { date => new Timestamp(date.getTime) },    // map a java Date to a sql TimeStamp
  { timestamp => new Date(timestamp.getTime) } // map a sql TimeStamp to a java Date
  )


}
