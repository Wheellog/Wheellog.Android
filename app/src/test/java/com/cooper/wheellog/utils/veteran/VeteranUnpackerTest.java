package com.cooper.wheellog.utils.veteran;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;

public class VeteranUnpackerTest {

    private VeteranUnpacker unpacker = new VeteranUnpacker(new ByteArrayOutputStream());

    @Test
    public void testAddCharCollectingDataVerificationFails() throws IOException {
        unpacker.state = VeteranUnpacker.UnpackerState.COLLECTING;
        unpacker.buffer.write(new byte[22]); // Buffer size is now 22
        boolean result = unpacker.addChar(1); // Should fail as data verification does not pass
        assertFalse(result);
        assertEquals(VeteranUnpacker.UnpackerState.UNKNOWN, unpacker.state); // State should reset to UNKNOWN
    }

    //    @Test
    public void testAddCharCollectingCRC32CheckPasses() {
        // Prepare the Unpacker to reach the COLLECTING state
        unpacker.reset();
        unpacker.addChar((byte) 0xDC);
        unpacker.addChar((byte) 0x5A);
        unpacker.addChar((byte) 0x5C);
        unpacker.addChar(39); // This should be the length of the packet + 3

        // Simulate adding 39 bytes of data (length+3) to the buffer
        for (int i = 0; i < 39; i++) {
            unpacker.addChar(0xFF);
        }

        // Calculate the CRC32 of the dummy data to append at the end
        CRC32 crc = new CRC32();
        crc.update(new byte[39]); // Assuming the data is all 0xFF, update with actual data if different
        long crcValue = crc.getValue();

        // Add the CRC32 checksum to the buffer
        unpacker.addChar((int) (crcValue & 0xFF));
        unpacker.addChar((int) ((crcValue >> 8) & 0xFF));
        unpacker.addChar((int) ((crcValue >> 16) & 0xFF));
        unpacker.addChar((int) ((crcValue >> 24) & 0xFF));

        // The next call to addChar should return true if CRC check passes
        assertTrue(unpacker.addChar(0)); // This could be any value; it should trigger the CRC check
    }

    @Test
    public void testAddCharLensSearchUpdatesLengthAndState() {
        unpacker.state = VeteranUnpacker.UnpackerState.LENS_SEARCH;
        int testLength = 20;
        unpacker.addChar(testLength);
        assertEquals(testLength, unpacker.len);
        assertEquals(VeteranUnpacker.UnpackerState.COLLECTING, unpacker.state);
    }

    @Test
    public void testAddCharUnknownToLensSearchTransition() {
        unpacker.old1 = (byte) 0x5A;
        unpacker.old2 = (byte) 0xDC;
        unpacker.addChar((byte) 0x5C);
        assertEquals(VeteranUnpacker.UnpackerState.LENS_SEARCH, unpacker.state);
    }

    @Test
    public void testResetFunctionality() {
        unpacker.old1 = 1;
        unpacker.old2 = 1;
        unpacker.state = VeteranUnpacker.UnpackerState.DONE;
        unpacker.reset();
        assertEquals(0, unpacker.old1);
        assertEquals(0, unpacker.old2);
        assertEquals(VeteranUnpacker.UnpackerState.UNKNOWN, unpacker.state);
    }

}