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
    private boolean passwordSent = false;
	private boolean settingCommandReady = false;
	private static int updateStep = 0;
	private byte[] settingCommand;



    NinebotZUnpacker unpacker = new NinebotZUnpacker();
/*
    public void startKeepAliveTimer(final BluetoothLeService mBluetoothLeService, final String ninebotPassword) {
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
*/

	public void resetConnection() {
		passwordSent = false;
	}
	
//	public void setLightState(final boolean lightEnable) {
//		settingCommandReady = true;
		//needSlowData = true;
		//mBluetoothLeService.writeBluetoothGattCharacteristic(InMotionAdapter.CANMessage.setLight(lightEnable).writeBuffer());
//		settingCommand = InMotionAdapter.CANMessage.setLight(lightEnable).writeBuffer();
//	}





	
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
	
/*
    public static class Infos extends Status {
        private final String serialNumber;
        //private final Model model;
        private final String version;
		private final boolean light;
		private final boolean led;
		private final boolean handleButtonDisabled;
		private final int maxSpeed;
		private final int speakerVolume;
		private final int tiltHorizon;
		
*/
//        Infos(String serialNumber, Model model, String version, boolean light, boolean led, boolean handleButtonDisabled, int maxSpeed, int speakerVolume, int tiltHorizon) {
//            super();
//            this.serialNumber = serialNumber;
//            this.model = model;
//            this.version = version;
//			this.light = light;
//			this.led = led;
//			this.handleButtonDisabled = handleButtonDisabled;
//			this.maxSpeed = maxSpeed;
//			this.speakerVolume = speakerVolume;
//			this.tiltHorizon = tiltHorizon;
  //      }


//		public boolean getLedState() {
//            return led;
//        }
		
/*
        @Override
        public String toString() {
            return "Infos{" +
                    "serialNumber='" + serialNumber + '\'' +
//                    ", model=" + model +
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
*/

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

        int len = 0;
        int source = 0;
        int destination = 0;
        int command = 0;
        int parameter = 0;
        byte[] data;
        int crc = 0;
        int something = 0;

        CANMessage(byte[] bArr) {
            if (bArr.length < 6) return;
            len = bArr[0];
            source = bArr[1];
            destination = bArr[2];
            command = bArr[3];
            parameter = bArr[4];
            data = Arrays.copyOfRange(bArr, 5, bArr.length-2);
            crc = bArr[bArr.length-2];
            something = bArr[bArr.length-1];
        }

        private CANMessage() {

        }

        public byte[] writeBuffer() {

            byte[] canBuffer = getBytes();
            //byte check = computeCheck(canBuffer);

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
            buff.write(crc);
            buff.write(something);

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

        static CANMessage verify(byte[] buffer) {


            byte[] dataBuffer = Arrays.copyOfRange(buffer, 2, buffer.length);

            return new CANMessage(dataBuffer);

        }


/*

        public static CANMessage getSerialNumber() {
            CANMessage msg = new CANMessage();
            msg.source = 0x3e;
            msg.destination = 0x14;
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

*/
/*
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
                    model == V8 || model == V10 || model == V10F || model == V10_test || model == V10F_test) {
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
*/
        // Return SerialNumber, Model, Version
/*
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
*/
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
					/*
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
                    */
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
                    Timber.i("buffer size %d", buffer.size());
                    if (buffer.size() == len+9) {
                        state = UnpackerState.done;
                        Timber.i("Step reset");
                        return true;
                    }
                    break;

                case started:

                    buffer.write(c);
                    len = c & 0xff;
                    Timber.i("Len %d", len);
                    state = UnpackerState.collecting;
                    break;

                default:
                    if (c == (byte) 0x5A && oldc == (byte) 0xA5) {
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
        if (INSTANCE == null) {
            INSTANCE = new NinebotZAdapter();
        }
        return INSTANCE;
    }

    public static void newInstance() {
        if (INSTANCE != null && INSTANCE.keepAliveTimer != null) {
            INSTANCE.keepAliveTimer.cancel();
            INSTANCE.keepAliveTimer = null;
        }
        INSTANCE = new NinebotZAdapter();
    }

    public static void stopTimer() {
        if (INSTANCE != null && INSTANCE.keepAliveTimer != null) {
            INSTANCE.keepAliveTimer.cancel();
            INSTANCE.keepAliveTimer = null;
        }
        INSTANCE = null;
    }

}
