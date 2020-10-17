package com.cooper.wheellog.utils;

import com.cooper.wheellog.BluetoothLeService;
import com.cooper.wheellog.WheelData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import timber.log.Timber;

// created by palachzzz October 2020

public class InmotionAdapterV2 implements IWheelAdapter {
    private static InmotionAdapterV2 INSTANCE;
    private Timer keepAliveTimer;
    private boolean settingCommandReady = false;
    private static int updateStep = 0;
    private static int stateCon = 0;
    private byte[] settingCommand;
    InmotionUnpackerV2 unpacker = new InmotionUnpackerV2();

    @Override
    public boolean decode(byte[] data) {
        if (decodeNewData(data)) {
            return true;
        } else {
            return false;
        }

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
                            stateCon += 1;
                            Timber.i("Sent car type message");
                        } else updateStep = 39;

                    } else if (stateCon == 1) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(Message.getSerialNumber().writeBuffer())) {
                            stateCon += 1;
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
                            Timber.i("Sent statistics data message");
                            stateCon += 1;
                        } else updateStep = 39;

                    }
                    else if (stateCon == 5) {
                        if (mBluetoothLeService.writeBluetoothGattCharacteristic(Message.getStatistics().writeBuffer())) {
                            Timber.i("Sent useless data message");
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
            if (bArr.length < 3) return;
            flags = bArr[0];
            len = bArr[1];
            command = bArr[2] & 0x7F;
            if (len > 1) {
                data = Arrays.copyOfRange(bArr, 3, len);
            }
        }

        private Message() {

        }

        boolean parseMainData(){
            Timber.i("Parse main data");
            WheelData wd = WheelData.getInstance();
            wd.resetRideTime();
            if ((data[0] == (byte) 0x01) || len >= 8) {
                Timber.i("Parse car type");
                int mainSeries = data[1]; //02
                int series = data[2];    // 06
                int type = data[3];      // 01
                int batch = data[4];     // 02
                int feature = data[5];   // 01
                int reverse = data[6];   // 00
                wd.setModel(String.format("Inmotion V11 rev:%d.%d",batch,feature));

            } else if ((data[0] == (byte) 0x02) || len >= 19) {
                Timber.i("Parse serial num");
                String serialNumber = "";
                for (int j = 0; j < 15; j++) {
                    serialNumber += String.format("%02X", data[j+1]);

                }
                wd.setSerial(serialNumber);
            } else if ((data[0] == (byte) 0x06) || len >= 28) {
                Timber.i("Parse versions");
            }
            return false;
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
                        Timber.i("Len %d", len);
                        Timber.i("Step reset");
                        return true;
                    }
                    break;

                case lensearch:
                    buffer.write(c);
                    len = c & 0xff;
                    state = UnpackerState.collecting;
                    break;

                case flagsearch:
                    buffer.write(c);
                    flags = c & 0xff;
                    state = UnpackerState.lensearch;
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
            state = InmotionAdapterV2.InmotionUnpackerV2.UnpackerState.unknown;

        }
    }

    private boolean decodeNewData(byte[] data) {
        for (byte c : data) {
            if (unpacker.addChar(c)) {

                Message result = Message.verify(unpacker.getBuffer());

                if (result != null) {
                    if (result.command == Message.Command.MainInfo.getValue()) {
                        return result.parseMainData();
                    }

                }
            }
        }
        return false;
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



