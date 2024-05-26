import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.rpc.EthereumClient
import io.github.jyc228.ethereum.rpc.fromRpcUrl
import io.github.jyc228.ethereum.state.OffchainStateDatabase
import io.github.jyc228.ethereum.state.account.Address
import io.github.jyc228.ethereum.vm.EVMConsoleLogger
import io.github.jyc228.ethereum.vm.EVMContract
import io.github.jyc228.ethereum.vm.EVMFrame
import io.github.jyc228.ethereum.vm.EVMInterpreter
import io.github.jyc228.ethereum.vm.InstructionSet
import java.math.BigInteger

suspend fun main() {
    val client = EthereumClient.fromRpcUrl("https://rpc.ankr.com/eth")
    simulateTransaction(Hash.create("0x791167e07b6654c298f6763f299fedf778ae5fce810710472d0a3d9383640e09"), client)
}

@OptIn(ExperimentalStdlibApi::class)
private suspend fun simulateTransaction(txHash: Hash, client: EthereumClient) {
    val tx = client.eth.getTransactionByHash(txHash).awaitOrThrow()!!
    val header = client.eth.getHeaderByNumber(tx.blockNumber.number).awaitOrThrow()
    val database = OffchainStateDatabase(header.hash, client)
    val context = EVMFrame(
        contract = database.withAccountOrThrow(Address.fromHexString(requireNotNull(tx.to).hex), EVMContract::of),
        callData = tx.input.removePrefix("0x").hexToByteArray(),
        caller = Address.fromHexString(tx.from.hex),
        callValue = tx.value.number,
        remainGas = tx.gas.number.toInt()
    ).with(
        database,
        EVMFrame.BlockContext(
            number = header.number.number,
            difficulty = header.totalDifficulty?.number?.toLong()?.toULong() ?: 0uL,
            time = header.timestamp.epochSeconds.toULong(),
            gasLimit = header.gasLimit.number,
            random = header.mixHash.hex.removePrefix("0x").toByteArray(),
            coinbase = Address.fromHexString(header.miner?.hex ?: "0x"),
            baseFee = header.baseFeePerGas?.number ?: BigInteger.ZERO
        ),
        EVMFrame.TransactionContext(
            Address.fromHexString(tx.from.hex),
            Address.fromHexString(requireNotNull(tx.to).hex),
            requireNotNull(tx.gasPrice?.number),
            tx.value.number
        )
    )
    EVMInterpreter.of(InstructionSet.all(), EVMConsoleLogger()).execute(context)
}
