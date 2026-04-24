package network.protocol.kad;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import network.node.NodeInfo;

/**
 * Tabla de enrutamiento Kademlia completa.
 * Mantiene 128 buckets basados en la distancia XOR respecto al ID local.
 */
public class RoutingTable {

    private final KadId localId; // ID del nodo local (el mío)
    private final KBucket[] buckets; // Array de buckets

    // Constructor de la Tabla de Rutas
    // @param localId -> ID del nodo local
    public RoutingTable(KadId localId) {

        this.localId = localId; // ID del nodo local (el mío)

        this.buckets = new KBucket[128]; // Instancia el array de buckets a 128 (uno por cada bit)

        // Recorre e inicializa cada bucket con una nueva instancia de KBucket
        for (int i = 0; i < 128; i++) {
            buckets[i] = new KBucket();
        }
    }

    /**
     * Añade un nodo a la tabla en el bucket correspondiente.
     * @param node -> Nodo a añadir
     */
    public void addNode(NodeInfo node) {

        // Si el nodo es el propio nodo, no se añade
        if (node.getNodeId().equals(localId)) return;

        // Calcula el bucket correspondiente
        int index = getBucketIndex(node.getNodeId());

        // Añade el nodo al bucket correspondiente
        buckets[index].addNode(node);
    }

    /**
     * Retorna los N nodos más cercanos al target.
     * 
     * @param target -> ID objetivo
     * @param count -> Número de nodos a retornar
     * @return Lista de nodos más cercanos
     */
    public List<NodeInfo> findClosest(KadId target, int count) {

        // ArrayList auxiliar para almacenar todos los nodos de la Routing Table
        List<NodeInfo> allNodes = new ArrayList<>(); 

        // Recorre todos los buckets y añade todos los nodos de cada bucket a la lista
        for (KBucket bucket : buckets) { 
            allNodes.addAll(bucket.getNodes()); 
        }

        return allNodes.stream() // Convierte la lista en un stream
                .sorted(Comparator.comparing(n -> n.getNodeId().xor(target))) // Ordena los nodos por distancia XOR al target
                .limit(count) // Limita el nº de nodos a retornar al parámetro 'count'
                .collect(Collectors.toList()); // Convierte el stream en una lista y la retorna
    }

    /**
     * Calcula el índice del bucket para un ID externo.
     * Se basa en el primer bit donde difieren (distancia XOR).
     * 
     * @param otherId -> ID externo
     * @return Índice del bucket
     */
    public int getBucketIndex(KadId otherId) {

        // Distancia XOR
        KadId distance = localId.xor(otherId); 

        // Busca el primer bit donde difieren, desde el MSB (bit 0) al LSB (bit 127)
        for (int i = 0; i < 128; i++) { 
            if (distance.getBit(i) == 1) {  // Si el bit XOR resultante es 1, ese es el índice del bucket donde se debe añadir el nodo
                return i;
            }
        }
        return 127; // Caso extremadamente raro de colisión casi total (IDs iguales)
    }

    /**
     * Elimina un nodo de la tabla.
     * @param nodeId -> ID del nodo a eliminar
     */
    public void removeNode(KadId nodeId) {

        int index = getBucketIndex(nodeId); // Calcula el bucket donde se encuentra el nodo

        buckets[index].removeNode(nodeId); // Elimina el nodo de dicho bucket
    }

    /**
     * Obtiene el ID del nodo local.
     * @return ID del nodo local.
     */
    public KadId getLocalId() {
        return localId;
    }

    /**
     * Obtiene el número total de nodos en la tabla de rutas.
     * @return Número total de nodos.
     */
    public int getTotalNodes() {

        int total = 0;

        for (KBucket bucket : buckets) { // Recorre todos los buckets
            total += bucket.size(); // Suma el número de nodos de cada bucket al contador total
        }

        return total;
    }

    /**
     * Retorna el array de buckets de la tabla de rutas (para el Dashboard web).
     * @return Array de buckets.
     */
    public KBucket[] getBuckets() {
        return buckets;
    }
}
