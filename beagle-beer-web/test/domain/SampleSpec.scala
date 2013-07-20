package domain

import play.api.db.slick.DB
import play.api.test._
import play.api.test.Helpers._
import models._
import org.specs2.mutable.Specification
import play.api.db.slick.Config.driver.simple._
import java.util.Date
import io.util.StringFileUtil.DateString

/**
 * /**
 * Checks conformance to requirements of the Device model, and persistence.
 */
 */
class SampleSpec extends Specification {



  "Sample" should {
    "have their id set when inserted" in
      new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withSession {
          implicit session =>
            val log: Log = createLog
            val ds1820 = createDs1820
            val found = SamplesDb.insert(Sample(None, log.id.get, ds1820.id.get, new Date, 21.2F))
            found.id.get must be greaterThan 0
        }
      }

    "be ordered by start date when finding all for a Log" in
      new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withSession {
          implicit session =>
            val log: Log = createLog
            val ds1820a = createDs1820
            val ds1820b = createDs1820
            val six = SamplesDb.insert(Sample(None, log.id.get, ds1820b.id.get, "2013-07-16 10:00:00".toDate, 6F))
            val four = SamplesDb.insert(Sample(None, log.id.get, ds1820b.id.get, "2013-07-15 10:00:00".toDate, 5F))
            val five = SamplesDb.insert(Sample(None, log.id.get, ds1820b.id.get, "2013-07-14 10:00:00".toDate, 4F))

            val three = SamplesDb.insert(Sample(None, log.id.get, ds1820a.id.get, "2013-07-16 10:00:00".toDate, 3F))
            val one = SamplesDb.insert(Sample(None, log.id.get, ds1820a.id.get, "2013-07-14 10:00:00".toDate, 1F))
            val two = SamplesDb.insert(Sample(None, log.id.get, ds1820a.id.get, "2013-07-15 10:00:00".toDate, 2F))


            val found = SamplesDb.find(log)
            found.mapValues(l => l.map(s => s.value)) must be equalTo Map(
              (ds1820a -> List(1F, 2F, 3F)),
              (ds1820b -> List(4F, 5F, 6F))
            )
        }
      }
  }

  def createLog(implicit session: Session): Log = LogsDb.insert(Log(None, new Date, None))

  def createDs1820(implicit session: Session): DS1820 = DS1820sDb.insert(DS1820(None, "/path", "name", true, true))
}
