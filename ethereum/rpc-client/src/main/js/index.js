abi = require('web3-eth-abi')

decodeLog = (inputs, hex, topics) => abi.decodeLog([...inputs], hex, [...topics]);
decodeParameters = (types, hex) => abi.decodeParameters([...types], hex);
encodeParameters = (types, parameters) => abi.encodeParameters([...types], [...parameters])
encodeFunctionCall = (abiItem, params) => abi.encodeFunctionCall(JSON.parse(abiItem), JSON.parse(params))