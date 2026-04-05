package simpleblockchain;

import java.security.Key;
import java.security.MessageDigest;
import java.security.PrivateKey; // Used to convert Objects to JSON strings
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Base64;

import com.google.gson.GsonBuilder;

// Class to handle SHA-256 hashing and JSON conversion
public class StringUtil {


    // Public static method that applies SHA-256 to the received String, and returns the result
    public static String applySha256(String input) {

        try {

            // Create a MessageDigest object, using the SHA-256 algorithm
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Applies sha256 to our String
            byte[] hash = digest.digest(input.getBytes("UTF-8"));

            /*
            getBytes() converts the String to an array of bytes.
            digest() calculates the SHA-256 hash of the byte array.
            byte[] is an array of bytes. Each byte is a number between -128 and 127.
            */

            // Dynamic container for the hash as a hexidecimal string
            StringBuilder hexString = new StringBuilder(); 

            /*
            StringBuilder is a modern, mutable sequence of characters (faster than StringBuffer for single-threaded tasks).
            It is used to build strings in a more efficient way than using the "+" operator with Strings.
            */

            // Loop through the 32 hash bytes
            for (int i = 0; i < hash.length; i++) {

                String hex = Integer.toHexString(0xff & hash[i]);

                /*
                '0xff & hash[i]' ensures that the byte is treated as an unsigned integer (signed range (-128 to 127) -> unsigned range(0 to 255)).
                'toHexString()' converts the byte to a hexidecimal character.
                */

                // If the hexidecimal character is only one character long (e.g. 15 -> f), add a '0' to the beginning (f -> 0f)
                if (hex.length() == 1){
                    hexString.append('0');
                }

                // Appends the hexidecimal character to the end of the container
                hexString.append(hex); 
            }

            // Convert the container to a String (unmutable) and return it
            return hexString.toString(); // Returns 64 hexadecimal characters (256 bits)

        } catch (java.security.NoSuchAlgorithmException
                 | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e); //
        }
    }


    // Public static method that turns an Object into a JSON String
    public static String getJson(Object o) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(o);
    }


    // Public static method that returns the difficulty String target, to compare to hash.
    public static String getDifficultyString(int difficulty) {
        return new String(new char[difficulty]).replace('\0', '0');
    }

    /*
    Difficulty is the number of leading zeros required for a hash to be considered valid.
    The higher the difficulty, the more zeros are required, and the harder it is to mine a block.

    For example; a difficulty of 5 will return "00000"
    */

    // ECDSA stands for Elliptic Curve Digital Signature Algorithm

    // Method to create the cryptographic signature (ECDSA)
    // > Receives the sender's private key (PrivateKey) and the Transaction Data (String)
    // >> Returns the signature (array of bytes)
    public static byte[] applyECDSASig(PrivateKey privateKey, String input) {

    Signature sig; // Object used to create the signature (dsa = digital signature algorithm)

    byte[] output = new byte[0]; // Array of bytes to store the signature (initialized to empty)

    try {

        sig = Signature.getInstance("ECDSA", "BC"); // Create a Signature object using the ECDSA algorithm and the Bouncy Castle provider
       
        sig.initSign(privateKey); // Initialize the Signature object with the private key

        byte[] strByte = input.getBytes(); // Convert the transaction data to an array of bytes

        sig.update(strByte); // Update the Signature object with the transaction data

        byte[] realSignature = sig.sign(); // Create the full signature (Private Key + Transaction Data)

        output = realSignature; // Store the signature in the output array

    } catch (java.security.NoSuchAlgorithmException
             | java.security.NoSuchProviderException
             | java.security.InvalidKeyException
             | java.security.SignatureException e) {
        throw new RuntimeException(e);
    }

    return output;
}
	

	// Method to verify an ECDSA signature
    // > Receives the sender's public key (PublicKey), the Transaction Data (String), and the signature (byte[])
    // >> Returns a boolean (true if the signature is valid, false otherwise)
	public static boolean verifyECDSASig(PublicKey publicKey, String data, byte[] signature) {
		
        try {

            // Create a Signature object using the ECDSA algorithm and the Bouncy Castle provider
			Signature sig = Signature.getInstance("ECDSA", "BC");

            // Initialize the Signature object with the Public Key
			sig.initVerify(publicKey);

            // Update the Signature object with the transaction data
			sig.update(data.getBytes());

            // Now we have the full signature (Public Key + Transaction Data)

            // Now we can verify the signature
			return sig.verify(signature);

		} catch (java.security.NoSuchAlgorithmException
				 | java.security.NoSuchProviderException
				 | java.security.InvalidKeyException
				 | java.security.SignatureException e) {
			throw new RuntimeException(e);
		}
	}


    // Method to convert a Key to a String
    // > Receives a Key (PublicKey or PrivateKey) and returns its String representation (Base64 encoded)
	public static String getStringFromKey(Key key) {
        byte[] encodedKey = key.getEncoded(); // Converts the Key to an array of bytes
		return Base64.getEncoder().encodeToString(encodedKey); // Converts the array of bytes to a String (Base64 encoded)
	}

    /*
    In Java, a cryptographic key is an Object, therefore it cannot be directly printed to the console.
    We must first convert the Key to an array of bytes (using the 'getEncoded()' method).
    Then, we can convert the array of bytes to a String (using the 'Base64.getEncoder().encodeToString()' method).
    */


    // === LOGIC FOR MERKLE TREE ===

    // Method to calculate the Merkle Root (the root hash of all the transactions)
    // > Receives an ArrayList of Transactions
    // >> Returns a String (the Merkle Root)
    public static String getMerkleRoot(ArrayList<Transaction> transactions) {

		int count = transactions.size(); // Number of transactions/hashes in the current layer

		// 1. Create an empty ArrayList to store the transaction IDs (Strings)
		ArrayList<String> previousTreeLayer = new ArrayList<>();

		// Loop through all the transactions
		for(Transaction transaction : transactions) {
			previousTreeLayer.add(transaction.transactionId); // Add the transaction ID to the ArrayList
		}

		// 2. Create an ArrayList to store the current layer of the Merkle Tree
		ArrayList<String> treeLayer = previousTreeLayer;

        // 3. Loop until we have a single hash (the Merkle Root)

		while(count > 1) { // While the count is greater than 1 (meaning we don't have a single hash yet)

			treeLayer = new ArrayList<>(); // Create a new empty ArrayList to store the current layer

            // Loop through the previous layer (taking two hashes at a time)
            // We increment by 2 to take two hashes at a time
			for(int i=0; i < previousTreeLayer.size(); i+=2) { 
				
                // Get the left and right hashes 
                String left = previousTreeLayer.get(i);
                String right = (i + 1 < previousTreeLayer.size()) ? previousTreeLayer.get(i + 1) : left;

                // Concatenate the left and right hashes and hash them
                String combined = applySha256(left + right);

                // Add the combined hash to the current layer
                treeLayer.add(combined);

			}

			count = treeLayer.size(); // Update the count
			previousTreeLayer = treeLayer; // Update the previous layer
		}

        /*
        Example:

        Transactions: [T1, T2, T3, T4], each transaction has its own hash (H1, H2, H3, H4) -> previousTreeLayer, previousTreeLayer.size() = 4

        Layer 1: [H1, H2, H3, H4] -> treeLayer (first iteration), treeLayer.size() = 4 -> count = 4 -> new previousLayer
        Layer 2: [H1H2, H3H4] -> treeLayer (second iteration), treeLayer.size() = 2 -> count = 2 -> new previousLayer
        Layer 3: [H1H2H3H4] -> Merkle Root (third iteration), treeLayer.size() = 1 (Merkle Root) -> count = 1 (so the while loop ends)
        */

		// If the treeLayer has only one element, it's the Merkle Root (ternary operator)
		String merkleRoot = (treeLayer.size() == 1) ? treeLayer.get(0) : ""; 
     
        return merkleRoot;
	}

}