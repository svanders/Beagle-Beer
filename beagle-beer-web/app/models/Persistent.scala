package models

import scala.slick.driver.H2Driver.simple._
import scala.reflect.runtime.{ universe => ru }

/**
 * Provides methods to help us reason about the persistent state of
 * our domain objects.
 */
trait Persistent {

  val id:Option[Any]

  def isPersisted:Boolean = id match {
      case None => false
      case Some(_) => true
  }

}

trait InsertOrUpdate[T <: Persistent] {

  import scala.slick.driver.H2Driver.simple._
  import scala.reflect.runtime.{ universe => ru }

  def insertOrUpdate(persistMe: T)(implicit session: Session) = {
    require(persistMe != null)
    if (persistMe.isPersisted) {
      update(persistMe)
    } else {
      insert(persistMe)
    }
  }

  def update(updateMe: T)(implicit session: Session)
  def insert(insertMe: T)(implicit session: Session)

}

