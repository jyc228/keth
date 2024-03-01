package ethereum.type

import ethereum.evm.Address

class Withdrawals(list: List<Withdrawal>) : ArrayList<Withdrawal>(list)

data class Withdrawal(
    val index: UInt,         // monotonically increasing identifier issued by consensus layer
    val validator: UInt,         // index of validator associated with withdrawal
    val address: Address, // target address for withdrawn ether
    val amount: UInt         // value of withdrawal in Gwei
)
