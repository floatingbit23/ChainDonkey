package network.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import network.protocol.ed2k.Ed2kCodec;
import network.protocol.ed2k.Ed2kObfuscationHandler;

/**
 * Servidor TCP basado en Netty para el protocolo eD2K.
 * Gestiona las conexiones entrantes (el "callback" de conexión a un servidor y de otros clientes eD2K)
 * al nodo de la blockchain de ChainDonkey.
 * 
 * Funciones:
 * 1. Lograr el HighID (cuando la conexión sea con un servidor eD2K).
 * 2. Intercambio de Bloques: En una implementación posterior, otros nodos de la red eD2K
 * se conectarán a este servidor para pedirme bloques de la blockchain o archivos.
 */

public class TcpServer {

    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);

    private final int port; // Puerto en el que escucha el servidor eD2K
    private EventLoopGroup bossGroup; // Grupo de eventos para manejar las conexiones
    private EventLoopGroup workerGroup; // Grupo de eventos para manejar las conexiones
    private ChannelFuture serverChannelFuture; // Futuro del canal del servidor

    /**
     * Constructor del servidor TCP.
     * @param port Puerto en el que escucha el servidor eD2K
     */
    public TcpServer(int port) {
        this.port = port;
    }

    /**
     * Método que arranca el servidor TCP.
     * @throws InterruptedException Si el servidor no se puede arrancar.
     */
    public void start() throws InterruptedException {

        // "Doble Grupo" de Netty 4.2+:
        // bossGroup: Un hilo que acepta conexiones entrantes.
        // workerGroup: Un hilo que procesa las conexiones entrantes.
        bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        try {
            // Bootstrap es una clase ayudante que facilita la creación de Channels
            ServerBootstrap b = new ServerBootstrap(); 
            
            b.group(bossGroup, workerGroup) // Asigna los grupos de hilos
             
             // Configura el tipo de canal (NIO -> non-blocking IO -> entrada/salida no bloqueante)
             .channel(NioServerSocketChannel.class) // Socket de servidor TCP (acepta nuevas conexiones)
             
             // Configura el pipeline de handlers (Se encarga del procesamiento de los mensajes)
             .childHandler(new ChannelInitializer<SocketChannel>() { 

                // Método que se ejecuta cuando se establece una conexión
                 @Override
                 public void initChannel(SocketChannel ch) {

                     // El servidor eD2K siempre usa ofuscación en modo RESPONDER para el test de puertos
                     // y texto plano si se detecta así (manejado por el Ed2kObfuscationHandler).
                     ch.pipeline().addLast(new Ed2kObfuscationHandler(false)); // false: Actúa en modo RESPONDER
                     ch.pipeline().addLast(new Ed2kCodec()); // Decodificador de eD2K (Bytes -> Objetos eD2KMessage)

                     // TO DO: Aquí añadiremos el manejador de mensajes eD2K en la siguiente etapa
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128) // Conexiones pendientes
             .childOption(ChannelOption.SO_KEEPALIVE, true); // Keepalive 

            logger.info("[TCP SERVER] Escuchando en el puerto {}...", port);
            
            serverChannelFuture = b.bind(port).sync(); // El servidor se vincula al puerto 4662 y empieza a escuchar.
            
        } catch (InterruptedException e) {
            logger.error("[TCP SERVER] Error al arrancar el servidor en el puerto {}", port, e);
            stop(); // Detiene el servidor si hay un error
            throw e; // Lanza la excepción
        }
    }

    /*
     * Detiene el servidor de forma segura.
     */
    public void stop() {

        // Si el servidor está corriendo, se detiene
        if (serverChannelFuture != null) {
            serverChannelFuture.channel().close(); // Cierra el canal del servidor
        }

        // Si el grupo de eventos está corriendo, se detiene
        if (bossGroup != null) {
            bossGroup.shutdownGracefully(); // Cierra el grupo de eventos de forma controlada
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully(); // Cierra el grupo de eventos de forma controlada
        }

        logger.info("[TCP SERVER] Servidor detenido.");

    }

    /**
     * Getter que permite obtener el puerto en el que escucha el servidor eD2K.
     * @return Puerto en el que escucha el servidor eD2K
     */
    public int getPort() {
        return port;
    }
}
