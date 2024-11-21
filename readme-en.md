### Introduction
This repository is based on [NoobChain](https://github.com/CryptoKass/NoobChain-Tutorial-Part-2) by [@CryptoKass](https://github.com/CryptoKass), extended to operate on a network as a simple blockchain system.

### Preconditions
#### UTXOs
All UTXOs are managed in the list.  
UTXOs used when creating transactions are removed from the list, and UTXOs generated as a result of transactions are added to the list.  
During transaction verification, **if the UTXO used in the transaction is not in the list, the transaction is considered invalid**.
#### Mempool
All transactions not yet included in a block exist in the mempool.  
When mining a block, all transactions in the mempool are included to create the block.  
During block verification, **if a transaction not in the mempool is included in the block, the block is considered invalid**.
#### Genesis Block
All nodes are given identical Genesis transactions via file input.  
Genesis transactions are not digitally signed. (Transactions should be signed with the sender's private key, but Genesis transactions have no sender)  
All nodes begin mining a Genesis block containing Genesis transactions. The node that successfully mines the block broadcasts it.
#### Other
Block mining difficulty and the minimum number of transactions per block remain fixed during application execution.  
These values can be adjusted before the application starts.  
When the application starts, it first requests and stores the public keys of nodes, then attempts to mine the Genesis block. After the Genesis block is added to the blockchain, it begins user input for block mining and transaction creation.

### Implemented Features
#### Transaction Creation
Nodes create and broadcast transactions based on user input.
#### Transaction Reception
Verify transactions created by other nodes.  
If the transaction is valid, update the UTXO list and add the transaction to the mempool.
#### Block Mining
Continuously mine blocks containing transactions in the mempool.  
If mining is successful, attempt to add the block to the blockchain.  
If another block containing mempool transactions is received and added to the blockchain during mining, this block will not be added to the blockchain.
#### Block Reception
Attempt to add blocks created by other nodes to the blockchain. (Block verification is performed during this process)

### Unimplemented Features
#### P2P Network
Nodes are assumed to know the addresses of all nodes participating in the blockchain. Scenarios of existing nodes leaving or new nodes joining the network are not considered.
#### Mining Rewards

#### Mining Difficulty Adjustment

### Implementation Details
#### Block Mining Thread `MinorThread`
Blocked when fewer than the minimum number of transactions exist in the mempool.  
Begins mining a block including these transactions when sufficient transactions are present.
#### User Input and Transaction Creation Thread `UserThread`
Creates transactions based on user-provided information.
#### Web Server Thread `WebReceiverThread`
Runs a web server capable of receiving public key requests, transactions, and blocks.
#### Key Methods
#### Transaction
##### `transaction.process()`
- Called when a transaction is created or received from another node.
- Verifies the transaction, and if valid, updates the UTXO list and adds the transaction to the mempool.
#### Block
##### `block.process()`
- Called before mining a created block and when receiving a block from another node.
- If the `block` is a created `Block` object, it includes transactions from the mempool and attempts to mine, then tries to add to the blockchain.
- If the `block` is a block created by another node, it attempts to add to the blockchain.
- Block verification occurs during the blockchain addition attempt.
##### `block.mineBlock()`
- Performs mining on the created `Block` object.

### Areas for Improvement
#### Consensus Algorithm Improvement
In the current situation, if multiple nodes successfully mine blocks with identical transactions, it is impossible for these nodes to reach a consensus.
#### Blockchain Branching Handling