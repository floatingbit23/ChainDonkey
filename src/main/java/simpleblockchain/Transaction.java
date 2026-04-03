package simpleblockchain;

import java.security.*;
import java.util.ArrayList;

/*
Each transaction will carry a certain amount of data:

    1. The public key (address) of the sender of funds.
    2. The public key (address) of the receiver of funds.
    3. The value/amount of funds to be transferred.
    4. Inputs, which are references to previous transactions that prove the sender has funds to send.
    5. Outputs, which shows the amount relevant addresses received in the transaction.
        ( These outputs are referenced as inputs in new transactions )
    6. A cryptographic signature, that proves the following:
        - The owner of the address is the one sending this transaction
        - The transaction data hasn’t been changed.
        ( for example: preventing a third party from changing the amount sent) 
*/

public class Transaction {
    
    public String transactionId; // this is also the hash of the transaction
	public PublicKey senderPK; // Sender's address/public key
	public PublicKey recipientPK; // Recipient's address/public key
	public float value; // Amount of funds to be transferred
	public byte[] signature; // Cryptographic signature (array of bytes) to prove ownership and prevent tampering
	
	public ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
	public ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
	
	private static int sequence = 0; // a rough count of how many transactions have been generated

    // Constructor
    public Transaction(PublicKey from, PublicKey to, float value, ArrayList<TransactionInput> inputs) {
        this.senderPK = from;
        this.recipientPK = to;
        this.value = value;
        this.inputs = inputs;
    }
    

    // Method to calculate the hash of the transaction
    private String calculateHash() {

        // Increment the sequence to ensure uniqueness (prevents identical transactions from having the same hash)
        sequence++;

        // Create a concatenated String from the transaction data (sender + recipient + value + sequence)
        String data = StringUtil.getStringFromKey(senderPK) + 
                      StringUtil.getStringFromKey(recipientPK) + 
                      Float.toString(value) +
                      Integer.toString(sequence);

        // Apply SHA256 hash to the final String
        return StringUtil.applySha256(data);
    }
    
    
    // Method to generate the cryptographic signature using the sender's private key
    public void generateSignature(PrivateKey privateKey) {
        
        // Prepare the data to be signed (a concatenated String of -> senderPK + recipientPK + value)
        String data = StringUtil.getStringFromKey(senderPK) + // From
                      StringUtil.getStringFromKey(recipientPK) + // To
                      Float.toString(value); // Amount of funds
        
        // Generate the signature
        signature = StringUtil.applyECDSASig(privateKey, data);
    }

    // Method to verify that the signature is valid and the data hasn't been tampered with
    public boolean isSignatureValid() {
        
        // Reconstruct the same data used during signing
        String data = StringUtil.getStringFromKey(senderPK) + // From
                      StringUtil.getStringFromKey(recipientPK) + // To
                      Float.toString(value); // Amount of funds
        
        // Verify the signature
        return StringUtil.verifyECDSASig(senderPK, data, signature);
    }

}
