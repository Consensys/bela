package org.hyperledger.bela;

import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.plugin.data.Quantity;

import java.math.BigInteger;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockUtils {
  private static final ObjectMapper mapper = new ObjectMapper()
      .enable(SerializationFeature.INDENT_OUTPUT);;
  private static final Logger LOG = LoggerFactory.getLogger(Bela.class);

  public static String prettyPrintBlockHeader(final BlockSearchResult header) {
    try {
      return mapper.writeValueAsString(header);
    } catch (JsonProcessingException e) {
      LOG.error("error writing block", e);
    }
    return "error writing block";
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonPropertyOrder({
      "number",
      "hash",
      "mixHash",
      "parentHash",
      "nonce",
      "sha3Uncles",
      "logsBloom",
      "transactionsRoot",
      "stateRoot",
      "receiptsRoot",
      "miner",
      "difficulty",
      "totalDifficulty",
      "extraData",
      "baseFeePerGas",
      "size",
      "gasLimit",
      "gasUsed",
      "timestamp",
      "uncles",
      "transactions"
  })
  public static class BlockSearchResult {

    protected final String hash;
    private final long number;
    private final String mixHash;
    private final String parentHash;
    private final long nonce;
//    private final String sha3Uncles;
//    private final String logsBloom;
    private final String transactionsRoot;
    private final String stateRoot;
    private final String receiptsRoot;
    private final String miner;
    private final String difficulty;
    private final String extraData;
    private final BigInteger baseFeePerGas;
    private final long gasLimit;
    private final long gasUsed;
    private final long timestamp;
    private final String coinbase;

    public BlockSearchResult(
        final BlockHeader header) {
      this.number = header.getNumber();
      this.hash = header.getHash().toHexString();
      this.mixHash = header.getMixHash().toHexString();
      this.parentHash = header.getParentHash().toHexString();
      this.nonce = header.getNonce();
//      this.sha3Uncles = header.getOmmersHash().toString();
//      this.logsBloom = header.getLogsBloom().toShortHexString();
      this.transactionsRoot = header.getTransactionsRoot().toHexString();
      this.stateRoot = header.getStateRoot().toHexString();
      this.receiptsRoot = header.getReceiptsRoot().toHexString();
      this.miner = header.getCoinbase().toString();
      this.difficulty = header.getDifficulty().toHexString();
      this.extraData = header.getExtraData().toHexString();
      this.baseFeePerGas = header.getBaseFee().map(Quantity::getAsBigInteger).orElse(null);
      this.gasLimit = header.getGasLimit();
      this.gasUsed = header.getGasUsed();
      this.timestamp = header.getTimestamp();
      this.coinbase = header.getCoinbase().toString();
    }

    @JsonGetter(value = "number")
    public long getNumber() {
      return number;
    }

    @JsonGetter(value = "hash")
    public String getHash() {
      return hash;
    }

    @JsonGetter(value = "mixHash")
    public String getMixHash() {
      return mixHash;
    }

    @JsonGetter(value = "parentHash")
    public String getParentHash() {
      return parentHash;
    }

    @JsonGetter(value = "nonce")
    public long getNonce() {
      return nonce;
    }

//    @JsonGetter(value = "sha3Uncles")
//    public String getSha3Uncles() {
//      return sha3Uncles;
//    }
//
//    @JsonGetter(value = "logsBloom")
//    public String getLogsBloom() {
//      return logsBloom;
//    }

    @JsonGetter(value = "transactionsRoot")
    public String getTransactionsRoot() {
      return transactionsRoot;
    }

    @JsonGetter(value = "stateRoot")
    public String getStateRoot() {
      return stateRoot;
    }

    @JsonGetter(value = "receiptsRoot")
    public String getReceiptsRoot() {
      return receiptsRoot;
    }

    @JsonGetter(value = "miner")
    public String getMiner() {
      return miner;
    }

    @JsonGetter(value = "difficulty")
    public String getDifficulty() {
      return difficulty;
    }

    @JsonGetter(value = "extraData")
    public String getExtraData() {
      return extraData;
    }

    @JsonGetter(value = "baseFeePerGas")
    public BigInteger getBaseFeePerGas() {
      return baseFeePerGas;
    }

    @JsonGetter(value = "gasLimit")
    public long getGasLimit() {
      return gasLimit;
    }

    @JsonGetter(value = "gasUsed")
    public long getGasUsed() {
      return gasUsed;
    }

    @JsonGetter(value = "timestamp")
    public long getTimestamp() {
      return timestamp;
    }

    @JsonGetter(value = "coinbase")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCoinbase() {
      return coinbase;
    }
  }

}
