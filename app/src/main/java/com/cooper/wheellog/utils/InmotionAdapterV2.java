package com.cooper.wheellog.utils;

import com.cooper.wheellog.BluetoothLeService;
import com.cooper.wheellog.WheelData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import timber.log.Timber;

public class InmotionAdapterV2 extends BaseAdapter {
    private static InmotionAdapterV2 INSTANCE;
    private Timer keepAliveTimer;
    private boolean settingCommandReady = false;
    private static int updateStep = 0;
    private static int stateCon = 0;
    private byte[] settingCommand;
    InmotionUnpackerV2 unpacker = new InmotionUnpackerV2();

    @Override
    public boolean decode(byte[] data) {
        for (byte c : data) {
            if (unpacker.addChar(c)) {

                Message result = Message.verify(unpacker.getBuffer());

                if (result != null) {
                    Timber.i("Get new data, command: %02X", result.command);

                    if (result.command == Message.Command.MainInfo.getValue()) {
                        return result.parseMainData();

                    } else if (result.command == Message.Command.TotalStats.getValue()) {
                        return result.parseTotalStats();
                    } else if (result.command == Message.Command.RealTimeInfo.getValue()) {
                        return result.parseRealTimeInfo();
                    } else {
                        Timber.i("Get unknown command: %02X",result.command);
                    }

                }
            }
        }
        return false;
    }

    @Override
    public void updatePedalsMode(int pedalsMode) {
    }

    @Override
    public void updateLightMode(int lightMode) {
    }

    @Override
    public void updateMaxSpeed(int wheelMaxSpeed) {
    }

    public static InmotionAdapterV2 getInstance() {
        if (INSTANCE == null) {
            Timber.i("New instance");
            INSTANCE = new InmotionAdapterV2();
        }
        Timber.i("Get instance");
        return INSTANCE;

    }

	public void startKeepAliveTimer(final BluetoothLeService mBluetoothLeService) {
        updateStep = 0;
        stateCon = 0;
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (updateStep == 0) {
                    if (stateCon == 0) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(Message.getCarType().writeBuffer())) {
                            Timber.i("Sent car type message");
                        } else updateStep = 39;

                    } else if (stateCon == 1) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(Message.getSerialNumber().writeBuffer())) {
                            Timber.i("Sent s/n message");
                        } else updateStep = 39;

                    } else if (stateCon == 2) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(Message.getVersions().writeBuffer())) {
                            stateCon += 1;
                            Timber.i("Sent versions message");
                        } else updateStep = 39;

                    } else if (settingCommandReady) {
    					if (mBluetoothLeService.writeBluetoothGattCharacteristic(settingCommand)) {
                            settingCommandReady = false;
                            Timber.i("Sent command message");
                        } else updateStep = 39; // after +1 and %10 = 0
    				} else if (stateCon == 3) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(Message.getUnknownData().writeBuffer())) {
                            stateCon += 1;
                            Timber.i("Sent unknown data message");
                        } else updateStep = 39;

                    }
                    else if (stateCon == 4) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(Message.getUselessData().writeBuffer())) {
                            Timber.i("Sent useless data message");
                            stateCon += 1;
                        } else updateStep = 39;

                    }
                    else if (stateCon == 5) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(Message.getStatistics().writeBuffer())) {
                            Timber.i("Sent statistics data message");
                            stateCon += 1;
                        } else updateStep = 39;

                    }
                    else  {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(InmotionAdapterV2.Message.getRealTimeData().writeBuffer())) {
                            Timber.i("Sent realtime data message");
                            stateCon = 5;
                        } else updateStep = 39;

                    }


				}
                updateStep += 1;
                updateStep %= 20;
                Timber.i("Step: %d", updateStep);
            }
        };
        keepAliveTimer = new Timer();
        keepAliveTimer.scheduleAtFixedRate(timerTask, 0, 25);
    }

    public static class Message {

        enum Flag {
            NoOp(0),
            Initial(0x11),
            Default(0x14);

            private int value;

            Flag(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        enum Command {
            NoOp(0),
            MainInfo(0x02),
            RealTimeInfo(0x04),
            Something1(0x10),
            TotalStats(0x11),
            Something2(0x20);

            private int value;

            Command(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        int flags = Flag.NoOp.getValue();
        int len = 0;
        int command = 0;
        byte[] data;

        Message(byte[] bArr) {
            if (bArr.length < 5) return;
            flags = bArr[2];
            len = bArr[3];
            command = bArr[4] & 0x7F;
            if (len > 1) {
                data = Arrays.copyOfRange(bArr, 5, len+4);
            }
        }

        private Message() {

        }

        boolean parseMainData(){
            Timber.i("Parse main data");
            WheelData wd = WheelData.getInstance();
            wd.resetRideTime();
            if ((data[0] == (byte) 0x01) && len >= 6) {
                stateCon += 1;
                Timber.i("Parse car type");
                int mainSeries = data[1]; //02
                int series = data[2];    // 06
                int type = data[3];      // 01
                int batch = data[4];     // 02
                int feature = data[5];   // 01
                int reverse = data[6];   // 00
                wd.setModel(String.format(Locale.ENGLISH,"Inmotion V11",batch,feature));
                wd.setVersion(String.format(Locale.ENGLISH,"rev: %d.%d",batch,feature));
            } else if ((data[0] == (byte) 0x02) && len >= 17) {
                stateCon += 1;
                Timber.i("Parse serial num");
                String serialNumber = "";
                serialNumber = new String(data, 1, 16);

                wd.setSerial(serialNumber);
            } else if ((data[0] == (byte) 0x06) && len >= 10) {
                Timber.i("Parse versions");
            }
            return false;
        }

        boolean parseTotalStats() {
            Timber.i("Parse total stats data");
            WheelData wd = WheelData.getInstance();
            long mTotal = MathsUtil.intFromBytesLE(data, 0);
            long mTotal2 = MathsUtil.getInt4(data, 0);
            long mDissipation = MathsUtil.intFromBytesLE(data, 4);
            long mRecovery = MathsUtil.intFromBytesLE(data, 8);
            long mRideTime = MathsUtil.intFromBytesLE(data, 12);
            int sec = (int)(mRideTime % 60);
            int min = (int)((mRideTime / 60) % 60);
            int hour = (int) (mRideTime/ 3600);
            String mRideTimeStr = String.format("%d:%02d:%02d",hour,min,sec);
            long mPowerOnTime = MathsUtil.intFromBytesLE(data, 16);
            sec = (int)(mPowerOnTime % 60);
            min = (int)((mPowerOnTime / 60) % 60);
            hour = (int) (mPowerOnTime/ 3600);
            String mPowerOnTimeStr = String.format("%d:%02d:%02d",hour,min,sec);
            wd.setTotalDistance(mTotal*10);
            wd.setDistance(mTotal*10);
            return false;
        }

        boolean parseRealTimeInfo() {
            Timber.i("Parse realtime stats data");
            WheelData wd = WheelData.getInstance();
            int mVoltage = MathsUtil.shortFromBytesLE(data, 0);
            //int mVoltage2 = MathsUtil.getInt2R(data, 0); looks ok
            int mCurrent = MathsUtil.signedShortFromBytesLE(data, 2);
            int mSpeed = MathsUtil.signedShortFromBytesLE(data, 4);
            int mTorque = MathsUtil.signedShortFromBytesLE(data, 6);
            int mBatPower = MathsUtil.signedShortFromBytesLE(data, 8);
            int mMotPower = MathsUtil.signedShortFromBytesLE(data, 10);
            int mMileage = MathsUtil.shortFromBytesLE(data, 12) * 10;
            int mRemainMileage = MathsUtil.shortFromBytesLE(data, 14) * 10;
            int mBatLevel = data[16] & 0x7f;
            int mBatMode = (data[16] >> 7)  & 0x1;
            int mMosTemp = (data[17] & 0xff) + 80 - 256;
            int mMotTemp = (data[18] & 0xff) + 80 - 256;
            int mBatTemp = (data[19] & 0xff) + 80 - 256;
            int mBoardTemp = (data[20] & 0xff) + 80 - 256;
            int mLampTemp = (data[21] & 0xff) + 80 - 256;
            int mPitchAngle = MathsUtil.signedShortFromBytesLE(data, 22);
            int mPitchAimAngle = MathsUtil.signedShortFromBytesLE(data, 24);
            int mRollAngle = MathsUtil.signedShortFromBytesLE(data, 26);
            int mDynamicSpeedLimit = MathsUtil.shortFromBytesLE(data, 28);
            int mDynamicCurrentLimit = MathsUtil.shortFromBytesLE(data, 30);
            int mBrightness = data[32]& 0xff;
            int mLightBrightness = data[33]& 0xff;
            int mCpuTemp = (data[34] & 0xff) + 80 - 256;
            int mImuTemp = (data[35] & 0xff) + 80 - 256;
            wd.setVoltage(mVoltage);
            wd.setCurrent(mCurrent);
            wd.setSpeed(mSpeed);
            wd.setBatteryPercent(mBatLevel);
            wd.setTemperature(mMosTemp * 100);
            wd.setTemperature2(mBoardTemp * 100);
            wd.setAngle((double)mPitchAngle/100.0);
            wd.setRoll((double)mRollAngle/100.0);
            wd.updateRideTime();
            wd.setTopSpeed(mSpeed);
            wd.setVoltageSag(mVoltage);
            wd.setPower(mBatPower);
            //// state data
            int mPcMode = data[36] & 0x07;
            int mMcMode = (data[36]>>3)&0x07;
            int mMotState = (data[36]>>6)&0x01;
            int chrgState = (data[36]>>7)&0x01;
            int lightState = (data[37])&0x01;
            int decorLiState = (data[37] >> 1) & 0x01;
            int liftedState = (data[37]>>2)&0x01;
            int tailLiState = (data[37]>>3)&0x03;
            int fanState = (data[37]>>5)&0x01;
            String wmode = "";
            if (mMotState == 1) {wmode = wmode + " Active";}
            if (chrgState == 1) {wmode = wmode + " Charging";}
            if (liftedState == 1) {wmode = wmode + " Lifted";}
            wd.setModeStr(wmode);
            //// rest data



            return true;
        }

        public static Message getCarType() {
            Message msg = new Message();
            msg.flags = Flag.Initial.getValue();
            msg.command = Command.MainInfo.getValue();
            msg.data = new byte[]{(byte)0x01};
            return msg;
        }

        public static Message getSerialNumber() {
            Message msg = new Message();
            msg.flags = Flag.Initial.getValue();
            msg.command = Command.MainInfo.getValue();
            msg.data = new byte[]{(byte)0x02};
            return msg;
        }

        public static Message getVersions() {
            Message msg = new Message();
            msg.flags = Flag.Initial.getValue();
            msg.command = Command.MainInfo.getValue();
            msg.data = new byte[]{(byte)0x06};
            return msg;
        }

        public static Message getUnknownData() {
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Something2.getValue();
            msg.data = new byte[]{(byte)0x20};
            return msg;
        }

        public static Message getUselessData() {
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.Something1.getValue();
            msg.data = new byte[]{(byte)0x00, (byte)0x01};
            return msg;
        }

        public static Message getStatistics() {
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.TotalStats.getValue();
            msg.data = new byte[0];
            return msg;
        }

        public static Message getRealTimeData() {
            Message msg = new Message();
            msg.flags = Flag.Default.getValue();
            msg.command = Command.RealTimeInfo.getValue();
            msg.data = new byte[0];
            return msg;
        }



        public byte[] writeBuffer() {

            byte[] buffer = getBytes();
            byte check = calcCheck(buffer);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                out.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            out.write(check);
            return out.toByteArray();
        }

        private byte[] getBytes() {

            ByteArrayOutputStream buff = new ByteArrayOutputStream();
            buff.write(0xAA);
            buff.write(0xAA);
            buff.write(flags);
            buff.write(data.length+1);
            buff.write(command);
            try {
                buff.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return buff.toByteArray();
        }

        private static byte calcCheck(byte[] buffer) {

            int check = 0;
            for (byte c : buffer) {
                check = (check ^ c) & 0xFF;
            }
            return (byte) check;
        }

        public static String toHexString(byte[] buffer) {
            String str = "[";
            for (int c : buffer) {
                str += String.format("%02X", (c & 0xFF));
            }
            str += "]";
            return str;
        }

        static Message verify(byte[] buffer) {

            Timber.i("Verify: %s", Message.toHexString(buffer));
            byte[] dataBuffer = Arrays.copyOfRange(buffer, 0, buffer.length - 1);
            byte check = calcCheck(dataBuffer);

            byte bufferCheck = buffer[buffer.length - 1];
            if (check == bufferCheck) {
                Timber.i("Check OK");
            } else {
                Timber.i("Check FALSE, calc: %02X, packet: %02X",check, bufferCheck);
            }
            return (check == bufferCheck) ? new Message(dataBuffer) : null;

        }

    }
	
    static class InmotionUnpackerV2 {

        enum UnpackerState {
            unknown,
            flagsearch,
            lensearch,
            collecting,
            done
        }


        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int oldc = 0;
        int len = 0;
        int flags = 0;
        UnpackerState state = UnpackerState.unknown;

        byte[] getBuffer() {
            return buffer.toByteArray();
        }

        boolean addChar(int c) {

            switch (state) {

                case collecting:

                    buffer.write(c);
                    if (buffer.size() == len+5) {
                        state = UnpackerState.done;
                        updateStep = 0;
                        oldc = 0;
                        Timber.i("Len %d", len);
                        Timber.i("Step reset");
                        return true;
                    }
                    break;

                case lensearch:
                    buffer.write(c);
                    len = c & 0xff;
                    state = UnpackerState.collecting;
                    oldc = c;
                    break;

                case flagsearch:
                    buffer.write(c);
                    flags = c & 0xff;
                    state = UnpackerState.lensearch;
                    oldc = c;
                    break;

                default:
                    if (c == (byte) 0xAA && oldc == (byte) 0xAA) {
                        buffer = new ByteArrayOutputStream();
                        buffer.write(0xAA);
                        buffer.write(0xAA);
                        state = UnpackerState.flagsearch;
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

    @Override
    public int getCellSForWheel() {
        return 20;
    }

    public static void newInstance() {
        if (INSTANCE != null && INSTANCE.keepAliveTimer != null) {
            INSTANCE.keepAliveTimer.cancel();
            INSTANCE.keepAliveTimer = null;
        }
        Timber.i("New instance");
        INSTANCE = new InmotionAdapterV2();
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



