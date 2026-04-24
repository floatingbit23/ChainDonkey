package network.protocol.kad;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class KadIdTest {

    @Test
    public void testXorDistance() {
        byte[] b1 = new byte[16];
        byte[] b2 = new byte[16];
        b1[0] = (byte) 0b10101010;
        b2[0] = (byte) 0b01010101;
        
        KadId id1 = new KadId(b1);
        KadId id2 = new KadId(b2);
        
        KadId distance = id1.xor(id2);
        assertEquals((byte) 0b11111111, distance.getBytes()[0]);
    }

    @Test
    public void testGetBit() {
        byte[] b = new byte[16];
        b[0] = (byte) 0b10000000;
        KadId id = new KadId(b);
        
        assertEquals(1, id.getBit(0));
        assertEquals(0, id.getBit(1));
        assertEquals(0, id.getBit(127));
    }

    @Test
    public void testCompareTo() {
        KadId id1 = KadId.random();
        KadId id2 = KadId.random();
        
        // id1 xor id1 should be 0
        KadId d1 = id1.xor(id1);
        KadId d2 = id1.xor(id2);
        
        assertTrue(d1.compareTo(d2) <= 0);
    }
}
