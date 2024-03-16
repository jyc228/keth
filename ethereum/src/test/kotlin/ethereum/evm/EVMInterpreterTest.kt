package ethereum.evm

import org.junit.jupiter.api.Test

class EVMInterpreterTest {
    @Test
    fun `loop interrupt`() {
        listOf(
            "60025b8056", // infinite loop using JUMP: push(2) jumpdest dup1 jump
            "600160045b818157" // infinite loop using JUMPI: push(1) push(4) jumpdest dup2 dup2 jumpi
        )
    }
}


//func TestLoopInterrupt(t *testing.T) {
//    address := common.BytesToAddress([]byte("contract"))
//    vmctx := BlockContext{
//        Transfer: func(StateDB, common.Address, common.Address, *big.Int) {},
//    }
//
//    for i, tt := range loopInterruptTests {
//        statedb, _ := state.New(types.EmptyRootHash, state.NewDatabase(rawdb.NewMemoryDatabase()), nil)
//        statedb.CreateAccount(address)
//        statedb.SetCode(address, common.Hex2Bytes(tt))
//        statedb.Finalise(true)
//
//        evm := NewEVM(vmctx, TxContext{}, statedb, params.AllEthashProtocolChanges, Config{})
//
//        errChannel := make(chan error)
//        timeout := make(chan bool)
//
//        go func(evm *EVM) {
//            _, _, err := evm.Call(AccountRef(common.Address{}), address, nil, math.MaxUint64, new(big.Int))
//            errChannel <- err
//        }(evm)
//
//        go func() {
//            <-time.After(time.Second)
//            timeout <- true
//        }()
//
//        evm.Cancel()
//
//        select {
//            case <-timeout:
//            t.Errorf("test %d timed out", i)
//            case err := <-errChannel:
//            if err != nil {
//                t.Errorf("test %d failure: %v", i, err)
//            }
//        }
//    }
//}
