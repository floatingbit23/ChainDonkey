package network.node;

import java.net.InetSocketAddress;

/**
 * Un Data Transfer Object (DTO) inmutable que contiene información sobre un nodo remoto.
 * Almacena el ID de Kademlia del nodo, sus direcciones de red (TCP/UDP) y sus capacidades (e.g. si es un nodo de ChainDonkey).
 */
public final class NodeInfo {

    private final byte[] nodeId;
    private final InetSocketAddress tcpAddress; 
    private final int udpPort;
    private final long lastSeen;
    private final boolean isChainDonkeyNode;

    /**
     * Construye una nueva instancia de NodeInfo.
     *
     * @param nodeId            el ID de Kademlia de 16 bytes del nodo
     * @param tcpAddress        el socket TCP del nodo (direccion IP + puerto TCP) (para la transferencia de archivos)
     * @param udpPort           el puerto de escucha UDP para paquetes Kademlia (para las consultas Kademlia)
     * @param lastSeen          el Timestamp de la última vez que se vio el nodo online (en milisegundos)
     * @param isChainDonkeyNode indica si este nodo admite extensiones de ChainDonkey
     */
    public NodeInfo(byte[] nodeId, InetSocketAddress tcpAddress, int udpPort, long lastSeen, boolean isChainDonkeyNode) {
       
        // Validación de seguridad
        if (nodeId == null || nodeId.length != 16) {
            throw new IllegalArgumentException("El ID del nodo debe tener exactamente 16 bytes.");
        }

        // Asignación de valores (hacemos una copia del nodeId para mantener la inmutabilidad)
        this.nodeId = nodeId.clone(); 
        this.tcpAddress = tcpAddress;
        this.udpPort = udpPort;
        this.lastSeen = lastSeen;
        this.isChainDonkeyNode = isChainDonkeyNode;
    }

    /**
     * Obtiene el identificador Kademlia de 16 bytes del nodo.
     * @return el array de 16 bytes que representa el ID del nodo
     */
    public byte[] getNodeId() {
        return nodeId.clone();
    }

    /**
     * Obtiene la dirección TCP del nodo.
     * @return el InetSocketAddress TCP
     */
    public InetSocketAddress getTcpAddress() {
        return tcpAddress;
    }

    /**
     * Obtiene el puerto de escucha UDP del nodo.
     * @return el número de puerto UDP
     */
    public int getUdpPort() {
        return udpPort;
    }

    /**
     * Obtiene el timestamp de la última vez que este nodo fue visto en línea recientemente.
     * @return el timestamp en milisegundos desde el epoch
     */
    public long getLastSeen() {
        return lastSeen;
    }

    /**
     * Comprueba si el nodo se identifica a sí mismo como un nodo de ChainDonkey.
     * @return {@code true} si es un nodo de ChainDonkey, {@code false} si es un nodo de eMule clásico.
     */
    public boolean isChainDonkeyNode() {
        return isChainDonkeyNode;
    }

    // Sobreescritura de métodos equals() y hashCode() y toString() para facilitar el uso en colecciones

    /**
     * Compara dos instancias de NodeInfo basándose en el nodeId.
     * @param o el objeto a comparar
     * @return true si los nodeIds son iguales, false en caso contrario
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Si es la misma referencia, son iguales
        if (o == null || getClass() != o.getClass()) return false; // Si es nulo o de otra clase, no son iguales
        NodeInfo nodeInfo = (NodeInfo) o; // Si es una instancia de NodeInfo, comparamos los nodeIds...
        return java.util.Arrays.equals(nodeId, nodeInfo.nodeId); // Si los nodeIds son iguales, entonces los objetos NodeInfo son iguales
    }

    /**
     * Calcula el valor hash de esta instancia de NodeInfo basándose en el nodeId.
     * @return el hash code del nodeId
     */
    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(nodeId);
    }

    /**
     * Devuelve una representación en formato String de esta instancia de NodeInfo.
     * @return una cadena que representa el nodeId y la dirección TCP
     */
    @Override
    public String toString() {
        return "NodeInfo{" +
               "nodeId=" + java.util.Arrays.toString(nodeId) +
               ", tcpAddress=" + tcpAddress +
               ", udpPort=" + udpPort +
               ", lastSeen=" + lastSeen +
               ", isChainDonkeyNode? " + isChainDonkeyNode +
               '}';
    }
}

