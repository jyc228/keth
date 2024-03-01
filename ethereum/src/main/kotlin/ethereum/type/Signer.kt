package ethereum.type

import ethereum.collections.Hash
import ethereum.crypto.ECDSASignature

interface Signer {
    //    // Sender returns the sender address of the transaction.
//    Sender(tx *Transaction) (common.Address, error)
//
    // SignatureValues returns the raw R, S, V values corresponding to the
    // given signature.
    fun signatureValues(txType: Byte, sig: ByteArray): ECDSASignature
//    ChainID() *big.Int

    // Hash returns 'signature hash', i.e. the transaction hash that is signed by the
    // private key. This hash does not uniquely identify the transaction.
    fun hash(tx: Transaction): Hash

    // Equal returns true if the given signer is the same as the receiver.
//    Equal(Signer) bool
}
