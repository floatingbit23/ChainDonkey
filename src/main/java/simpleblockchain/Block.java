package simpleblockchain;

import java.util.Date;

// Class to represent a block in the blockchain
public class Block {
	
	public String hash; // digital signature of the current block
	public String previousHash; // digital signature of the previous block
	private String data; // current block data (for now, just a String)

	private long timeStamp; // milliseconds since Epoch (1/1/1970)

	private int nonce; // private 'magic number' used once to calculate the hash
	
	//Block Constructor
	public Block(String data,String previousHash ) {

		// Set the data and previous hash
		this.data = data;
		this.previousHash = previousHash;

		// Set the time stamp
		this.timeStamp = new Date().getTime(); // Get current time

		// Calculate the hash of the block
		this.hash = calculateHash(); // Making sure we do this after we set the other values.
	}
	

	//Public method to calculate new hash based on block's content
	public String calculateHash() {

		//Concatenate all the block's data (previousHash + timeStamp + nonce + data) in a String and apply SHA256 to it
		String calculatedhash = StringUtil.applySha256( 
				previousHash +
				Long.toString(timeStamp) +
				Integer.toString(nonce) + 
				data 
				);

		return calculatedhash; // Returns the calculated hash
	}
	

	// Public method that mines a block
	// It receives the Difficulty and increases nonce value until hash target is reached
	public void mineBlock(int difficulty) {

		// Create a string of zeros with the same length as the Difficulty specified (e.g. 5 -> "00000")
		String target = StringUtil.getDifficultyString(difficulty); 

		// Loop until the hash starts with the target string
		while(! hash.substring(0, difficulty).equals(target)) { // while substring != "00000"...
			nonce ++; // Increment nonce
			hash = calculateHash(); // Recalculate hash
		}

		// The loop is exited once the hash starts with the target string, then we print the hash
		System.out.println("Block Mined! -> " + hash);
	}

	/*
	If we mine a block and the data never changes, the hash will never change. 
	The incremental nonce ensures that the hash will always change, even if the data doesn't.
	This is what makes mining computationally expensive in real blockchains.
	*/
	
}