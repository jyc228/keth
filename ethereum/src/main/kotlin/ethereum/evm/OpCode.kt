package ethereum.evm

enum class OpCode(val v: Byte) {
    // 0x0 range - arithmetic ops.
    STOP(0x0),
    ADD(0x1),
    MUL(0x2),
    SUB(0x3),
    DIV(0x4),
    SDIV(0x5),
    MOD(0x6),
    SMOD(0x7),
    ADDMOD(0x8),
    MULMOD(0x9),
    EXP(0xa),
    SIGNEXTEND(0xb),

    // 0x10 range - comparison ops.
    LT(0x10),
    GT(0x11),
    SLT(0x12),
    SGT(0x13),
    EQ(0x14),
    ISZERO(0x15),
    AND(0x16),
    OR(0x17),
    XOR(0x18),
    NOT(0x19),
    BYTE(0x1a),
    SHL(0x1b),
    SHR(0x1c),
    SAR(0x1d),

    // 0x20 range - crypto.
    KECCAK256(0x20),

    // 0x30 range - closure state.
    ADDRESS(0x30),
    BALANCE(0x31),
    ORIGIN(0x32),
    CALLER(0x33),
    CALLVALUE(0x34),
    CALLDATALOAD(0x35),
    CALLDATASIZE(0x36),
    CALLDATACOPY(0x37),
    CODESIZE(0x38),
    CODECOPY(0x39),
    GASPRICE(0x3a),
    EXTCODESIZE(0x3b),
    EXTCODECOPY(0x3c),
    RETURNDATASIZE(0x3d),
    RETURNDATACOPY(0x3e),
    EXTCODEHASH(0x3f),

    // 0x40 range - block operations.
    BLOCKHASH(0x40),
    COINBASE(0x41),
    TIMESTAMP(0x42),
    NUMBER(0x43),
    DIFFICULTY(0x44),
    RANDOM(0x44), // Same as DIFFICULTY
    PREVRANDAO(0x44), // Same as DIFFICULTY
    GASLIMIT(0x45),
    CHAINID(0x46),
    SELFBALANCE(0x47),
    BASEFEE(0x48),

    // 0x50 range - 'storage' and execution.
    POP(0x50),
    MLOAD(0x51),
    MSTORE(0x52),
    MSTORE8(0x53),
    SLOAD(0x54),
    SSTORE(0x55),
    JUMP(0x56),
    JUMPI(0x57),
    PC(0x58),
    MSIZE(0x59),
    GAS(0x5a),
    JUMPDEST(0x5b),
    PUSH0(0x5f),

    // 0x60 range - pushes.
    PUSH1(0x60),
    PUSH2(0x61),
    PUSH3(0x62),
    PUSH4(0x63),
    PUSH5(0x64),
    PUSH6(0x65),
    PUSH7(0x66),
    PUSH8(0x67),
    PUSH9(0x68),
    PUSH10(0x69),
    PUSH11(0x6a),
    PUSH12(0x6b),
    PUSH13(0x6c),
    PUSH14(0x6d),
    PUSH15(0x6e),
    PUSH16(0x6f),
    PUSH17(0x70),
    PUSH18(0x71),
    PUSH19(0x72),
    PUSH20(0x73),
    PUSH21(0x74),
    PUSH22(0x75),
    PUSH23(0x76),
    PUSH24(0x77),
    PUSH25(0x78),
    PUSH26(0x79),
    PUSH27(0x7a),
    PUSH28(0x7b),
    PUSH29(0x7c),
    PUSH30(0x7d),
    PUSH31(0x7e),
    PUSH32(0x7f),

    // 0x80 range - dups.
    DUP1(0x80.toByte()),
    DUP2(0x81.toByte()),
    DUP3(0x82.toByte()),
    DUP4(0x83.toByte()),
    DUP5(0x84.toByte()),
    DUP6(0x85.toByte()),
    DUP7(0x86.toByte()),
    DUP8(0x87.toByte()),
    DUP9(0x88.toByte()),
    DUP10(0x89.toByte()),
    DUP11(0x8a.toByte()),
    DUP12(0x8b.toByte()),
    DUP13(0x8c.toByte()),
    DUP14(0x8d.toByte()),
    DUP15(0x8e.toByte()),
    DUP16(0x8f.toByte()),

    // 0x90 range - swaps.
    SWAP1(0x90.toByte()),
    SWAP2(0x91.toByte()),
    SWAP3(0x92.toByte()),
    SWAP4(0x93.toByte()),
    SWAP5(0x94.toByte()),
    SWAP6(0x95.toByte()),
    SWAP7(0x96.toByte()),
    SWAP8(0x97.toByte()),
    SWAP9(0x98.toByte()),
    SWAP10(0x99.toByte()),
    SWAP11(0x9a.toByte()),
    SWAP12(0x9b.toByte()),
    SWAP13(0x9c.toByte()),
    SWAP14(0x9d.toByte()),
    SWAP15(0x9e.toByte()),
    SWAP16(0x9f.toByte()),

    // 0xa0 range - logging ops.
    LOG0(0xa0.toByte()),// 160
    LOG1(0xa1.toByte()),
    LOG2(0xa2.toByte()),
    LOG3(0xa3.toByte()),
    LOG4(0xa4.toByte()),

    // 0xb0 range.
    TLOAD(0xb3.toByte()), // 179
    TSTORE(0xb4.toByte()), // 180

    // 0xf0 range - closures.
    CREATE(0xf0.toByte()), // 240
    CALL(0xf1.toByte()),
    CALLCODE(0xf2.toByte()),
    RETURN(0xf3.toByte()),
    DELEGATECALL(0xf4.toByte()),
    CREATE2(0xf5.toByte()),

    STATICCALL(0xfa.toByte()), // 250
    REVERT(0xfd.toByte()), // 253
    INVALID(0xfe.toByte()),
    SELFDESTRUCT(0xff.toByte())
}
