import java.security.MessageDigest;
import java.util.HexFormat;

public class BruteForceObfuscation {

    private static final String SHARED_SECRET_HEX = "286EBB54563F2048B6F5125926682DE79046AD07053D10E5A4936FE387417912557566BB976D99A57FA91D0DD1CE44304022D9C5FAF36C681281F473F6A2C33D853C86AA2F7C90EDD392BF708792AEBC6FA7742AE47B752840BC71AE23BCCE33";
    private static final String ENCRYPTED_MAGIC_HEX = "42 93 77 D8";
    private static final byte[] TARGET_MAGIC_LE = {(byte) 0xC4, (byte) 0x6F, (byte) 0x5E, (byte) 0x83}; // LE of 0x835E6FC4
    private static final byte[] TARGET_MAGIC_BE = {(byte) 0x83, (byte) 0x5E, (byte) 0x6F, (byte) 0xC4}; // BE of 0x835E6FC4

    public static void main(String[] args) throws Exception {
        byte[] sBytes = HexFormat.of().parseHex(SHARED_SECRET_HEX);
        byte[] encMagic = HexFormat.ofDelimiter(" ").parseHex(ENCRYPTED_MAGIC_HEX);
        
        byte[] ip = {(byte) 176, (byte) 123, (byte) 5, (byte) 89};
        int port = 4725;
        
        byte[][] sVariants = {sBytes, reverse(sBytes)};
        byte[][] ipVariants = {
            {ip[3], ip[2], ip[1], ip[0]}, // LE
            {ip[0], ip[1], ip[2], ip[3]}, // BE
            null // Sin IP
        };
        byte[][] portVariants = {
            {(byte)(port & 0xFF), (byte)((port >> 8) & 0xFF)}, // LE
            {(byte)((port >> 8) & 0xFF), (byte)(port & 0xFF)}, // BE
            null // Sin Port
        };
        byte[] flags = {0x00, 0x01, (byte)0xCB, (byte)0x22, 0x11, 0x10, (byte)0x23, (byte)0xCA};
        int[] discards = {0, 1024};

        System.out.println("Iniciando búsqueda exhaustiva...");

        for (byte[] sv : sVariants) {
            for (byte[] ipv : ipVariants) {
                for (byte[] pv : portVariants) {
                    for (byte flag : flags) {
                        // Intentar todas las permutaciones de S, Flag, IP, Port
                        testPermutations(sv, flag, ipv, pv, encMagic, discards);
                    }
                }
            }
        }
        System.out.println("Fin de la búsqueda.");
    }

    private static byte[] reverse(byte[] data) {
        byte[] res = new byte[data.length];
        for (int i = 0; i < data.length; i++) res[i] = data[data.length - 1 - i];
        return res;
    }

    private static void testPermutations(byte[] s, byte f, byte[] ip, byte[] p, byte[] enc, int[] discards) throws Exception {
        // Generar combinaciones de los componentes no nulos
        // Componentes: s (96), f (1), ip (0-4), p (0-2)
        
        // Caso simple: S + Flag + IP + Port (Estructura eMule)
        byte[] b1 = combine(s, new byte[]{f}, ip, p);
        verify(b1, enc, "S + Flag + IP + Port", discards);

        // Caso usuario: S + IP + Flag (sin port)
        if (p == null) {
            byte[] b2 = combine(s, ip, new byte[]{f});
            verify(b2, enc, "S + IP + Flag", discards);
        }
        
        // Caso eMule salt: S + Flag + IP (sin port)
        if (p == null) {
            byte[] b3 = combine(s, new byte[]{f}, ip);
            verify(b3, enc, "S + Flag + IP", discards);
        }
    }

    private static byte[] combine(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) if (p != null) total += p.length;
        byte[] res = new byte[total];
        int pos = 0;
        for (byte[] p : parts) {
            if (p != null) {
                System.arraycopy(p, 0, res, pos, p.length);
                pos += p.length;
            }
        }
        return res;
    }

    private static void verify(byte[] buf, byte[] enc, String desc, int[] discards) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] key = md.digest(buf);
        
        for (int d : discards) {
            Cipher rc4 = new Cipher(key);
            if (d > 0) rc4.update(new byte[d]);
            byte[] dec = rc4.update(enc);
            
            if (matches(dec, TARGET_MAGIC_LE) || matches(dec, TARGET_MAGIC_BE)) {
                System.out.println("¡ENCONTRADO!");
                System.out.println("Config: " + desc + ", Discard: " + d + ", Magic=" + (matches(dec, TARGET_MAGIC_LE) ? "LE" : "BE"));
                System.out.print("Key Buffer (MD5 input): ");
                for (byte b : buf) System.out.print(String.format("%02X", b));
                System.out.println();
            }
        }
    }

    private static boolean matches(byte[] a, byte[] b) {
        if (a.length < b.length) return false;
        for (int i = 0; i < b.length; i++) if (a[i] != b[i]) return false;
        return true;
    }

    private static class Cipher {
        private final byte[] S = new byte[256];
        private int i = 0, j = 0;
        public Cipher(byte[] key) {
            for (int k = 0; k < 256; k++) S[k] = (byte) k;
            int j2 = 0;
            for (int k = 0; k < 256; k++) {
                j2 = (j2 + (S[k] & 0xFF) + (key[k % key.length] & 0xFF)) & 0xFF;
                byte temp = S[k];
                S[k] = S[j2];
                S[j2] = temp;
            }
        }
        public byte[] update(byte[] data) {
            byte[] out = new byte[data.length];
            for (int k = 0; k < data.length; k++) {
                i = (i + 1) & 0xFF;
                j = (j + (S[i] & 0xFF)) & 0xFF;
                byte temp = S[i];
                S[i] = S[j];
                S[j] = temp;
                int t = ((S[i] & 0xFF) + (S[j] & 0xFF)) & 0xFF;
                out[k] = (byte) (data[k] ^ S[t]);
            }
            return out;
        }
    }
}
