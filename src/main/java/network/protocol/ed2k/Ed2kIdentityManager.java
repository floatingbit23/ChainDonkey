package network.protocol.ed2k;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gestiona la identidad criptográfica del nodo (RSA KeyPair + UserHash).
 * Permite persistir la identidad para mantener créditos y reputación en la red.
 */
public class Ed2kIdentityManager {
    static {
        // Añadir BouncyCastle para soportar RSA 384 bits (necesario para eMule)
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    private static final Logger logger = LoggerFactory.getLogger(Ed2kIdentityManager.class);
    private static final String IDENTITY_FILE = "node_identity.dat";
    
    private KeyPair keyPair;
    private byte[] userHash;

    public Ed2kIdentityManager() {
        loadOrCreateIdentity();
    }

    private void loadOrCreateIdentity() {
        Path path = Paths.get(IDENTITY_FILE);
        if (Files.exists(path)) {
            try (DataInputStream dis = new DataInputStream(new FileInputStream(path.toFile()))) {
                // Leer UserHash
                userHash = new byte[16];
                dis.readFully(userHash);
                
                // Leer Private Key
                int privLen = dis.readInt();
                byte[] privBytes = new byte[privLen];
                dis.readFully(privBytes);
                
                // Leer Public Key
                int pubLen = dis.readInt();
                byte[] pubBytes = new byte[pubLen];
                dis.readFully(pubBytes);
                
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PrivateKey priv = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
                PublicKey pub = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
                
                keyPair = new KeyPair(pub, priv);
                logger.info("[IDENTITY] Identidad cargada correctamente. Hash: {}", bytesToHex(userHash));
            } catch (Exception e) {
                logger.error("[IDENTITY] Error al cargar identidad, generando una nueva...", e);
                generateNewIdentity();
            }
        } else {
            generateNewIdentity();
        }
    }

    private void generateNewIdentity() {
        try {
            logger.info("[IDENTITY] Generando nueva identidad criptográfica (RSA 384 bits - Modo eMule)...");
            // Usamos BouncyCastle ("BC") para permitir claves de 384 bits
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
            kpg.initialize(384);
            keyPair = kpg.generateKeyPair();
            
            userHash = new byte[16];
            new Random().nextBytes(userHash);
            
            saveIdentity();
            logger.info("[IDENTITY] Nueva identidad generada y guardada. Hash: {}", bytesToHex(userHash));
        } catch (IOException | NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("No se pudo generar la identidad del nodo", e);
        }
    }

    private void saveIdentity() throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(IDENTITY_FILE))) {
            dos.write(userHash);
            
            byte[] privBytes = keyPair.getPrivate().getEncoded();
            dos.writeInt(privBytes.length);
            dos.write(privBytes);
            
            byte[] pubBytes = keyPair.getPublic().getEncoded();
            dos.writeInt(pubBytes.length);
            dos.write(pubBytes);
        }
    }

    /**
     * Firma un reto de eMule. 
     * eMule espera que se firme: [ClavePúblicaDelQueRecibeLaFirma] + [Challenge].
     * 
     * @param challenge El reto de 4 bytes enviado por eMule.
     * @param remotePublicKey La clave pública del cliente eMule (el que nos retó).
     * @return La firma RSA.
     */
    public byte[] signChallenge(int challenge, byte[] remotePublicKey) {
        try {
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initSign(keyPair.getPrivate());
            
            // eMule espera que firmemos SU clave pública + el reto
            if (remotePublicKey != null) {
                sig.update(remotePublicKey);
            }
            
            byte[] challengeBytes = new byte[4];
            challengeBytes[0] = (byte) (challenge & 0xFF);
            challengeBytes[1] = (byte) ((challenge >> 8) & 0xFF);
            challengeBytes[2] = (byte) ((challenge >> 16) & 0xFF);
            challengeBytes[3] = (byte) ((challenge >> 24) & 0xFF);
            
            // Logs para depuración
            logger.info("[IDENTITY] Firmando reto con: Key={}, Challenge={}", bytesToHex(remotePublicKey != null ? remotePublicKey : new byte[0]), bytesToHex(challengeBytes));
            
            sig.update(challengeBytes);
            
            byte[] signature = sig.sign();
            logger.info("[IDENTITY] Firma generada: {}", bytesToHex(signature));
            return signature;
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            logger.error("[IDENTITY] Error al firmar el reto", e);
            return new byte[0];
        }
    }

    public byte[] getPublicKeyBytes() {
        if (keyPair == null) return new byte[0];
        // eMule espera formato X.509 (SubjectPublicKeyInfo)
        // Con 384 bits, esto ocupa unos 76 bytes, lo que cabe en el buffer de 80 bytes de eMule.
        byte[] encoded = keyPair.getPublic().getEncoded();
        logger.info("[IDENTITY] Clave Pública X.509 enviada ({} bytes): {}", encoded.length, bytesToHex(encoded));
        return encoded;
    }

    public byte[] getUserHash() {
        return userHash;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, 4); i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString() + "...";
    }
}
