package network.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * Servidor UDP basado en Netty para el protocolo Kademlia (Kad).
 * Al ser UDP, no tiene estado de conexión y gestiona datagramas individuales.
 */

public class UdpServer {

    private static final Logger logger = LoggerFactory.getLogger(UdpServer.class);

    private final int port; // Puerto en el que escuchará el servidor UDP
    private EventLoopGroup group; // Grupo de eventos de Netty
    private ChannelFuture serverChannelFuture; // Futuro del canal UDP

    /**
     * Constructor del servidor UDP.
     * @param port Puerto en el que escuchará el servidor UDP
     */
    public UdpServer(int port) {
        this.port = port;
    }

    /**
     * Método que arranca el servidor UDP.
     * @throws InterruptedException Si el hilo es interrumpido
     */
    public void start() throws InterruptedException {

        group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        // Misma lógica que TcpServer, salvo que usamos un canal NioDatagramChannel en lugar de un canal NioServerSocketChannel.
        // Como UDP no tiene conexión persistente (connectionless), no usamos un Pipeline, sino un ChannelInitializer que añade un handler genérico.
        
        try {

            // Bootstrap es una clase ayudante que facilita la creación de Channels
            Bootstrap b = new Bootstrap();

            // Configura el EventLoopGroup
            b.group(group)
             .channel(NioDatagramChannel.class) // Socket UDP para paquetes individuales
             .option(ChannelOption.SO_BROADCAST, true) // Permite broadcast (para ping a todas las máquinas de la red local)
             
             // Configura el pipeline de handlers (Se encarga del procesamiento de los mensajes)
             .handler(new io.netty.channel.ChannelInitializer<NioDatagramChannel>() {

                // Método que se ejecuta cuando se crea un nuevo canal
                 @Override
                 protected void initChannel(NioDatagramChannel ch) {
                     // TO DO: Aquí añadiremos KadCodec y KadMessageHandler en la Fase 2.5
                 }

             });

            logger.info("[UDP SERVER] Escuchando en el puerto {} (Kademlia)...", port);

            serverChannelFuture = b.bind(port).sync(); // El servidor UDP se vincula al puerto y empieza a escuchar.
            
        } catch (InterruptedException e) {

            logger.error("[UDP SERVER] Error al arrancar el servidor UDP en el puerto {}", port, e);

            stop(); // Se detiene el servidor si hay un error

            throw e; // Se lanza la excepción
        }
    }

    /**
     * Método que detiene el servidor UDP de forma segura.
     */
    public void stop() {
        if (serverChannelFuture != null) {
            serverChannelFuture.channel().close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        logger.info("[UDP SERVER] Servidor UDP detenido.");
    }

    /**
     * Getter que permite obtener el puerto en el que escucha el servidor UDP.
     * @return Puerto en el que escucha el servidor UDP
     */
    public int getPort() {
        return port;
    }
}
