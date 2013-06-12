package controllers

import play.api.mvc.{Action, Controller}
import models.DeviceConfig
import play.api.data._
import play.api.data.Forms._
import devices.DS1820Scanner
import java.io.File
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

import org.slf4j.LoggerFactory;

/**
 * A Play Controller to allow setup of the BeagleBoard.
 * e.g. Where the application should look for the temperature probes.
 */
object DeviceSetup extends Controller {

  val log = LoggerFactory.getLogger(this.getClass)

  def view = Action {
    implicit request =>
      val devices = readDevices
      Ok(views.html.deviceSetup(deviceConfigForm.fill(DeviceConfig.config), devices))
  }

  def save = Action {
    implicit request =>
      deviceConfigForm.bindFromRequest.fold(

        formWithErrors => BadRequest(views.html.deviceSetup(formWithErrors, Map())),
        value => {
          DeviceConfig.config = value
          Redirect(routes.DeviceSetup.view).flashing("message" -> "Device Configuration Saved")
        }
      )
  }

  def readDevices: Map[String, Float] = {
    val sensorDir = DeviceConfig.config.sensorsDir
    if ((new File(sensorDir)).isDirectory) {
      val asyncReads = new DS1820Scanner(sensorDir).readAllAsync
      val reads = for {
        d <- asyncReads.keys
        f <- asyncReads.get(d)
      } yield (d, extractFutureFloat(f))
      reads.toMap
    } else {
      log.error("Directory " + sensorDir + " not found")
      Map()
    }

  }

  def extractFutureFloat(f: Future[Float]): Float = {
    var result = Float.NaN
    f.onSuccess {
      case value => result = value
    }
    f.onFailure {
      case e => log.error("Could not read Future", e)
    }
    result
  }

  val deviceConfigForm = Form(
    mapping(
      "sensorsDir" -> nonEmptyText
    )(DeviceConfig.apply)(DeviceConfig.unapply)
  )
}
