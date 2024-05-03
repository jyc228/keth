package io.github.jyc228.ethereum.state.snapshot

class SnapshotConfig(
    val cacheSize: Int,
    val recovery: Boolean,
    val noBuild: Boolean,
    val asyncBuild: Boolean
)