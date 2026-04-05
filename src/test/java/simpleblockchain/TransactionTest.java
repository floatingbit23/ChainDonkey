package simpleblockchain;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.security.Security;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

public class TransactionTest {

    private Wallet walletA;
    private Wallet walletB;

    @BeforeAll
    public static void setupBC() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @BeforeEach
    public void init() {
        walletA = new Wallet();
        walletB = new Wallet();
        SimpleBlockchain.UTXOs.clear();
    }

    @Test
    public void testSignature() {
        Transaction tx = new Transaction(walletA.publicKey, walletB.publicKey, 5f, null);
        tx.generateSignature(walletA.privateKey);
        assertTrue(tx.isSignatureValid());
        
        // Tamper with data
        tx.value = 10f;
        assertFalse(tx.isSignatureValid());
    }

    @Test
    public void testProcessTransactionSuccess() {
        // Give walletA some funds
        TransactionOutput genesisOutput = new TransactionOutput(walletA.publicKey, 100f, "genesis");
        SimpleBlockchain.UTXOs.put(genesisOutput.id, genesisOutput);
        
        ArrayList<TransactionInput> inputs = new ArrayList<>();
        inputs.add(new TransactionInput(genesisOutput.id));
        
        Transaction tx = new Transaction(walletA.publicKey, walletB.publicKey, 40f, inputs);
        tx.generateSignature(walletA.privateKey);
        
        assertTrue(tx.processTransaction());
        assertEquals(2, tx.outputs.size()); // Payment + Change
        assertEquals(40f, tx.outputs.get(0).value);
        assertEquals(60f, tx.outputs.get(1).value);
        
        // Check UTXOs updated
        assertFalse(SimpleBlockchain.UTXOs.containsKey(genesisOutput.id));
        assertTrue(SimpleBlockchain.UTXOs.containsKey(tx.outputs.get(0).id));
        assertTrue(SimpleBlockchain.UTXOs.containsKey(tx.outputs.get(1).id));
    }

    @Test
    public void testProcessTransactionInsufficientFunds() {
        Transaction tx = walletA.sendFunds(walletB.publicKey, 10f);
        assertNull(tx); // sendFunds checks balance and returns null
    }
}
