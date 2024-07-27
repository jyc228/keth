package com.github.jyc228.keth.vm.interpreter

import com.github.jyc228.keth.vm.EVMFrame
import com.github.jyc228.keth.vm.EVMReturn
import com.github.jyc228.keth.vm.Operation

class EVMConsoleLogger : EVMInterpreterDelegate {
    private var index = 0

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun execute(
        frame: EVMFrame,
        operation: Operation?,
        execute: suspend (EVMFrame, Operation?) -> EVMReturn?
    ): EVMReturn? {
        index++
        if (operation != null) {
            val beforeGas = frame.remainGas
            var prefix = "$index\t${frame.pc}\t${frame.remainGas}\t${operation.opCode}"
            if (operation.opCode.name.startsWith("SLOAD")) {
                prefix += "(${frame.contract.address}, ${frame.stack.last()})"
            }
            println(prefix)
        }
        return execute(frame, operation)
    }
}