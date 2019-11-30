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
public class NinebotAdapter {
    private static NinebotAdapter INSTANCE;
    private Timer keepAliveTimer;
	private boolean settingCommandReady = false;
	private static int updateStep = 0;
	private byte[] settingCommand;
	private static byte[] gamma = new byte[16];
    private static int stateCon = 0;
    private static byte protoVersion = 0;


    NinebotUnpacker unpacker = new NinebotUnpacker();

    public void startKeepAliveTimer(final BluetoothLeService mBluetoothLeService, final String ninebotPassword, final String protoVer) {
        Timber.i("Ninebot timer starting");
        if (protoVer.compareTo("S2")==0) protoVersion = 1;
        if (protoVer.compareTo("Mini")==0) protoVersion = 2;
        updateStep = 0;
        stateCon = 0;
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (updateStep == 0) {
                    if (stateCon == 0) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotAdapter.CANMessage.getSerialNumber().writeBuffer())) {
                            //stateCon +=1;
                            Timber.i("Sent serial number message");
                        } else updateStep = 39;

                    } else if (stateCon == 1) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotAdapter.CANMessage.getVersion().writeBuffer())) {
                            //stateCon +=1;
                            Timber.i("Sent serial version message");
                        } else updateStep = 39;

                    } else if (settingCommandReady) {
    					if (mBluetoothLeService.writeBluetoothGattCharacteristic(settingCommand)) {
                            //needSlowData = true;
                            settingCommandReady = false;
                            Timber.i("Sent command message");
                        } else updateStep = 39; // after +1 and %10 = 0
    				}
    				else {
                        if (!mBluetoothLeService.writeBluetoothGattCharacteristic(NinebotAdapter.CANMessage.getLiveData().writeBuffer())) {
                            Timber.i("Unable to send keep-alive message");
                            updateStep = 39;
    					} else {
                            Timber.i("Sent keep-alive message");
    					}
                    }

				}
                updateStep += 1;
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
            Controller(0x01,0x01, 0x01),
            KeyGenerator(0x16,0x16, 0x16),
            App(0x09,0x11,0x0A);

            private int value_def;
            private int value_s2;
            private int value_mini;
            Addr(int value_def, int value_s2, int value_mini) {
                    this.value_def = value_def;
                    this.value_s2 = value_s2;
                    this.value_mini = value_mini;
                }
            public int getValue() {
                if (protoVersion == 1) {
                    return value_s2;
                } else if (protoVersion == 2) {
                    return value_mini;
                } else {
                    return value_def;
                }
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

            SerialNumber(0x10),
			SerialNumber2(0x13),
			SerialNumber3(0x16),
            Firmware(0x1a),
            Angles(0x61),
            BatteryLevel(0x22),
            ActivationDate(0x69),
            LiveData(0xb0),
			LiveData2(0xb3),
			LiveData3(0xb6),
			LiveData4(0xb9),
			LiveData5(0xbc),
			LiveData6(0xbf);


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
//        int command = 0;
        int parameter = 0;
        byte[] data;
        int crc = 0;

		static int batt = 0;
        static int speed = 0;
        static int distance = 0;
        static int temperature = 0;
        static int voltage = 0;
        static int current = 0;
        static int power = 0;
		static String serialNum = "";

        CANMessage(byte[] bArr) {
            if (bArr.length < 7) return;
            len = bArr[0] & 0xff;
            source = bArr[1] & 0xff;
            destination = bArr[2] & 0xff;
//            command = bArr[3] & 0xff;
            parameter = bArr[3] & 0xff;
            data = Arrays.copyOfRange(bArr, 4, bArr.length-2);
            crc = bArr[bArr.length-1] << 8 + bArr[bArr.length-2];

        }

        private CANMessage() {

        }

        public byte[] writeBuffer() {

            byte[] canBuffer = getBytes();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(0x55);
            out.write(0xAA);

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
//            buff.write(command);
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
                return (((int)((bytes[starting + 1] & 255) << 8)) | (bytes[starting] & 255));
            }
            return 0;
        }

        public short signedShortFromBytes(byte[] bytes, int starting) {
            if (bytes.length >= starting + 2) {
                return (short) (((short) (((short) ((bytes[starting + 1] & 255))) << 8)) | (bytes[starting] & 255));
            }
            return (short) 0;
        }


        public static CANMessage getSerialNumber() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
//            msg.command = Comm.Read.getValue();
            msg.parameter = Param.SerialNumber.getValue();
            msg.data = new byte[]{0x0e};
            msg.len = msg.data.length+2;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getVersion() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
//            msg.command = Comm.Read.getValue();
            msg.parameter = Param.Firmware.getValue();
            msg.data = new byte[]{0x02};
            msg.len = msg.data.length+2;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getActivationDate() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
//            msg.command = Comm.Read.getValue();
            msg.parameter = Param.ActivationDate.getValue();
            msg.data = new byte[]{0x02};
            msg.len = msg.data.length+2;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getLiveData() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
//            msg.command = Comm.Read.getValue();
            msg.parameter = Param.LiveData.getValue();
            msg.data = new byte[]{0x20};
            msg.len = msg.data.length+2;
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
            serialNum = new String (data);//"";
			Timber.i("Serial Number: %s", serialNum);
            /*
            for (int j = 0; j < data.length; j++) {
                serialNumber += String.format("%02X", data[j]);
            }*/
            return new serialNumberStatus(serialNum);
        }
        serialNumberStatus parseSerialNumber2() {
            serialNum = serialNum + new String (data);//"";
			Timber.i("Serial Number: %s", serialNum);
            /*
            for (int j = 0; j < data.length; j++) {
                serialNumber += String.format("%02X", data[j]);
            }*/
            return new serialNumberStatus(serialNum);
        }



        versionStatus parseVersionNumber() {
            String versionNumber = String.format(Locale.US, "%d.%d.%d", data[1] >> 4, data[0] >> 4, data[0] & 0xf);
			Timber.i("Version Number: %s", versionNumber);
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
            //int speed = this.shortFromBytes(data, 10) / 10; //speed up to 32.000 km/h
            int speed =0;
            if (protoVersion == 1) speed = this.shortFromBytes(data, 28); //speed up to 320.00 km/h
            else speed = this.shortFromBytes(data, 10) / 10; //speed up to 32.000 km/h
            //12 -avg speed
            //Timber.i("NINE ADAPTER Speed: %d, speed %04X", speed, speed);
            int distance = this.intFromBytes(data,14);
            int temperature = this.shortFromBytes(data,22);
            int voltage = this.shortFromBytes(data, 24);
            if (protoVersion == 2) {
                voltage = voltage/10;
            }
            int current = this.signedShortFromBytes(data, 26);
            int power = voltage*current;
            //speed = 10;
           return new Status(speed, voltage, batt, current, power, distance, temperature);
        }

		Status parseLiveData2() {
            batt = this.shortFromBytes(data, 2);
            speed = this.shortFromBytes(data, 4) / 10;
            //speed = 10;
           return new Status(speed, voltage, batt, current, power, distance, temperature);
        }
		Status parseLiveData3() {
            distance = this.intFromBytes(data,2);;
            //speed = 10;
           return new Status(speed, voltage, batt, current, power, distance, temperature);
        }
		Status parseLiveData4() {
            temperature = this.shortFromBytes(data,4);
            //speed = 10;
           return new Status(speed, voltage, batt, current, power, distance, temperature);
        }
		Status parseLiveData5() {
            voltage = this.shortFromBytes(data, 0);
            current = this.signedShortFromBytes(data, 2);
            power = voltage*current;
            //speed = 10;
           return new Status(speed, voltage, batt, current, power, distance, temperature);
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
        Timber.i("Got data ");
        for (byte c : data) {
            if (unpacker.addChar(c)) {
                Timber.i("Starting verification");
				CANMessage result = CANMessage.verify(unpacker.getBuffer());
				
                if (result != null) { // data OK
                    Timber.i("Verification successful, command %02X", result.parameter);
                    if (result.parameter == CANMessage.Param.SerialNumber.getValue()){
                        Timber.i("Get serial number");
                        serialNumberStatus infos = result.parseSerialNumber();
                        stateCon = 1;
                        //Alert alert = result.parseAlertInfoMessage();
						if ((result.len-2)==14){						
							if (infos != null)
								outValues.add(infos);
						}
                    } else if (result.parameter == CANMessage.Param.SerialNumber2.getValue()){
                        Timber.i("Get serial number2");
                        serialNumberStatus infos = result.parseSerialNumber2();
                        //Alert alert = result.parseAlertInfoMessage();

                    } else if (result.parameter == CANMessage.Param.SerialNumber3.getValue()){
                        Timber.i("Get serial number3");
                        serialNumberStatus infos = result.parseSerialNumber2();                        
                        //Alert alert = result.parseAlertInfoMessage();
                        if (infos != null)
                            outValues.add(infos);

                    } else if (result.parameter == CANMessage.Param.Firmware.getValue()){
                        Timber.i("Get version number");
                        versionStatus infos = result.parseVersionNumber();
                        stateCon = 2;
                        //Alert alert = result.parseAlertInfoMessage();
                        if (infos != null)
                            outValues.add(infos);

                    } else if (result.parameter == CANMessage.Param.LiveData.getValue()){
                        Timber.i("Get life data1");
						if (result.len-2==32) {
							Status status = result.parseLiveData();
							if (status != null) {
								outValues.add(status);
							}
						}
                    } else if (result.parameter == CANMessage.Param.LiveData2.getValue()){
                        Timber.i("Get life data2");
                        Status status = result.parseLiveData2();
                    } else if (result.parameter == CANMessage.Param.LiveData3.getValue()){
                        Timber.i("Get life data3");
                        Status status = result.parseLiveData3();
                       
                    } else if (result.parameter == CANMessage.Param.LiveData4.getValue()){
                        Timber.i("Get life data4");
                        Status status = result.parseLiveData4();
                       
                    } else if (result.parameter == CANMessage.Param.LiveData5.getValue()){
                        Timber.i("Get life data5");
                        Status status = result.parseLiveData5();
                        if (status != null) {
                            outValues.add(status);
                        }
                    } else if (result.parameter == CANMessage.Param.LiveData6.getValue()){
                        Timber.i("Get life data");
//                        Status status = result.parseLiveData6();
                        
                    }

                } 
            }
        }
        return outValues;
    }

    static class NinebotUnpacker {

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
                    if (buffer.size() == len+6) {
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
                    if (c == (byte) 0xAA && oldc == (byte) 0x55) {
                        Timber.i("Find start");
                        buffer = new ByteArrayOutputStream();
                        buffer.write(0x55);
                        buffer.write(0xAA);
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

    public static NinebotAdapter getInstance() {
        Timber.i("Get instance");
        if (INSTANCE == null) {
            Timber.i("New instance");
            INSTANCE = new NinebotAdapter();
        }
        return INSTANCE;
    }

    public static void newInstance() {
        if (INSTANCE != null && INSTANCE.keepAliveTimer != null) {
            INSTANCE.keepAliveTimer.cancel();
            INSTANCE.keepAliveTimer = null;

        }
        Timber.i("New instance");
        INSTANCE = new NinebotAdapter();
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
