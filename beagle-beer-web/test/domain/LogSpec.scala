package domain

import play.api.db.slick.DB
import play.api.test._
import play.api.test.Helpers._
import models._
import org.specs2.mutable.Specification
import play.api.db.slick.Config.driver.simple._
import java.util.Date
import io.util.StringExtras.DateString

/**
 * Checks conformance to requirements of the Device model, and persistence.
 */
class LogSpec extends Specification {


  val testLogs = List(
    Log(None, "L1", 1,  "2013-07-16 10:00:00".toDate, None),
    Log(None, "L2", 2, "2013-07-14 10:00:00".toDate, Some("2013-07-14 24:00:00".toDate)),
    Log(None, "L3", 3, "2013-07-15 10:00:00".toDate, Some("2013-07-15 13:00:00".toDate))
  )

  "Logs" should {
    "have their id set when inserted" in
      new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withSession {
          implicit session =>
            val log = LogsDb.insert(Log(None, "Log", 100, new Date, None))
            log.id.get must be greaterThan 0
        }
      }

    "be ordered by start date when finding all" in
      new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withSession {
          implicit session =>
            LogsDb.insertAll(testLogs: _*)
            val found = LogsDb.all
            found must have size 3
            found.map(l => l.start) must be equalTo List(
              "2013-07-16 10:00:00".toDate,
              "2013-07-15 10:00:00".toDate,
              "2013-07-14 10:00:00".toDate
            )
        }
      }
  }
}
