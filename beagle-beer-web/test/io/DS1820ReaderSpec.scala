package io

import org.specs2.mutable._


/**
 * Tests that the DS1820Reader can properly read the device output.
 */
class DS1820Spec extends Specification {

  val reader: DS1820Reader = new DS1820Reader("test/data/28-000002a6c659/w1_slave")
  val negativeReader: DS1820Reader = new DS1820Reader("test/data/28-000002a6c660/w1_slave")
  val scanner: DS1820Scanner = new DS1820Scanner("test/data/")


   "The reader" should {
     "be able to read the output from a DS1820 temperature sensor" in {
       reader.read  must be equalTo(("test/data/28-000002a6c659/w1_slave", Some(20.111f))) }
       
     "be able to read negative temperatures" in {
       negativeReader.read must be equalTo(("test/data/28-000002a6c660/w1_slave", Some(-22.123f)))
     }  
   }

  "The scanner" should {
    "find all DS1820 devices in a directory" in {
      val foundDevices = scanner.scan
      foundDevices must have size(2)
    }

    "be able to read all devices in a dirctory" in {
      val readings = scanner.readAll
      readings must have size(2)
    }

    "be able to read no devices" in {
      DS1820BulkReader.readAll(Nil) must be empty
    }
  }

}
