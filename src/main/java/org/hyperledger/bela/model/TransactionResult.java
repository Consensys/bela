package org.hyperledger.bela.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import org.hyperledger.besu.datatypes.AccessListEntry;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.ethereum.core.Transaction;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public class TransactionResult {

    private final String type;
    private final String hash;
    private final long nonce;
    private final String gasPrice;
    private final long gasLimit;
    private final String to;
    private final String sender;
    private final String value;
    private final String payload;
    private final String chainId;
    private final String signature;
    private final String maxPriorityFeePerGas;
    private final String maxFeePerGas;
    private final Optional<List<AccessListEntry>> accessList;

    public TransactionResult(final Transaction transaction) {
        type = transaction.getType().name();
        hash = transaction.getHash().toHexString();
        nonce = transaction.getNonce();
        gasPrice = transaction.getGasPrice().map(Wei::toHexString).orElse("");
        gasLimit = transaction.getGasLimit();
        to = transaction.getTo().map(Address::toHexString).orElse("");
        sender = transaction.getSender().toHexString();
        value = transaction.getValue().toHexString();
        payload = transaction.getPayload().toHexString();
        chainId = transaction.getChainId().map(BigInteger::toString).orElse("");
        signature = transaction.getSignature().encodedBytes().toHexString();

        // EIP-1559 fields
        maxPriorityFeePerGas = transaction.getMaxPriorityFeePerGas().map(Wei::toHexString).orElse("");
        maxFeePerGas = transaction.getMaxFeePerGas().map(Wei::toHexString).orElse("");
        accessList = transaction.getAccessList();
    }

    @JsonGetter(value = "type")
    public String getType() {
        return type;
    }

    @JsonGetter(value = "hash")
    public String getHash() {
        return hash;
    }

    @JsonGetter(value = "nonce")
    public long getNonce() {
        return nonce;
    }

    @JsonGetter(value = "gasPrice")
    public String getGasPrice() {
        return gasPrice;
    }

    @JsonGetter(value = "gasLimit")
    public long getGasLimit() {
        return gasLimit;
    }

    @JsonGetter(value = "to")
    public String getTo() {
        return to;
    }

    @JsonGetter(value = "sender")
    public String getSender() {
        return sender;
    }

    @JsonGetter(value = "value")
    public String getValue() {
        return value;
    }

    @JsonGetter(value = "payload")
    public String getPayload() {
        return payload;
    }

    @JsonGetter(value = "chainId")
    public String getChainId() {
        return chainId;
    }

    @JsonGetter(value = "signature")
    public String getSignature() {
        return signature;
    }

    @JsonGetter(value = "maxPriorityFeePerGas")
    public String getMaxPriorityFeePerGas() {
        return maxPriorityFeePerGas;
    }

    @JsonGetter(value = "maxFeePerGas")
    public String getMaxFeePerGas() {
        return maxFeePerGas;
    }

    @JsonGetter(value = "accessList")
    public Optional<List<AccessListEntry>> getAccessList() {
        return accessList;
    }
}
