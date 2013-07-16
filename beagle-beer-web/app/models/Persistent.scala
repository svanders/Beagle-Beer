package models

import scala.reflect.runtime.{universe => ru}

/**
 * Provides methods to help us reason about the persistent state of
 * our domain objects.
 */
trait Persistent {

  val id: Option[Any]

  def isPersisted: Boolean = id match {
    case None => false
    case Some(_) => true
  }

}

/**
 * Provides an easy way to insert or update a Persistent using existing insert and update methods.
 * @tparam T
 */
trait InsertOrUpdate[T <: Persistent] {

  import scala.slick.driver.H2Driver.simple._
  import scala.reflect.runtime.{universe => ru}

  /**
   * Either performs an insert or an update (both delegated to) on the provided persistent, the choice to
   * insert or update is made based on the result of <code>Persistent#isPersisted</code>.
   * result
   * @param persistMe
   * @param session
   * @return
   */
  def insertOrUpdate(persistMe: T)(implicit session: Session): T = {
    require(persistMe != null)
    if (persistMe.isPersisted) {
      update(persistMe)
    } else {
      insert(persistMe)
    }
  }

  /** The update method that will be delegated to for already persisted Persistent objects */
  def update(updateMe: T)(implicit session: Session): T

  /** The insert method that will be delegated to for non-persisted (new) Persistent objects */
  def insert(insertMe: T)(implicit session: Session): T

}

