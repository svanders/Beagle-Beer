package models

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
