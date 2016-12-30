package com.cooper.wheellog.utils;

import com.cooper.wheellog.BluetoothLeService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static com.cooper.wheellog.utils.InMotionAdapter.Model.L6;
import static com.cooper.wheellog.utils.InMotionAdapter.Model.R0;
import static com.cooper.wheellog.utils.InMotionAdapter.Model.UNKNOWN;

/**
 * Created by cedric on 29/12/2016.
 */
public class InMotionAdapter {
    private static InMotionAdapter INSTANCE;
    private Timer keepAliveTimer;
    private boolean passwordSent = false;

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

        R1N("0"),
        R1S("1"),
        R1AP("3"),
        R1CF("2"),
        R1EX("4"),
        R1Sample("5"),
        R1T("6"),
        R10("7"),
        V3("10"),
        R2N("21"),
        R2S("22"),
        R2Sample("23"),
        R2("20"),
        R2EX("24"),
        L6("60"),
        V3C("11"),
        V3S("13"),
        V3PRO("12"),
        R0("30"),
        V5("50"),
        V5PLUS("51"),
        UNKNOWN("x");

        private String value;

        Model(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
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

    public void startKeepAliveTimer(final BluetoothLeService mBluetoothLeService) {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (!passwordSent) {
                    if (mBluetoothLeService.writeBluetoothGattCharacteristic(InMotionAdapter.CANMessage.getPassword("005654").writeBuffer())) passwordSent = true;
                    System.out.println("Sent password message");
                } else if (model == UNKNOWN) {
                    mBluetoothLeService.writeBluetoothGattCharacteristic(InMotionAdapter.CANMessage.getSlowData().writeBuffer());
                    System.out.println("Sent infos message");
                }
                else if (!mBluetoothLeService.writeBluetoothGattCharacteristic(CANMessage.standardMessage().writeBuffer())) {
                    System.out.println("Unable to send keep-alive message");
                } else {
                    System.out.println("Sent keep-alive message");
                }
            }
        };
        keepAliveTimer = new Timer();
        keepAliveTimer.scheduleAtFixedRate(timerTask, 0, 1000);
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

    static Model byteToModel(byte[] data) {

        if (data.length < 108) {
            return Model.UNKNOWN;
        }

        switch (data[107]) {

            case 0:     // Model R1
                switch (data[104]) {

                    case 0:
                        return Model.R1N;

                    case 1:
                        return Model.R1S;

                    case 2:
                        return Model.R1CF;

                    case 3:
                        return Model.R1AP;

                    case 4:
                        return Model.R1EX;

                    case 5:
                        return Model.R1Sample;

                    case 6:
                        return Model.R1T;

                    case 7:
                        return Model.R10;

                    default:
                        return Model.UNKNOWN;

                }

            case 1:         // Model V3
                switch (data[104]) {

                    case 1:
                        return Model.V3C;

                    case 2:
                        return Model.V3PRO;

                    case 3:
                        return Model.V3S;

                    default:
                        return Model.V3;


                }

            case 2:         // Model R2
                switch (data[104]) {

                    case 1:
                        return Model.R2;

                    case 4:
                        return Model.R2EX;

                    default:
                        return Model.R2;

                }

            case 3:     // Model R0
                return Model.R0;

            case 5:     // Model V5
                switch (data[104]) {
                    case 1:
                        return Model.V5PLUS;

                    default:
                        return Model.V5;
                }

            case 6:
                return Model.L6;


            default:
                return Model.UNKNOWN;

        }
    }


    static boolean isCarTypeBelongToInputType(String carType, String type) {
        return type.equals("0") && carType.length() > 0 || carType.charAt(0) == type.charAt(0) && carType.length() == 2;
    }

    static double batteryFromVoltage(double volts, Model model) {

        double batt;

        if (isCarTypeBelongToInputType(model.getValue(), "1")) {

            if (volts >= 83.50) {
                batt = 1.0;
            } else if (volts <= 68.5) {
                batt = 0;
            } else {
                batt = (volts - 68.5) / 15.0;
            }
        } else if (isCarTypeBelongToInputType(model.getValue(), "5") || model == Model.UNKNOWN) {

            if (volts >= 83.50) {
                batt = 1.0;
            } else if (volts > 80.0) {
                batt = ((volts - 80.0) / 3.5) * 0.2 + 0.8;
            } else if (volts > 77.0) {
                batt = ((volts - 77.0) / 3.0) * 0.2 + 0.6;
            } else if (volts > 74.0) {
                batt = ((volts - 74.0) / 3.0) * 0.2 + 0.4;
            } else if (volts > 71.0) {
                batt = ((volts - 71.0) / 3.0) * 0.2 + 0.2;
            } else if (volts > 55.0) {
                batt = ((volts - 55.0) / 16.0) * 0.2;
            } else {
                batt = 0.0;
            }
        } else if (model == Model.R0) {
            if (volts >= 82.00) {
                batt = 1.0;
            } else if (volts > 80.0) {
                batt = ((volts - 80.0) / 2.0) * 0.25 + 0.75;
            } else if (volts > 77.0) {
                batt = ((volts - 77.0) / 3.0) * 0.25 + 0.5;
            } else if (volts > 72.5) {
                batt = ((volts - 72.50) / 5.2) * 0.25 + 0.25;
            } else if (volts > 70.5) {
                batt = ((volts - 70.5) / 2.0) * 0.25;
            } else {
                batt = 0.0;
            }
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
        private final double speed;
        private final double voltage;
        private final double batt;
        private final double current;
        private final double power;
        private final double distance;
        private final double lock;

        Status() {
            angle = 0;
            speed = 0;
            voltage = 0;
            batt = 0;
            current = 0;
            power = 0;
            distance = 0;
            lock = 0;
        }

        Status(double angle, double speed, double voltage, double batt, double current, double power, double distance, double lock) {
            this.angle = angle;
            this.speed = speed;
            this.voltage = voltage;
            this.batt = batt;
            this.current = current;
            this.power = power;
            this.distance = distance;
            this.lock = lock;
        }

        public double getAngle() {
            return angle;
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
    }

    public static class Infos extends Status {
        private final String serialNumber;
        private final Model model;
        private final String version;

        Infos(String serialNumber, Model model, String version) {
            super();
            this.serialNumber = serialNumber;
            this.model = model;
            this.version = version;
        }

        public String getSerialNumber() {
            return serialNumber;
        }

        public Model getModel() {
            return model;
        }

        public String getVersion() {
            return version;
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
            RemoteControl(0x0F550116),
            PinCode(0x0F550307);

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
            data = Arrays.copyOfRange(bArr, 4, 11);
            len = bArr[12];
            ch = bArr[13];
            format = bArr[14] == 0 ? CanFormat.StandardFormat.getValue() : CanFormat.ExtendedFormat.getValue();
            type = bArr[15] == 0 ? CanFrame.DataFrame.getValue() : CanFrame.RemoteFrame.getValue();

            if (len == (byte) 0xFE) {
                int ldata = this.intFromBytes(data, 0);

                if (ldata == bArr.length - 16) {
                    ex_data = Arrays.copyOfRange(bArr, 16, 15 + ldata);
                }
            }

        }

        private CANMessage() {

        }

        public byte[] writeBuffer() {

            byte[] canBuffer = getBytes();
            int check = computeCheck(canBuffer);

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

        private static int computeCheck(byte[] buffer) {

            int check = 0;
            for (byte c : buffer) {
                check = (check + (int) c) % 256;
            }
            return check;
        }

        private int intFromBytes(byte[] bytes, int starting) {

            if (bytes.length >= starting + 4) {

                return bytes[starting] + bytes[starting + 1] * 256 + bytes[starting + 2] * 256 * 256 + bytes[starting + 3] * 256 * 256 * 256;
            } else {
                return 0;
            }


        }

        private int longFromBytes(byte[] bytes, int starting) {

            if (bytes.length >= starting + 8) {

                return bytes[starting] +
                        bytes[starting + 1] * 256 +
                        bytes[starting + 2] * 256 * 256 +
                        bytes[starting + 3] * 256 * 256 * 256 +
                        bytes[starting + 5] * 256 * 256 * 256 * 256 +
                        bytes[starting + 6] * 256 * 256 * 256 * 256 * 256 +
                        bytes[starting + 7] * 256 * 256 * 256 * 256 * 256 * 256;

            } else {
                return 0;
            }


        }

        long signedIntFromBytes(byte[] bytes, int starting) {

            if (bytes.length >= starting + 4) {

                long v = bytes[starting] + bytes[starting + 1] * 256 + bytes[starting + 2] * 256 * 256 + bytes[starting + 3] * 256 * 256 * 256;

                long value = 0;

                if (v >= 2147483648L) {        // No tinc clar que funcioni
                    value = v - 4294967295L - 1;
                } else {
                    value = v;
                }
                return value;
            } else {
                return 0;
            }
        }


        static CANMessage verify(byte[] buffer) {

            if (buffer[0] != (byte) 0xAA || buffer[1] != (byte) 0xAA || buffer[buffer.length - 1] != (byte) 0x55 || buffer[buffer.length - 2] != (byte) 0x55) {
                return null;  // Header and tail not correct
            }

            byte[] dataBuffer = Arrays.copyOfRange(buffer, 2, buffer.length - 3);

            dataBuffer = CANMessage.unescape(dataBuffer);
            int check = CANMessage.computeCheck(dataBuffer);

            int bufferCheck = buffer[buffer.length - 3];

            return (check == bufferCheck) ? new CANMessage(dataBuffer) : null;
        }


        private byte[] escape(byte[] buffer) {

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            for (int c : buffer) {
                if (c == (byte) 0xAA || c == (byte) 0x55 || c == (byte) 0xA5) {
                    out.write(0xA5);
                }
                out.write(c);
            }

            return out.toByteArray();
        }

        private static byte[] unescape(byte[] buffer) {

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int oldc = 0;

            for (int c : buffer) {
                if (c != (byte) 0xA5 || oldc == (byte) 0xA5) {
                    out.write(c);
                }
                oldc = c;
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
            msg.data = new byte[]{33, 0, 0, 2, 0, 0, 0, 0};

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
            msg.type = CanFrame.RemoteFrame.getValue();
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
            if (ex_data == null) return new Status(0, 0, 0, 0, 0, 0, 0, 0);
            // Angle

            Date d = new Date();
            double angle = (double) (this.intFromBytes(ex_data, 0)) / 65536.0;
            double speed = Math.abs(((double) (this.signedIntFromBytes(ex_data, 12)) + (double) (this.signedIntFromBytes(ex_data, 16))) / (3812.0 * 2.0));       // 3812.0 depen del tipus de vehicle
            double voltage = (double) (this.intFromBytes(ex_data, 24)) / 100.0;  // Falta conversio a battery level
            double current = (double) (this.signedIntFromBytes(ex_data, 20)) / 100.0;
            double power = voltage * current;

            double distance;

            if (isCarTypeBelongToInputType(model.getValue(), "1") ||
                    isCarTypeBelongToInputType(model.getValue(), "5")) {
                distance = (double) (this.longFromBytes(ex_data, 44));
            } else if (model == R0) {
                distance = (double) (this.longFromBytes(ex_data, 44));

            } else if (model == L6) {
                distance = (double) (this.longFromBytes(ex_data, 44)) * 100.0;

            } else {
                distance = (double) (this.longFromBytes(ex_data, 44)) / 5.711016379455429E4;
            }

            WorkMode workMode = intToWorkMode(this.intFromBytes(ex_data, 60));
            double lock = 0.0;
            switch (workMode) {

                case lock:
                    lock = 1.0;

                default:
                    break;
            }

            double batt = batteryFromVoltage(voltage, model);
            return new Status(angle, speed, voltage, batt, current, power, distance, lock);
        }

        // Return SerialNumber, Model, Version

        Infos parseSlowInfoMessage() {
            if (ex_data == null) return new Infos("", Model.UNKNOWN, "");
            String serialNumber = new String(Arrays.copyOfRange(ex_data, 0, 7));
            Model model = byteToModel(ex_data);  // CarType is just model.rawValue
            int v = this.intFromBytes(ex_data, 24);
            int v0 = v / 0xFFFFFF;
            int v1 = (v - v0 * 0xFFFFFF) / 0xFFFF;
            int v2 = v - v0 * 0xFFFFFF - v1 * 0xFFFF;
            String version = String.format(Locale.ENGLISH, "%d.%d.%d", v0, v1, v2);
            return new Infos(serialNumber, model, version);
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

                str += String.format("%02X", c);
                comma = true;
            }

            str += "]";

            return str;
        }
    }

    public ArrayList<Status> charUpdated(BluetoothLeService mBluetoothLeService, byte[] data) {
        ArrayList<Status> outValues = new ArrayList<>();
        for (byte c : data) {
            if (unpacker.addChar(c)) {

                CANMessage result = CANMessage.verify(unpacker.getBuffer());

                if (result != null) { // data OK
                    if (result.id == CANMessage.IDValue.GetFastInfo.getValue()) {
                        Status vals = result.parseFastInfoMessage(model);
                        outValues.add(vals);
                    } else {
                        Infos infos = result.parseSlowInfoMessage();
                        model = infos.getModel();
                        outValues.add(infos);
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
                        oldc = c;
                        return true;
                    }
                    oldc = c;

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
            INSTANCE = new InMotionAdapter();
        }
        return INSTANCE;
    }

    public static void newInstance() {
        if (INSTANCE.keepAliveTimer != null) {
            INSTANCE.keepAliveTimer.cancel();
            INSTANCE.keepAliveTimer = null;
        }
        INSTANCE = new InMotionAdapter();
    }
}
