package simpleblockchain;

import java.util.ArrayList;
import java.util.Date;

// Class to represent a block in the blockchain
public class Block {

	public String hash; // digital signature of the current block
	public String previousHash; // digital signature of the previous block
	public String merkleRoot; // hash of the transactions in this block

	// List of transactions included in this block
	public ArrayList<Transaction> transactions = new ArrayList<>();

	private final long timeStamp; // milliseconds since Epoch (1/1/1970)
	private int nonce; // private 'magic number' used once to calculate the hash

	// Block Constructor
	public Block(String previousHash) {

		// Set the data and previous hash
		this.previousHash = previousHash;

		// Set the time stamp
		this.timeStamp = new Date().getTime(); // Get current time

		// Calculate the hash of the block
		this.hash = calculateHash(); // Making sure we do this after we set the other values.
	}

	// Public method to calculate new hash based on block's content
	public final String calculateHash() {

		// Concatenate all the block's data (previousHash + timeStamp + nonce + data) in
		// a String and apply SHA256 to it
		String calculatedhash = StringUtil.applySha256(
				previousHash +
						Long.toString(timeStamp) +
						Integer.toString(nonce) +
						merkleRoot);

		return calculatedhash; // Returns the calculated hash
	}

	// Public method that mines a block
	// It receives the Difficulty and increases nonce value until hash target is reached
	public void mineBlock(int difficulty) {

		// Get the Merkle Root of the transactions of the block
		merkleRoot = StringUtil.getMerkleRoot(transactions);

		// Create a string of zeros with the same length as the Difficulty specified
		// (e.g. 5 -> "00000")
		String target = StringUtil.getDifficultyString(difficulty);

		// Loop until the hash starts with the target string
		while (!hash.substring(0, difficulty).equals(target)) { // while substring != "00000"...
			nonce++; // Increment nonce
			hash = calculateHash(); // Recalculate hash
		}

		// The loop is exited once the hash starts with the target string, then we print
		// the hash
		System.out.println("\n[ MINER ] Block Mined! -> " + hash);
	}

	/*
	 * If we mine a block and the data never changes, the hash will never change.
	 * The incremental nonce ensures that the hash will always change, even if the
	 * data doesn't.
	 * This is what makes mining computationally expensive in real blockchains.
	 */

	// Boolean method to add a transaction to the block
	public boolean addTransaction(Transaction tx) {

		// process transaction and check if valid, unless block is Genesis block, then
		// ignore.
		if (tx == null)
			return false;

		if ((!previousHash.equals("0"))) { // if previous block IS NOT the Genesis block

			if ((tx.processTransaction() != true)) { // if transaction is not valid (could not be processed)
				System.out.println("\n[ FAIL ] Transaction failed to process. Discarded from block.");
				return false;
			}
		}

		// Add the transaction to the list of transactions
		transactions.add(tx);
		System.out.println("\n[ OK ] Transaction successfully added to block.");
		return true;
	}

}