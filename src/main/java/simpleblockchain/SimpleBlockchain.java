package simpleblockchain;

import java.util.ArrayList;

// Class to represent the blockchain
public class SimpleBlockchain {

    public static ArrayList<Block> blockchain = new ArrayList<>(); // ArrayList to store the blocks
    public static int difficulty = 6; // Difficulty of the blockchain (static variable)

    public static void main(String[] args) {

        // Adding our blocks to the blockchain ArrayList...

        // === GENESIS BLOCK ===

        System.out.println("\nTrying to Mine block 1... ");

        // Create the genesis block (the first block in the chain, previousHash is "0" because it doesn't exist)
        Block genesisBlock = new Block("Hi im the first block", "0");

        // Mine the genesis block
        genesisBlock.mineBlock(difficulty);

        // Add the genesis block to the blockchain
        blockchain.add(genesisBlock);

        // === BLOCK 2 ===

        System.out.println("\nTrying to Mine block 2... ");
        // the previous block is located obtaining the current size of the blockchain minus 1
        Block block2 = new Block("Yo im the second block", blockchain.get(blockchain.size() - 1).hash);
        block2.mineBlock(difficulty);
        blockchain.add(block2);

        // === BLOCK 3 ===

        System.out.println("\nTrying to Mine block 3... ");
        Block block3 = new Block("Hey im the third block", blockchain.get(blockchain.size() - 1).hash);
        block3.mineBlock(difficulty);
        blockchain.add(block3);

        // === CHECKING IF THE BLOCKCHAIN IS VALID ===

        System.out.println("\nIs the blockchain valid? -> " + isChainValid());

        // Convert the blockchain to a JSON string
        String blockchainJson = StringUtil.getJson(blockchain);

        // Print the blockchain
        System.out.println("\nThe JSON blockchain: ");
        System.out.println(blockchainJson);
    }


    // Boolean method to check if the blockchain is valid
    public static Boolean isChainValid() {

        Block currentBlock; 
        Block previousBlock;

        // Create a string of zeros with the same length as the Difficulty specified (e.g. 5 -> "00000")
        String hashTarget = new String(new char[difficulty]).replace('\0', '0'); 

        // loop through the blocks (starting from the second block) to check hashes
        for (int i = 1; i < blockchain.size(); i++) {

            // Get the current block and the previous block
            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i - 1);

            // If the current block's hash is not equal to its calculated hash, the blockchain is invalid
            if (! currentBlock.hash.equals(currentBlock.calculateHash())) {
                System.out.println("❌  Current Hashes don't match. Blockchain is invalid!");
                return false;
            }

            // If the previous block's hash is not equal to the current block's previous hash, the blockchain is invalid
            if (! previousBlock.hash.equals(currentBlock.previousHash)) {
                System.out.println("❌  Previous Hashes don't match. Blockchain is invalid!");
                return false;
            }

            // If the current block's hash does not start with the target String, the blockchain is invalid
            if (! currentBlock.hash.substring(0, difficulty).equals(hashTarget)) {
                System.out.println("❌  This block hasn't been mined. Blockchain is invalid!");
                return false;
            }

        }

        return true; // If all checks pass, the blockchain is valid
    }


    // Public method to add a block to the blockchain
    public static void addBlock(Block newBlock) {
        newBlock.mineBlock(difficulty); // Mine the new block with the established difficulty
        blockchain.add(newBlock); // Add the new block to the blockchain
    }
}