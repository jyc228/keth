package ethereum

import java.util.HexFormat
import org.bouncycastle.jcajce.provider.digest.Keccak

fun String.hexToByteArray(): ByteArray = HexFormat.of().parseHex(this)
fun ByteArray.toKeccak256(): ByteArray = Keccak.Digest256().digest(this)
