package network.protocol.kad;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import network.node.NodeInfo;

/**
 * Estructura de datos k-bucket que almacena nodos vistos.
 * Capacidad k = 40. Implementa política LRU (los más recientes al final).
 */
public class KBucket {

    private static final int K = 40; // Capacidad del bucket (Aumentado para redes modernas)
    private final List<NodeInfo> nodes = new CopyOnWriteArrayList<>(); // Lista de nodos en el bucket (CopyOnWriteArrayList para seguridad en hilos)

    /**
     * Intenta añadir un nodo al bucket.
     * Si el nodo ya existe, lo mueve al final (más reciente).
     * Si el nodo no existe y hay espacio, lo añade al final.
     * Si el bucket está lleno, el nodo no se añade
     * 
     * @param node Nodo a añadir.
     * @return true si el nodo está/entra en el bucket (ya sea nuevo o actualizado).
     */
    public synchronized boolean addNode(NodeInfo node) {

        // Eliminar si ya existe para moverlo al final (política Least Recently Used)
        nodes.removeIf(n -> n.getNodeId().equals(node.getNodeId()));
        // removeIf() es un método de la interfaz Collection que elimina todos los elementos del stream que coinciden con el predicado especificado
        

        // Si hay espacio en el bucket
        if (nodes.size() < K) {
            nodes.add(node); // Añade al final (más reciente)
            return true;
        }

        // Si el bucket está lleno, el nodo no se añade (en una fase posterior se podría implementar reemplazo)
        return false;
    }

    /**
     * Elimina un nodo del bucket.
     * @param nodeId ID del nodo a eliminar.
     */
    public synchronized void removeNode(KadId nodeId) {
        nodes.removeIf(n -> n.getNodeId().equals(nodeId));
        // equals() compara el contenido de los objetos (IDs en este caso), no la referencia a los mismos. 
        // Es decir, compara que los IDs sean iguales.
    }

    /**
     * Obtiene todos los nodos del bucket.
     * @return Lista de nodos en el bucket.
     */
    public List<NodeInfo> getNodes() {
        return new ArrayList<>(nodes); // Devuelve una copia para evitar modificaciones externas
    }

    /**
     * Obtiene el número de nodos en el bucket.
     * @return Número de nodos en el bucket.
     */
    public int size() {
        return nodes.size();
    }

    /**
     * Verifica si el bucket contiene un nodo con el ID especificado.
     * @param nodeId ID del nodo a verificar.
     * @return true si el bucket contiene el nodo, false en caso contrario.
     */
    public boolean contains(KadId nodeId) {
        return nodes.stream().anyMatch(n -> n.getNodeId().equals(nodeId)); 
        // anyMatch() es un método de la interfaz Stream que devuelve true si algún elemento del stream coincide con el predicado especificado
    }
}
