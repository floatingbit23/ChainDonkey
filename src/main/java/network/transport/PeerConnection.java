package network.transport;

import java.util.concurrent.atomic.AtomicLong; // Para el seguimiento de bytes de forma concurrente

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import network.node.NodeInfo;
import network.protocol.ed2k.Ed2kMessage;

/**
 * Representa una conexión activa con un Peer en la red eD2K.
 * Envuelve (encapsula) el canal de Netty y mantiene estadísticas y estado de la conexión.
 */

public class PeerConnection {

    private static final Logger logger = LoggerFactory.getLogger(PeerConnection.class);

    private final Channel channel; // Canal de Netty
    private final NodeInfo nodeInfo; // Información del Peer
    private final long connectedAt; // Momento en el que se estableció la conexión (timestamp)
    private final AtomicLong bytesSent = new AtomicLong(0); // Bytes enviados
    private final AtomicLong bytesReceived = new AtomicLong(0); // Bytes recibidos
    
    private ConnectionState state = ConnectionState.CONNECTED; // Estado inicial al establecerse la conexión

    // Enum para representar los diferentes estados de la conexión con un par eD2K
    public enum ConnectionState {
        CONNECTED,      // Conexión establecida
        HANDSHAKING,    // Handshake en curso
        AUTHENTICATED,  // Handshake completado
        DISCONNECTED    // Desconectado
    }

    // Constructor de PeerConnection
    public PeerConnection(Channel channel, NodeInfo nodeInfo) { 
        // Asigna el canal de Netty
        this.channel = channel;
        // Asigna la información del Peer
        this.nodeInfo = nodeInfo;
        // Momento en el que se estableció la conexión (timestamp)
        this.connectedAt = System.currentTimeMillis();
    }

    /**
     * Método que envía un mensaje eD2K al par remoto.
     * @param message Mensaje eD2K a enviar
     */
    public void send(Ed2kMessage message) {

        // Si el canal está activo
        if (channel.isActive()) {
            // Envía el mensaje y vacía el buffer (flush)
            channel.writeAndFlush(message);
        } else {
            // Si el canal no está activo, muestra un mensaje de advertencia
            logger.warn("[PEER] Intento de enviar mensaje a par desconectado: {}", nodeInfo.getTcpAddress());
        }
    }

    /**
     * Método queierra la conexión.
     */
    public void disconnect() {
        // Si el canal está abierto
        if (channel.isOpen()) {
            // Cierra el canal
            channel.close();
        }

        // Actualiza el estado a desconectado
        this.state = ConnectionState.DISCONNECTED;
    }

    // ============================================
    // GETTERS Y SETTER
    // ============================================
    
    public Channel getChannel() {
        return channel;
    }

    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    public ConnectionState getState() {
        return state;
    }

    public void setState(ConnectionState state) {
        this.state = state;
    }

    // ============================================
    // MÉTODOS PARA EL SEGUIMIENTO DE BYTES
    // ============================================

    /**
     * Método que suma los bytes recibidos.
     * @param bytes Bytes a sumar
     */
    public void addBytesReceived(long bytes) {
        this.bytesReceived.addAndGet(bytes);
    }

    /**
     * Método que suma los bytes enviados.
     * @param bytes Bytes a sumar
     */
    public void addBytesSent(long bytes) {
        this.bytesSent.addAndGet(bytes);
    }

    // ============================================
    // GETTERS PARA EL SEGUIMIENTO DE BYTES
    // ============================================
    
    public long getBytesReceived() {
        return bytesReceived.get();
    }

    public long getBytesSent() {
        return bytesSent.get();
    }

    /**
     * Método que devuelve el tiempo en milisegundos desde que se estableció la conexión.
     */
    public long getUptime() {
        return System.currentTimeMillis() - connectedAt;
    }
}
