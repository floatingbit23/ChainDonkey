package network.node;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import network.protocol.kad.KadId;

/**
 * Un Data Transfer Object (DTO) inmutable que contiene información sobre un nodo remoto.
 * Esta clase es el núcleo de la Routing Table y es 100% compatible con el formato 
 * de 25 bytes de Kademlia v2 (el estándar actual de eMule/aMule).
 */
public final class NodeInfo {

    // Identificador único de 128 bits del nodo en la red Kademlia
    private final KadId nodeId;
    
    // Dirección IP del nodo (soportando tanto IPv4 como IPv6 internamente)
    private final InetAddress address;
    
    // Puerto UDP: Se utiliza para todas las consultas Kademlia (FIND_NODE, PING, etc.)
    private final int udpPort;
    
    // Puerto TCP: Se utiliza para la transferencia de datos y protocolo eD2K
    private final int tcpPort;
    
    // Versión del protocolo: Permite saber qué capacidades tiene el cliente remoto
    private final int version;
    
    // Marca de tiempo de la última vez que el nodo respondió a una petición
    private final long lastSeen;
    
    // Indica si el nodo es un cliente oficial de ChainDonkey (permite funciones extra de blockchain)
    private final boolean isChainDonkeyNode;

    /**
     * Constructor completo para Kademlia v2.
     * Corresponde a la estructura de 25 bytes que recibimos de la red:
     * ID(16) + IP(4) + UDP(2) + TCP(2) + Version(1).
     * 
     * @param nodeId ID del nodo remoto
     * @param address Dirección IP
     * @param udpPort Puerto para Kademlia
     * @param tcpPort Puerto para transferencias TCP
     * @param version Versión del software cliente
     */
    public NodeInfo(KadId nodeId, InetAddress address, int udpPort, int tcpPort, int version) {
        this.nodeId = nodeId;
        this.address = address;
        this.udpPort = udpPort;
        this.tcpPort = tcpPort;
        this.version = version;
        this.lastSeen = System.currentTimeMillis();
        this.isChainDonkeyNode = false;
    }

    /**
     * Constructor de conveniencia para compatibilidad y entornos de prueba.
     * Asume que los puertos UDP y TCP son los mismos y asigna una versión por defecto (8).
     * 
     * @param nodeId ID del nodo
     * @param addr Dirección socket (IP + Puerto)
     */
    public NodeInfo(KadId nodeId, InetSocketAddress addr) {
        this(nodeId, addr.getAddress(), addr.getPort(), addr.getPort(), 8);
    }

    // --- Getters ---

    public KadId getNodeId() { return nodeId; }
    
    public InetAddress getAddress() { return address; }
    
    public int getUdpPort() { return udpPort; }
    
    public int getTcpPort() { return tcpPort; }
    
    public int getVersion() { return version; }
    
    public long getLastSeen() { return lastSeen; }
    
    public boolean isChainDonkeyNode() { return isChainDonkeyNode; }

    /**
     * Devuelve la dirección UDP completa del nodo.
     * @return InetSocketAddress con la IP y el puerto UDP.
     */
    public InetSocketAddress getUdpAddress() {
        return new InetSocketAddress(address, udpPort);
    }

    /**
     * Devuelve la dirección TCP completa del nodo.
     * @return InetSocketAddress con la IP y el puerto TCP.
     */
    public InetSocketAddress getTcpAddress() {
        return new InetSocketAddress(address, tcpPort);
    }

    /**
     * Los nodos se consideran iguales si tienen el mismo KadID, 
     * independientemente de si han cambiado de IP o puerto.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeInfo nodeInfo = (NodeInfo) o;
        return nodeId.equals(nodeInfo.nodeId);
    }

    @Override
    public int hashCode() {
        return nodeId.hashCode();
    }

    /**
     * Representación legible para los logs: [ShortID@IP:Puerto]
     */
    @Override
    public String toString() {
        return String.format("Node[%s@%s:%d]", 
            nodeId.toString().substring(0, 8), 
            address.getHostAddress(), 
            udpPort);
    }
}
