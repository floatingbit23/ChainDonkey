package simpleblockchain;

import java.security.PrivateKey;
import java.security.PublicKey;
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
	
    // Array list of TransactionInput objects
	public ArrayList<TransactionInput> inputs = new ArrayList<>();

    // Array list of TransactionOutput objects (UTXOs)
	public ArrayList<TransactionOutput> outputs = new ArrayList<>();
	
	private static int sequence = 0; // a rough count of how many transactions have been generated

    // Constructor
    public Transaction(PublicKey from, PublicKey to, float value, ArrayList<TransactionInput> inputs) {
        this.senderPK = from;
        this.recipientPK = to;
        this.value = value;
        this.inputs = inputs;
    }
    

    // Method to calculate the SHA256 hash of the transaction
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
        
        // Prepare the data to be signed (a concatenated String of -> senderPK (from) + recipientPK (to) + value)
        String data = StringUtil.getStringFromKey(senderPK) + // From
                      StringUtil.getStringFromKey(recipientPK) + // To
                      Float.toString(value); // Amount of funds
        
        // Generate the signature
        signature = StringUtil.applyECDSASig(privateKey, data); // (privateKey, from+to+value)
    }


    // Method to verify that the signature is valid and the data hasn't been tampered with
    public boolean isSignatureValid() {
        
        // Reconstruct the same data used during signing
        String data = StringUtil.getStringFromKey(senderPK) + // From
                      StringUtil.getStringFromKey(recipientPK) + // To
                      Float.toString(value); // Amount of funds
        
        // Verify the signature
        return StringUtil.verifyECDSASig(senderPK, data, signature); // (publicKey, from+to+value, signature)
    }


    // Boolean method to process the transaction
    public boolean processTransaction(){

        // 1. Verify the transaction signature
        if(! isSignatureValid()){
            System.out.println("#Transaction Signature failed to verify");
            return false;
        }

        // 2. Gather input amounts (making sure they are unspent)
        for(TransactionInput input : inputs){ // Loop through all the inputs in the ArrayList

            // Get the UTXO (Unspent Transaction Output) from the HashMap
            input.UTXO = SimpleBlockchain.UTXOs.get(input.transactionOutputId);

            // Maps UTXO object to its transaction output ID, and stores it in input.UTXO
            // If the UTXO is not found, it means it has already been spent
        }

        // 3. Check if the transaction is valid
        if(getInputsValue() < SimpleBlockchain.minimumTransaction) { // If the transaction is too small (less than the minimum transaction amount), it is invalid
            System.out.println("#Transaction Input is  too small: " + getInputsValue());
            return false;
        }

        // 4. Generate transaction outputs ("las vueltas")
		float leftOver = getInputsValue() - value; //   Get value of inputs then the left over change

		transactionId = calculateHash(); // Calculate the hash of the transaction

		outputs.add(new TransactionOutput( this.recipientPK, value,transactionId)); // Sends value to the Recipient's address
		outputs.add(new TransactionOutput( this.senderPK, leftOver,transactionId)); // Send the leftOver 'change' ("las vueltas") back to the Sender's address

        /*
        Example:
        - getInputsValue() = 10 (coins I have)
        - value = 2 (coins I want to pay)
        - leftOver = 8 (coins I get back)
        - outputs.add(...) -> Sends 2 coins to the Recipient's address (payments)
        - outputs.add(...) -> Sends 8 coins to the Sender's address (change)
        */
        
        // 5. Add outputs to Unspent Map
		for(TransactionOutput output : outputs) { // Loop through all the outputs in the ArrayList
			SimpleBlockchain.UTXOs.put(output.id , output); // Add the output to the Unspent list (key = output ID, value = output Object)
		}
            
        // 6. Remove transaction inputs from UTXO lists as spent
        // Ensuring a transaction output can only be used once as an input
        
		for(TransactionInput input : inputs) { // Loop through all the inputs in the ArrayList
            
            // if UTXO can't be found, skip it 
			if(input.UTXO == null) continue; 

            // Remove the output from the Unspent list
			SimpleBlockchain.UTXOs.remove(input.UTXO.id); // removing the key (ID) from the HashMap
		}


        return true; // If all checks pass, then the transaction is valid
    }


    // Returns sum of inputs (UTXOs) values
	public float getInputsValue() {
		float total = 0; // Initialize total to 0

		for(TransactionInput i : inputs) { // Loop through all the inputs in the ArrayList
			if(i.UTXO == null) continue; //if UTXO can't be found, skip it 
			total += i.UTXO.value; // If the UTXO is found, add its value to the total
		}

		return total; // Return the total
	}

    // Returns sum of outputs
	public float getOutputsValue() {

		float total = 0; // Initialize total to 0

		for(TransactionOutput o : outputs) { // Loop through all the outputs in the ArrayList
			total += o.value; // If the output is found, add its value to the total
		}

		return total; // Return the total
	}

}
