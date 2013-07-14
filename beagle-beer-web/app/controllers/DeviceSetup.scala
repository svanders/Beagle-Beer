package controllers

import play.api.db.slick.DB
import play.api.mvc.{Action, Controller}
import models.{DS1820s, DS1820}
import play.api.data._
import play.api.data.Forms._
import io.{DS1820BulkReader, DS1820Scanner, DS1820NameParser}
import java.io.File
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import play.api.Play.current

import org.slf4j.LoggerFactory
import play.api.data.validation.{Valid, ValidationError, Invalid, Constraint}
import controllers.util.{FlashScope, FormExtension}


/**
 * A Play Controller to allow setup of the BeagleBoard.
 * e.g. Where the application should look for the temperature probes.
 */
object DeviceSetup extends Controller {

  val log = LoggerFactory.getLogger(this.getClass)

  // Watch out a global variable, yes 2 people on the device setup page will interfere
  // with each other.  Too bad, it's a device config page.
  var scanDir = "/sys/devices/w1_bus_master1"


  def view = Action {
    implicit request =>
      DB.withSession {
        implicit session =>
          val devices = loadDevices
          val reads = scala.concurrent.Future {
            readDevices(devices)
          }
          Async {
            reads.map(d => Ok(views.html.deviceSetup(scanForm.fill(scanDir), probeForm.fill(loadDevices), d)))
          }
      }
  }

  def scan = Action {
    implicit request =>
      DB.withSession {
        implicit session =>
          scanForm.bindFromRequest.fold(
            formWithErrors =>
              BadRequest(views.html.deviceSetup(formWithErrors, probeForm.fill(loadDevices), Map())),
            value => {
              scanDir = value
              Redirect(routes.DeviceSetup.view)
            }
          )
      }
  }

  def save = Action {
    implicit request =>
      DB.withTransaction {
        implicit session =>
          probeForm.bindFromRequest.fold(
            formWithErrors => BadRequest(views.html.deviceSetup(scanForm.fill(scanDir), formWithErrors, Map())),
            value => {
               value.foreach(DS1820s.insertOrUpdate)
              Redirect(routes.DeviceSetup.view).flashing(FlashScope.success -> "Device Configuration Saved")
            }
          )
      }
  }

  def loadDevices: List[DS1820] = {

    def scan: List[DS1820] = {
      if ((new File(scanDir)).isDirectory) {
        val paths = new DS1820Scanner(scanDir).scan
        val ds1820s = for {
          path <- paths
        } yield new DS1820(None, path, DS1820NameParser.extractDeviceId(path), true, false)
        ds1820s.toList
      } else {
        log.error("Directory " + scanDir + " not found")
        Nil
      }
    }

    DB.withSession {
      implicit session =>
        val devices = DS1820s.all
        if (devices.isEmpty) {
          scan
        } else {
          devices
        }
    }

  }


  def readDevices(devices: List[DS1820]): Map[String, Float] = {
    val reads = DS1820BulkReader.readAll(devices.map(d => d.path))
    reads.toMap
  }


  val scanForm = Form(
    "sensorsDir" -> nonEmptyText
  )


  val probeForm = Form(
    list(
      mapping(
        "id" -> optional(number),
        "path" -> text,
        "name" -> nonEmptyText(minLength = 1, maxLength = 50).verifying(FormExtension.nonBlank),
        "enabled" -> boolean,
        "master" -> boolean
      ) (DS1820.apply)(DS1820.unapply)
        verifying("Cannot be disabled when selected as master", {
        f => !(!f.enabled && f.master)
      })
    )
  verifying("One (only) probe must be selected as the master", {
      f => f.filter(d => d.master).size == 1
    })
  )


}
