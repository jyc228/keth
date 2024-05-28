package io.github.jyc228.ethereum.vm.interpreter

import io.github.jyc228.ethereum.vm.EVMFrame
import io.github.jyc228.ethereum.vm.EVMReturn
import io.github.jyc228.ethereum.vm.Operation

interface EVMInterpreterDelegate {
    suspend fun execute(
        frame: EVMFrame,
        execute: suspend (EVMFrame) -> EVMReturn
    ): EVMReturn = execute(frame)

    suspend fun execute(
        frame: EVMFrame,
        operation: Operation?,
        execute: suspend (EVMFrame, Operation?) -> EVMReturn?
    ): EVMReturn? = execute(frame, operation)
}