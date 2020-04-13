package com.cooper.wheellog.utils;

import com.cooper.wheellog.BluetoothLeService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import timber.log.Timber;

//import static com.cooper.wheellog.utils.InMotionAdapter.Model.*;

/**
 * Created by cedric on 29/12/2016.
 */
public class NinebotZAdapter {
    private static NinebotZAdapter INSTANCE;
    private Timer keepAliveTimer;
	private boolean settingCommandReady = false;
	private static int updateStep = 0;
	private byte[] settingCommand;
	private static byte[] gamma = new byte[16];
    private static int stateCon = 0;
    private static boolean bmsMode = false;


    NinebotZUnpacker unpacker = new NinebotZUnpacker();

    public void startKeepAliveTimer(final BluetoothLeService mBluetoothLeService, final String ninebotPassword) {
        Timber.i("Ninebot timer starting");
        updateStep = 0;
        stateCon = 0;
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (updateStep == 0) {
                    Timber.i("State connection %d", stateCon) ;
                    if (stateCon == 0) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotZAdapter.CANMessage.startCommunication().writeBuffer())) {
                            //stateCon +=1;
                            Timber.i("Sent start message");
                        } else updateStep = 39;

                    } else if (stateCon == 1) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotZAdapter.CANMessage.getKey().writeBuffer())) {
                            //stateCon +=1;
                            Timber.i("Sent getkey message");
                        } else updateStep = 39;

                    } else if (stateCon == 2) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotZAdapter.CANMessage.getSerialNumber().writeBuffer())) {
                            //stateCon +=1;
                            Timber.i("Sent serial number message");
                        } else updateStep = 39;

                    } else if (stateCon == 3) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotZAdapter.CANMessage.getVersion().writeBuffer())) {
                            //stateCon +=1;
                            Timber.i("Sent serial version message");
                        } else updateStep = 39;

                    } else if (stateCon == 4) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotZAdapter.CANMessage.getBms1Sn().writeBuffer())) {
                            //stateCon +=1;
                            Timber.i("Sent BMS1 SN message");
                        } else updateStep = 39;
                    } else if (stateCon == 5) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotZAdapter.CANMessage.getBms1Life().writeBuffer())) {
                            //stateCon +=1;
                            Timber.i("Sent BMS1 life message");
                        } else updateStep = 39;
                    } else if (stateCon == 6) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotZAdapter.CANMessage.getBms1Cells().writeBuffer())) {
                            //stateCon +=1;
                            Timber.i("Sent BMS1 cells message");
                        } else updateStep = 39;
                    } else if (stateCon == 7) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotZAdapter.CANMessage.getBms2Sn().writeBuffer())) {
                            //stateCon +=1;
                            Timber.i("Sent BMS2 SN message");
                        } else updateStep = 39;
                    } else if (stateCon == 8) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotZAdapter.CANMessage.getBms2Life().writeBuffer())) {
                            //stateCon +=1;
                            Timber.i("Sent BMS2 life message");
                        } else updateStep = 39;
                    } else if (stateCon == 9) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotZAdapter.CANMessage.getBms2Cells().writeBuffer())) {
                            //stateCon +=1;
                            Timber.i("Sent BMS2 cells message");
                        } else updateStep = 39;

                    } else if (settingCommandReady) {
    					if (mBluetoothLeService.writeBluetoothGattCharacteristic(settingCommand)) {
                            //needSlowData = true;
                            settingCommandReady = false;
                            Timber.i("Sent command message");
                        } else updateStep = 39; // after +1 and %10 = 0
    				} else {
                        if (!mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotZAdapter.CANMessage.getLiveData().writeBuffer())) {
                            Timber.i("Unable to send keep-alive message");
                            updateStep = 39;
    					} else {
                            Timber.i("Sent keep-alive message");
    					}
                    }

				}
                updateStep += 1;

                if ((updateStep == 5) && (stateCon > 3) && (stateCon < 10)) {
                    Timber.i("Change state to %d 1", stateCon);
                    stateCon += 1;
                    if (stateCon > 9) stateCon = 4;
                }
                if (bmsMode && (stateCon == 10)) {
                    Timber.i("Change state to %d 2", stateCon);
                    stateCon = 4;
                }
                if (!bmsMode && (stateCon > 3) && (stateCon < 10)) {
                    Timber.i("Change state to %d 3", stateCon);
                    stateCon = 10;
                }
                updateStep %= 5;
                Timber.i("Step: %d", updateStep);
            }
        };
        Timber.i("Ninebot timer started");
        keepAliveTimer = new Timer();
        keepAliveTimer.scheduleAtFixedRate(timerTask, 0, 25);
    }


	public void resetConnection() {
        stateCon = 0;
        updateStep = 0;
        gamma = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        stopTimer();
	}
	
    public void setBmsReadingMode(boolean mode) {
        bmsMode = mode;
    }






	
    public static class Status {

        private final int speed;
        private final int voltage;
        private final int batt;
        private final int current;
        private final int power;
        private final int distance;
		private final int temperature;


        Status() {

            speed = 0;
            voltage = 0;
            batt = 0;
            current = 0;
            power = 0;
            distance = 0;
			temperature = 0;

        }

        Status(int speed, int voltage, int batt, int current, int power, int distance,  int temperature) {

            this.speed = speed;
            this.voltage = voltage;
            this.batt = batt;
            this.current = current;
            this.power = power;
            this.distance = distance;
			this.temperature = temperature;

        }

        public int getSpeed() {
            return speed;
        }

        public int getVoltage() {
            return voltage;
        }

        public int getBatt() {
            return batt;
        }

        public int getCurrent() {
            return current;
        }

        public int getPower() {
            return power;
        }

        public int getDistance() {
            return distance;
        }

		public int getTemperature() {
            return temperature;
        }



        @Override
        public String toString() {
            return "Status{" +
                    "speed=" + speed +
                    ", voltage=" + voltage +
                    ", batt=" + batt +
                    ", current=" + current +
                    ", power=" + power +
                    ", distance=" + distance +
					", temperature=" + temperature +

                    '}';
        }
    }
	

    public static class serialNumberStatus extends Status {
        private final String serialNumber;

        serialNumberStatus(String serialNumber) {
            super();
            this.serialNumber = serialNumber;
      }

      public String getSerialNumber() {
            return serialNumber;
      }


        @Override
        public String toString() {
            return "Infos{" +
                    "serialNumber='" + serialNumber + '\'' +

                    '}';
        }
    }

    public static class bmsStatusSn extends Status {
        private final int bmsNum;
        private final String serialNumber;
        private final String versionNumber;
        private final int factoryCap;
        private final int actualCap;
        private final int fullCycles;
        private final int chargeCount;
        private final String mfgDateStr;

        bmsStatusSn(int bmsNum, String serialNumber, String versionNumber, int factoryCap, int actualCap, int fullCycles, int chargeCount, String mfgDateStr) {
            super();
            this.bmsNum = bmsNum;
            this.serialNumber = serialNumber;
            this.versionNumber = versionNumber;
            this.factoryCap = factoryCap;
            this.actualCap = actualCap;
            this.fullCycles = fullCycles;
            this.chargeCount = chargeCount;
            this.mfgDateStr = mfgDateStr;
        }
        public int getBmsNumber() {
            return bmsNum;
        }
        public String getSerialNumber() {
            return serialNumber;
        }
        public String getVersionNumber() {
            return versionNumber;
        }
        public int getFactoryCap() {
            return factoryCap;
        }
        public int getActualCap() {
            return actualCap;
        }
        public int getFullCycles() {
            return fullCycles;
        }
        public int getChargeCount() {
            return chargeCount;
        }
        public String getMfgDateStr() {
            return mfgDateStr;
        }

        @Override
        public String toString() {
            return "BMS{" +
                    "bmsNum=" + bmsNum +
                    ", serialNumber='" + serialNumber + '\'' +
                    ", versionNumber='" + versionNumber + '\'' +
                    ", factoryCap=" + factoryCap +
                    ", actualCap=" + actualCap +
                    ", fullCycles=" + fullCycles +
                    ", chargeCount=" + chargeCount +
                    ", mfgDateStr='" + mfgDateStr + '\'' +
                    '}';
        }

    }

    public static class bmsStatusLife extends Status {
        private final int bmsNum;
        private final int bmsStatus;
        private final int remCap;
        private final int remPerc;
        private final int bmsCurrent;
        private final int bmsVoltage;
        private final int bmsTemp1;
        private final int bmsTemp2;
        private final int balanceMap;
        private final int health;

        bmsStatusLife(int bmsNum, int bmsStatus, int remCap, int remPerc, int bmsCurrent, int bmsVoltage, int bmsTemp1, int bmsTemp2, int balanceMap, int health) {
            super();
            this.bmsNum = bmsNum;
            this.bmsStatus = bmsStatus;
            this.remCap = remCap;
            this.remPerc = remPerc;
            this.bmsCurrent = bmsCurrent;
            this.bmsVoltage = bmsVoltage;
            this.bmsTemp1 = bmsTemp1;
            this.bmsTemp2 = bmsTemp2;
            this.balanceMap = balanceMap;
            this.health = health;

        }
        public int getBmsNumber() {
            return bmsNum;
        }
        public int getBmsStatus() {
            return bmsStatus;
        }
        public int getRemCap() {
            return remCap;
        }
        public int getRemPerc() {
            return remPerc;
        }
        public int getBmsCurrent() {
            return bmsCurrent;
        }
        public int getBmsVoltage() {
            return bmsVoltage;
        }
        public int getBmsTemp1() {
            return bmsTemp1;
        }
        public int getBmsTemp2() {
            return bmsTemp2;
        }
        public int getBalanceMap() {
            return balanceMap;
        }
        public int getHealth() {
            return health;
        }


        @Override
        public String toString() {
            return "BMS{" +
                    "bmsNum=" + bmsNum +
                    ", bmsStatus=" + bmsStatus +
                    ", remCap=" + remCap +
                    ", remPerc=" + remPerc +
                    ", bmsCurrent=" + bmsCurrent +
                    ", bmsVoltage=" + bmsVoltage +
                    ", bmsTem1=" + bmsTemp1 +
                    ", bmsTemp2=" + bmsTemp2 +
                    ", balanceMap=" + balanceMap +
                    ", health=" + health +


                    '}';
        }

    }

    public static class bmsStatusCells extends Status {
        private final int bmsNum;
        private final int cell1;
        private final int cell2;
        private final int cell3;
        private final int cell4;
        private final int cell5;
        private final int cell6;
        private final int cell7;
        private final int cell8;
        private final int cell9;
        private final int cell10;
        private final int cell11;
        private final int cell12;
        private final int cell13;
        private final int cell14;
        private final int cell15;
        private final int cell16;

        bmsStatusCells(int bmsNum, int cell1, int cell2, int cell3, int cell4, int cell5, int cell6,
                      int cell7, int cell8, int cell9, int cell10, int cell11, int cell12, int cell13,
                      int cell14, int cell15, int cell16) {
            super();
            this.bmsNum = bmsNum;
            this.cell1 = cell1;
            this.cell2 = cell2;
            this.cell3 = cell3;
            this.cell4 = cell4;
            this.cell5 = cell5;
            this.cell6 = cell6;
            this.cell7 = cell7;
            this.cell8 = cell8;
            this.cell9 = cell9;
            this.cell10 = cell10;
            this.cell11 = cell11;
            this.cell12 = cell12;
            this.cell13 = cell13;
            this.cell14 = cell14;
            this.cell15 = cell15;
            this.cell16 = cell16;

        }
        public int getBmsNumber() {
            return bmsNum;
        }
        public int getCell1() {
            return cell1;
        }
        public int getCell2() {
            return cell2;
        }
        public int getCell3() {
            return cell3;
        }
        public int getCell4() {
            return cell4;
        }
        public int getCell5() {
            return cell5;
        }
        public int getCell6() {
            return cell6;
        }
        public int getCell7() {
            return cell7;
        }
        public int getCell8() {
            return cell8;
        }
        public int getCell9() {
            return cell9;
        }
        public int getCell10() {
            return cell10;
        }
        public int getCell11() {
            return cell11;
        }
        public int getCell12() {
            return cell12;
        }
        public int getCell13() {
            return cell13;
        }
        public int getCell14() {
            return cell14;
        }
        public int getCell15() {
            return cell15;
        }
        public int getCell16() {
            return cell16;
        }


        @Override
        public String toString() {
            return "BMS{" +
                    "bmsNum=" + bmsNum +
                    ", cell1=" + cell1 +
                    ", cell2=" + cell2 +
                    ", cell3=" + cell3 +
                    ", cell4=" + cell4 +
                    ", cell5=" + cell5 +
                    ", cell6=" + cell6 +
                    ", cell7=" + cell7 +
                    ", cell8=" + cell8 +
                    ", cell9=" + cell9 +
                    ", cell10=" + cell10 +
                    ", cell11=" + cell11 +
                    ", cell12=" + cell12+
                    ", cell13=" + cell13 +
                    ", cell14=" + cell14 +
                    ", cell15=" + cell15 +
                    ", cell16=" + cell16 +

                    '}';
        }

    }

    public static class versionStatus extends Status {
        private final String version;

        versionStatus(String version) {
            super();
            this.version = version;
        }

        public String getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return "Infos{" +
                    "version='" + version + '\'' +
                    '}';
        }
    }


    public static class activationStatus extends Status {
        private final String activationDate;

        activationStatus(String activationDate) {
            super();
            this.activationDate = activationDate;
        }

        public String getVersion() {
            return activationDate;
        }

        @Override
        public String toString() {
            return "Infos{" +
                    "activation='" + activationDate + '\'' +
                    '}';
        }
    }

    private static String toHexString(byte[] buffer){
        String str = "[";

        for (int c : buffer) {
            str += String.format("%02X", (c & 0xFF));
        }
        str += "]";
        return str;
    }
    /**
     * Created by cedric on 29/12/2016.
     */
    public static class CANMessage {

        enum Addr {
            BMS1(0x11),
            BMS2(0x12),
            Controller(0x14),
            KeyGenerator(0x16),
            App(0x3e);

            private int value;

            Addr(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        enum Comm {
            Read(0x01),
            Write(0x03),
            Get(0x04),
            GetKey(0x5b);

            private int value;

            Comm(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        enum Param {
            GetKey(0x00),
            Start(0x68),
            SerialNumber(0x10),
            Firmware(0x1a),
            Angles(0x61),
            BatteryLevel(0x22),
            ActivationDate(0x69),
            LiveData(0xb0);


            private int value;

            Param(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        int len = 0;
        int source = 0;
        int destination = 0;
        int command = 0;
        int parameter = 0;
        byte[] data;
        int crc = 0;


        CANMessage(byte[] bArr) {
            if (bArr.length < 7) return;
            len = bArr[0] & 0xff;
            source = bArr[1] & 0xff;
            destination = bArr[2] & 0xff;
            command = bArr[3] & 0xff;
            parameter = bArr[4] & 0xff;
            data = Arrays.copyOfRange(bArr, 5, bArr.length-2);
            crc = bArr[bArr.length-1] << 8 + bArr[bArr.length-2];

        }

        private CANMessage() {

        }

        public byte[] writeBuffer() {

            byte[] canBuffer = getBytes();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(0x5A);
            out.write(0xA5);

            try {
                out.write(canBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return out.toByteArray();

        }


        private byte[] getBytes() {

            ByteArrayOutputStream buff = new ByteArrayOutputStream();

            buff.write(len);
            buff.write(source);
            buff.write(destination);
            buff.write(command);
            buff.write(parameter);
            try {
                buff.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            crc = computeCheck(buff.toByteArray());
            buff.write(crc&0xff);
            buff.write((crc>>8)&0xff);
            byte[] cryptedBuffer = crypto(buff.toByteArray());
            return cryptedBuffer;
        }

        public void clearData() {
            data = new byte[data.length];
        }

        private static int computeCheck(byte[] buffer) {

            int check = 0;
            for (byte c : buffer) {
                check = check + ((int)c & 0xff);
				//check = (check + (int) c) % 256;
            }
            check ^= 0xFFFF;
            check &= 0xFFFF;

            return check;
        }
/////////////// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< fix ME
        static CANMessage verify(byte[] buffer) {

            Timber.i("Verifying");
            byte[] dataBuffer = Arrays.copyOfRange(buffer, 2, buffer.length);
            dataBuffer = crypto(dataBuffer);

            int check = (dataBuffer[dataBuffer.length-1]<<8 | ((dataBuffer[dataBuffer.length-2]) & 0xff)) & 0xffff;
            byte [] dataBufferCheck = Arrays.copyOfRange(dataBuffer, 0, dataBuffer.length-2);
            int checkBuffer = computeCheck(dataBufferCheck);
            if (check == checkBuffer) {
                Timber.i("Check OK");
            } else {
                Timber.i("Check FALSE, packet: %02X, calc: %02X",check, checkBuffer);
            }
            return (check == checkBuffer) ? new CANMessage(dataBuffer) : null;

            //return new CANMessage(dataBuffer);

        }

        static byte[] crypto(byte[] buffer) {

            byte[] dataBuffer = Arrays.copyOfRange(buffer, 0, buffer.length);
            //String crypto_text = "";
            //for (int j = 0; j < dataBuffer.length; j++) {
            //    crypto_text += String.format("%02X", dataBuffer[j]);
            //}
            //Timber.i("Initial packet: %s", crypto_text);
            Timber.i("Initial packet: %s", toHexString(dataBuffer));
            for (int j = 1; j < dataBuffer.length; j++) {
                dataBuffer[j] ^= gamma[(j-1)%16];
            }
            //crypto_text = "";
            //for (int j = 0; j < dataBuffer.length; j++) {
            //    crypto_text += String.format("%02X", dataBuffer[j]);
            //}
            //Timber.i("Decrypted packet: %s", crypto_text);
            Timber.i("En/Decrypted packet: %s", toHexString(dataBuffer));


            return dataBuffer;

        }

        private int intFromBytes(byte[] bytes, int starting) {
            if (bytes.length >= starting + 4) {
                return (((((((bytes[starting + 3] & 255)) << 8) | (bytes[starting + 2] & 255)) << 8) | (bytes[starting + 1] & 255)) << 8) | (bytes[starting] & 255);
            }
            return 0;
        }

        public int shortFromBytes(byte[] bytes, int starting) {
            if (bytes.length >= starting + 2) {
                return (((bytes[starting + 1] & 255) << 8) | (bytes[starting] & 255));
            }
            return 0;
        }

        public short signedShortFromBytes(byte[] bytes, int starting) {
            if (bytes.length >= starting + 2) {
                return (short) (((short) (((short) ((bytes[starting + 1] & 255))) << 8)) | (bytes[starting] & 255));
            }
            return (short) 0;
        }

        public static CANMessage startCommunication() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.Start.getValue();
            msg.data = new byte[]{0x02};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getKey() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.KeyGenerator.getValue();
            msg.command = Comm.GetKey.getValue();
            msg.parameter = Param.GetKey.getValue();
            msg.data = new byte[]{};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getSerialNumber() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.SerialNumber.getValue();
            msg.data = new byte[]{0x0e};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getVersion() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.Firmware.getValue();
            msg.data = new byte[]{0x02};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getActivationDate() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.ActivationDate.getValue();
            msg.data = new byte[]{0x02};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getLiveData() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = Param.LiveData.getValue();
            msg.data = new byte[]{0x20};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getBms1Sn() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.BMS1.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = 0x10; //
            msg.data = new byte[]{0x22};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getBms1Life() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.BMS1.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = 0x30; //
            msg.data = new byte[]{0x18};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getBms1Cells() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.BMS1.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = 0x40; //
            msg.data = new byte[]{0x20};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getBms2Sn() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.BMS2.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = 0x10; //
            msg.data = new byte[]{0x22};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getBms2Life() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.BMS2.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = 0x30; //
            msg.data = new byte[]{0x18};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getBms2Cells() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.BMS2.getValue();
            msg.command = Comm.Read.getValue();
            msg.parameter = 0x40; //
            msg.data = new byte[]{0x20};
            msg.len = msg.data.length;
            msg.crc = 0;
            return msg;
        }


        private byte[] parseKey() {
            byte [] gammaTemp = Arrays.copyOfRange(data, 0, data.length);
            String gamma_text = "";
            for (int j = 0; j < data.length; j++) {
                gamma_text += String.format("%02X", data[j]);
            }
            Timber.i("New key: %s", gamma_text);
            return gammaTemp;
        }

        serialNumberStatus parseSerialNumber() {
            String serialNumber = new String (data);//"";
            /*
            for (int j = 0; j < data.length; j++) {
                serialNumber += String.format("%02X", data[j]);
            }*/
            return new serialNumberStatus(serialNumber);
        }

        versionStatus parseVersionNumber() {
            String versionNumber = "";
            versionNumber += String.format("%X.",(data[1]&0x0f));
            versionNumber += String.format("%1X.", (data[0]>>4)&0x0f );
            versionNumber += String.format("%1X", (data[0])&0x0f );
 //           for (int j = 0; j < data.length; j++) {
 //               versionNumber += String.format("%02X", data[j]);
 //           }
            return new versionStatus(versionNumber);
        }

        activationStatus parseActivationDate() {

            int activationDate = this.shortFromBytes(data, 0);
            int year = activationDate>>9;
            int mounth = (activationDate>>5) & 0x0f;
            int day = activationDate & 0x1f;
            String activationDateStr = String.format("%02d.%02d.20%02d", day, mounth,year);
            return new activationStatus(activationDateStr);
        }

        Status parseLiveData() {
            int batt = this.shortFromBytes(data, 8);
            int speed = this.shortFromBytes(data, 10);
            int distance = this.intFromBytes(data,14);
            int temperature = this.shortFromBytes(data,22);
            int voltage = this.shortFromBytes(data, 24);
            int current = this.signedShortFromBytes(data, 26);
            int power = voltage*current;


           return new Status(speed, voltage, batt, current, power, distance, temperature);
        }

        bmsStatusSn parseBmsSn(int bmsnum) {
            String serialNumber = new String(data,0,14);
            String versionNumber = "";
            versionNumber += String.format("%X.",(data[15]));
            versionNumber += String.format("%1X.", (data[14]>>4)&0x0f );
            versionNumber += String.format("%1X", (data[14])&0x0f );
//            for (int j = 14; j < 16; j++) {
//                versionNumber += String.format("%02X", data[j]);
//            }
            int factoryCap = this.shortFromBytes(data, 16);
            int actualCap = this.shortFromBytes(data, 18);
            int fullCycles = this.shortFromBytes(data, 22);
            int chargeCount = this.shortFromBytes(data, 24);
            int mfgDate = this.shortFromBytes(data, 32);
            int year = mfgDate>>9;
            int mounth = (mfgDate>>5) & 0x0f;
            int day = mfgDate & 0x1f;
            String mfgDateStr = String.format("%02d.%02d.20%02d", day, mounth,year);

            return new bmsStatusSn(bmsnum, serialNumber, versionNumber, factoryCap, actualCap, fullCycles, chargeCount, mfgDateStr);
        }

        bmsStatusLife parseBmsLife(int bmsnum) {
            int bmsStatus = this.shortFromBytes(data, 0);
            int remCap = this.shortFromBytes(data, 2);
            int remPerc = this.shortFromBytes(data, 4);
            int current = this.signedShortFromBytes(data, 6);
            int voltage = this.shortFromBytes(data, 8);
            int temp1 = data[10]-20;
            int temp2 = data[11]-20;
            int balanceMap = this.shortFromBytes(data, 12);
            int health = this.shortFromBytes(data, 22);

            return new bmsStatusLife(bmsnum, bmsStatus, remCap, remPerc, current, voltage, temp1, temp2, balanceMap, health);
        }

        bmsStatusCells parseBmsCells(int bmsnum) {
            int cell1 = this.shortFromBytes(data, 0);
            int cell2 = this.shortFromBytes(data, 2);
            int cell3 = this.shortFromBytes(data, 4);
            int cell4 = this.shortFromBytes(data, 6);
            int cell5 = this.shortFromBytes(data, 8);
            int cell6 = this.shortFromBytes(data, 10);
            int cell7 = this.shortFromBytes(data, 12);
            int cell8 = this.shortFromBytes(data, 14);
            int cell9 = this.shortFromBytes(data, 16);
            int cell10 = this.shortFromBytes(data, 18);
            int cell11 = this.shortFromBytes(data, 20);
            int cell12 = this.shortFromBytes(data, 22);
            int cell13 = this.shortFromBytes(data, 24);
            int cell14 = this.shortFromBytes(data, 26);
            int cell15 = this.shortFromBytes(data, 28);
            int cell16 = this.shortFromBytes(data, 30);
            return new bmsStatusCells(bmsnum, cell1, cell2, cell3, cell4, cell5, cell6, cell7, cell8, cell9, cell10, cell11, cell12, cell13, cell14, cell15, cell16);
        }


        public byte[] getData() {
            return data;
        }

        public static String toHexString(byte[] buffer) {

            String str = "[";

            boolean comma = false;

            for (int c : buffer) {

                if (comma) {
                    str += ", ";
                }

                str += String.format("%02X", (c & 0xFF));
                //comma = true;
            }

            str += "]";

            return str;
        }
    }

    public ArrayList<Status> charUpdated(byte[] data) {
        ArrayList<Status> outValues = new ArrayList<>();

        for (byte c : data) {
            if (unpacker.addChar(c)) {
                Timber.i("Starting verification");
				CANMessage result = CANMessage.verify(unpacker.getBuffer());
				
                if (result != null) { // data OK
                    Timber.i("Verification successful, command %02X", result.parameter);
                    if ((result.parameter == CANMessage.Param.Start.getValue()) && (result.source == CANMessage.Addr.Controller.getValue())) {
						Timber.i("Get start answer");
						stateCon = 1;
						
					} else if ((result.parameter == CANMessage.Param.GetKey.getValue()) && (result.source == CANMessage.Addr.KeyGenerator.getValue())){
                        Timber.i("Get encryption key");
                        gamma = result.parseKey();
                        stateCon = 2;
						//Alert alert = result.parseAlertInfoMessage();
						//if (alert != null)
						//	outValues.add(alert);
						
					} else if ((result.parameter == CANMessage.Param.SerialNumber.getValue()) && (result.source == CANMessage.Addr.Controller.getValue())){
                        Timber.i("Get serial number");
                        serialNumberStatus infos = result.parseSerialNumber();
                        stateCon = 3;
                        //Alert alert = result.parseAlertInfoMessage();
                        if (infos != null)
                        	outValues.add(infos);

                    } else if ((result.parameter == CANMessage.Param.Firmware.getValue()) && (result.source == CANMessage.Addr.Controller.getValue())){
                        Timber.i("Get version number");
                        versionStatus infos = result.parseVersionNumber();
                        stateCon = 10;
                        //Alert alert = result.parseAlertInfoMessage();
                        if (infos != null)
                            outValues.add(infos);

                    } else if ((result.parameter == CANMessage.Param.LiveData.getValue()) && (result.source == CANMessage.Addr.Controller.getValue())){
                        Timber.i("Get life data");
                        Status status = result.parseLiveData();
                        if (status != null) {
                            outValues.add(status);
                        }
                    } else if (result.source == CANMessage.Addr.BMS1.getValue()) {
                        /// BMS1
                        Timber.i("Get info from BMS1");
                        if (result.parameter == 0x10) {
                            bmsStatusSn status = result.parseBmsSn(1);
                            if (status != null) {
                                outValues.add(status);
                            }
                            stateCon = 5;
                        }
                        if (result.parameter == 0x30) {
                            bmsStatusLife status = result.parseBmsLife(1);
                            if (status != null) {
                                outValues.add(status);
                            }
                            stateCon = 6;
                        }
                        if (result.parameter == 0x40) {
                            bmsStatusCells status = result.parseBmsCells(1);
                            if (status != null) {
                                outValues.add(status);
                            }
                            stateCon = 7;
                        }

                    } else if (result.source == CANMessage.Addr.BMS2.getValue()) {
                        /// BMS2
                        Timber.i("Get info from BMS2");
                        if (result.parameter == 0x10) {
                            bmsStatusSn status = result.parseBmsSn(2);
                            if (status != null) {
                                outValues.add(status);
                            }
                            stateCon = 8;
                        }
                        if (result.parameter == 0x30) {
                            bmsStatusLife status = result.parseBmsLife(2);
                            if (status != null) {
                                outValues.add(status);
                            }
                            stateCon = 9;
                        }
                        if (result.parameter == 0x40) {
                            bmsStatusCells status = result.parseBmsCells(2);
                            if (status != null) {
                                outValues.add(status);
                            }
                            stateCon = 4;
                        }
                    }

                } 
            }
        }
        return outValues;
    }

    static class NinebotZUnpacker {

        enum UnpackerState {
            unknown,
            started,
            collecting,
            done
        }


        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int oldc = 0;
        int len = 0;
        UnpackerState state = UnpackerState.unknown;

        byte[] getBuffer() {
            return buffer.toByteArray();
        }

        boolean addChar(int c) {

            switch (state) {

                case collecting:

                    buffer.write(c);
                    if (buffer.size() == len+9) {
                        state = UnpackerState.done;
                        updateStep = 0;
                        Timber.i("Len %d", len);
                        Timber.i("Step reset");
                        return true;
                    }
                    break;

                case started:

                    buffer.write(c);
                    len = c & 0xff;
                    state = UnpackerState.collecting;

                    break;

                default:
                    if (c == (byte) 0xA5 && oldc == (byte) 0x5A) {
                        Timber.i("Find start");
                        buffer = new ByteArrayOutputStream();
                        buffer.write(0x5A);
                        buffer.write(0xA5);
                        state = UnpackerState.started;
                    }
                    oldc = c;

            }
            return false;
        }

        void reset() {
            buffer = new ByteArrayOutputStream();
            oldc = 0;
            state = UnpackerState.unknown;

        }
    }

    public static NinebotZAdapter getInstance() {
        Timber.i("Get instance");
        if (INSTANCE == null) {
            Timber.i("New instance");
            INSTANCE = new NinebotZAdapter();
        }
        return INSTANCE;
    }

    public static void newInstance() {
        if (INSTANCE != null && INSTANCE.keepAliveTimer != null) {
            INSTANCE.keepAliveTimer.cancel();
            INSTANCE.keepAliveTimer = null;

        }
        Timber.i("New instance");
        INSTANCE = new NinebotZAdapter();
    }

    public static void stopTimer() {
        if (INSTANCE != null && INSTANCE.keepAliveTimer != null) {
            INSTANCE.keepAliveTimer.cancel();
            INSTANCE.keepAliveTimer = null;
        }
        Timber.i("Kill instance, stop timer");
        INSTANCE = null;
    }

}
