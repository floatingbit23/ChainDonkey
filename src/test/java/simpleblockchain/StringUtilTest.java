package simpleblockchain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;

public class StringUtilTest {

    @Test
    public void testApplySha256() {
        String input = "Hello World";
        String hash1 = StringUtil.applySha256(input);
        String hash2 = StringUtil.applySha256(input);
        
        // Hashes should be consistent
        assertEquals(hash1, hash2);
        // SHA-256 for "Hello World" is known
        assertEquals("a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e", hash1);
    }

    @Test
    public void testGetDifficultyString() {
        assertEquals("000", StringUtil.getDifficultyString(3));
        assertEquals("00000", StringUtil.getDifficultyString(5));
        assertEquals("", StringUtil.getDifficultyString(0));
    }

    @Test
    public void testGetMerkleRoot() {
        ArrayList<Transaction> transactions = new ArrayList<>();
        
        // Mock some transactions (we only need their IDs for Merkle Root)
        Transaction t1 = new Transaction(null, null, 0, null);
        t1.transactionId = "1";
        Transaction t2 = new Transaction(null, null, 0, null);
        t2.transactionId = "2";
        Transaction t3 = new Transaction(null, null, 0, null);
        t3.transactionId = "3";
        
        transactions.add(t1);
        transactions.add(t2);
        
        String root1 = StringUtil.getMerkleRoot(transactions);
        assertNotNull(root1);
        
        // Odd number of transactions
        transactions.add(t3);
        String root2 = StringUtil.getMerkleRoot(transactions);
        assertNotNull(root2);
        assertNotEquals(root1, root2);
    }
}
