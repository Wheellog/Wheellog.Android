package com.cooper.wheellog.utils

class Bms {
    var serialNumber: String = ""
    var versionNumber: String = ""
    var factoryCap: Int = 0
    var actualCap: Int = 0
    var fullCycles: Int = 0
    var chargeCount: Int = 0
    var mfgDateStr: String = ""
    var status: Int = 0
    var remCap: Int = 0
    var remPerc: Int = 0
    var current: Double = 0.0
    var voltage: Double = 0.0
    var temp1: Int = 0
    var temp2: Int = 0
    var balanceMap: Int = 0
    var health: Int = 0
    var cells: Array<Double> = Array(16) { 0.0 }
}