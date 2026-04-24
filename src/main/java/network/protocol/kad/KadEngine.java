package network.protocol.kad;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import network.node.NodeInfo;

/**
 * Orquestador del motor Kademlia.
 * 
 * Gestiona la tabla de enrutamiento y las comunicaciones RPC (Remote Procedure Call) por UDP.
 * Mantiene el estado de los nodos conocidos y coordina las consultas.
 */
public class KadEngine {
    private static final Logger logger = LoggerFactory.getLogger(KadEngine.class);
    
    // Timeout por defecto para peticiones RPC (2 segundos para redes modernas)
    public static final int DEFAULT_TIMEOUT_MS = 2000;

    private final RoutingTable routingTable;
    private final Channel channel;
    private final AtomicLong messageIdGenerator = new AtomicLong(new Random().nextLong());
    private final Map<Long, CompletableFuture<List<NodeInfo>>> pendingFindNode = new ConcurrentHashMap<>();

    public KadEngine(KadId localId, Channel channel) {
        this.routingTable = new RoutingTable(localId);
        this.channel = channel;
    }

    /**
     * Envía una solicitud RPC de tipo FIND_NODE a un nodo remoto.
     * 
     * @param targetNode Nodo al que se envía la solicitud
     * @param targetId ID objetivo de la búsqueda
     * @return Un CompletableFuture que contendrá los nodos encontrados
     */
    public CompletableFuture<List<NodeInfo>> sendFindNode(NodeInfo targetNode, KadId targetId) {

        // Genera un ID único para la transacción
        long msgId = messageIdGenerator.incrementAndGet(); 

        // Crea un futuro para almacenar la respuesta
        CompletableFuture<List<NodeInfo>> future = new CompletableFuture<>(); 

        // Añade el futuro al mapa de transacciones pendientes
        pendingFindNode.put(msgId, future); 

        // Crea la solicitud FIND_NODE
        KadMessage.FindNodeRequest req = new KadMessage.FindNodeRequest(routingTable.getLocalId(), msgId, targetId); 

        // Asignamos el destino UDP para que el Codec lo use
        req.setRecipient(targetNode.getUdpAddress()); 
        
        logger.debug("[ENGINE] Enviando FIND_NODE a {} (ID: {}) para buscar {}", targetNode.getUdpAddress(), msgId, targetId);

        // Enviar vía UDP - Enviamos el objeto directamente para que el KadCodec lo procese
        channel.writeAndFlush(req);
        
        // Aplicamos el timeout de red 
        return future.orTimeout(DEFAULT_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Procesa un mensaje Kademlia recibido.
     * 
     * @param msg Mensaje recibido
     * @param senderAddress Dirección del remitente
     */
    public void receiveMessage(KadMessage msg, InetSocketAddress senderAddress) {

        // Actualizar tabla de enrutamiento con el remitente
        routingTable.addNode(new NodeInfo(msg.getSenderId(), senderAddress));

        switch (msg) {

            // Si recibimos una respuesta FIND_NODE_RES
            case KadMessage.FindNodeResponse res -> {

                // Recupera el futuro pendiente
                CompletableFuture<List<NodeInfo>> future = pendingFindNode.remove(res.getMessageId());
                
                // Si el futuro existe, lo completamos con la respuesta
                if (future != null) {
                   
                    logger.trace("[ENGINE] Recibida respuesta FIND_NODE de {} con {} nodos", res.getSenderId(), res.getClosestNodes().size());
                    
                    future.complete(res.getClosestNodes()); // Completamos el futuro con la respuesta
                }
            }

            // Si recibimos una solicitud FIND_NODE
            case KadMessage.FindNodeRequest req -> {
                
                logger.debug("[ENGINE] Recibida solicitud FIND_NODE de {} buscando {}", msg.getSenderId(), req.getTargetId());
                
                // Buscamos los nodos más cercanos al objetivo en nuestra tabla (máximo K=40)
                List<NodeInfo> closest = routingTable.findClosest(req.getTargetId(), 40);

                // Creamos la respuesta (FIND_NODE -> FIND_NODE_RES)
                KadMessage.FindNodeResponse res = new KadMessage.FindNodeResponse(
                        routingTable.getLocalId(), req.getMessageId(), closest);
                
                // Asignamos el destino de la respuesta
                res.setRecipient(senderAddress);
                
                // Enviamos la respuesta directamente al canal
                channel.writeAndFlush(res);
            }

            // Si recibimos un PING
            case KadMessage.PingRequest req -> {
                
                logger.trace("[ENGINE] Recibido PING de {}", msg.getSenderId());
                
                // Creamos la respuesta (PING -> PONG)
                KadMessage.PingResponse res = new KadMessage.PingResponse(
                        routingTable.getLocalId(), req.getMessageId());

                // Asignamos el destino del PONG
                res.setRecipient(senderAddress); 
                
                // Enviamos la respuesta directamente al canal
                channel.writeAndFlush(res);
            }

            // No implementamos otros tipos de mensajes por ahora
            default -> {}
        }
    }

    /**
     * Inicia un proceso de búsqueda recursiva (Lookup) de un nodo.
     * 
     * @param target KadId objetivo de la búsqueda
     * @return Un CompletableFuture que contendrá los nodos encontrados
     */
    public CompletableFuture<List<NodeInfo>> lookup(KadId target) {
        return new KadLookup(target, routingTable, this).execute();
    }

    // Getter de la Routing Table
    public RoutingTable getRoutingTable() {
        return routingTable;
    }
}
