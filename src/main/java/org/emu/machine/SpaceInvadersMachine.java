package org.emu.machine;

import org.emu.cpu.CPU8080;
import org.emu.mem.Memory;

import java.io.IOException;

public class SpaceInvadersMachine implements CPU8080.IOHandler {
    private CPU8080 cpu;
    private Memory memory;
    private int port0;
    private int port1;
    private int port2;
    private int shiftData;
    private int shiftOffset;

    public SpaceInvadersMachine() {
        this.memory = new Memory();
        this.cpu = new CPU8080(memory);
        cpu.setIOHandler(this);
        port0 = 0x00;
        port0 |= 0x02;
        port0 |= 0x04;
        port1 = 0x00;
        port1 |= 0x08;
        port2 = 0x00;
        port2 |= 0x00;
        shiftData = 0;
        shiftOffset = 0;
    }

    public CPU8080 getCPU() {
        return cpu;
    }
    public Memory getMemory() {
        return memory;
    }

    public void loadRoms() throws IOException {
        memory.loadROMs();
        cpu.reset();
        cpu.enableInterrupts();
}

    @Override
    public int portIn(int port) {
        port &= 0xFF;
        switch (port) {
            case 0:  return port0;
            case 1:  return port1;
            case 2:  return port2;
            case 3:
            {
                int result = (shiftData >> (8 - shiftOffset)) & 0xFF;
                return result;
            }
            case 4:
            case 5:
            case 6:
            default:
                return 0;
        }
    }

    @Override
    public void portOut(int port, int value) {
        port &= 0xFF;
        value &= 0xFF;
        switch (port) {
            case 2:
                shiftOffset = value & 0x07;
                break;
            case 3:
                break;
            case 4:
                shiftData = ((value & 0xFF) << 8) | ((shiftData >> 8) & 0xFF);
                break;
            case 5:
                break;
            case 6:
                break;
            case 7:
                break;
        }
    }

    public void setCoinInserted(boolean inserted) {
        if (inserted) port1 |= 0x01;
        else port1 &= ~0x01;
    }

    public void setStart1Pressed(boolean pressed) {
        if (pressed) port1 |= 0x04;
        else port1 &= ~0x04;
    }

    public void setStart2Pressed(boolean pressed) {
        if (pressed) port1 |= 0x02;
        else port1 &= ~0x02;
    }

    public void setLeftPressed(boolean pressed) {
        if (pressed) port1 |= 0x20;
        else port1 &= ~0x20;
    }

    public void setRightPressed(boolean pressed) {
        if (pressed) port1 |= 0x40;
        else port1 &= ~0x40;
    }

    public void setFirePressed(boolean pressed) {
        if (pressed) port1 |= 0x10;
        else port1 &= ~0x10;
    }

    public void executeFrame() {
        int cyclesPerHalfFrame = 16667;
        int cycles = 0;
        while (cycles < cyclesPerHalfFrame) {
            cycles += cpu.executeInstruction();
        }
        cpu.requestInterrupt(1);
        cycles = 0;
        while (cycles < cyclesPerHalfFrame) {
            cycles += cpu.executeInstruction();
        }
        cpu.requestInterrupt(2);
    }
}
