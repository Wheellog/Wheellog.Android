package com.cooper.wheellog.utils

class SmartBms {
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
    var temp1: Double = 0.0
    var temp2: Double = 0.0
    var temp3: Double = 0.0
    var temp4: Double = 0.0
    var temp5: Double = 0.0
    var temp6: Double = 0.0
    var tempMos: Double = 0.0
    var tempMosEnv: Double = 0.0
    var balanceMap: Int = 0
    var health: Int = 0
    var minCell: Double = 0.0
    var maxCell: Double = 0.0
    var cellDiff: Double = 0.0
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
        temp1 = 0.0
        temp2 = 0.0
        temp3 = 0.0
        temp4 = 0.0
        temp5 = 0.0
        temp6 = 0.0
        tempMos = 0.0
        tempMosEnv = 0.0
        balanceMap = 0
        health = 0
        minCell = 0.0
        maxCell = 0.0
        cellDiff = 0.0
        cells = Array(32) { 0.0 }
    }
}
