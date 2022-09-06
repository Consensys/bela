package org.hyperledger.besu.plugin.services.storage.rocksdb.segmented;

import org.hyperledger.besu.plugin.services.metrics.Counter;

public class NoOpCounter implements Counter {

    @Override
    public void inc() {}

    @Override
    public void inc(final long amount) {}
}
