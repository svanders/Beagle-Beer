package domain

import play.api.db.slick.DB
import play.api.test._
import play.api.test.Helpers._
import models._
import org.specs2.mutable.Specification
import play.api.db.slick.Config.driver.simple._


/**
 * Checks conformance to requirements of the Device model, and persistence.
 */
class DS1820Spec extends Specification {

  val testDS1820s = List(
    DS1820(None, "pathCCC", "d1", true, false),
    DS1820(None, "pathAAA", "d2", false, false),
    DS1820(None, "pathBBB", "d3", true, false)
  )

  "DS1820s" should {
    "have their id set when inserted" in
      new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        DB.withSession {
          implicit session =>
            val ds1820 = DS1820(None, "path1", "d1", true, false)
            val found = DS1820sDb.insert(ds1820)
            found.id.get must be greaterThan 0
        }
      }

  "be searchable by the enabled flag" in
    new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
      DB.withSession {
        implicit session =>
          DS1820sDb.insertAll(testDS1820s: _*)
          DS1820sDb.filterByEnabled(true) must haveSize(2)
      }
    }

  "be ordered by path when finding all" in
    new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
      DB.withSession {
        implicit session =>
          DS1820sDb.insertAll(testDS1820s: _*)
          val found = DS1820sDb.all
          found must have size 3
          found.map(d => d.path) must be equalTo List("pathAAA", "pathBBB", "pathCCC")
      }
    }
  }

}
