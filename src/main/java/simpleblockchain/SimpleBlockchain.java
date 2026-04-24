package simpleblockchain;

import java.security.Security;
import java.util.ArrayList; 
import java.util.HashMap;

import network.protocol.kad.KadId;
import network.protocol.kad.RoutingTable;

// Class to represent the blockchain
public class SimpleBlockchain {

    public static ArrayList<Block> blockchain = new ArrayList<>(); // ArrayList to store the blocks
    public static int difficulty = 6; // Difficulty of the blockchain (static)
    public static float minimumTransaction = 0.1f; // Minimum amount of coins to be transferred

    // Unspent Map(key, value) -> mapea los UTXOs (Unspent Transaction Outputs) y evita duplicar transacciones
    // Key: ID de un billete viejo (TransactionOutput.id) -> String
    // Value: Billete viejo (TransactionOutput) -> Object
    public static HashMap<String, TransactionOutput> UTXOs = new HashMap<>();

    public static Wallet walletA;
    public static Wallet walletB; 
    
    public static Transaction genesisTransaction; // Transacción génesis (la primera transacción en la blockchain)
    public static RoutingTable routingTable; // Tabla de rutas Kad para el Dashboard

    public static void main(String[] args) {
        
        // Setup Bouncey castle as a Security Provider
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); 

        // Inicializar Kademlia para el Dashboard
        KadId localId = KadId.random();
        routingTable = new RoutingTable(localId);

        // Start the Web Dashboard
        BlockchainServer.start(blockchain, routingTable);

        // Create the new wallets
        walletA = new Wallet();
        walletB = new Wallet();
        Wallet walletC = new Wallet();
        Wallet walletD = new Wallet();
        Wallet coinbase = new Wallet(); // The wallet that will receive the mining rewards

        /*
         When a new wallet is created, the method generateKeyPair() is automatically called (because it is inside the constructor of the Wallet class).
         This means that the private and public keys are generated automatically when the wallet instance is created.
         They are stored in the privateKey and publicKey variables of the Wallet class.
        */

        // === HARDCODING THE GENESIS BLOCK and GENESIS TRANSACTION ===

        // 0. Create the genesis transaction (the first transaction in the blockchain)
        genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, 100f, null);
        genesisTransaction.generateSignature(coinbase.privateKey);
        genesisTransaction.transactionId = "0"; // First transaction, no ID (we'll set it to "0" to make it easier to identify)
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.recipientPK, 100, genesisTransaction.transactionId)); // Add the output (100 coins) to the transaction
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); // Add the output (100 coins) to the UTXOs HashMap
        // it's very important to store our first transaction in the UTXOs list
        
        // 1. Create the genesis block (the first block in the chain, previousHash is "0" because it doesn't exist)
        System.out.println("\n[ INFO ] Creating and Mining Genesis block... ");
        Block genesisBlock = new Block("0"); 

        // 2. Add the genesis transaction to the genesis block
        genesisBlock.addTransaction(genesisTransaction);

        // 3. Mine the genesis block
        genesisBlock.mineBlock(difficulty);

        // 4. Finally, add the genesis block to the blockchain
        blockchain.add(genesisBlock);

         // === Testing with new blocks ===

		// Block 1
        Block block1 = new Block(genesisBlock.hash);

		System.out.println("\nWalletA's balance is: " + walletA.getBalance());
		System.out.println("WalletA is Attempting to send funds (40 coins) to WalletB...");

		block1.addTransaction(walletA.sendFunds(walletB.publicKey, 40f)); // sendFunds() returns a Transaction object, addTransaction() processes it and adds it to the block (if valid)
		addBlock(block1); // addBlock() mines the block (mineBlock()) and adds it to the blockchain (blockchain.add())

		System.out.println("\nWalletA's balance is: " + walletA.getBalance()); // 100 - 40 = 60 coins
		System.out.println("WalletB's balance is: " + walletB.getBalance()); // 40 coins

       // Block 2

       Block block2 = new Block(block1.hash);

       System.out.println("\nWalletB is Attempting to send funds (10 coins) to WalletA...");
       block2.addTransaction(walletB.sendFunds(walletA.publicKey, 10f));
       addBlock(block2);

       System.out.println("\nWalletA's balance is: " + walletA.getBalance()); // 60 + 10 = 70 coins
       System.out.println("WalletB's balance is: " + walletB.getBalance()); // 40 - 10 = 30 coins
        

       // Block 3

       Block block3 = new Block(block2.hash);

       System.out.println("\nWalletA is Attempting to send funds (57 coins) to WalletB...");
       block3.addTransaction(walletA.sendFunds(walletB.publicKey, 57f));
       addBlock(block3);

       System.out.println("\nWalletA's balance is: " + walletA.getBalance()); // 13 coins
       System.out.println("WalletB's balance is: " + walletB.getBalance()); // 87 coins

       // Block 4: Distributing to new users
       Block block4 = new Block(block3.hash);
       System.out.println("\nWalletB is distributing funds to WalletC and WalletD...");
       block4.addTransaction(walletB.sendFunds(walletC.publicKey, 25f));
       block4.addTransaction(walletB.sendFunds(walletD.publicKey, 15f));
       addBlock(block4);

       System.out.println("WalletC balance: " + walletC.getBalance()); // 25
       System.out.println("WalletD balance: " + walletD.getBalance()); // 15

       // Block 5: Complex flow and verification
       Block block5 = new Block(block4.hash);
       System.out.println("\nWalletC sends funds to WalletA and WalletD sends back to WalletB...");
       block5.addTransaction(walletC.sendFunds(walletA.publicKey, 10f));
       block5.addTransaction(walletD.sendFunds(walletB.publicKey, 5f));
       addBlock(block5);

       System.out.println("Final WalletA balance: " + walletA.getBalance()); // 23
       System.out.println("Final WalletB balance: " + walletB.getBalance()); // 52

        // === CHECKING IF THE BLOCKCHAIN IS VALID ===
        isChainValid();

        // Convert the blockchain to a JSON string and print it
        String blockchainJson = StringUtil.getJson(blockchain);
        System.out.println("\nThe JSON blockchain: ");
        System.out.println(blockchainJson);
        
    }


    // Boolean method to check if the blockchain is valid
    public static Boolean isChainValid() {

        Block currentBlock; 
        Block previousBlock;

        // Create a string of zeros with the same length as the Difficulty specified (e.g. 5 -> "00000")
        String hashTarget = new String(new char[difficulty]).replace('\0', '0'); 

        // A temporary working list of unspent transactions at a given block state.
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<>(); 
        
        // Add the UTXO from the genesis transaction to the temporary list
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));
		
        // == Nested loops to check validity of the blockchain==

        // Loop through blockchain to check hashes
		for(int i=1; i < blockchain.size(); i++) {
			
			currentBlock = blockchain.get(i); 
			previousBlock = blockchain.get(i-1);
			
			// Compare registered hash and calculated hash
			if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
				System.out.println("#Current Hashes not equal");
				return false;
			}

			// Compare previous hash and registered previous hash
			if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
				System.out.println("#Previous Hashes not equal");
				return false;
			}

			// Check if hash is solved
			if(!currentBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
				System.out.println("#This block hasn't been mined");
				return false;
			}

			TransactionOutput tempOutput; // Temporary working list of unspent transactions at a given block state.

            // Loop through all transactions of the current block
			for(int j=0; j < currentBlock.transactions.size(); j++) {

				Transaction currentTransaction = currentBlock.transactions.get(j); // Get the current transaction
				
				// Verify the transaction signature
				if(!currentTransaction.isSignatureValid()) {
					System.out.println("#Signature on Transaction(" + j + ") is Invalid");
					return false; 
				}

				// Verify that the inputs value equals the outputs value
				if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
					System.out.println("#Inputs are note equal to outputs on Transaction(" + j + ")");
					return false; 
				}
				
				// Loop through all inputs of the current transaction
				for(TransactionInput input: currentTransaction.inputs) {	

					tempOutput = tempUTXOs.get(input.transactionOutputId); // Get the UTXO from the temporary list
					
					// Check if the UTXO is null (meaning it doesn't exist)
					if(tempOutput == null) {
						System.out.println("#Referenced input on Transaction(" + j + ") is Missing");
						return false;
					}
					
                    // Check if the UTXO's value is equal to the input's value
					if(input.UTXO.value != tempOutput.value) {
						System.out.println("#Referenced input Transaction(" + j + ") value is Invalid");
						return false;
					}
					
					tempUTXOs.remove(input.transactionOutputId); // Remove the UTXO from the temporary list (it's been spent)
				}
				
                // Loop through all outputs of the current transaction
				for(TransactionOutput output: currentTransaction.outputs) {
					tempUTXOs.put(output.id, output); // Add the output to the temporary list (it's now unspent)
				}
				
                // Check if the recipient of the first output is the same as the recipient in the transaction
				if(!currentTransaction.outputs.get(0).recipient.equals(currentTransaction.recipientPK)) {
					System.out.println("#Transaction(" + j + ") output reciepient is not who it should be"); // if so, it's invalid
					return false;
				}
				
                // Check if the recipient of the second output (change) is the same as the sender in the transaction
				if(currentTransaction.outputs.size() > 1 && !currentTransaction.outputs.get(1).recipient.equals(currentTransaction.senderPK)) {
					System.out.println("#Transaction(" + j + ") output 'change' is not sender."); // if so, it's invalid
					return false;
				}
				
			}

        }

        // If all checks pass, theN THE blockchain is valid
		System.out.println("\n[ SUCCESS ] Blockchain is valid!");
		
        return true;
    }


    // Public method to add a block to the blockchain
    public static void addBlock(Block newBlock) {
        newBlock.mineBlock(difficulty); // Mine the new block with the established difficulty
        blockchain.add(newBlock); // Add the new block to the blockchain
    }
}