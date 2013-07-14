package controllers.util

import play.api.mvc.Flash

/**
 * Valid values for flash keys that our main view will detect and display automatically
 */
object FlashScope {

  val success = "success"
  val info = "info"
  val error = "error"

  /** An empty flash value that controller can use when they have nothing to flash.
    * It's implicit so just importing it will make it the default flash. */
  implicit val emptyFlash: Flash = Flash.apply(Map())
}
