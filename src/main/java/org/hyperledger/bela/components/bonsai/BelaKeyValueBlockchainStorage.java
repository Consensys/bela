package org.hyperledger.bela.components.bonsai;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.chain.BlockchainStorage;
import org.hyperledger.besu.ethereum.chain.TransactionLocation;
import org.hyperledger.besu.ethereum.chain.VariablesStorage;
import org.hyperledger.besu.ethereum.chain.VariablesStorage.Keys;
import org.hyperledger.besu.ethereum.core.BlockBody;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.BlockHeaderFunctions;
import org.hyperledger.besu.ethereum.core.Difficulty;
import org.hyperledger.besu.ethereum.core.TransactionReceipt;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorageTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Remove me.
 * This class is temporary until changes in besu to KeyValueStoragePrefixedKeyBlockchainStorage
 * make it optional to migrate variable storage, to support "read only" opening of besu databases.
 */
public class BelaKeyValueBlockchainStorage implements BlockchainStorage {
  private static final Logger LOG = LoggerFactory.getLogger(
      org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage.class);

  private static final Bytes VARIABLES_PREFIX = Bytes.of(1);
  private static final Bytes BLOCK_HEADER_PREFIX = Bytes.of(2);
  private static final Bytes BLOCK_BODY_PREFIX = Bytes.of(3);
  private static final Bytes TRANSACTION_RECEIPTS_PREFIX = Bytes.of(4);
  private static final Bytes BLOCK_HASH_PREFIX = Bytes.of(5);
  private static final Bytes TOTAL_DIFFICULTY_PREFIX = Bytes.of(6);
  private static final Bytes TRANSACTION_LOCATION_PREFIX = Bytes.of(7);
  final KeyValueStorage blockchainStorage;
  final VariablesStorage variablesStorage;
  final BlockHeaderFunctions blockHeaderFunctions;
  final boolean receiptCompaction;

  public BelaKeyValueBlockchainStorage(KeyValueStorage blockchainStorage,
      VariablesStorage variablesStorage, BlockHeaderFunctions blockHeaderFunctions,
      boolean receiptCompaction) {
    this.blockchainStorage = blockchainStorage;
    this.variablesStorage = variablesStorage;
    this.blockHeaderFunctions = blockHeaderFunctions;
    this.receiptCompaction = receiptCompaction;
  }

  public Optional<Hash> getChainHead() {
    return this.variablesStorage.getChainHead();
  }

  public Collection<Hash> getForkHeads() {
    return this.variablesStorage.getForkHeads();
  }

  public Optional<Hash> getFinalized() {
    return this.variablesStorage.getFinalized();
  }

  public Optional<Hash> getSafeBlock() {
    return this.variablesStorage.getSafeBlock();
  }

  public Optional<BlockHeader> getBlockHeader(Hash blockHash) {
    return this
        .get(BLOCK_HEADER_PREFIX, blockHash)
        .map((b) -> BlockHeader.readFrom(RLP.input(b), this.blockHeaderFunctions));
  }

  public Optional<BlockBody> getBlockBody(Hash blockHash) {
    return this
        .get(BLOCK_BODY_PREFIX, blockHash)
        .map((bytes) -> BlockBody.readWrappedBodyFrom(RLP.input(bytes), this.blockHeaderFunctions));
  }

  public Optional<List<TransactionReceipt>> getTransactionReceipts(Hash blockHash) {
    return this.get(TRANSACTION_RECEIPTS_PREFIX, blockHash).map(this::rlpDecodeTransactionReceipts);
  }

  public Optional<Hash> getBlockHash(long blockNumber) {
    return this.get(BLOCK_HASH_PREFIX, UInt256.valueOf(blockNumber)).map(this::bytesToHash);
  }

  public Optional<Difficulty> getTotalDifficulty(Hash blockHash) {
    return this
        .get(TOTAL_DIFFICULTY_PREFIX, blockHash)
        .map((b) -> Difficulty.wrap(Bytes32.wrap(b, 0)));
  }

  public Optional<TransactionLocation> getTransactionLocation(Hash transactionHash) {
    return this
        .get(TRANSACTION_LOCATION_PREFIX, transactionHash)
        .map((bytes) -> TransactionLocation.readFrom(RLP.input(bytes)));
  }

  public Updater updater() {
    return new Updater(this.blockchainStorage.startTransaction(), this.variablesStorage.updater(),
        this.receiptCompaction);
  }

  private List<TransactionReceipt> rlpDecodeTransactionReceipts(Bytes bytes) {
    return RLP.input(bytes).readList(TransactionReceipt::readFrom);
  }

  private Hash bytesToHash(Bytes bytes) {
    return Hash.wrap(Bytes32.wrap(bytes, 0));
  }

  Optional<Bytes> get(Bytes prefix, Bytes key) {
    return this.blockchainStorage
        .get(Bytes.concatenate(new Bytes[] {prefix, key}).toArrayUnsafe())
        .map(Bytes::wrap);
  }

  public void migrateVariables() {
    Updater blockchainUpdater = this.updater();
    VariablesStorage.Updater variablesUpdater = this.variablesStorage.updater();
    this
        .get(VARIABLES_PREFIX, Keys.CHAIN_HEAD_HASH.getBytes())
        .map(this::bytesToHash)
        .ifPresent((bch) -> {
          this.variablesStorage.getChainHead().ifPresentOrElse((vch) -> {
            if (!vch.equals(bch)) {
              logInconsistencyAndFail(Keys.CHAIN_HEAD_HASH, bch, vch);
            }

          }, () -> {
            variablesUpdater.setChainHead(bch);
            LOG.info("Migrated key {} to variables storage", Keys.CHAIN_HEAD_HASH);
          });
        });
    this
        .get(VARIABLES_PREFIX, Keys.FINALIZED_BLOCK_HASH.getBytes())
        .map(this::bytesToHash)
        .ifPresent((bfh) -> {
          this.variablesStorage.getFinalized().ifPresentOrElse((vfh) -> {
            if (!vfh.equals(bfh)) {
              logInconsistencyAndFail(Keys.FINALIZED_BLOCK_HASH, bfh, vfh);
            }

          }, () -> {
            variablesUpdater.setFinalized(bfh);
            LOG.info("Migrated key {} to variables storage", Keys.FINALIZED_BLOCK_HASH);
          });
        });
    this
        .get(VARIABLES_PREFIX, Keys.SAFE_BLOCK_HASH.getBytes())
        .map(this::bytesToHash)
        .ifPresent((bsh) -> {
          this.variablesStorage.getSafeBlock().ifPresentOrElse((vsh) -> {
            if (!vsh.equals(bsh)) {
              logInconsistencyAndFail(Keys.SAFE_BLOCK_HASH, bsh, vsh);
            }

          }, () -> {
            variablesUpdater.setSafeBlock(bsh);
            LOG.info("Migrated key {} to variables storage", Keys.SAFE_BLOCK_HASH);
          });
        });
    this
        .get(VARIABLES_PREFIX, Keys.FORK_HEADS.getBytes())
        .map((bytes) -> RLP.input(bytes).readList((in) -> this.bytesToHash(in.readBytes32())))
        .ifPresent((bfh) -> {
          Collection<Hash> vfh = this.variablesStorage.getForkHeads();
          if (vfh.isEmpty()) {
            variablesUpdater.setForkHeads(bfh);
            LOG.info("Migrated key {} to variables storage", Keys.FORK_HEADS);
          } else if (!List.copyOf(vfh).equals(bfh)) {
            logInconsistencyAndFail(Keys.FORK_HEADS, bfh, vfh);
          }

        });
    this.get(Bytes.EMPTY, Keys.SEQ_NO_STORE.getBytes()).ifPresent((bsns) -> {
      this.variablesStorage.getLocalEnrSeqno().ifPresentOrElse((vsns) -> {
        if (!vsns.equals(bsns)) {
          logInconsistencyAndFail(Keys.SEQ_NO_STORE, bsns, vsns);
        }

      }, () -> {
        variablesUpdater.setLocalEnrSeqno(bsns);
        LOG.info("Migrated key {} to variables storage", Keys.SEQ_NO_STORE);
      });
    });
    blockchainUpdater.removeVariables();
    variablesUpdater.commit();
    blockchainUpdater.commit();
  }

  private static void logInconsistencyAndFail(VariablesStorage.Keys key, Object bch, Object vch) {
    LOG.error(
        "Inconsistency found when migrating {} to variables storage, probably this is due to a downgrade done without running the `storage revert-variables` subcommand first, see https://github.com/hyperledger/besu/pull/5471",
        key);
    String var10002 = String.valueOf(key);
    throw new IllegalStateException(
        var10002 + " mismatch: blockchain storage value=" + String.valueOf(
            bch) + ", variables storage value=" + String.valueOf(vch));
  }

  public static class Updater implements BlockchainStorage.Updater {
    private final KeyValueStorageTransaction blockchainTransaction;
    private final VariablesStorage.Updater variablesUpdater;
    private final boolean receiptCompaction;

    Updater(KeyValueStorageTransaction blockchainTransaction,
        VariablesStorage.Updater variablesUpdater, boolean receiptCompaction) {
      this.blockchainTransaction = blockchainTransaction;
      this.variablesUpdater = variablesUpdater;
      this.receiptCompaction = receiptCompaction;
    }

    public void putBlockHeader(Hash blockHash, BlockHeader blockHeader) {
      Bytes var10001 = BLOCK_HEADER_PREFIX;
      Objects.requireNonNull(blockHeader);
      this.set(var10001, blockHash, RLP.encode(blockHeader::writeTo));
    }

    public void putBlockBody(Hash blockHash, BlockBody blockBody) {
      Bytes var10001 = BLOCK_BODY_PREFIX;
      Objects.requireNonNull(blockBody);
      this.set(var10001, blockHash, RLP.encode(blockBody::writeWrappedBodyTo));
    }

    public void putTransactionLocation(Hash transactionHash,
        TransactionLocation transactionLocation) {
      Bytes var10001 = TRANSACTION_LOCATION_PREFIX;
      Objects.requireNonNull(transactionLocation);
      this.set(var10001, transactionHash, RLP.encode(transactionLocation::writeTo));
    }

    public void putTransactionReceipts(Hash blockHash,
        List<TransactionReceipt> transactionReceipts) {
      this.set(TRANSACTION_RECEIPTS_PREFIX, blockHash, this.rlpEncode(transactionReceipts));
    }

    public void putBlockHash(long blockNumber, Hash blockHash) {
      this.set(BLOCK_HASH_PREFIX, UInt256.valueOf(blockNumber), blockHash);
    }

    public void putTotalDifficulty(Hash blockHash, Difficulty totalDifficulty) {
      this.set(TOTAL_DIFFICULTY_PREFIX, blockHash, totalDifficulty);
    }

    public void setChainHead(Hash blockHash) {
      this.variablesUpdater.setChainHead(blockHash);
    }

    public void setForkHeads(Collection<Hash> forkHeadHashes) {
      this.variablesUpdater.setForkHeads(forkHeadHashes);
    }

    public void setFinalized(Hash blockHash) {
      this.variablesUpdater.setFinalized(blockHash);
    }

    public void setSafeBlock(Hash blockHash) {
      this.variablesUpdater.setSafeBlock(blockHash);
    }

    public void removeBlockHash(long blockNumber) {
      this.remove(BLOCK_HASH_PREFIX, UInt256.valueOf(blockNumber));
    }

    public void removeBlockHeader(Hash blockHash) {
      this.remove(BLOCK_HEADER_PREFIX, blockHash);
    }

    public void removeBlockBody(Hash blockHash) {
      this.remove(BLOCK_BODY_PREFIX, blockHash);
    }

    public void removeTransactionReceipts(Hash blockHash) {
      this.remove(TRANSACTION_RECEIPTS_PREFIX, blockHash);
    }

    public void removeTransactionLocation(Hash transactionHash) {
      this.remove(TRANSACTION_LOCATION_PREFIX, transactionHash);
    }

    public void removeTotalDifficulty(Hash blockHash) {
      this.remove(TOTAL_DIFFICULTY_PREFIX, blockHash);
    }

    public void commit() {
      this.blockchainTransaction.commit();
      this.variablesUpdater.commit();
    }

    public void rollback() {
      this.variablesUpdater.rollback();
      this.blockchainTransaction.rollback();
    }

    void set(Bytes prefix, Bytes key, Bytes value) {
      this.blockchainTransaction.put(Bytes.concatenate(new Bytes[] {prefix, key}).toArrayUnsafe(),
          value.toArrayUnsafe());
    }

    private void remove(Bytes prefix, Bytes key) {
      this.blockchainTransaction.remove(
          Bytes.concatenate(new Bytes[] {prefix, key}).toArrayUnsafe());
    }

    private Bytes rlpEncode(List<TransactionReceipt> receipts) {
      return RLP.encode((o) -> {
        o.writeList(receipts, (r, rlpOutput) -> {
          r.writeToForStorage(rlpOutput, this.receiptCompaction);
        });
      });
    }

    private void removeVariables() {
      this.remove(VARIABLES_PREFIX, Keys.CHAIN_HEAD_HASH.getBytes());
      this.remove(VARIABLES_PREFIX, Keys.FINALIZED_BLOCK_HASH.getBytes());
      this.remove(VARIABLES_PREFIX, Keys.SAFE_BLOCK_HASH.getBytes());
      this.remove(VARIABLES_PREFIX, Keys.FORK_HEADS.getBytes());
      this.remove(Bytes.EMPTY, Keys.SEQ_NO_STORE.getBytes());
    }
  }
}
