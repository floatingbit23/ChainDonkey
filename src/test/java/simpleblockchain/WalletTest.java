package simpleblockchain;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.security.Security;
import static org.junit.jupiter.api.Assertions.*;

public class WalletTest {

    @BeforeAll
    public static void setup() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @Test
    public void testWalletKeyGeneration() {
        Wallet wallet = new Wallet();
        assertNotNull(wallet.privateKey);
        assertNotNull(wallet.publicKey);
        
        // Ensure keys are unique
        Wallet wallet2 = new Wallet();
        assertNotEquals(wallet.privateKey, wallet2.privateKey);
        assertNotEquals(wallet.publicKey, wallet2.publicKey);
    }

    @Test
    public void testGetBalanceInitial() {
        Wallet wallet = new Wallet();
        // New wallet should have 0 balance
        assertEquals(0f, wallet.getBalance());
    }

    @Test
    public void testGetBalanceWithUTXOs() {
        Wallet wallet = new Wallet();
        
        // Manually simulate receiving coins in UTXOs
        TransactionOutput output1 = new TransactionOutput(wallet.publicKey, 10f, "tx1");
        TransactionOutput output2 = new TransactionOutput(wallet.publicKey, 20f, "tx2");
        
        // Note: TransactionOutput constructor now generates an ID
        assertNotNull(output1.id);
        
        SimpleBlockchain.UTXOs.put(output1.id, output1);
        SimpleBlockchain.UTXOs.put(output2.id, output2);
        
        assertEquals(30f, wallet.getBalance());
        
        // Clean up
        SimpleBlockchain.UTXOs.remove(output1.id);
        SimpleBlockchain.UTXOs.remove(output2.id);
    }
}
