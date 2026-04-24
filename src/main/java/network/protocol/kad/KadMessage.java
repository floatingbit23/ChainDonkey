package network.protocol.kad;

import java.net.InetSocketAddress;
import java.util.List;

import network.node.NodeInfo;

/**
 * Clase base abstracta para todos los mensajes del protocolo Kademlia (UDP).
 * Define la estructura común que comparten todos los paquetes Kad.
 */
public abstract class KadMessage {
    
    /**
     * ENUM de los tipos de mensajes Kad y sus opcodes oficiales (basados en eMule).
     */
    public enum Type {
        PING(0x01),           // Verificación básica (v1)
        PONG(0x02),           // Respuesta básica (v1)
        FIND_NODE(0x21),      // KADEMLIA2_REQ: Búsqueda de nodos (v2)
        FIND_NODE_RES(0x29),  // KADEMLIA2_RES: Respuesta de nodos (v2)
        HELLO_REQ(0x11),      // KADEMLIA2_HELLO_REQ: Saludo inicial (v2)
        HELLO_RES(0x19),      // KADEMLIA2_HELLO_RES: Respuesta al saludo (v2)
        HELLO_RES_ACK(0x13);  // KADEMLIA2_HELLO_RES_ACK: Confirmación de saludo (v2)

        private final int opcode; // 0x01, 0x02, 0x21, 0x29, 0x11, 0x19

        // Constructor -> Asigna el opcode
        Type(int opcode) { 
            this.opcode = opcode; 
        } 
        
        // Getter
        public int getOpcode() { 
            return opcode; 
        } 
        

        /**
         * Convierte un byte de opcode en un tipo de mensaje.
         * @param opcode Byte que representa el opcode del mensaje.
         * @return Tipo de mensaje correspondiente al opcode.
         */
        public static Type fromOpcode(int opcode) {
            // Recorre todos los tipos del enum
            for (Type t : values()) { 
                // Si el opcode coincide con el tipo en el enum
                if (t.opcode == opcode) { 
                    // Devuelve el tipo
                    return t; 
                }
            }

            return null; // Si no se encuentra el opcode, devuelve null
        }
    }

    private final Type type;        // Tipo de mensaje (Opcode)
    private final KadId senderId;   // ID del nodo que envía el mensaje
    private final long messageId;   // ID único de la transacción (para emparejar Req/Res)
    private InetSocketAddress recipient;     // Dirección de destino (usado para enviar)
    private InetSocketAddress senderAddress; // Dirección de origen (rellenado al recibir)

    /**
     * Constructor base para mensajes Kad
     * @param type Tipo de mensaje
     * @param senderId ID del emisor (16 bytes)
     * @param messageId ID del mensaje (8 bytes)
     */
    protected KadMessage(Type type, KadId senderId, long messageId) {
        this.type = type;
        this.senderId = senderId;
        this.messageId = messageId;
    }

    // Getters
    public Type getType() { return type; }
    public KadId getSenderId() { return senderId; }
    public long getMessageId() { return messageId; }

    // Métodos para gestionar la dirección de red
    public InetSocketAddress getRecipient() { return recipient; }

    /**
     * Establece la dirección de destino del mensaje.
     * Esta dirección será utilizada por el Netty Channel para enviar el paquete.
     * @param recipient Dirección IP y puerto de destino.
     */
    public void setRecipient(InetSocketAddress recipient) { 
        this.recipient = recipient; 
    }

    public InetSocketAddress getSenderAddress() { return senderAddress; }
    public void setSenderAddress(InetSocketAddress senderAddress) { this.senderAddress = senderAddress; }

    // --- SUBCLASES CONCRETAS PARA CADA TIPO DE MENSAJE ---

    /**
     * Solicitud PING (KadID + MessageID) para comprobar si un nodo sigue activo.
     */
    public static class PingRequest extends KadMessage {
        public PingRequest(KadId senderId, long messageId) {
            super(Type.PING, senderId, messageId);
        }
    }

    /**
     * Respuesta PONG a una solicitud de PING (KadID + MessageID).
     */
    public static class PingResponse extends KadMessage {

        // Constructor
        public PingResponse(KadId senderId, long messageId) {
            super(Type.PONG, senderId, messageId);
        }
    }

    /**
     * Solicitud para encontrar los nodos más cercanos a una ID objetivo (FIND_NODE).
     */
    public static class FindNodeRequest extends KadMessage {

        private final KadId targetId; // ID que estamos buscando

        // Constructor 
        public FindNodeRequest(KadId senderId, long messageId, KadId targetId) {
            super(Type.FIND_NODE, senderId, messageId);
            this.targetId = targetId; 
        }

        // Getter
        public KadId getTargetId() { 
            return targetId; 
        } 
    }

    /**
     * Respuesta que contiene la lista de nodos más cercanos encontrados (FIND_NODE_RES).
     */
    public static class FindNodeResponse extends KadMessage {

        private final List<NodeInfo> closestNodes; // Lista de contactos (k-nodos)

        // Constructor
        public FindNodeResponse(KadId senderId, long messageId, List<NodeInfo> closestNodes) {
            super(Type.FIND_NODE_RES, senderId, messageId);
            this.closestNodes = closestNodes;
        }

        // Getter
        public List<NodeInfo> getClosestNodes() { 
            return closestNodes; 
        }
    }

    /**
     * Saludo inicial (Handshake) en Kademlia 2.
     * Incluye información del puerto TCP y la versión del cliente.
     */
    public static class HelloRequest extends KadMessage {

        private final int receiverPort; // Puerto TCP del emisor
        private final int version;      // Versión del protocolo Kad

        // Constructor 
        public HelloRequest(KadId senderId, int receiverPort, int version) {
            super(Type.HELLO_REQ, senderId, 0); // Los Hello no suelen usar MessageID en eMule
            this.receiverPort = receiverPort;
            this.version = version;
        }

        // Getters
        public int getReceiverPort() { return receiverPort; }
        public int getVersion() { return version; }

    }

    /**
     * Respuesta al saludo inicial.
     */
    public static class HelloResponse extends KadMessage {

        // Mismos parámetros que el HelloRequest
        private final int receiverPort; 
        private final int version;      

        // Constructor
        public HelloResponse(KadId senderId, int receiverPort, int version) {
            super(Type.HELLO_RES, senderId, 0);
            this.receiverPort = receiverPort;
            this.version = version;
        }

        // Getters
        public int getReceiverPort() { return receiverPort; }
        public int getVersion() { return version; }
        
    }
}
