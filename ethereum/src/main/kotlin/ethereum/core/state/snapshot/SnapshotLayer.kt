package ethereum.core.state.snapshot

interface SnapshotLayer {
    // Root returns the root hash for which this snapshot was made.
//    Root() common.Hash

    // Account directly retrieves the account associated with a particular hash in
    // the snapshot slim data format.
//    Account(hash common.Hash) (*types.SlimAccount, error)

    // AccountRLP directly retrieves the account RLP associated with a particular
    // hash in the snapshot slim data format.
//    AccountRLP(hash common.Hash) ([]byte, error)

    // Storage directly retrieves the storage data associated with a particular hash,
    // within a particular account.
//    Storage(accountHash, storageHash common.Hash) ([]byte, error)
}