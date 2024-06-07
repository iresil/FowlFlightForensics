package FowlFlightForensics.config;

import org.apache.kafka.streams.state.RocksDBConfigSetter;
import org.apache.kafka.streams.state.internals.BlockBasedTableConfigWithAccessibleCache;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.CompactionStyle;
import org.rocksdb.Options;

import java.util.Map;

public class RocksDBConfig implements RocksDBConfigSetter {
    @Override
    public void setConfig(final String storeName, final Options options, final Map<String, Object> configs) {
        // BlockBasedTableConfig tableConfig = new org.rocksdb.BlockBasedTableConfig();
        // Reduce block cache size from the default as the total number of store RocksDB databases is
        // partitions (40) * segments (3) = 120.
        // @see https://github.com/apache/kafka/blob/1.0/streams/src/main/java/org/apache/kafka/streams/state/internals/RocksDBStore.java#L81
        BlockBasedTableConfig tableConfig = new BlockBasedTableConfigWithAccessibleCache();
        tableConfig.setBlockCacheSize(16 * 1024 * 1024L);
        // tableConfig.setBlockSize(16 * 1024L);
        // Modify the default block size per these instructions from the RocksDB GitHub.
        // @see https://github.com/facebook/rocksdb/wiki/Memory-usage-in-RocksDB#indexes-and-filter-blocks
        tableConfig.setBlockSize(16 * 1024L);
        // tableConfig.setCacheIndexAndFilterBlocks(true);
        // Do not let the index and filter blocks grow unbounded.
        // For more information, @see https://github.com/facebook/rocksdb/wiki/Block-Cache#caching-index-and-filter-blocks
        tableConfig.setCacheIndexAndFilterBlocks(true);
        options.setTableFormatConfig(tableConfig);
        // options.setMaxWriteBufferNumber(2);
        // See the advanced options in the RocksDB GitHub.
        // @see https://github.com/facebook/rocksdb/blob/8dee8cad9ee6b70fd6e1a5989a8156650a70c04f/include/rocksdb/advanced_options.h#L103
        options.setMaxWriteBufferNumber(2);
        // @see https://www.confluent.io/blog/how-to-tune-rocksdb-kafka-streams-state-stores-performance/
        options.setCompactionStyle(CompactionStyle.UNIVERSAL);
    }

    @Override
    public void close(final String s, final Options options) {
    }
}
