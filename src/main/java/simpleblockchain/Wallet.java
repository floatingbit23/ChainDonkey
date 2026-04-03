package simpleblockchain;
 
import java.security.KeyPair; // Used for KeyPairGenerator, PrivateKey, and PublicKey
import java.security.KeyPairGenerator; // Used to specify the ECC parameter spec
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;

// Class to represent a wallet
public class Wallet {
    
    public PrivateKey privateKey; // to sign transactions
    public PublicKey publicKey; // to verify transactions

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

        } catch (Exception e) {
            throw new RuntimeException(e);
        } 

    }

}