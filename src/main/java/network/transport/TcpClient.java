package network.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import network.protocol.ed2k.Ed2kCodec;
import network.protocol.ed2k.Ed2kObfuscationHandler;

/**
 * Cliente TCP basado en Netty para el protocolo eD2K.
 * Se utiliza para conectar a servidores eD2K y otros clientes de la red.
 */

public class TcpClient {

    private static final Logger logger = LoggerFactory.getLogger(TcpClient.class);

    private final EventLoopGroup group; // EventLoopGroup para el cliente

    // Constructor que inicializa el EventLoopGroup (grupo de hilos que manejan las conexiones)
    public TcpClient() {
        this.group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    }

    /**
     * Método que conecta a un host y puerto remotos.
     * @param host Dirección IP o dominio.
     * @param port Puerto TCP.
     * @param targetUserHash Hash del usuario destino (opcional, null si no aplica).
     * @return Un ChannelFuture que permite monitorizar el progreso de la conexión.
     */
    public ChannelFuture connect(String host, int port, byte[] targetUserHash) {
        
        // Bootstrap es una clase ayudante que facilita la creación de Channels
        Bootstrap b = new Bootstrap();

        // Configura el EventLoopGroup
        b.group(group)

         // Configura el tipo de canal (NIO -> non-blocking IO -> entrada/salida no bloqueante)
         .channel(NioSocketChannel.class) // Socket TCP
         
         // Configura las opciones del canal
         .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // Tiempo máximo de espera para la conexión (5s)
         .option(ChannelOption.SO_KEEPALIVE, true) // Mantenimiento de la conexión activa
         
         // Configura el pipeline de handlers (Se encarga del procesamiento de los mensajes)
         .handler(new ChannelInitializer<SocketChannel>() {

            // Método que se ejecuta cuando se crea un nuevo canal
             @Override
             public void initChannel(SocketChannel ch) {
                 // El cliente siempre usa ofuscación en modo INICIADOR. 
                 // Si hay un targetUserHash, lo usamos para el protocolo Peer (isServer=false).
                 ch.pipeline().addLast(new Ed2kObfuscationHandler(true, targetUserHash)); // true = modo INICIADOR, targetUserHash = hash del usuario destino (Opcional)
                 ch.pipeline().addLast(new Ed2kCodec()); // Decodificador y codificador de eD2K

                 // TO DO: Aquí añadiremos los otros handlers cuando los implementemos
             }
         });

        logger.debug("[TCP CLIENT] Intentando conectar a {}:{}...", host, port);

        // connect() inicia la conexión y devuelve un ChannelFuture
        return b.connect(host, port);
    }

    /*
     * Método que cierra el cliente y libera los recursos.
     */
    public void shutdown() {
        group.shutdownGracefully(); // Cierra el EventLoopGroup de forma controlada
        logger.debug("[TCP CLIENT] Cliente cerrado.");
    }
}
