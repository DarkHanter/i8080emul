package org.emu.cpu;

import org.emu.mem.Memory;

import java.util.Arrays;

public class CPU8080 {
    // 8-битные регистры
    private int A, B, C, D, E, H, L;
    // 16-битные регистры
    private int PC;
    private int SP;

    private boolean signFlag;     // S
    private boolean zeroFlag;     // Z
    private boolean auxCarryFlag; // AC (Auxiliary carry)
    private boolean parityFlag;   // P
    private boolean carryFlag;    // CY
    // Флаг остановки CPU (HLT)
    private boolean halted;
    // Флаг разрешения прерываний
    private boolean interruptsEnabled;
    // Интерфейс для работы с портами ввода-вывода
    private IOHandler ioHandler;
    // Ссылка на память
    private Memory memory;

    private static final int[] CYCLES = new int[256];
    static {
        Arrays.fill(CYCLES, 5);
        CYCLES[0x00] = 4;   // NOP
        CYCLES[0x01] = 10;  // LXI B,d16
        CYCLES[0x02] = 7;   // STAX B
        CYCLES[0x03] = 5;   // INX B
        CYCLES[0x04] = 5;   // INR B
        CYCLES[0x05] = 5;   // DCR B
        CYCLES[0x06] = 7;   // MVI B,d8
        CYCLES[0x07] = 4;   // RLC
        CYCLES[0x08] = 4;   // NOP
        CYCLES[0x09] = 10;  // DAD B
        CYCLES[0x0A] = 7;   // LDAX B
        CYCLES[0x0B] = 5;   // DCX B
        CYCLES[0x0C] = 5;   // INR C
        CYCLES[0x0D] = 5;   // DCR C
        CYCLES[0x0E] = 7;   // MVI C,d8
        CYCLES[0x0F] = 4;   // RRC
        CYCLES[0x11] = 10;  // LXI D,d16
        CYCLES[0x12] = 7;   // STAX D
        CYCLES[0x13] = 5;   // INX D
        CYCLES[0x14] = 5;   // INR D
        CYCLES[0x15] = 5;   // DCR D
        CYCLES[0x16] = 7;   // MVI D,d8
        CYCLES[0x17] = 4;   // RAL
        CYCLES[0x19] = 10;  // DAD D
        CYCLES[0x1A] = 7;   // LDAX D
        CYCLES[0x1B] = 5;   // DCX D
        CYCLES[0x1C] = 5;   // INR E
        CYCLES[0x1D] = 5;   // DCR E
        CYCLES[0x1E] = 7;   // MVI E,d8
        CYCLES[0x1F] = 4;   // RAR
        CYCLES[0x21] = 10;  // LXI H,d16
        CYCLES[0x22] = 16;  // SHLD addr
        CYCLES[0x23] = 5;   // INX H
        CYCLES[0x24] = 5;   // INR H
        CYCLES[0x25] = 5;   // DCR H
        CYCLES[0x26] = 7;   // MVI H,d8
        CYCLES[0x27] = 4;   // DAA
        CYCLES[0x29] = 10;  // DAD H
        CYCLES[0x2A] = 16;  // LHLD addr
        CYCLES[0x2B] = 5;   // DCX H
        CYCLES[0x2C] = 5;   // INR L
        CYCLES[0x2D] = 5;   // DCR L
        CYCLES[0x2E] = 7;   // MVI L,d8
        CYCLES[0x2F] = 4;   // CMA
        CYCLES[0x31] = 10;  // LXI SP,d16
        CYCLES[0x32] = 13;  // STA addr
        CYCLES[0x33] = 5;   // INX SP
        CYCLES[0x34] = 10;  // INR M
        CYCLES[0x35] = 10;  // DCR M
        CYCLES[0x36] = 10;  // MVI M,d8
        CYCLES[0x37] = 4;   // STC
        CYCLES[0x39] = 10;  // DAD SP
        CYCLES[0x3A] = 13;  // LDA addr
        CYCLES[0x3B] = 5;   // DCX SP
        CYCLES[0x3C] = 5;   // INR A
        CYCLES[0x3D] = 5;   // DCR A
        CYCLES[0x3E] = 7;   // MVI A,d8
        CYCLES[0x3F] = 4;   // CMC
        // 0x40-0x75 MOV регистров
        for (int op = 0x40; op <= 0x75; op++) {
            if (op == 0x76) continue;
            boolean destIsM = ((op >> 3) & 0x7) == 6;
            boolean srcIsM = (op & 0x7) == 6;
            CYCLES[op] = (destIsM || srcIsM) ? 7 : 5;
        }
        CYCLES[0x76] = 7;   // HLT
        CYCLES[0x77] = 7;   // MOV M,A
        CYCLES[0x78] = 5;   // MOV A,B
        for (int op = 0x78; op <= 0x7F; op++) {
            CYCLES[op] = ((op & 0x7) == 6) ? 7 : 5;
        }
        // 0x80-0x87 ADD, 0x88-0x8F ADC, 0x90-0x97 SUB, 0x98-0x9F SBB,
        // 0xA0-0xA7 ANA, 0xA8-0xAF XRA, 0xB0-0xB7 ORA, 0xB8-0xBF CMP.
        for (int op = 0x80; op <= 0xBF; op++) {
            CYCLES[op] = ((op & 0x7) == 6) ? 7 : 4;
        }
        CYCLES[0xC0] = 5;   // RNZ
        CYCLES[0xC1] = 10;  // POP B
        CYCLES[0xC2] = 10;  // JNZ addr
        CYCLES[0xC3] = 10;  // JMP addr
        CYCLES[0xC4] = 11;  // CNZ addr
        CYCLES[0xC5] = 11;  // PUSH B
        CYCLES[0xC6] = 7;   // ADI d8
        CYCLES[0xC7] = 11;  // RST 0
        CYCLES[0xC8] = 5;   // RZ
        CYCLES[0xC9] = 10;  // RET
        CYCLES[0xCA] = 10;  // JZ addr
        CYCLES[0xCB] = 10;  // JMP (alt)
        CYCLES[0xCC] = 17;  // CZ addr
        CYCLES[0xCD] = 17;  // CALL addr
        CYCLES[0xCE] = 7;   // ACI d8
        CYCLES[0xCF] = 11;  // RST 1
        CYCLES[0xD0] = 5;   // RNC
        CYCLES[0xD1] = 10;  // POP D
        CYCLES[0xD2] = 10;  // JNC addr
        CYCLES[0xD3] = 10;  // OUT d8
        CYCLES[0xD4] = 17;  // CNC addr
        CYCLES[0xD5] = 11;  // PUSH D
        CYCLES[0xD6] = 7;   // SUI d8
        CYCLES[0xD7] = 11;  // RST 2
        CYCLES[0xD8] = 5;   // RC
        CYCLES[0xD9] = 10;  // RET (alt)
        CYCLES[0xDA] = 10;  // JC addr
        CYCLES[0xDB] = 10;  // IN d8
        CYCLES[0xDC] = 17;  // CC addr
        CYCLES[0xDD] = 17;  // CALL (alt)
        CYCLES[0xDE] = 7;   // SBI d8
        CYCLES[0xDF] = 11;  // RST 3
        CYCLES[0xE0] = 5;   // RPO
        CYCLES[0xE1] = 10;  // POP H
        CYCLES[0xE2] = 10;  // JPO addr
        CYCLES[0xE3] = 18;  // XTHL
        CYCLES[0xE4] = 17;  // CPO addr
        CYCLES[0xE5] = 11;  // PUSH H
        CYCLES[0xE6] = 7;   // ANI d8
        CYCLES[0xE7] = 11;  // RST 4
        CYCLES[0xE8] = 5;   // RPE
        CYCLES[0xE9] = 5;   // PCHL
        CYCLES[0xEA] = 10;  // JPE addr
        CYCLES[0xEB] = 5;   // XCHG
        CYCLES[0xEC] = 17;  // CPE addr
        CYCLES[0xED] = 17;  // CALL (alt)
        CYCLES[0xEE] = 7;   // XRI d8
        CYCLES[0xEF] = 11;  // RST 5
        CYCLES[0xF0] = 5;   // RP
        CYCLES[0xF1] = 10;  // POP PSW
        CYCLES[0xF2] = 10;  // JP addr
        CYCLES[0xF3] = 4;   // DI
        CYCLES[0xF4] = 17;  // CP addr
        CYCLES[0xF5] = 11;  // PUSH PSW
        CYCLES[0xF6] = 7;   // ORI d8
        CYCLES[0xF7] = 11;  // RST 6
        CYCLES[0xF8] = 5;   // RM
        CYCLES[0xF9] = 5;   // SPHL
        CYCLES[0xFA] = 10;  // JM addr
        CYCLES[0xFB] = 4;   // EI
        CYCLES[0xFC] = 17;  // CM addr
        CYCLES[0xFD] = 17;  // CALL (alt)
        CYCLES[0xFE] = 7;   // CPI d8
        CYCLES[0xFF] = 11;  // RST 7
    }

    public interface IOHandler {
        int portIn(int port);
        void portOut(int port, int value);
    }

    public CPU8080(Memory memory) {
        this.memory = memory;
        reset();
    }

    public void setIOHandler(IOHandler handler) {
        this.ioHandler = handler;
    }

    public void reset() {
        A = B = C = D = E = H = L = 0;
        PC = 0;
        SP = 0;
        signFlag = zeroFlag = parityFlag = carryFlag = auxCarryFlag = false;
        halted = false;
        interruptsEnabled = true;
    }

    public void enableInterrupts() {
        interruptsEnabled = true;
    }
    public void disableInterrupts() {
        interruptsEnabled = false;
    }

    public int executeInstruction() {
        if (halted) {
            return 0;
        }
        int opcode = memory.readByte(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;  // инкремент PC (0xFFFF -> 0x0000)
        int cycles = 0;
        switch (opcode) {
            // 8-разрядные загрузки и перемещения (MOV, MVI, LXI, LDAX, STAX, etc.)
            case 0x00:  /* NOP */
                break;
            case 0x01:  /* LXI B, d16 */
                C = memory.readByte(PC) & 0xFF;
                B = memory.readByte(PC + 1) & 0xFF;
                PC += 2;
                break;
            case 0x02:  /* STAX B (Store A into [BC]) */
                memory.writeByte((B << 8) | C, A);
                break;
            case 0x03:  /* INX B (BC = BC + 1) */
                int bc = ((B << 8) | C) + 1;
                bc &= 0xFFFF;
                B = (bc >> 8) & 0xFF;
                C = bc & 0xFF;
                break;
            case 0x04:  /* INR B (B = B+1) */
                B = incrementByte(B);
                break;
            case 0x05:  /* DCR B (B = B-1) */
                B = decrementByte(B);
                break;
            case 0x06:  /* MVI B, d8 */
                B = memory.readByte(PC) & 0xFF;
                PC += 1;
                break;
            case 0x07:  /* RLC (Rotate A left) */
                // Циклический сдвиг A влево: бит7 -> Carry
                carryFlag = ((A & 0x80) != 0);
                A = ((A << 1) & 0xFF) | (carryFlag ? 1 : 0);
                break;
            case 0x08:
                break;
            case 0x09:  /* DAD B (HL = HL + BC) */
            {
                int hl = getHL();
                int bcVal = (B << 8) | C;
                int result = hl + bcVal;
                carryFlag = (result & 0x10000) != 0;
                setHL(result & 0xFFFF);
                break;
            }
            case 0x0A:  /* LDAX B (A = [BC]) */
                A = memory.readByte((B << 8) | C) & 0xFF;
                break;
            case 0x0B:  /* DCX B (BC = BC - 1) */
            {
                int bcVal = ((B << 8) | C) - 1;
                bcVal &= 0xFFFF;
                B = (bcVal >> 8) & 0xFF;
                C = bcVal & 0xFF;
                break;
            }
            case 0x0C:  /* INR C */
                C = incrementByte(C);
                break;
            case 0x0D:  /* DCR C */
                C = decrementByte(C);
                break;
            case 0x0E:  /* MVI C, d8 */
                C = memory.readByte(PC) & 0xFF;
                PC += 1;
                break;
            case 0x0F:  /* RRC (Rotate A right) */
                // Циклический сдвиг A вправо: бит0 -> Carry
                carryFlag = ((A & 0x01) != 0);
                A = ((carryFlag ? 0x80 : 0x00) | (A >> 1)) & 0xFF;
                break;
            case 0x10:  /* NOP (не используется) */
                break;
            case 0x11:  /* LXI D, d16 */
                E = memory.readByte(PC) & 0xFF;
                D = memory.readByte(PC + 1) & 0xFF;
                PC += 2;
                break;
            case 0x12:  /* STAX D (Store A into [DE]) */
                memory.writeByte((D << 8) | E, A);
                break;
            case 0x13:  /* INX D (DE = DE + 1) */
            {
                int de = ((D << 8) | E) + 1;
                de &= 0xFFFF;
                D = (de >> 8) & 0xFF;
                E = de & 0xFF;
                break;
            }
            case 0x14:  /* INR D */
                D = incrementByte(D);
                break;
            case 0x15:  /* DCR D */
                D = decrementByte(D);
                break;
            case 0x16:  /* MVI D, d8 */
                D = memory.readByte(PC) & 0xFF;
                PC += 1;
                break;
            case 0x17:  /* RAL (Rotate A left through carry) */
            {
                boolean newCarry = (A & 0x80) != 0;
                A = ((A << 1) & 0xFF) | (carryFlag ? 1 : 0);
                carryFlag = newCarry;
                break;
            }
            case 0x18:  /* NOP (не используется) */
                break;
            case 0x19:  /* DAD D (HL = HL + DE) */
            {
                int hl = getHL();
                int de = (D << 8) | E;
                int result = hl + de;
                carryFlag = (result & 0x10000) != 0;
                setHL(result & 0xFFFF);
                break;
            }
            case 0x1A:  /* LDAX D (A = [DE]) */
                A = memory.readByte((D << 8) | E) & 0xFF;
                break;
            case 0x1B:  /* DCX D (DE = DE - 1) */
            {
                int de = ((D << 8) | E) - 1;
                de &= 0xFFFF;
                D = (de >> 8) & 0xFF;
                E = de & 0xFF;
                break;
            }
            case 0x1C:  /* INR E */
                E = incrementByte(E);
                break;
            case 0x1D:  /* DCR E */
                E = decrementByte(E);
                break;
            case 0x1E:  /* MVI E, d8 */
                E = memory.readByte(PC) & 0xFF;
                PC += 1;
                break;
            case 0x1F:
            {
                boolean newCarry = (A & 0x01) != 0;
                A = (carryFlag ? 0x80 : 0x00) | ((A >> 1) & 0x7F);
                carryFlag = newCarry;
                break;
            }
            case 0x20:
                break;
            case 0x21:  /* LXI H, d16 */
                L = memory.readByte(PC) & 0xFF;
                H = memory.readByte(PC + 1) & 0xFF;
                PC += 2;
                break;
            case 0x22:  /* SHLD addr */
            {
                int addr = memory.readByte(PC) & 0xFF;
                addr |= (memory.readByte(PC + 1) & 0xFF) << 8;
                PC += 2;
                memory.writeByte(addr, L);
                memory.writeByte(addr + 1, H);
                break;
            }
            case 0x23:  /* INX H (HL = HL + 1) */
            {
                int hl = getHL() + 1;
                hl &= 0xFFFF;
                setHL(hl);
                break;
            }
            case 0x24:  /* INR H */
                H = incrementByte(H);
                break;
            case 0x25:  /* DCR H */
                H = decrementByte(H);
                break;
            case 0x26:  /* MVI H, d8 */
                H = memory.readByte(PC) & 0xFF;
                PC += 1;
                break;
            case 0x27:  /* DAA (Decimal Adjust Accumulator) */
                decimalAdjustAccumulator();
                break;
            case 0x28:  /* NOP (не используется) */
                break;
            case 0x29:  /* DAD H (HL = HL + HL) */
            {
                int hl = getHL();
                int result = hl + getHL();
                carryFlag = (result & 0x10000) != 0;
                setHL(result & 0xFFFF);
                break;
            }
            case 0x2A:  /* LHLD addr (Load HL direct) */
            {
                int addr = memory.readByte(PC) & 0xFF;
                addr |= (memory.readByte(PC + 1) & 0xFF) << 8;
                PC += 2;
                L = memory.readByte(addr) & 0xFF;
                H = memory.readByte(addr + 1) & 0xFF;
                break;
            }
            case 0x2B:  /* DCX H (HL = HL - 1) */
            {
                int hl = getHL() - 1;
                hl &= 0xFFFF;
                setHL(hl);
                break;
            }
            case 0x2C:  /* INR L */
                L = incrementByte(L);
                break;
            case 0x2D:  /* DCR L */
                L = decrementByte(L);
                break;
            case 0x2E:  /* MVI L, d8 */
                L = memory.readByte(PC) & 0xFF;
                PC += 1;
                break;
            case 0x2F:  /* CMA */
                A = (~A) & 0xFF;
                break;
            case 0x30:
                break;
            case 0x31:  /* LXI SP, d16 */
                SP = (memory.readByte(PC) & 0xFF) | ((memory.readByte(PC + 1) & 0xFF) << 8);
                PC += 2;
                break;
            case 0x32:  /* STA addr */
            {
                int addr = memory.readByte(PC) & 0xFF;
                addr |= (memory.readByte(PC + 1) & 0xFF) << 8;
                PC += 2;
                memory.writeByte(addr, A);
                break;
            }
            case 0x33:  /* INX SP (SP = SP + 1) */
                SP = (SP + 1) & 0xFFFF;
                break;
            case 0x34:  /* INR M ([HL] = [HL] + 1) */
            {
                int addr = getHL();
                int value = memory.readByte(addr) & 0xFF;
                value = incrementByte(value);
                memory.writeByte(addr, value);
                break;
            }
            case 0x35:  /* DCR M ([HL] = [HL] - 1) */
            {
                int addr = getHL();
                int value = memory.readByte(addr) & 0xFF;
                value = decrementByte(value);
                memory.writeByte(addr, value);
                break;
            }
            case 0x36:  /* MVI M */
            {
                int addr = getHL();
                int byteVal = memory.readByte(PC) & 0xFF;
                PC += 1;
                memory.writeByte(addr, byteVal);
                break;
            }
            case 0x37:  /* STC (Set Carry) */
                carryFlag = true;
                break;
            case 0x38:
                break;
            case 0x39:  /* DAD SP (HL = HL + SP) */
            {
                int hl = getHL();
                int result = hl + SP;
                carryFlag = (result & 0x10000) != 0;
                setHL(result & 0xFFFF);
                break;
            }
            case 0x3A:  /* LDA addr */
            {
                int addr = memory.readByte(PC) & 0xFF;
                addr |= (memory.readByte(PC + 1) & 0xFF) << 8;
                PC += 2;
                A = memory.readByte(addr) & 0xFF;
                break;
            }
            case 0x3B:  /* DCX SP (SP = SP - 1) */
                SP = (SP - 1) & 0xFFFF;
                break;
            case 0x3C:  /* INR A */
                A = incrementByte(A);
                break;
            case 0x3D:  /* DCR A */
                A = decrementByte(A);
                break;
            case 0x3E:  /* MVI A, d8 */
                A = memory.readByte(PC) & 0xFF;
                PC += 1;
                break;
            case 0x3F:  /* CMC (Complement Carry) */
                carryFlag = !carryFlag;
                break;
            // 0x40-0x75: MOV r1, r2
            case 0x40: case 0x41: case 0x42: case 0x43: case 0x44: case 0x45: case 0x46: case 0x47:
            case 0x48: case 0x49: case 0x4A: case 0x4B: case 0x4C: case 0x4D: case 0x4E: case 0x4F:
            case 0x50: case 0x51: case 0x52: case 0x53: case 0x54: case 0x55: case 0x56: case 0x57:
            case 0x58: case 0x59: case 0x5A: case 0x5B: case 0x5C: case 0x5D: case 0x5E: case 0x5F:
            case 0x60: case 0x61: case 0x62: case 0x63: case 0x64: case 0x65: case 0x66: case 0x67:
            case 0x68: case 0x69: case 0x6A: case 0x6B: case 0x6C: case 0x6D: case 0x6E: case 0x6F:
            case 0x70: case 0x71: case 0x72: case 0x73: case 0x74: case 0x75:
            {
                int dest = (opcode >> 3) & 0x7;  // (0=B,1=C,2=D,3=E,4=H,5=L,6=M[HL],7=A)
                int src  = opcode & 0x7;         // код регистра-источника
                int value;
                if (src == 6) {
                    // источник - память [HL]
                    value = memory.readByte(getHL()) & 0xFF;
                } else {
                    value = getRegister(src);
                }
                if (dest == 6) {
                    // назначение - память [HL]
                    memory.writeByte(getHL(), value);
                } else {
                    setRegister(dest, value);
                }
                break;
            }
            case 0x76:  /* HLT (halt CPU until interrupt) */
                halted = true;
                break;
            case 0x77:  /* MOV M, A */
                memory.writeByte(getHL(), A);
                break;
            case 0x78: case 0x79: case 0x7A: case 0x7B: case 0x7C: case 0x7D: case 0x7E: case 0x7F:
            {
                // MOV A, r (0x78-0x7F)
                int src = opcode & 0x7;
                int val = (src == 6) ? memory.readByte(getHL()) & 0xFF : getRegister(src);
                A = val;
                break;
            }
            // 8-битные арифметические и логические операции:
            case 0x80: case 0x81: case 0x82: case 0x83: case 0x84: case 0x85: case 0x86: case 0x87:
            case 0x88: case 0x89: case 0x8A: case 0x8B: case 0x8C: case 0x8D: case 0x8E: case 0x8F:
            case 0x90: case 0x91: case 0x92: case 0x93: case 0x94: case 0x95: case 0x96: case 0x97:
            case 0x98: case 0x99: case 0x9A: case 0x9B: case 0x9C: case 0x9D: case 0x9E: case 0x9F:
            case 0xA0: case 0xA1: case 0xA2: case 0xA3: case 0xA4: case 0xA5: case 0xA6: case 0xA7:
            case 0xA8: case 0xA9: case 0xAA: case 0xAB: case 0xAC: case 0xAD: case 0xAE: case 0xAF:
            case 0xB0: case 0xB1: case 0xB2: case 0xB3: case 0xB4: case 0xB5: case 0xB6: case 0xB7:
            case 0xB8: case 0xB9: case 0xBA: case 0xBB: case 0xBC: case 0xBD: case 0xBE: case 0xBF:
            {
                int operation = (opcode >> 3) & 0x7;  // (0=ADD,1=ADC,...6=ORA,7=CMP)
                int regCode = opcode & 0x7;
                int operand = (regCode == 6) ? memory.readByte(getHL()) & 0xFF : getRegister(regCode);
                switch (operation) {
                    case 0: add(operand); break;      // ADD
                    case 1: adc(operand); break;      // ADC
                    case 2: sub(operand); break;      // SUB
                    case 3: sbb(operand); break;      // SBB
                    case 4: ana(operand); break;      // ANA
                    case 5: xra(operand); break;      // XRA
                    case 6: ora(operand); break;      // ORA
                    case 7: cmp(operand); break;      // CMP
                }
                break;
            }
            case 0xC6:  /* ADI d8 */
                add(memory.readByte(PC) & 0xFF);
                PC += 1;
                break;
            case 0xCE:  /* ACI d8 */
                adc(memory.readByte(PC) & 0xFF);
                PC += 1;
                break;
            case 0xD6:  /* SUI d8 */
                sub(memory.readByte(PC) & 0xFF);
                PC += 1;
                break;
            case 0xDE:  /* SBI d8 */
                sbb(memory.readByte(PC) & 0xFF);
                PC += 1;
                break;
            case 0xE6:  /* ANI d8 */
                ana(memory.readByte(PC) & 0xFF);
                PC += 1;
                break;
            case 0xEE:  /* XRI d8 */
                xra(memory.readByte(PC) & 0xFF);
                PC += 1;
                break;
            case 0xF6:  /* ORI d8 */
                ora(memory.readByte(PC) & 0xFF);
                PC += 1;
                break;
            case 0xFE:  /* CPI d8 */
            {
                int value = memory.readByte(PC) & 0xFF;
                PC += 1;
                cmp(value);
                break;
            }
            case 0xC3:  /* JMP addr */
            {
                int addr = memory.readByte(PC) & 0xFF;
                addr |= (memory.readByte(PC + 1) & 0xFF) << 8;
                PC = addr;
                break;
            }
            case 0xC2: case 0xCA: case 0xD2: case 0xDA: case 0xE2: case 0xEA: case 0xF2: case 0xFA:
            {
                // Условные переходы: JNZ, JZ, JNC, JC, JPO, JPE, JP, JM
                int addr = memory.readByte(PC) & 0xFF;
                addr |= (memory.readByte(PC + 1) & 0xFF) << 8;
                PC += 2;
                int condCode = (opcode >> 3) & 0x7;
                if (checkCondition(condCode)) {
                    PC = addr;
                }
                break;
            }
            case 0xE9:  /* PCHL (PC = HL) */
                PC = getHL();
                break;
            case 0xCD:  /* CALL addr */
            {
                int addr = (memory.readByte(PC) & 0xFF) | ((memory.readByte(PC + 1) & 0xFF) << 8);
                PC += 2;
                pushWord(PC);
                PC = addr;
                break;
            }
            case 0xC4: case 0xCC: case 0xD4: case 0xDC: case 0xE4: case 0xEC: case 0xF4: case 0xFC:
            {
                // CNZ, CZ, CNC, CC, CPO, CPE, CP, CM
                int addr = (memory.readByte(PC) & 0xFF) | ((memory.readByte(PC + 1) & 0xFF) << 8);
                PC += 2;
                int condCode = (opcode >> 3) & 0x7;
                if (checkCondition(condCode)) {
                    pushWord(PC);
                    PC = addr;
                }
                break;
            }
            case 0xC7: case 0xCF: case 0xD7: case 0xDF: case 0xE7: case 0xEF: case 0xF7: case 0xFF:
            {
                int rstIndex = (opcode >> 3) & 0x7;
                pushWord(PC);
                PC = rstIndex * 8;
                break;
            }
            case 0xC9:  /* RET */
                PC = popWord();
                break;
            case 0xC0: case 0xC8: case 0xD0: case 0xD8: case 0xE0: case 0xE8: case 0xF0: case 0xF8:
            {
                // Условные RET: RNZ, RZ, RNC, RC, RPO, RPE, RP, RM
                int condCode = (opcode >> 3) & 0x7;
                if (checkCondition(condCode)) {
                    PC = popWord();
                }
                break;
            }
            case 0xC1:  /* POP B (C <- [SP]; B <- [SP+1]) */
            {
                int value = popWord();
                B = (value >> 8) & 0xFF;
                C = value & 0xFF;
                break;
            }
            case 0xC5:  /* PUSH B */
            {
                int value = (B << 8) | C;
                pushWord(value);
                break;
            }
            case 0xD1:  /* POP D */
            {
                int value = popWord();
                D = (value >> 8) & 0xFF;
                E = value & 0xFF;
                break;
            }
            case 0xD5:  /* PUSH D */
            {
                int value = (D << 8) | E;
                pushWord(value);
                break;
            }
            case 0xE1:  /* POP H */
            {
                int value = popWord();
                H = (value >> 8) & 0xFF;
                L = value & 0xFF;
                break;
            }
            case 0xE5:  /* PUSH H */
            {
                int value = (H << 8) | L;
                pushWord(value);
                break;
            }
            case 0xF1:  /* POP PSW (Flags:A from stack) */
            {
                int value = popWord();
                A = (value >> 8) & 0xFF;
                int flags = value & 0xFF;
                carryFlag    = (flags & 0x01) != 0;
                parityFlag   = (flags & 0x04) != 0;
                auxCarryFlag = (flags & 0x10) != 0;
                zeroFlag     = (flags & 0x40) != 0;
                signFlag     = (flags & 0x80) != 0;
                break;
            }
            case 0xF5:  /* PUSH PSW */
            {
                int flags =
                        (carryFlag ? 0x01 : 0) |
                                0x02 |
                                (parityFlag ? 0x04 : 0) |
                                (auxCarryFlag ? 0x10 : 0) |
                                (zeroFlag ? 0x40 : 0) |
                                (signFlag ? 0x80 : 0);
                int value = (A << 8) | (flags & 0xFF);
                pushWord(value);
                break;
            }
            case 0xE3:  /* XTHL */
            {
                // L <-> [SP], H <-> [SP+1]
                int valL = memory.readByte(SP) & 0xFF;
                int valH = memory.readByte(SP + 1) & 0xFF;
                int oldL = L, oldH = H;
                memory.writeByte(SP, oldL);
                memory.writeByte(SP + 1, oldH);
                H = valH;
                L = valL;
                break;
            }
            case 0xEB:  /* XCHG */
            {
                int oldD = D, oldE = E;
                D = H;
                E = L;
                H = oldD;
                L = oldE;
                break;
            }
            case 0xF9:  /* SPHL (SP = HL) */
                SP = getHL();
                break;
            case 0xDB:  /* IN port */
            {
                int port = memory.readByte(PC) & 0xFF;
                PC += 1;
                if (ioHandler != null) {
                    A = ioHandler.portIn(port) & 0xFF;
                } else {
                    A = 0;
                }
                break;
            }
            case 0xD3:  /* OUT port */
            {
                int port = memory.readByte(PC) & 0xFF;
                PC += 1;
                if (ioHandler != null) {
                    ioHandler.portOut(port, A);
                }
                break;
            }
            case 0xFB:  /* EI */
                enableInterrupts();
                break;
            case 0xF3:  /* DI */
                disableInterrupts();
                break;
            default:
                break;
        }
        cycles = CYCLES[opcode];
        return cycles;
    }

    // Чтение значения 16-битного регистра HL
    private int getHL() {
        return ((H & 0xFF) << 8) | (L & 0xFF);
    }
    // Запись значения в HL
    private void setHL(int value) {
        H = (value >> 8) & 0xFF;
        L = value & 0xFF;
    }

    private int getRegister(int code) {
        switch (code) {
            case 0: return B;
            case 1: return C;
            case 2: return D;
            case 3: return E;
            case 4: return H;
            case 5: return L;
            case 6:
                return memory.readByte(getHL()) & 0xFF;
            case 7: return A;
        }
        return 0;
    }

    private void setRegister(int code, int value) {
        value &= 0xFF;
        switch (code) {
            case 0: B = value; break;
            case 1: C = value; break;
            case 2: D = value; break;
            case 3: E = value; break;
            case 4: H = value; break;
            case 5: L = value; break;
            case 6: // M (память [HL])
                memory.writeByte(getHL(), value);
                break;
            case 7: A = value; break;
        }
    }

    private int incrementByte(int val) {
        val = (val & 0xFF);
        int result = (val + 1) & 0xFF;
        // Флаги: Z, S, P, AC; флаг Carry не изменяется для INR
        signFlag = (result & 0x80) != 0;
        zeroFlag = (result == 0);
        parityFlag = calculateParity(result);
        auxCarryFlag = ((val & 0x0F) + 1) > 0x0F;
        return result;
    }

    private int decrementByte(int val) {
        val = (val & 0xFF);
        int result = (val - 1) & 0xFF;
        signFlag = (result & 0x80) != 0;
        zeroFlag = (result == 0);
        parityFlag = calculateParity(result);
        auxCarryFlag = (val & 0x0F) < 1;
        return result;
    }

    // Установка флагов Z, S, P
    private void setFlagsZS(int value) {
        value &= 0xFF;
        signFlag = (value & 0x80) != 0;
        zeroFlag = (value == 0);
        parityFlag = calculateParity(value);
    }

    private boolean calculateParity(int value) {
        value &= 0xFF;
        return (Integer.bitCount(value) % 2 == 0);
    }

    private boolean checkCondition(int condCode) {
        switch (condCode) {
            case 0: return !zeroFlag;       // NZ
            case 1: return zeroFlag;        // Z
            case 2: return !carryFlag;      // NC
            case 3: return carryFlag;       // C
            case 4: return !parityFlag;     // PO (Parity Odd: P=0)
            case 5: return parityFlag;      // PE (Parity Even: P=1)
            case 6: return !signFlag;       // P (Plus: S=0)
            case 7: return signFlag;        // M (Minus: S=1)
        }
        return false;
    }

    private void add(int value) {
        int a = A;
        int result = a + (value & 0xFF);
        carryFlag = (result & 0x100) != 0;
        auxCarryFlag = (((a & 0x0F) + (value & 0x0F)) & 0x10) != 0;
        A = result & 0xFF;
        setFlagsZS(A);
    }

    private void adc(int value) {
        int a = A;
        int c = carryFlag ? 1 : 0;
        int result = a + (value & 0xFF) + c;
        carryFlag = (result & 0x100) != 0;
        auxCarryFlag = (((a & 0x0F) + (value & 0x0F) + c) & 0x10) != 0;
        A = result & 0xFF;
        setFlagsZS(A);
    }

    private void sub(int value) {
        int a = A;
        int val = value & 0xFF;
        int result = a - val;
        carryFlag = (result & 0x100) != 0;
        auxCarryFlag = ((a & 0x0F) - (val & 0x0F)) < 0;
        A = result & 0xFF;
        setFlagsZS(A);
    }

    private void sbb(int value) {
        int a = A;
        int c = carryFlag ? 1 : 0;
        int val = value & 0xFF;
        int result = a - val - c;
        carryFlag = (result & 0x100) != 0;
        auxCarryFlag = ((a & 0x0F) - (val & 0x0F) - c) < 0;
        A = result & 0xFF;
        setFlagsZS(A);
    }

    private void ana(int value) {
        A = A & (value & 0xFF);
        carryFlag = false;
        auxCarryFlag = true;
        setFlagsZS(A);
    }

    private void xra(int value) {
        A = A ^ (value & 0xFF);
        carryFlag = false;
        auxCarryFlag = false;
        setFlagsZS(A);
    }

    private void ora(int value) {
        A = A | (value & 0xFF);
        carryFlag = false;
        auxCarryFlag = false;
        setFlagsZS(A);
    }

    private void cmp(int value) {
        int val = value & 0xFF;
        int res = (A - val) & 0x1FF;
        carryFlag = (res & 0x100) != 0;
        auxCarryFlag = ((A & 0x0F) - (val & 0x0F)) < 0;
        int res8 = res & 0xFF;
        signFlag = (res8 & 0x80) != 0;
        zeroFlag = (res8 == 0);
        parityFlag = calculateParity(res8);
    }

    private void decimalAdjustAccumulator() {
        int correction = 0;
        if (auxCarryFlag || (A & 0x0F) > 0x09) {
            correction |= 0x06;
        }
        if (carryFlag || (A > 0x99)) {
            correction |= 0x60;
            carryFlag = true;
        }
        int result = (A + correction) & 0xFF;
        auxCarryFlag = ((A ^ result) & 0x10) != 0;
        A = result;
        signFlag = (A & 0x80) != 0;
        zeroFlag = (A == 0);
        parityFlag = calculateParity(A);
    }

    private void pushWord(int value) {
        int high = (value >> 8) & 0xFF;
        int low = value & 0xFF;
        SP = (SP - 1) & 0xFFFF;
        memory.writeByte(SP, high);
        SP = (SP - 1) & 0xFFFF;
        memory.writeByte(SP, low);
    }

    private int popWord() {
        int low = memory.readByte(SP) & 0xFF;
        SP = (SP + 1) & 0xFFFF;
        int high = memory.readByte(SP) & 0xFF;
        SP = (SP + 1) & 0xFFFF;
        return (high << 8) | low;
    }

    public void requestInterrupt(int rstVec) {
        if (!interruptsEnabled) {
            return;
        }
        interruptsEnabled = false;
        halted = false;
        pushWord(PC);
        PC = (rstVec & 0x07) * 8;
    }
}
