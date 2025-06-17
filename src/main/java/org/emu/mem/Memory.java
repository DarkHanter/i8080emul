package org.emu.mem;

import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;

public class Memory {
    public static final int MEM_SIZE = 65536;
    public static final int VIDEO_RAM_START = 0x2400;
    public static final int VIDEO_RAM_END   = 0x4000;

    private final byte[] mem = new byte[MEM_SIZE];

    public byte[] getRaw() {
        return mem;
    }

    public Memory() {
        Arrays.fill(mem, (byte)0);
    }

    public int readByte(int address) {
        address &= 0xFFFF;
        return mem[address] & 0xFF;
    }

    public void writeByte(int address, int value) {
        address &= 0xFFFF;
        value &= 0xFF;
        mem[address] = (byte) value;
    }

    public void loadROMs() throws IOException {
        String[] romFiles = {"invaders.h", "invaders.g", "invaders.f", "invaders.e"};
        int[] loadAddresses = {0x0000, 0x0800, 0x1000, 0x1800};
        for (int i = 0; i < romFiles.length; i++) {
            String romName = romFiles[i];
            InputStream is = Memory.class.getResourceAsStream("/roms/" + romName);
            if (is == null) {
                throw new IOException("ROM файл не найден: " + romName);
            }
            int offset = loadAddresses[i];
            for (int addr = offset; addr < offset + 0x0800; addr++) {
                int data = is.read();
                if (data == -1) {
                    throw new IOException("Ошибка: ROM файл " + romName + " имеет неправильный размер");
                }
                mem[addr] = (byte) (data & 0xFF);
            }
            is.close();
        }
    }
}
