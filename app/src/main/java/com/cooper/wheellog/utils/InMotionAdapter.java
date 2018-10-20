package com.cooper.wheellog.utils;

import com.cooper.wheellog.BluetoothLeService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import timber.log.Timber;

import static com.cooper.wheellog.utils.InMotionAdapter.Model.*;

/**
 * Created by cedric on 29/12/2016.
 */
public class InMotionAdapter {
    private static InMotionAdapter INSTANCE;
    private Timer keepAliveTimer;
    private boolean passwordSent = false;
	private boolean needSlowData = true;
	private boolean settingCommandReady = false;
	private static int updateStep = 0;
	private byte[] settingCommand;

    enum Mode {
        rookie(0),
        general(1),
        smoothly(2),
        unBoot(3),
        bldc(4),
        foc(5);

        private int value;

        Mode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

    }


    public enum Model {

        R1N("0", 3812.0d),
        R1S("1", 1000.0d),
        R1CF("2", 3812.0d),
        R1AP("3", 3812.0d),
        R1EX("4", 3812.0d),
        R1Sample("5", 1000.0d),
        R1T("6", 3810.0d),
        R10("7", 3812.0d),
        V3("10", 3812.0d),
        V3C("11", 3812.0d),
        V3PRO("12", 3812.0d),
        V3S("13", 3812.0d),
        R2N("21", 3812.0d),
        R2S("22", 3812.0d),
        R2Sample("23", 3812.0d),
        R2("20", 3812.0d),
        R2EX("24", 3812.0d),
        R0("30", 1000.0d),
        L6("60", 3812.0d),
        Lively("61", 3812.0d),
        V5("50", 3812.0d),
        V5PLUS("51", 3812.0d),
        V5F("52", 3812.0d),
        V5FPLUS("53", 3812.0d),
        V8("80", 3812.0d),
        Glide3("85", 3812.0d),
        V10_test("100", 3812.0d),
        V10F_test("101", 3812.0d),
        V10("140", 3812.0d),
        V10F("141", 3812.0d),
        UNKNOWN("x", 3812.0d);

        private String value;
        private double speedCalculationFactor;

        Model(String value, double speedCalculationFactor) {
            this.value = value;
            this.speedCalculationFactor = speedCalculationFactor;
        }

        public String getValue() {
            return value;
        }

        public double getSpeedCalculationFactor() {
            return speedCalculationFactor;
        }

        public boolean belongToInputType(String type) {
            if ("0".equals(type)) {
                return value.length() == 1;
            } else return value.substring(0, 1).equals(type) && value.length() == 2;
        }

        public static Model findById(String id) {
            Timber.i("Model %s", id);
            for (Model m : Model.values()) {
                if (m.getValue().equals(id)) return m;
            }
            return Model.UNKNOWN;
            //return Model.V8;
        }

        public static Model findByBytes(byte[] data) {
            StringBuilder stringBuffer = new StringBuilder();
            if (data.length >= 108) {
                if (data[107] > (byte) 0) {
                    stringBuffer.append(data[107]);
                    //stringBuffer.append(0x0a);
                }
                stringBuffer.append(data[104]);
                //stringBuffer.append(0x01);
            }

            return Model.findById(stringBuffer.toString());
        }
    }


    enum WorkMode {
        idle(0),
        drive(1),
        zero(2),
        largeAngle(3),
        checkc(4),
        lock(5),
        error(6),
        carry(7),
        remoteControl(8),
        shutdown(9),
        pomStop(10),
        unknown(11),
        unlock(12);

        private int value;

        WorkMode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    Model model = Model.UNKNOWN;
    InMotionUnpacker unpacker = new InMotionUnpacker();

    public void startKeepAliveTimer(final BluetoothLeService mBluetoothLeService, final String inmotionPassword) {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (updateStep == 0) {
                    if (!passwordSent) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(InMotionAdapter.CANMessage.getPassword(inmotionPassword).writeBuffer())) {
                            passwordSent = true;
                            Timber.i("Sent password message");
                        } else updateStep = 39;

                    } else if ((model == UNKNOWN) | needSlowData ) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(InMotionAdapter.CANMessage.getSlowData().writeBuffer())) {
                            Timber.i("Sent infos message");
                        } else updateStep = 39;

                    } else if (settingCommandReady) {
    					if (mBluetoothLeService.writeBluetoothGattCharacteristic(settingCommand)) {
                            needSlowData = true;
                            settingCommandReady = false;
                            Timber.i("Sent command message");
                        } else updateStep = 39; // after +1 and %10 = 0
    				}
    				else {
                        if (!mBluetoothLeService.writeBluetoothGattCharacteristic(CANMessage.standardMessage().writeBuffer())) {
                            Timber.i("Unable to send keep-alive message");
                            updateStep = 39;
    					} else {
                            Timber.i("Sent keep-alive message");
    					}
                    }

				}
                updateStep += 1;
                updateStep %= 40;
                Timber.i("Step: %d", updateStep);
            }
        };
        keepAliveTimer = new Timer();
        keepAliveTimer.scheduleAtFixedRate(timerTask, 0, 25);
    }
	

	public void resetConnection() {
		passwordSent = false;
	}
	
	public void setLightState(final boolean lightEnable) {
		settingCommandReady = true;
		//needSlowData = true;
		//mBluetoothLeService.writeBluetoothGattCharacteristic(InMotionAdapter.CANMessage.setLight(lightEnable).writeBuffer());
		settingCommand = InMotionAdapter.CANMessage.setLight(lightEnable).writeBuffer();
	}
	public void setLedState(final boolean ledEnable) {
		settingCommandReady = true;
		//needSlowData = true;
		//mBluetoothLeService.writeBluetoothGattCharacteristic(InMotionAdapter.CANMessage.setLed(ledEnable).writeBuffer());
		settingCommand = InMotionAdapter.CANMessage.setLed(ledEnable).writeBuffer();
	}
	public void setHandleButtonState(final boolean handleButtonEnable) {
		settingCommandReady = true;
		//needSlowData = true;
		//mBluetoothLeService.writeBluetoothGattCharacteristic(InMotionAdapter.CANMessage.setHandleButton(handleButtonEnable).writeBuffer());
		settingCommand = InMotionAdapter.CANMessage.setHandleButton(handleButtonEnable).writeBuffer();
	}
	public void setMaxSpeedState(final int maxSpeed) {
		settingCommandReady = true;
		//needSlowData = true;
		//mBluetoothLeService.writeBluetoothGattCharacteristic(InMotionAdapter.CANMessage.setMaxSpeed(maxSpeed).writeBuffer());
		settingCommand = InMotionAdapter.CANMessage.setMaxSpeed(maxSpeed).writeBuffer();
	}
	public void setSpeakerVolumeState(final int speakerVolume) {
		settingCommandReady = true;
		//needSlowData = true;
		//mBluetoothLeService.writeBluetoothGattCharacteristic(InMotionAdapter.CANMessage.setSpeakerVolume(speakerVolume).writeBuffer());
		settingCommand = InMotionAdapter.CANMessage.setSpeakerVolume(speakerVolume).writeBuffer();
	}
	
	public void setTiltHorizon(final int tiltHorizon) {
		settingCommandReady = true;
		//needSlowData = true;
		//mBluetoothLeService.writeBluetoothGattCharacteristic(InMotionAdapter.CANMessage.setTiltHorizon(tiltHorizon).writeBuffer());
		settingCommand = InMotionAdapter.CANMessage.setTiltHorizon(tiltHorizon).writeBuffer();
		
	}

    static Mode intToMode(int mode) {
        if ((mode & 16) != 0) {
            return Mode.rookie;
        } else if ((mode & 32) != 0) {
            return Mode.general;
        } else if (((mode & 64) == 0) || ((mode & 128) == 0)) {
            return Mode.unBoot;
        } else {
            return Mode.smoothly;
        }
    }


    static Mode intToModeWithL6(int mode) {
        if ((mode & 15) != 0) {
            return Mode.bldc;
        } else {
            return Mode.foc;
        }
    }

    static WorkMode intToWorkModeWithL6(int mode) {
        if ((mode & 240) != 0) {
            return WorkMode.lock;
        } else {
            return WorkMode.unlock;
        }
    }


    static WorkMode intToWorkMode(int mode) {

        int v = mode & 0xF;

        switch (v) {

            case 0:
                return WorkMode.idle;

            case 1:
                return WorkMode.drive;

            case 2:
                return WorkMode.zero;

            case 3:
                return WorkMode.largeAngle;

            case 4:
                return WorkMode.checkc;

            case 5:
                return WorkMode.lock;

            case 6:
                return WorkMode.error;

            case 7:
                return WorkMode.carry;

            case 8:
                return WorkMode.remoteControl;

            case 9:
                return WorkMode.shutdown;

            case 16:
                return WorkMode.pomStop;

            default:
                return WorkMode.unknown;
        }

    }

    static double batteryFromVoltage(double volts, Model model) {

        double batt;

        if (model.belongToInputType("1") || model == R0) {

            if (volts >= 82.50) {
                batt = 1.0;
            } else if (volts > 68.0) {
                batt = (volts - 68.0) / 14.50;
            } else {
                batt = 0.0;
            }
        } else if (model.belongToInputType( "5") || model == Model.V8 || model == Model.Glide3 || model == Model.V10 || model == Model.V10F || model == Model.V10_test || model == Model.V10F_test) {//            if (volts > 84.00) {
            if (volts > 82.50) {
                batt = 1.0;
            } else if (volts > 68.0) {
                batt = (volts - 68.0) / 14.5;
            } else {
                batt = 0.0;
            }

//                batt = 1.0;
//            } else if (volts > 68.5) {
//                batt = (volts - 68.5) / 15.5;
//            } else {
//                batt = 0.0;
//            }
        } else if (model.belongToInputType("6")) {
            batt = 0.0;
        } else {
            if (volts >= 82.00) {
                batt = 1.0;
            } else if (volts > 77.8) {
                batt = ((volts - 77.8) / 4.2) * 0.2 + 0.8;
            } else if (volts > 74.8) {
                batt = ((volts - 74.8) / 3.0) * 0.2 + 0.6;
            } else if (volts > 71.8) {
                batt = ((volts - 71.8) / 3.0) * 0.2 + 0.4;
            } else if (volts > 70.3) {
                batt = ((volts - 70.3) / 1.5) * 0.2 + 0.2;
            } else if (volts > 68.0) {
                batt = ((volts - 68.0) / 2.3) * 0.2;
            } else {
                batt = 0.0;
            }

        }
        return batt * 100.0;

    }


	
    public static class Status {
        private final double angle;
		private final double roll;
        private final double speed;
        private final double voltage;
        private final double batt;
        private final double current;
        private final double power;
        private final double distance;
        private final double lock;
		private final double temperature;
		private final double temperature2;
		private final int workModeInt;

        Status() {
            angle = 0;
			roll = 0;
            speed = 0;
            voltage = 0;
            batt = 0;
            current = 0;
            power = 0;
            distance = 0;
            lock = 0;
			temperature = 0;
			temperature2 = 0;
			workModeInt=0;
        }

        Status(double angle, double roll, double speed, double voltage, double batt, double current, double power, double distance, double lock, double temperature, double temperature2, int workModeInt) {
            this.angle = angle;
			this.roll = roll;
            this.speed = speed;
            this.voltage = voltage;
            this.batt = batt;
            this.current = current;
            this.power = power;
            this.distance = distance;
            this.lock = lock;
			this.temperature = temperature;
			this.temperature2 = temperature2;
			this.workModeInt = workModeInt;
        }	

        public double getAngle() {
            return angle;
        }
		
        public double getRoll() {
            return roll;
        }

        public double getSpeed() {
            return speed;
        }

        public double getVoltage() {
            return voltage;
        }

        public double getBatt() {
            return batt;
        }

        public double getCurrent() {
            return current;
        }

        public double getPower() {
            return power;
        }

        public double getDistance() {
            return distance;
        }

        public double getLock() {
            return lock;
        }
		public double getTemperature() {
            return temperature;
        }
		public double getTemperature2() {
            return temperature2;
        }
		public String getWorkModeString() {
            switch(workModeInt) {
				case 0: return "Idle";
				case 1: return "Drive";
				case 2: return "Zero";
				case 3: return "LargeAngle";
				case 4: return "Checkc";
				case 5: return "Lock";
				case 6: return "Error";
				case 7: return "Carry";
				case 8: return "RemoteControl";
				case 9: return "Shutdown";
				case 10: return "pomStop";
				case 11: return "Unknown";
				case 12: return "Unlock";
				default: return "Unknown";
			}
        }

        @Override
        public String toString() {
            return "Status{" +
                    "angle=" + angle +
					", roll=" + roll +
                    ", speed=" + speed +
                    ", voltage=" + voltage +
                    ", batt=" + batt +
                    ", current=" + current +
                    ", power=" + power +
                    ", distance=" + distance +
                    ", lock=" + lock +
					", temperature=" + temperature +
					", temperature2=" + temperature2 +
					", workmode=" + workModeInt +
                    '}';
        }
    }
	
	public static class Alert extends Status {
		private final int alertId;
		private final double alertValue;
		private final double alertValue2;
		private final String fullText;

		
		Alert() {
			super();
			alertId = 0;
			alertValue = 0;
			alertValue2 = 0;
			fullText = "";
	
		}
		
		Alert(int alertId, double alertValue, double alertValue2, String fullText) {
			super();
			this.alertId = alertId;
			this.alertValue = alertValue;
			this.alertValue2 = alertValue2;
			this.fullText = fullText;

		}
		
		public String getfullText() {
			return fullText;
			
        }
		
		
	}
	
    public static class Infos extends Status {
        private final String serialNumber;
        private final Model model;
        private final String version;
		private final boolean light;
		private final boolean led;
		private final boolean handleButtonDisabled;
		private final int maxSpeed;
		private final int speakerVolume;
		private final int tiltHorizon;
		

        Infos(String serialNumber, Model model, String version, boolean light, boolean led, boolean handleButtonDisabled, int maxSpeed, int speakerVolume, int tiltHorizon) {
            super();
            this.serialNumber = serialNumber;
            this.model = model;
            this.version = version;
			this.light = light;
			this.led = led;
			this.handleButtonDisabled = handleButtonDisabled;
			this.maxSpeed = maxSpeed;
			this.speakerVolume = speakerVolume;
			this.tiltHorizon = tiltHorizon;
        }

		public boolean getLightState() {
            return light;
        }
		
		public boolean getLedState() {
            return led;
        }
		
		public boolean getHandleButtonState() {
            return handleButtonDisabled;
        }
		
		public int getMaxSpeedState() {
            return maxSpeed;
        }
		
		public int getSpeakerVolumeState() {
            return speakerVolume;
        }
		
        public String getSerialNumber() {
            return serialNumber;
        }

        public Model getModel() {
            return model;
        }
		
		public int getTiltHorizon() {
            return tiltHorizon;
        }
		
		public String getModelString() {
            switch (model.getValue()) {
				case "0": return "Inmotion R1N";
				case "1": return "Inmotion R1S";
				case "2": return "Inmotion R1CF";
				case "3": return "Inmotion R1AP";
				case "4": return "Inmotion R1EX";
				case "5": return "Inmotion R1Sample";
				case "6": return "Inmotion R1T";
				case "7": return "Inmotion R10";
				case "10": return "Inmotion V3";
				case "11": return "Inmotion V3C";
				case "12": return "Inmotion V3PRO";
				case "13": return "Inmotion V3S";
				case "21": return "Inmotion R2N";
				case "22": return "Inmotion R2S";
				case "23": return "Inmotion R2Sample";
				case "20": return "Inmotion R2";
				case "24": return "Inmotion R2EX";
				case "30": return "Inmotion R0";
				case "60": return "Inmotion L6";
				case "61": return "Inmotion Lively";
				case "50": return "Inmotion V5";
				case "51": return "Inmotion V5PLUS";
				case "52": return "Inmotion V5F";
				case "53": return "Inmotion V5FPLUS";
				case "80": return "Inmotion V8";
                case "85": return "Solowheel Glide 3";
                case "100": return "Inmotion V10 test";
                case "101": return "Inmotion V10F test";
                case "140": return "Inmotion V10";
                case "141": return "Inmotion V10F";
				default: return "Unknown";
			}
        }

        public String getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return "Infos{" +
                    "serialNumber='" + serialNumber + '\'' +
                    ", model=" + model +
                    ", version='" + version + '\'' +
                    ", light='" + light + '\'' +
                    ", led='" + led + '\'' +
                    ", handleButton='" + handleButtonDisabled + '\'' +
                    ", maxspeed='" + maxSpeed + '\'' +
                    ", speakervolume='" + speakerVolume + '\'' +
					", pedals='" + tiltHorizon + '\'' +
                    '}';
        }
    }
	

    /**
     * Created by cedric on 29/12/2016.
     */
    public static class CANMessage {
        enum CanFormat {
            StandardFormat(0),
            ExtendedFormat(1);

            private int value;

            CanFormat(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        enum CanFrame {
            DataFrame(0),
            RemoteFrame(1);

            private int value;

            CanFrame(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        enum IDValue {
            NoOp(0),
            GetFastInfo(0x0F550113),
            GetSlowInfo(0x0F550114),
            //RemoteControl(0x0F550116),
            RideMode(0x0F550115),
            PinCode(0x0F550307),
			Light(0x0F55010D),  
			Led(0x0F550116),  
			HandleButton(0x0F55012E),  
			MaxSpeed(0x0F550115),  
			SpeakerVolume(0x0F55060A),  
			Alert(0x0F780101);

            private int value;

            IDValue(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        int id = IDValue.NoOp.getValue();
        byte[] data = new byte[8];
        int len = 0;
        int ch = 0;
        int format = CanFormat.StandardFormat.getValue();
        int type = CanFrame.DataFrame.getValue();
        byte[] ex_data;

        CANMessage(byte[] bArr) {
            if (bArr.length < 16) return;
            id = (((bArr[3] * 256) + bArr[2]) * 256 + bArr[1]) * 256 + bArr[0];
            data = Arrays.copyOfRange(bArr, 4, 12);
            len = bArr[12];
            ch = bArr[13];
            format = bArr[14] == 0 ? CanFormat.StandardFormat.getValue() : CanFormat.ExtendedFormat.getValue();
            type = bArr[15] == 0 ? CanFrame.DataFrame.getValue() : CanFrame.RemoteFrame.getValue();

            if (len == (byte) 0xFE) {
                int ldata = this.intFromBytes(data, 0);

                if (ldata == bArr.length - 16) {
                    ex_data = Arrays.copyOfRange(bArr, 16, 16 + ldata);
                }
            }

        }

        private CANMessage() {

        }

        public byte[] writeBuffer() {

            byte[] canBuffer = getBytes();
            byte check = computeCheck(canBuffer);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(0xAA);
            out.write(0xAA);

            try {
                out.write(escape(canBuffer));
            } catch (IOException e) {
                e.printStackTrace();
            }
            out.write(check);
            out.write(0x55);
            out.write(0x55);

            return out.toByteArray();
        }


        private byte[] getBytes() {

            ByteArrayOutputStream buff = new ByteArrayOutputStream();

            int b3 = id / (256 * 256 * 256);
            int b2 = (id - b3 * 256 * 256 * 256) / (256 * 256);

            int b1 = (id - b3 * 256 * 256 * 256 - b2 * 256 * 256) / 256;
            int b0 = id % 256;

            buff.write(b0);
            buff.write(b1);
            buff.write(b2);
            buff.write(b3);

            try {
                buff.write(data);
                buff.write(len);
                buff.write(ch);
            } catch (IOException e) {
                e.printStackTrace();
            }

            buff.write(format == CanFormat.StandardFormat.getValue() ? 0 : 1);
            buff.write(type == CanFrame.DataFrame.getValue() ? 0 : 1);

            if (len == (byte) 0xFE) {
                try {
                    buff.write(ex_data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return buff.toByteArray();
        }

        public void clearData() {
            data = new byte[data.length];
        }

        private static byte computeCheck(byte[] buffer) {

            int check = 0;
            for (byte c : buffer) {
                check = (check + c) & 0xFF;
				//check = (check + (int) c) % 256;
            }
            return (byte) check;
        }

        private int intFromBytes(byte[] bytes, int starting) {
            if (bytes.length >= starting + 4) {
                return (((((((bytes[starting + 3] & 255)) << 8) | (bytes[starting + 2] & 255)) << 8) | (bytes[starting + 1] & 255)) << 8) | (bytes[starting] & 255);
            }
            return 0;
        }

        private long longFromBytes(byte[] bytes, int starting) {
            if (bytes.length >= starting + 8) {
                return ((((((((((((((((long) (bytes[starting + 7] & 255))) << 8) | ((long) (bytes[starting + 6] & 255))) << 8) | ((long) (bytes[starting + 5] & 255))) << 8) | ((long) (bytes[starting + 4] & 255))) << 8) | ((long) (bytes[starting + 3] & 255))) << 8) | ((long) (bytes[starting + 2] & 255))) << 8) | ((long) (bytes[starting + 1] & 255))) << 8) | ((long) (bytes[starting] & 255));
            }
            return 0;
        }

        long signedIntFromBytes(byte[] bytes, int starting) {
            if (bytes.length >= starting + 4) {
                return (((((((bytes[starting + 3] & 255)) << 8) | (bytes[starting + 2] & 255)) << 8) | (bytes[starting + 1] & 255)) << 8) | (bytes[starting] & 255);
            }
            return 0;
        }

        public static short shortFromBytes(byte[] bytes, int starting) {
            if (bytes.length >= starting + 2) {
                return (short) (((short) (((short) ((bytes[starting + 1] & 255))) << 8)) | (bytes[starting] & 255));
            }
            return (short) 0;
        }


        static CANMessage verify(byte[] buffer) {

            if (buffer[0] != (byte) 0xAA || buffer[1] != (byte) 0xAA || buffer[buffer.length - 1] != (byte) 0x55 || buffer[buffer.length - 2] != (byte) 0x55) {
                return null;  // Header and tail not correct
            }
            Timber.i("Before escape %s", CANMessage.toHexString(buffer));
            byte[] dataBuffer = Arrays.copyOfRange(buffer, 2, buffer.length - 3);

            dataBuffer = CANMessage.unescape(dataBuffer);
            Timber.i("After escape %s", CANMessage.toHexString(dataBuffer));
            byte check = CANMessage.computeCheck(dataBuffer);

            byte bufferCheck = buffer[buffer.length - 3];
            if (check == bufferCheck) {
                Timber.i("Check OK");
            } else {
                Timber.i("Check FALSE, calc: %02X, packet: %02X",check, bufferCheck);
            }
            return (check == bufferCheck) ? new CANMessage(dataBuffer) : null;

        }


        private byte[] escape(byte[] buffer) {

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            for (byte c : buffer) {
                if (c == (byte) 0xAA || c == (byte) 0x55 || c == (byte) 0xA5) {
                    out.write(0xA5);
                }
                out.write(c);
            }

            return out.toByteArray();
        }

        private static byte[] unescape(byte[] buffer) {

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            boolean oldca5 = false;

            for (byte c : buffer) {
                if (c != (byte) 0xA5 || oldca5)  {
                    out.write(c);
                    oldca5=false;
                } else {
                    oldca5 = true;
                }
            }
            return out.toByteArray();
        }

        public static CANMessage standardMessage() {
            CANMessage msg = new CANMessage();

            msg.len = 8;
            msg.id = IDValue.GetFastInfo.getValue();
            msg.ch = 5;
            msg.data = new byte[]{-1, -1, -1, -1, -1, -1, -1, -1};
            return msg;
        }

        public static CANMessage getFastData() {
            CANMessage msg = new CANMessage();

            msg.len = 8;
            msg.id = IDValue.GetFastInfo.getValue();
            msg.ch = 5;
            msg.data = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

            return msg;
        }

        public static CANMessage getSlowData() {
            CANMessage msg = new CANMessage();

            msg.len = 8;
            msg.id = IDValue.GetSlowInfo.getValue();
            msg.ch = 5;
            msg.type = CanFrame.RemoteFrame.getValue();
            msg.data = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

            return msg;
        }
		
        public static CANMessage setLight(boolean on) {
            CANMessage msg = new CANMessage();
			byte enable = 0;
			if (on) {
				enable = 1;
			}
		    msg.len = 8;
            msg.id = IDValue.Light.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
			msg.data = new byte[]{enable, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
			
            return msg;
        }
		
		public static CANMessage setLed(boolean on) {
            CANMessage msg = new CANMessage();
			byte enable = 0x10;
			if (on) {
				enable = 0x0F;
			}
		    msg.len = 8;
            msg.id = IDValue.Led.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
			msg.data = new byte[]{(byte) 0xB2, (byte) 0x00, (byte) 0x00, (byte) 0x00, enable, (byte) 0x00, (byte) 0x00, (byte) 0x00};
			
            return msg;
        }

		public static CANMessage setHandleButton(boolean on) {
            CANMessage msg = new CANMessage();
			byte enable = 1;
			if (on) {
				enable = 0;
			}
		    msg.len = 8;
            msg.id = IDValue.HandleButton.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
			msg.data = new byte[]{enable, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
			
            return msg;
        }
		
		public static CANMessage setMaxSpeed(int maxSpeed) {
            CANMessage msg = new CANMessage();
			int lowByte = (maxSpeed * 1000)&0xFF;
			int highByte = ((maxSpeed * 1000)/0x100)&0xFF;
		    msg.len = 8;
            msg.id = IDValue.MaxSpeed.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
			msg.data = new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) lowByte, (byte) highByte, (byte) 0x00, (byte) 0x00};
			
            return msg;
        }

        public static CANMessage setRideMode(int rideMode) {
            /// rideMode =0 -Comfort, =1 -Classic
            CANMessage msg = new CANMessage();
            msg.len = 8;
            msg.id = IDValue.RideMode.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
            msg.data = new byte[]{(byte) 0x0a, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) rideMode, (byte) 0x00 , (byte) 0x00, (byte) 0x00};

            return msg;
        }
		
		public static CANMessage setSpeakerVolume(int speakerVolume) {
            CANMessage msg = new CANMessage();
			int lowByte = (speakerVolume * 100)&0xFF;
			int highByte = ((speakerVolume * 100)/0x100)&0xFF;
		    msg.len = 8;
            msg.id = IDValue.SpeakerVolume.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
			msg.data = new byte[]{(byte) lowByte, (byte) highByte, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
			
            return msg;
        }
		
		public static CANMessage setTiltHorizon(int tiltHorizon) {
            CANMessage msg = new CANMessage();
			int tilt = (tiltHorizon * 65536)/10;
			int llowByte = (tilt)&0xFF;
			int lhighByte = (tilt >> 8)&0xFF;
			int hlowByte = (tilt >> 16)&0xFF;
			int hhighByte = (tilt >> 24)&0xFF;
		    msg.len = 8;
            msg.id = IDValue.MaxSpeed.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
			msg.data = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) llowByte, (byte) lhighByte, (byte) hlowByte, (byte) hhighByte};

            return msg;
        }
		
		
        public static CANMessage getBatteryLevelsdata() {
            CANMessage msg = new CANMessage();

            msg.len = 8;
            msg.id = IDValue.GetSlowInfo.getValue();
            msg.ch = 5;
            msg.type = CanFrame.RemoteFrame.getValue();
            msg.data = new byte[]{0, 0, 0, 15, 0, 0, 0, 0};

            return msg;
        }

        public static CANMessage getVersion() {
            CANMessage msg = new CANMessage();

            msg.len = 8;
            msg.id = IDValue.GetSlowInfo.getValue();
            msg.ch = 5;
            msg.type = CanFrame.RemoteFrame.getValue();
            msg.data = new byte[]{32, 0, 0, 0, 0, 0, 0, 0};

            return msg;
        }

        public static CANMessage getPassword(String password) {
            CANMessage msg = new CANMessage();

            msg.len = 8;
            msg.id = IDValue.PinCode.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
            byte[] pass = password.getBytes();
            msg.data = new byte[]{pass[0], pass[1], pass[2], pass[3], pass[4], pass[5], 0, 0};

            return msg;
        }

        public static CANMessage setMode(int mode) {
            CANMessage msg = new CANMessage();

            msg.len = 8;
            msg.id = IDValue.NoOp.getValue();
            msg.ch = 5;
            msg.type = CanFrame.DataFrame.getValue();
            msg.data = new byte[]{(byte) 0xB2, 0, 0, 0, (byte) mode, 0, 0, 0};

            return msg;
        }

        Status parseFastInfoMessage(Model model) {
            if (ex_data == null) return null;
            double angle = (double) (this.intFromBytes(ex_data, 0)) / 65536.0;
			double roll = (double) (this.intFromBytes(ex_data, 72)) / 90.0;
            double speed = ((double) (this.signedIntFromBytes(ex_data, 12)) + (double) (this.signedIntFromBytes(ex_data, 16))) / (model.getSpeedCalculationFactor() * 2.0);
            //if (model == R1S || model == R1Sample || model == R0 || model == V8) {
            speed = Math.abs(speed);
            //}
            double voltage = (double) (this.intFromBytes(ex_data, 24)) / 100.0;
            double current = (double) (this.signedIntFromBytes(ex_data, 20)) / 100.0;
			double temperature = ex_data[32] & 0xff;
			double temperature2 = ex_data[34] & 0xff;
            double batt = batteryFromVoltage(voltage, model);
            double power = voltage * current;

            double distance;

            if (model.belongToInputType( "1") || model.belongToInputType( "5") ||
                    model == V8 || model == Glide3 || model == V10 || model == V10F || model == V10_test || model == V10F_test) {
                distance = (double) (this.intFromBytes(ex_data, 44)) / 1000.0d; ///// V10F 48 byte - trip distance
            } else if (model == R0) {
                distance = (double) (this.longFromBytes(ex_data, 44)) / 1000.0d;

            } else if (model == L6) {
                distance = (double) (this.longFromBytes(ex_data, 44)) / 10.0;

            } else {
                distance = (double) (this.longFromBytes(ex_data, 44)) / 5.711016379455429E7d;
            }
			int workModeInt = this.intFromBytes(ex_data, 60)&0xF;
            WorkMode workMode = intToWorkMode(workModeInt);
            double lock = 0.0;
            switch (workMode) {

                case lock:
                    lock = 1.0;

                default:
                    break;
            }

            return new Status(angle, roll, speed, voltage, batt, current, power, distance, lock, temperature, temperature2, workModeInt);
        }

        // Return SerialNumber, Model, Version
		Alert parseAlertInfoMessage() {
			int alertId = (int)data[0];
			double alertValue = (double)((data[3] * 256) | (data[2] & 0xFF));
			//double alertValue2 = (double)(this.intFromBytes(data, 4));
			double alertValue2 = (double)((data[7]*256*256*256) | ((data[6]&0xFF)*256*256) | ((data[5]&0xFF)*256) | (data[4]&0xFF));
			double a_speed = Math.abs((alertValue2/3812.0) * 3.6);
			String fullText = "";
			
			String hex = "[";
            for (int c : data) {
                hex += String.format("%02X", (c & 0xFF));             
            }
			hex +="]";
			
			switch (alertId) {
				
				case 0x05:  
					fullText = String.format(Locale.ENGLISH, "Start from tilt angle %.2f at speed %.2f %s", (alertValue/100.0), a_speed, hex);
					break;
				case 0x06:
					fullText = String.format(Locale.ENGLISH, "Tiltback at speed %.2f at limit %.2f %s", a_speed, (alertValue/1000.0), hex);
					break;
				case 0x19:
					fullText = String.format(Locale.ENGLISH, "Fall Down %s", hex);
					break;
				case 0x20:
					fullText = String.format(Locale.ENGLISH, "Low battery at voltage %.2f %s", (alertValue2/100.0), hex);
					break;
				case 0x21:
					fullText = String.format(Locale.ENGLISH, "Speed cut-off at speed %.2f and something %.2f %s", a_speed, (alertValue/10.0), hex);
					break;
				case 0x26:
					fullText = String.format(Locale.ENGLISH, "High load at speed %.2f and current %.2f %s", a_speed, (alertValue/1000.0), hex);
					break;
                case 0x1d:
                    fullText = String.format(Locale.ENGLISH, "Please repair: bad battery cell found. At voltage %.2f %s", (alertValue2/100.0), hex);
                    break;
				default: 
					fullText = String.format(Locale.ENGLISH, "Unknown Alert %.2f %.2f, please contact palachzzz, hex %s", alertValue, alertValue2, hex);
			}
			return new Alert(alertId, alertValue, alertValue2, fullText);
			
		}
		
		
        Infos parseSlowInfoMessage() {
            if (ex_data == null) return null;
            Model model = Model.findByBytes(ex_data);  // CarType is just model.rawValue

			//model = V8;
            //int v = this.intFromBytes(ex_data, 24);
            int v0 = ex_data[27]&0xFF;
            int v1 = ex_data[26]&0xFF;
            int v2 = ((ex_data[25]&0xFF)*256) | (ex_data[24]&0xFF);
            String version = String.format(Locale.ENGLISH, "%d.%d.%d", v0, v1, v2);
            String serialNumber = "";
			//System.out.println(CANMessage.toHexString(ex_data));
			int maxspeed = 0;
			int speakervolume = 0;
			boolean light = false;
			boolean led = false;
			boolean handlebutton = false;
			int pedals = (int)(Math.round((this.intFromBytes(ex_data, 56)) / 6553.6));
			maxspeed = (((ex_data[61]&0xFF)*256) | (ex_data[60]&0xFF))/1000;
			light = (ex_data[80] == 1) ? true : false;
			if (ex_data.length > 126) {
				speakervolume = (((ex_data[126]&0xFF)*256) | (ex_data[125]&0xFF))/100;				
			}
			if (ex_data.length > 130) {
				led = (ex_data[130] == 1) ? true : false;				
			}
			if (ex_data.length > 129) {
				handlebutton = (ex_data[129] == 1) ? false : true;	
			}				
			
            for (int j = 0; j < 8; j++) {
                serialNumber += String.format("%02X", ex_data[7 - j]);
            }
            return new Infos(serialNumber, model, version, light, led, handlebutton, maxspeed, speakervolume, pedals);
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
                
				CANMessage result = CANMessage.verify(unpacker.getBuffer());
				
                if (result != null) { // data OK
					
                    if (result.id == CANMessage.IDValue.GetFastInfo.getValue()) {
						Status vals = result.parseFastInfoMessage(model);
                        if (vals != null)
                            outValues.add(vals);
						
					} else if (result.id == CANMessage.IDValue.Alert.getValue()){
						Alert alert = result.parseAlertInfoMessage();
						if (alert != null)
							outValues.add(alert);
						
					} else if (result.id == CANMessage.IDValue.GetSlowInfo.getValue()){
                        Infos infos = result.parseSlowInfoMessage();
                        if (infos != null) {
							needSlowData = false;
                            model = infos.getModel();
                            outValues.add(infos);
                        }
                    }					
                } 
            }
        }
        return outValues;
    }

    static class InMotionUnpacker {

        enum UnpackerState {
            unknown,
            collecting,
            done
        }


        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int oldc = 0;
        UnpackerState state = UnpackerState.unknown;

        byte[] getBuffer() {
            return buffer.toByteArray();
        }

        boolean addChar(int c) {

            switch (state) {

                case collecting:

                    buffer.write(c);
                    if (c == (byte) 0x55 && oldc == (byte) 0x55) {
                        state = UnpackerState.done;
                        updateStep = 0;
                        oldc = c;
                        Timber.i("Step reset");
                        return true;
                    }
                    oldc = c;
                    break;
                default:
                    if (c == (byte) 0xAA && oldc == (byte) 0xAA) {
                        buffer = new ByteArrayOutputStream();
                        buffer.write(0xAA);
                        buffer.write(0xAA);
                        state = UnpackerState.collecting;
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

    public static InMotionAdapter getInstance() {
        if (INSTANCE == null) {
            Timber.i("New instance");
            INSTANCE = new InMotionAdapter();
        }
        Timber.i("Get instance");
        return INSTANCE;
    }

    public static void newInstance() {
        if (INSTANCE != null && INSTANCE.keepAliveTimer != null) {
            INSTANCE.keepAliveTimer.cancel();
            INSTANCE.keepAliveTimer = null;
        }
        Timber.i("New instance");
        INSTANCE = new InMotionAdapter();
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
