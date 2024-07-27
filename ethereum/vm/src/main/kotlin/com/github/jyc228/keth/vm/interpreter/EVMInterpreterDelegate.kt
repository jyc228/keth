package com.github.jyc228.keth.vm.interpreter

import com.github.jyc228.keth.vm.EVMFrame
import com.github.jyc228.keth.vm.EVMReturn
import com.github.jyc228.keth.vm.Operation

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