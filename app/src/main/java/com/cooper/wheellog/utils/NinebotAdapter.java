package com.cooper.wheellog.utils;

import com.cooper.wheellog.WheelData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import timber.log.Timber;

/**
 * Created by palachzzz on Dec 2019.
 */
public class NinebotAdapter extends BaseAdapter {
    private static NinebotAdapter INSTANCE;
    private Timer keepAliveTimer;
    private boolean settingCommandReady = false;
    private static int updateStep = 0;
    private byte[] settingCommand;
    private static byte[] gamma = new byte[16];
    private static int stateCon = 0;
    private static byte protoVersion = 0;

    NinebotUnpacker unpacker = new NinebotUnpacker();

    public void startKeepAliveTimer(final String protoVer) {
        Timber.i("Ninebot timer starting");
        if (protoVer.compareTo("S2") == 0) protoVersion = 1;
        if (protoVer.compareTo("Mini") == 0) protoVersion = 2;
        updateStep = 0;
        stateCon = 0;
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (updateStep == 0) {
                    if (stateCon == 0) {
                        if (WheelData.getInstance().bluetoothCmd(NinebotAdapter.CANMessage.getSerialNumber().writeBuffer())) {
                            Timber.i("Sent serial number message");
                        } else updateStep = 39;

                    } else if (stateCon == 1) {
                        if (WheelData.getInstance().bluetoothCmd(NinebotAdapter.CANMessage.getVersion().writeBuffer())) {
                            Timber.i("Sent serial version message");
                        } else updateStep = 39;

                    } else if (settingCommandReady) {
                        if (WheelData.getInstance().bluetoothCmd(settingCommand)) {
                            settingCommandReady = false;
                            Timber.i("Sent command message");
                        } else updateStep = 39;
                    } else {
                        if (!WheelData.getInstance().bluetoothCmd(NinebotAdapter.CANMessage.getLiveData().writeBuffer())) {
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

    @Override
    public boolean decode(byte[] data) {
        Timber.i("Ninebot_decoding");
        ArrayList<NinebotAdapter.Status> statuses = charUpdated(data);
        if (statuses.size() < 1) {
            return false;
        }
        WheelData wd = WheelData.getInstance();
        wd.resetRideTime();
        for (NinebotAdapter.Status status : statuses) {
            Timber.i(status.toString());
            if (status instanceof NinebotAdapter.serialNumberStatus) {
                wd.setSerial(((serialNumberStatus) status).getSerialNumber());
                wd.setModel("Ninebot " + wd.getProtoVer());
            } else if (status instanceof NinebotAdapter.versionStatus) {
                wd.setVersion(((NinebotAdapter.versionStatus) status).getVersion());
            } else {
                int speed = status.getSpeed();
                int voltage = status.getVoltage();
                int battery = status.getBatt();
                wd.setSpeed(speed);
                wd.setVoltage(voltage);
                wd.setCurrent(status.getCurrent());
                wd.setTotalDistance(status.getDistance());
                wd.setTemperature(status.getTemperature() * 10);
                wd.updateRideTime();
                wd.setBatteryLevel(battery);
            }
        }
        return true;
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

        Status(int speed, int voltage, int batt, int current, int power, int distance, int temperature) {

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

    public static class CANMessage {
        enum Addr {
            Controller(0x01, 0x01, 0x01),
            KeyGenerator(0x16,0x16, 0x16),
            App(0x09, 0x11, 0x0A);

            private final int value_def;
            private final int value_s2;
            private final int value_mini;

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

            private final int value;

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

            private final int value;

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
            parameter = bArr[3] & 0xff;
            data = Arrays.copyOfRange(bArr, 4, bArr.length - 2);
            crc = bArr[bArr.length - 1] << 8 + bArr[bArr.length - 2];
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
            buff.write(parameter);
            try {
                buff.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            crc = computeCheck(buff.toByteArray());
            buff.write(crc & 0xff);
            buff.write((crc >> 8) & 0xff);
            return crypto(buff.toByteArray());
        }

        private static int computeCheck(byte[] buffer) {

            int check = 0;
            for (byte c : buffer) {
                check = check + ((int) c & 0xff);
            }
            check ^= 0xFFFF;
            check &= 0xFFFF;
            return check;
        }

        static CANMessage verify(byte[] buffer) {

            Timber.i("Verifying");
            byte[] dataBuffer = Arrays.copyOfRange(buffer, 2, buffer.length);
            dataBuffer = crypto(dataBuffer);

            int check = (dataBuffer[dataBuffer.length - 1] << 8 | ((dataBuffer[dataBuffer.length - 2]) & 0xff)) & 0xffff;
            byte[] dataBufferCheck = Arrays.copyOfRange(dataBuffer, 0, dataBuffer.length - 2);
            int checkBuffer = computeCheck(dataBufferCheck);
            if (check == checkBuffer) {
                Timber.i("Check OK");
            } else {
                Timber.i("Check FALSE, packet: %02X, calc: %02X", check, checkBuffer);
            }
            return (check == checkBuffer) ? new CANMessage(dataBuffer) : null;
        }

        static byte[] crypto(byte[] buffer) {
            byte[] dataBuffer = Arrays.copyOfRange(buffer, 0, buffer.length);
            Timber.i("Initial packet: %s", StringUtil.toHexString(dataBuffer));
            for (int j = 1; j < dataBuffer.length; j++) {
                dataBuffer[j] ^= gamma[(j - 1) % 16];
            }
            Timber.i("En/Decrypted packet: %s", StringUtil.toHexString(dataBuffer));
            return dataBuffer;
        }

        public static CANMessage getSerialNumber() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
            msg.parameter = Param.SerialNumber.getValue();
            msg.data = new byte[]{0x0e};
            msg.len = msg.data.length + 2;
            msg.crc = 0;
            return msg;
        }

        public static CANMessage getVersion() {
            CANMessage msg = new CANMessage();
            msg.source = Addr.App.getValue();
            msg.destination = Addr.Controller.getValue();
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
            msg.parameter = Param.LiveData.getValue();
            msg.data = new byte[]{0x20};
            msg.len = msg.data.length + 2;
            msg.crc = 0;
            return msg;
        }

        @Deprecated
        private byte[] parseKey() {
            byte [] gammaTemp = Arrays.copyOfRange(data, 0, data.length);
            StringBuilder gamma_text = new StringBuilder();
            for (byte datum : data) {
                gamma_text.append(String.format("%02X", datum));
            }
            Timber.i("New key: %s", gamma_text.toString());
            return gammaTemp;
        }

        serialNumberStatus parseSerialNumber() {
            serialNum = new String(data);
            Timber.i("Serial Number: %s", serialNum);
            return new serialNumberStatus(serialNum);
        }

        serialNumberStatus parseSerialNumber2() {
            serialNum = serialNum + new String(data);
            Timber.i("Serial Number: %s", serialNum);
            return new serialNumberStatus(serialNum);
        }

        versionStatus parseVersionNumber() {
            String versionNumber ="";
            if (protoVersion == 1) {
                versionNumber = String.format(Locale.US, "%d.%d.%d", data[1] >> 4, data[0] >> 4, data[0] & 0xf);
            } else if (protoVersion == 2) {
                versionNumber = String.format(Locale.US, "%d.%d.%d", data[1] & 0xf, data[0] >> 4, data[0] & 0xf);
            }
            Timber.i("Version Number: %s", versionNumber);
            return new versionStatus(versionNumber);
        }

        activationStatus parseActivationDate() {
            int activationDate = MathsUtil.shortFromBytesLE(data, 0);
            int year = activationDate>>9;
            int mounth = (activationDate>>5) & 0x0f;
            int day = activationDate & 0x1f;
            String activationDateStr = String.format("%02d.%02d.20%02d", day, mounth,year);
            return new activationStatus(activationDateStr);
        }

        Status parseLiveData() {
            int batt = MathsUtil.shortFromBytesLE(data, 8);
            int speed;
            if (protoVersion == 1) {
                speed = MathsUtil.shortFromBytesLE(data, 28); //speed up to 320.00 km/h
            }
            else {
                speed = Math.abs(MathsUtil.signedShortFromBytesLE(data, 10) / 10); //speed up to 32.000 km/h
            }
            int distance = MathsUtil.intFromBytesLE(data, 14);
            int temperature = MathsUtil.shortFromBytesLE(data, 22);
            int voltage = MathsUtil.shortFromBytesLE(data, 24);
            if (protoVersion == 2) {
                voltage = 0; // no voltage for mini
            }
            int current = MathsUtil.signedShortFromBytesLE(data, 26);
            int power = voltage * current;
            return new Status(speed, voltage, batt, current, power, distance, temperature);
        }

        Status parseLiveData2() {
            batt = MathsUtil.shortFromBytesLE(data, 2);
            speed = MathsUtil.shortFromBytesLE(data, 4) / 10;
            return new Status(speed, voltage, batt, current, power, distance, temperature);
        }

        Status parseLiveData3() {
            distance = MathsUtil.intFromBytesLE(data, 2);
            return new Status(speed, voltage, batt, current, power, distance, temperature);
        }

        Status parseLiveData4() {
            temperature = MathsUtil.shortFromBytesLE(data, 4);
            return new Status(speed, voltage, batt, current, power, distance, temperature);
        }

        Status parseLiveData5() {
            voltage = MathsUtil.shortFromBytesLE(data, 0);
            current = MathsUtil.signedShortFromBytesLE(data, 2);
            power = voltage * current;
            return new Status(speed, voltage, batt, current, power, distance, temperature);
        }

        public byte[] getData() {
            return data;
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
                    if (result.parameter == CANMessage.Param.SerialNumber.getValue()) {
                        Timber.i("Get serial number");
                        serialNumberStatus infos = result.parseSerialNumber();
                        stateCon = 1;
                        if ((result.len - 2) == 14) {
                            if (infos != null)
                                outValues.add(infos);
                        }
                    } else if (result.parameter == CANMessage.Param.SerialNumber2.getValue()) {
                        Timber.i("Get serial number2");
                        serialNumberStatus infos = result.parseSerialNumber2();

                    } else if (result.parameter == CANMessage.Param.SerialNumber3.getValue()) {
                        Timber.i("Get serial number3");
                        serialNumberStatus infos = result.parseSerialNumber2();
                        if (infos != null)
                            outValues.add(infos);

                    } else if (result.parameter == CANMessage.Param.Firmware.getValue()) {
                        Timber.i("Get version number");
                        versionStatus infos = result.parseVersionNumber();
                        stateCon = 2;
                        if (infos != null)
                            outValues.add(infos);

                    } else if (result.parameter == CANMessage.Param.LiveData.getValue()) {
                        Timber.i("Get life data1");
                        if (result.len - 2 == 32) {
                            Status status = result.parseLiveData();
                            if (status != null) {
                                outValues.add(status);
                            }
                        }
                    } else if (result.parameter == CANMessage.Param.LiveData2.getValue()) {
                        Timber.i("Get life data2");
                        result.parseLiveData2();
                    } else if (result.parameter == CANMessage.Param.LiveData3.getValue()) {
                        Timber.i("Get life data3");
                        result.parseLiveData3();
                    } else if (result.parameter == CANMessage.Param.LiveData4.getValue()) {
                        Timber.i("Get life data4");
                        result.parseLiveData4();
                    } else if (result.parameter == CANMessage.Param.LiveData5.getValue()) {
                        Timber.i("Get life data5");
                        Status status = result.parseLiveData5();
                        if (status != null) {
                            outValues.add(status);
                        }
                    } else if (result.parameter == CANMessage.Param.LiveData6.getValue()) {
                        Timber.i("Get life data");
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
                    if (buffer.size() == len + 6) {
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

    public static synchronized void newInstance() {
        if (INSTANCE != null && INSTANCE.keepAliveTimer != null) {
            INSTANCE.keepAliveTimer.cancel();
            INSTANCE.keepAliveTimer = null;

        }
        Timber.i("New instance");
        INSTANCE = new NinebotAdapter();
    }

    public static synchronized void stopTimer() {
        if (INSTANCE != null && INSTANCE.keepAliveTimer != null) {
            INSTANCE.keepAliveTimer.cancel();
            INSTANCE.keepAliveTimer = null;
        }
        Timber.i("Kill instance, stop timer");
        INSTANCE = null;
    }
}
