package com.cooper.wheellog.utils.gotway;

import java.io.ByteArrayOutputStream;

import timber.log.Timber;

public class GotwayUnpacker {

    enum UnpackerState {
        unknown,
        collecting,
        done
    }

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    GotwayUnpacker.UnpackerState state = UnpackerState.unknown;
    int oldc = -1;

    byte[] getBuffer() {
        return buffer.toByteArray();
    }

    boolean addChar(int c) {
        if (state == UnpackerState.collecting) {
            buffer.write(c);
            oldc = c;
            int size = buffer.size();
            if ((size == 20 && c != (byte) 0x18) || (size > 20 && size <= 24 && c != (byte) 0x5A)) {
                Timber.i("Invalid frame footer (expected 18 5A 5A 5A 5A)");
                state = UnpackerState.unknown;
                return false;
            }
            if (size == 24) {
                state = UnpackerState.done;
                Timber.i("Valid frame received");
                return true;
            }
        } else {
            if (c == (byte) 0xAA && oldc == (byte) 0x55) {
                Timber.i("Frame header found (55 AA), collecting data");
                buffer = new ByteArrayOutputStream();
                buffer.write(0x55);
                buffer.write(0xAA);
                state = UnpackerState.collecting;
            }
            oldc = c;
        }
        return false;
    }
}
