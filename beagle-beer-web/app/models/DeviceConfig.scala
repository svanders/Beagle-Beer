package models

/**
 * Represents the configuration of the device, i.e. where the
 * various IO devices are.
 */
case class DeviceConfig(sensorsDir: String)

object DeviceConfig {

  var config = new DeviceConfig("/sys/devices/w1_bus_master1 ")



}
