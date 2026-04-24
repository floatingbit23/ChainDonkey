package network.protocol.kad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import network.node.NodeInfo;

/**
 * Algoritmo de búsqueda iterativa FIND_NODE en Kademlia.
 * Consulta recursivamente a los nodos más cercanos al objetivo hasta que no se encuentran nodos más cercanos.
 */
public class KadLookup {

    private static final Logger logger = LoggerFactory.getLogger(KadLookup.class);

    private static final int K = 40; // Tamaño del k-bucket (Aumentado para mayor eficiencia en redes modernas)
    // Estándar del paper original de Kademlia era 20

    private static final int ALPHA = 6; // Grado de paralelismo (cuántas consultas lanzar a la vez)
    // Estándar del paper original de Kademlia es 3
 
    private final KadId targetId; // ID del nodo o recurso que estamos buscando
    private final KadEngine engine; // Referencia al motor Kad para realizar los envíos UDP
    
    // Conjunto de IDs que ya han sido consultados para evitar duplicidad
    private final Set<KadId> queried = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // Lista dinámica de los mejores candidatos encontrados hasta el momento (ordenados por cercanía)
    private final List<NodeInfo> shortlist = Collections.synchronizedList(new ArrayList<>());
    
    // Promesa que se completará cuando el algoritmo termine con la lista final de nodos
    private final CompletableFuture<List<NodeInfo>> future = new CompletableFuture<>();

    /**
     * Constructor del algoritmo de búsqueda iterativa FIND_NODE
     * 
     * @param targetId Nodo objetivo de la búsqueda (puede ser cualquier ID, no necesariamente un nodo real)
     * @param routingTable Tabla de enrutamiento del nodo local
     * @param engine Motor de Kademlia para enviar mensajes
     */
    public KadLookup(KadId targetId, RoutingTable routingTable, KadEngine engine) {

        this.targetId = targetId;
        this.engine = engine;
        
        // 1. Fase de Semilla: Cargamos los candidatos iniciales desde nuestra propia tabla de rutas
        List<NodeInfo> closest = routingTable.findClosest(targetId, K);
        shortlist.addAll(closest); 
        
        logger.debug("[LOOKUP] Iniciando búsqueda para target {}. Candidatos iniciales: {}", targetId, closest.size());
    }

    /**
     * Inicia la ejecución asíncrona del algoritmo de búsqueda.
     * @return Un 'CompletableFuture' (futuro) que contendrá los K nodos más cercanos al finalizar.
     */
    public CompletableFuture<List<NodeInfo>> execute() {

        // Si no tenemos ni un solo candidato (red aislada), terminamos inmediatamente
        if (shortlist.isEmpty()) {
            future.complete(new ArrayList<>());
            return future;
        }
        
        // Lanzamos el primer paso iterativo
        step();

        return future;
    }

    /**
     * Un paso iterativo del algoritmo (Fase Recursiva).
     * Selecciona los ALPHA (3) mejores candidatos no consultados y les envía un FIND_NODE.
     */
    private void step() {

        // Si la búsqueda ya se ha cancelado o completado, no hacemos nada
        if (future.isDone()) return;

        List<NodeInfo> toQuery; // Lista de nodos a consultar en este paso

        // synchronized para evitar race conditions
        synchronized (shortlist) { 

            // 2. Selección: Elegimos los mejores nodos a los que aún no hayamos preguntado
            toQuery = shortlist.stream()
                    .filter(n -> !queried.contains(n.getNodeId())) // Filtra los nodos que ya han sido consultados
                    .sorted(Comparator.comparing(n -> n.getNodeId().xor(targetId))) // Ordena los nodos por distancia XOR al target
                    .limit(ALPHA) // Limita el número de nodos a consultar a ALPHA
                    .collect(Collectors.toList()); // Convierte el stream en una lista y la retorna
        }

        // Logea cuántos nodos se están consultando en este paso
        logger.trace("[LOOKUP-STEP] Consultando a {} nuevos nodos", toQuery.size());

        // Si no hay nodos para consultar, se da por finalizada la búsqueda
        if (toQuery.isEmpty()) {

            // 3. Condición de Parada: No quedan más nodos mejores a los que preguntar en la shortlist
            synchronized (shortlist) {

                List<NodeInfo> result = shortlist.stream()
                        .sorted(Comparator.comparing(n -> n.getNodeId().xor(targetId)))
                        .limit(K) // Limita el resultado final a K (40) nodos
                        .collect(Collectors.toList());

                logger.info("[LOOKUP-DONE] Búsqueda finalizada. Encontrados {} nodos cercanos.", result.size());
                
                future.complete(result); // Completa el futuro con el resultado
            }

            return; // Finaliza la búsqueda
        }

        // 4. Ejecución Paralela: Lanzamos las consultas RPC de forma asíncrona
        List<CompletableFuture<List<NodeInfo>>> responses = new ArrayList<>();

        // Para cada nodo de 'toQuery' (los nodos a consultar)
        for (NodeInfo node : toQuery) {
            queried.add(node.getNodeId()); // Se añade el nodo al conjunto de consultados ('queried')
            responses.add(engine.sendFindNode(node, targetId)); // Se envía la solicitud RPC al nodo
        }


        // 5. Gestión de Respuestas: Esperamos a que terminen todas las consultas (o fallen)
        CompletableFuture.allOf(responses.toArray(CompletableFuture[]::new))
                .orTimeout(KadEngine.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS) // Usamos el timeout del motor (2s)
                .handle((v, ex) -> {

                    // Itera sobre cada respuesta del KadEngine
                    for (CompletableFuture<List<NodeInfo>> response : responses) {

                        try {
                            
                            List<NodeInfo> result = response.getNow(null); // Obtiene la lista de nodos encontrados

                            // Si el nodo respondió, procesamos los nuevos candidatos que nos ha dado
                            if (result != null) {
                                synchronized (shortlist) {

                                    // Para cada nodo encontrado en la respuesta
                                    for (NodeInfo found : result) {

                                        // Si el nodo NO está en nuestra lista ya...
                                        if (shortlist.stream().noneMatch(n -> n.getNodeId().equals(found.getNodeId()))) {
                                            // Lo añadimos
                                            shortlist.add(found);
                                        }

                                    }
                                }
                            }

                        } catch (Exception ignored) {
                            // Si un nodo falla (timeout/error), simplemente lo ignoramos y seguimos
                        }
                    }

                    // 6. Recursión: Una vez procesadas las respuestas, lanzamos el siguiente paso
                    step(); 

                    return null;
                });
    }
}
