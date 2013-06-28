package test

import models._
import org.specs2.mutable.Specification
import play.api.db.slick.DB
import play.api.test.{WithApplication, FakeApplication}
import play.api.test.Helpers._
import scala.slick.session.Session
import play.api.Play.current

/**
 * Helper to setup a fake application and database connection
 * to the test database
 */
object FakeApplicationHelper {


  def fakeApp[T](test: => T): T = {
    running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
      DB.withSession { implicit session: Session =>
        test
      }
    }
  }

}
