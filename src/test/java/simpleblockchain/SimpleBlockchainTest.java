package simpleblockchain;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.security.Security;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

public class SimpleBlockchainTest {

    @BeforeAll
    public static void setupBC() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @BeforeEach
    public void init() {
        SimpleBlockchain.blockchain.clear();
        SimpleBlockchain.UTXOs.clear();
        SimpleBlockchain.difficulty = 3; // Low difficulty for fast testing
    }

    @Test
    public void testChainValidity() {
        Wallet walletA = new Wallet();
        Wallet walletB = new Wallet();
        Wallet coinbase = new Wallet();

        // 1. Genesis Transaction
        Transaction genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, 100f, null);
        genesisTransaction.generateSignature(coinbase.privateKey);
        genesisTransaction.transactionId = "0";
        TransactionOutput genesisOutput = new TransactionOutput(genesisTransaction.recipientPK, 100, genesisTransaction.transactionId);
        genesisTransaction.outputs.add(genesisOutput);
        SimpleBlockchain.UTXOs.put(genesisOutput.id, genesisOutput);
        SimpleBlockchain.genesisTransaction = genesisTransaction;

        // 2. Genesis Block
        Block genesisBlock = new Block("0");
        genesisBlock.addTransaction(genesisTransaction);
        SimpleBlockchain.addBlock(genesisBlock);

        // 3. Chain should be valid
        assertTrue(SimpleBlockchain.isChainValid());

        // 4. Send funds
        Block block1 = new Block(genesisBlock.hash);
        Transaction tx1 = walletA.sendFunds(walletB.publicKey, 40f);
        block1.addTransaction(tx1);
        SimpleBlockchain.addBlock(block1);

        assertTrue(SimpleBlockchain.isChainValid());
        assertEquals(60f, walletA.getBalance());
        assertEquals(40f, walletB.getBalance());
    }

    @Test
    public void testTamperingDetection() {
        Wallet walletA = new Wallet();
        Wallet walletB = new Wallet();
        Wallet coinbase = new Wallet();

        // Genesis setup
        Transaction genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, 100f, null);
        genesisTransaction.generateSignature(coinbase.privateKey);
        genesisTransaction.transactionId = "0";
        TransactionOutput genesisOutput = new TransactionOutput(genesisTransaction.recipientPK, 100, genesisTransaction.transactionId);
        genesisTransaction.outputs.add(genesisOutput);
        SimpleBlockchain.UTXOs.put(genesisOutput.id, genesisOutput);
        SimpleBlockchain.genesisTransaction = genesisTransaction;

        Block genesisBlock = new Block("0");
        genesisBlock.addTransaction(genesisTransaction);
        SimpleBlockchain.addBlock(genesisBlock);

        // Tamper with Genesis Block (e.g. change timeStamp)
        // Note: hashing is done on previousHash + timeStamp + nonce + merkleRoot
        // We can't easily change private fields but we can check if isChainValid detects tampering.
        
        Block block1 = new Block(genesisBlock.hash);
        block1.addTransaction(walletA.sendFunds(walletB.publicKey, 10f));
        SimpleBlockchain.addBlock(block1);
        
        assertTrue(SimpleBlockchain.isChainValid());

        // MANUALLY Tamper with a transaction in the blockchain
        SimpleBlockchain.blockchain.get(1).transactions.get(0).value = 1000f; 
        
        // This should break the signature OR the hash or the balance logic
        assertFalse(SimpleBlockchain.isChainValid());
    }

    @Test
    public void testDoubleSpending() {
        Wallet walletA = new Wallet();
        Wallet walletB = new Wallet();
        Wallet coinbase = new Wallet();

        // Genesis setup
        Transaction genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, 100f, null);
        genesisTransaction.generateSignature(coinbase.privateKey);
        genesisTransaction.transactionId = "0";
        TransactionOutput genesisOutput = new TransactionOutput(genesisTransaction.recipientPK, 100, genesisTransaction.transactionId);
        genesisTransaction.outputs.add(genesisOutput);
        SimpleBlockchain.UTXOs.put(genesisOutput.id, genesisOutput);
        SimpleBlockchain.genesisTransaction = genesisTransaction;
        SimpleBlockchain.blockchain.add(new Block("0")); // need a base
        
        // Attempt to create two transactions spending the SAME output
        ArrayList<TransactionInput> inputs = new ArrayList<>();
        inputs.add(new TransactionInput(genesisOutput.id));
        
        // First spend
        Transaction tx1 = new Transaction(walletA.publicKey, walletB.publicKey, 50f, inputs);
        tx1.generateSignature(walletA.privateKey);
        assertTrue(tx1.processTransaction());
        
        // Second spend with EXACT SAME INPUT (Double Spend Attempt)
        Transaction tx2 = new Transaction(walletA.publicKey, walletB.publicKey, 50f, inputs);
        tx2.generateSignature(walletA.privateKey);
        
        // Should fail because tx1 already removed the UTXO from global UTXOs
        assertFalse(tx2.processTransaction());
    }
}
