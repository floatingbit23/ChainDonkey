package network.transport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import network.protocol.ed2k.Ed2kMessage;

/**
 * Gestiona el conjunto de conexiones activas con otros Peers en la red.
 * Controla límites de conexión y permite el envío masivo (broadcast) de mensajes.
 */

public class PeerManager {

    private static final Logger logger = LoggerFactory.getLogger(PeerManager.class);

    // Mapa concurrente que almacena las conexiones activas
    // Key = Dirección TCP del par (String formato IP:Puerto)
    // Value = Conexión de par (Clase PeerConnection que contiene el canal de Netty y la información del par)
    private final Map<String, PeerConnection> activePeers = new ConcurrentHashMap<>();

    // Número máximo de peers que se pueden conectar (se define en el constructor)
    private final int maxPeers;

    /**
     * Constructor del PeerManager.
     * @param maxPeers Número máximo de peers que se pueden conectar.
     */
    public PeerManager(int maxPeers) {
        this.maxPeers = maxPeers;
    }

    /**
     * Registra una nueva conexión de Peer.
     * @param peer Conexión del peer a registrar.
     */
    public void addPeer(PeerConnection peer) {
        // Si el número de peers es mayor o igual al máximo de peers, se rechaza la conexión
        if (activePeers.size() >= maxPeers) {
            logger.warn("[PEER MANAGER] Límite de peers alcanzado ({}). Rechazando {}", maxPeers, peer.getNodeInfo().getTcpAddress());
            peer.disconnect(); 
            return;
        }
        
        // Añade el Peer al mapa de Peers activos
        String key = peer.getNodeInfo().getTcpAddress().toString(); // incluye la dirección IP y el puerto
        activePeers.put(key, peer); // value es la instancia de PeerConnection

        logger.info("[PEER MANAGER] Par añadido: {}. Total de Peers activos: {}", key, activePeers.size());
    }

    /**
     * Elimina un Peer de la gestión.
     * @param address Dirección TCP del peer a eliminar (String formato IP:Puerto)
     */
    public void removePeer(String address) {
        // Elimina el Peer del mapa de Peers activos
        PeerConnection peer = activePeers.remove(address); 

        // Si el Peer existe, se muestra un mensaje de registro tras haberlo eliminado
        if (peer != null) {
            logger.info("[PEER MANAGER] Par eliminado: {}. Total de Peers activos: {}", address, activePeers.size());
        }
    }

    /**
     * Envía un mensaje a todos los peers conectados.
     * Útil para la propagación (Gossip) de bloques y transacciones.
     * @param message Mensaje eD2K a enviar.
     */
    public void broadcast(Ed2kMessage message) {

        // Muestra un mensaje de registro con el opcode del mensaje y el número de peers
        logger.debug("[PEER MANAGER] Difundiendo mensaje opcode 0x{} a {} peers", 
            Integer.toHexString(message.getOpcode() & 0xFF), activePeers.size()); // & 0xFF para asegurar que el opcode sea positivo
        
        // Envía el mensaje a todos los peers activos
        activePeers.values().forEach(peer -> peer.send(message));
    }

    // ============================================
    // GETTERS
    // ============================================

    /*
     * Devuelve el Map de peers activos.
     */
    public Map<String, PeerConnection> getActivePeers() {
        return activePeers;
    }

    /*
     * Devuelve el número de peers activos.
     */
    public int getPeerCount() {
        return activePeers.size();
    }
}
