
import org.specs2.mutable._

import play.api.db.slick.DB
import play.api.test._
import play.api.test.Helpers._
import models._
import play.api.db.slick.Config.driver.simple._


/**
 * Ensure our DB is what we want it to be
 */
class DBSpec extends Specification {

  "DB" should {

   "select the correct testing db settings by default" in
     new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        play.api.db.slick.DB.withSession{ implicit session =>
        session.conn.getMetaData.getURL must startWith("jdbc:h2:mem:play-test")
      }
    }

    "use the default db settings when no other possible options are available" in
      new WithApplication {
        play.api.db.slick.DB.withSession{ implicit session =>
        session.conn.getMetaData.getURL must equalTo("jdbc:h2:mem:play")
      }
    }


  }
}
