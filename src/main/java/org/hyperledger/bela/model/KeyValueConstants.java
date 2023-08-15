package org.hyperledger.bela.model;

import org.apache.tuweni.bytes.Bytes;

import java.nio.charset.StandardCharsets;

public enum KeyValueConstants {
    NONE(),
    BLOCKCHAIN_PREFIX_VARIABLES((byte) 0x1),
    BLOCKCHAIN_PREFIX_HEADER((byte) 0x2),
    BLOCKCHAIN_PREFIX_BODY((byte) 0x3),
    VARIABLES_SAFE_HEAD_HASH(
            Bytes.wrap("safeBlockHash".getBytes(StandardCharsets.UTF_8)).toArrayUnsafe()),
    VARIABLES_CHAIN_HEAD_HASH(
            Bytes.wrap("chainHeadHash".getBytes(StandardCharsets.UTF_8)).toArrayUnsafe()),
    VARIABLES_FINALIZED_HEAD_HASH(
            Bytes.wrap("finalizedBlockHash".getBytes(StandardCharsets.UTF_8)).toArrayUnsafe()),
    WORLDSTATE_ROOT_HASH(Bytes.wrap("worldRoot".getBytes(StandardCharsets.UTF_8)).toArrayUnsafe()),
    WORLDSTATE_BLOCK_HASH(Bytes.wrap("worldBlockHash".getBytes(StandardCharsets.UTF_8)).toArrayUnsafe());

    byte [] value;
    KeyValueConstants(byte ... val) {
        value = val;
    }
    KeyValueConstants(KeyValueConstants prefix, byte ... val) {
        value = new byte[prefix.value.length + val.length];
        System.arraycopy(prefix.value, 0, value, 0, prefix.value.length);
        System.arraycopy(val, 0, value, prefix.value.length, val.length);
    }

    public String getKeyValue() {
        return Bytes.wrap(value).toHexString();
    }
}
