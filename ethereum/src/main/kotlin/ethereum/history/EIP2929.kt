package ethereum.history

import ethereum.evm.Address
import ethereum.evm.EVM
import ethereum.evm.Stack
import java.math.BigInteger

/**
 * [Gas cost increases for state access opcodes](https://eips.ethereum.org/EIPS/eip-2929)
 */
object EIP2929 {
    val coldAccountAccessCost = 2600
    val warmStorageReadCost = 2100
    fun makeCallVariantGasCall(gasFunc: (EVM, Stack) -> BigInteger): (EVM, Stack) -> BigInteger {
        return { evm, stack ->
            val address = Address.fromString("")
            // Check slot presence in the access list
            val warmAccess = false
            if (!warmAccess) {
                // Charge the remaining difference here already, to correctly calculate available
                // gas for call
            }
            // warmAccess : = evm.StateDB.AddressInAccessList(addr)

            // The WarmStorageReadCostEIP2929 (100) is already deducted in the form of a constant cost, so
            // the cost to charge for cold access, if any, is Cold - Warm
            val coldCost = coldAccountAccessCost - warmStorageReadCost

            // Now call the old calculator, which takes into account
            // - create new account
            // - transfer value
            // - memory expansion
            // - 63/64ths rule
            val gas = gasFunc(evm, stack)
            if (warmAccess) {
                gas
            } else {
                // In case of a cold access, we temporarily add the cold charge back, and also
                // add it to the returned gas. By adding it to the return, it will be charged
                // outside of this function, as part of the dynamic gas, and that will make it
                // also become correctly reported to tracers.
                gas + coldCost.toBigInteger()
            }
        }
    }
}