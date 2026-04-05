package simpleblockchain;
 
import java.security.KeyPair; // Used for KeyPairGenerator, PrivateKey, and PublicKey
import java.security.KeyPairGenerator; // Used to specify the ECC parameter spec
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Class to represent a wallet
public class Wallet {
    
    public PrivateKey privateKey; // to sign transactions
    public PublicKey publicKey; // to verify transactions

    // Hash map used to store the UTXOs owned by this wallet
    public HashMap<String, TransactionOutput> UTXOs = new HashMap<>();

    // Constructor
    public Wallet() {
        generateKeyPair(); 
    }

    // Method to generate key pair (marked as final to be safe during constructor call)
    public final void generateKeyPair() {

        try {

            // Register Bouncy Castle provider (necessary if using the "BC" provider)
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

            // Create a KeyPairGenerator object, using the ECDSA (Elliptic Curve Digital Signature Algorithm) algorithm and the Bouncy Castle provider
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");

            // Create a SecureRandom object, using the SHA1PRNG algorithm
            // (SHA1PRNG is a cryptographically strong pseudo-random number generator)
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

            // Initialize the KeyPairGenerator with an ECC (Elliptic Curve Cryptography) parameter spec (prime192v1)
            // Standard 192-bit curve used in Bitcoin and Ethereum, very efficient computationally
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");

            // Initialize the KeyPairGenerator with the ECC parameter spec and the SecureRandom object
            keyGen.initialize(ecSpec, random);

            // Generate the key pair
            KeyPair keyPair = keyGen.generateKeyPair();

            // Set the private and public keys
            privateKey = keyPair.getPrivate(); 
            publicKey = keyPair.getPublic();

        } catch (java.security.NoSuchAlgorithmException
                 | java.security.NoSuchProviderException
                 | java.security.InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e); // throw the exception if any of the above three exceptions occur
        } 

    }

    // Method that returns balance of the wallet ('total')
    // and stores the UTXO's owned by this wallet in 'this.UTXOs'
    public float getBalance() {

        float total = 0;

        // Loop through all the values (the UTXOs) in the HashMap
        for (TransactionOutput output : SimpleBlockchain.UTXOs.values()) {

            // If the UTXO is owned by this wallet (True)
            if(output.isMine(publicKey)) {
                UTXOs.put(output.id, output); // Add the output pair (ID, UTXO) to the HashMap
                total += output.value; // Add the UTXO's value (coins) to the total
            }
        }

        return total; 
    }

    // Method that generates and returns a new transaction from this Wallet
    public Transaction sendFunds(PublicKey recipientPK, float value){
        
        if(getBalance() < value) {
            System.out.println("\n[ FAIL ] Not enough funds to send the transaction. Transaction discarded.");
            return null; 
        }

        // Create an Array List of inputs
        ArrayList<TransactionInput> inputs = new ArrayList<>();

        float total = 0;

        // Loop through all the UTXOs owned by this Wallet
        for (Map.Entry<String, TransactionOutput> item : UTXOs.entrySet()) {

            TransactionOutput output = item.getValue();  // Get the UTXO object

            inputs.add(new TransactionInput(output.id)); // Add the UTXO's ID to the inputs Array List

            total += output.value; // Add the UTXO's value (coins) to the total

            if(total >= value) break; // If the total is greater than or equal to the value, break the loop
        }

        // Create a new transaction
        Transaction newTransaction = new Transaction(this.publicKey, recipientPK, value, inputs);

        // Sign the transaction
        newTransaction.generateSignature(this.privateKey);

        // Remove the UTXOs used in the transaction
        for(TransactionInput input: inputs){
			UTXOs.remove(input.transactionOutputId);
		}

        // Return the new transaction
        return newTransaction;
    }




}