package com.cooper.wheellog.utils

class NinebotBms {
    lateinit var serialNumber: String
    lateinit var versionNumber: String
    var factoryCap: Int = 0
    var actualCap: Int = 0
    var fullCycles: Int = 0
    var chargeCount: Int = 0
    lateinit var mfgDateStr: String
    var status: Int = 0
    var remCap: Int = 0
    var remPerc: Int = 0
    var current: Double = 0.0
    var voltage: Double = 0.0
    var temp1: Int = 0
    var temp2: Int = 0
    var balanceMap: Int = 0
    var health: Int = 0
    lateinit var cells: Array<Double>

    init {
        reset()
    }
    
    fun reset() {
        serialNumber = ""
        versionNumber = ""
        factoryCap = 0
        actualCap = 0
        fullCycles = 0
        chargeCount = 0
        mfgDateStr = ""
        status = 0
        remCap = 0
        remPerc = 0
        current = 0.0
        voltage = 0.0
        temp1 = 0
        temp2 = 0
        balanceMap = 0
        health = 0
        cells = Array(16) { 0.0 }
    }
}
