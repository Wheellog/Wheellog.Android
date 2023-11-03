package com.cooper.wheellog.utils.veteran;

import com.cooper.wheellog.utils.MathsUtil;

import java.io.ByteArrayOutputStream;
import java.util.zip.CRC32;

import timber.log.Timber;

public class VeteranUnpacker {

    enum UnpackerState {
        UNKNOWN,
        COLLECTING,
        LENS_SEARCH,
        DONE
    }

    public ByteArrayOutputStream buffer;

    public VeteranUnpacker(
            ByteArrayOutputStream buffer
    ) {
        this.buffer = buffer;
    }

    int old1 = 0;
    int old2 = 0;
    int len = 0;

    UnpackerState state = UnpackerState.UNKNOWN;

    byte[] getBuffer() {
        return buffer.toByteArray();
    }

    boolean addChar(int c) {
        switch (state) {
            case COLLECTING -> {
                int bsize = buffer.size();
                if (((bsize == 22 || bsize == 30) && (c != 0x00)) || ((bsize == 23) && ((c & 0xFE) != 0x00)) || ((bsize == 31) && ((c & 0xFC) != 0x00))) {
                    state = UnpackerState.DONE;
                    Timber.i("Data verification failed");
                    reset();
                    return false;
                }
                buffer.write(c);
                if (bsize == len + 3) {
                    state = UnpackerState.DONE;
                    Timber.i("Len %d", len);
                    Timber.i("Step reset");
                    reset();
                    if (len > 38) { // new format with crc32
                        CRC32 crc = new CRC32();
                        crc.update(getBuffer(), 0, len);
                        long calc_crc = crc.getValue();
                        long provided_crc = MathsUtil.getInt4(getBuffer(), len);
                        if (calc_crc == provided_crc) {
                            Timber.i("CRC32 ok");
                            return true;
                        } else {
                            Timber.i("CRC32 fail");
                            return false;
                        }
                    }
                    return true; // old format without crc32
                }
            }
            case LENS_SEARCH -> {
                buffer.write(c);
                len = c & 0xff;
                state = UnpackerState.COLLECTING;
                old2 = old1;
                old1 = c;
            }
            case UNKNOWN, DONE -> {
                if (c == (byte) 0x5C && old1 == (byte) 0x5A && old2 == (byte) 0xDC) {
                    buffer = new ByteArrayOutputStream();
                    buffer.write(0xDC);
                    buffer.write(0x5A);
                    buffer.write(0x5C);
                    state = UnpackerState.LENS_SEARCH;
                } else if (c == (byte) 0x5A && old1 == (byte) 0xDC) {
                    old2 = old1;
                } else {
                    old2 = 0;
                }
                old1 = c;
            }
        }
        return false;
    }

    void reset() {
        old1 = 0;
        old2 = 0;
        state = UnpackerState.UNKNOWN;
    }
}
