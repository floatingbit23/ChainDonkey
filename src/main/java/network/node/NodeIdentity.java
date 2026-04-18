package network.node;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import simpleblockchain.Wallet;

/**
 * Gestiona la identidad de red persistente del nodo local de ChainDonkey.
 *
 * La identidad del nodo cumple un doble propósito:
 * 1. Proporciona un userHash de 16 bytes compatible con MD4 para la red eD2K clásica.
 * 2. Mantiene un enlace con la billetera (Wallet) ECDSA del nodo para operaciones de blockchain.
 */
public class NodeIdentity {

    private static final Logger logger = LoggerFactory.getLogger(NodeIdentity.class);
    private static final String IDENTITY_FILE = "node.identity";

    // Variables privadas finales 
    private final byte[] userHash;
    private final Wallet wallet;

    /**
     * Construye una NodeIdentity vinculada a la Wallet del nodo.
    
     * Si el userHash aún no se ha generado, se creará uno aleatorio.
     * En un entorno de producción, este userHash debería cargarse desde el disco para
     * mantener una identidad consistente entre los reinicios del nodo.
     
     * @param wallet la billetera criptográfica del nodo
     */
    public NodeIdentity(Wallet wallet) {
        this.wallet = wallet;
        this.userHash = loadOrGenerateUserHash(); // Carga o genera el hash de usuario
    }

    // ==========================================================================
    // Getters
    // ==========================================================================

    /**
     * Obtiene el hash de usuario eD2K de 16 bytes del nodo.
     * @return el array de hash de 16 bytes
     */
    public byte[] getUserHash() {
        return userHash.clone(); // Devuelve una copia para evitar la mutación
    }

    /**
     * Obtiene la Wallet asociada al nodo.
     * @return la instancia de Wallet
     */
    public Wallet getWallet() {
        return wallet;
    }

    /**
     * Método que carga el userHash desde la persistencia o genera uno nuevo.
     * @return un array aleatorio de 16 bytes
     */
    private byte[] loadOrGenerateUserHash() {

        Path path = Paths.get(IDENTITY_FILE); // Obtenemos la ruta del archivo

        // Si el archivo existe, comprobamos si el hash es correcto y lo cargamos
        if (Files.exists(path)) {
            try {
                byte[] loadedHash = Files.readAllBytes(path);
                if (loadedHash.length == 16) {
                    logger.info("Identidad cargada con éxito desde '{}'", IDENTITY_FILE);
                    return loadedHash;
                }
                logger.warn("El archivo de identidad '{}' no es válido. Generando uno nuevo...", IDENTITY_FILE);
            } catch (IOException e) {
                logger.error("Error al leer el archivo de identidad: {}", e.getMessage());
            }
        }

        // Si no existe el hash o hay error, generamos uno nuevo y lo guardamos
        byte[] newHash = generateRandomUserHash();

        try {
            Files.write(path, newHash); // Guardamos el hash en el archivo
            logger.info("Nueva identidad generada y guardada en '{}'", IDENTITY_FILE);
        } catch (IOException e) {
            logger.error("No se pudo guardar la nueva identidad en el disco: {}", e.getMessage());
        }

        return newHash; // Devolvemos el hash generado
    }

    /**
     * Genera un array aleatorio de 16 bytes que simula un userHash MD4.
     * Esto asegura que el nodo tenga un identificador de 128 bits (16 bytes) compatible con los requisitos de inicio de sesión del cliente.
     *
     * @return un nuevo array aleatorio de 16 bytes
     */
    private byte[] generateRandomUserHash() {
        byte[] hash = new byte[16];
        new java.security.SecureRandom().nextBytes(hash);
        // Byte 6 (índice 5) = 0x14 indica que el cliente soporta compresión zlib y extensiones modernas
        hash[5] = 0x14;
        
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02X", b));
        logger.info("UserHash generado: {}", hex.toString());
        
        return hash;
    }
}

