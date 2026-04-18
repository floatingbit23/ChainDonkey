package network.protocol.ed2k;

/**
 * Las constantes utilizadas en el protocolo eDonkey2000 (eD2K).
 *
 * Estas constantes incluyen los "Magic Bytes" del protocolo, los códigos de operación (opcodes) 
 * para varios tipos de mensajes y los puertos estándar utilizados en la red.
 */

public final class Ed2kConstants {

    // Constructor privado para evitar instanciación
    private Ed2kConstants() {
    }

    // --- Magic Bytes ---
    
    /** Magic Byte de la red eDonkey. */
    public static final byte PR_EDONKEY = (byte) 0xE3;
    
    /** Magic Byte de la red extendida eMule. */
    public static final byte PR_EMULE = (byte) 0xC5;
    
    // --- Opcodes ---

    /** Opcode de solicitud de inicio de sesión enviado de cliente a servidor. */
    public static final byte OP_LOGINREQUEST = 0x01;
    
    /** Opcode de respuesta de inicio de sesión enviado de servidor a cliente. */
    public static final byte OP_LOGINRESPONSE = 0x4C;

    /** Opcode de estado del servidor (contiene el ID del cliente). */
    public static final byte OP_SERVERSTATUS = 0x40;

    /** Opcode de mensaje de texto enviado por el servidor. */
    public static final byte OP_SERVERMESSAGE = 0x38;

    // --- Tipos de Etiquetas (Tags) ---

    /** Tipo de etiqueta para entero de 1 byte. */
    public static final byte TAG_TYPE_UINT8 = 0x08;
    
    /** Tipo de etiqueta para entero de 2 bytes. */
    public static final byte TAG_TYPE_UINT16 = 0x09;
    
    /** Tipo de etiqueta para entero de 4 bytes. */
    public static final byte TAG_TYPE_UINT32 = 0x03;
    
    /** Tipo de etiqueta para cadena de texto (string). */
    public static final byte TAG_TYPE_STRING = 0x02;
    
    /** Tipo de etiqueta para punto flotante (float). */
    public static final byte TAG_TYPE_FLOAT = 0x04;
    
    /** Tipo de etiqueta para hash de 16 bytes. */
    public static final byte TAG_TYPE_HASH = 0x0A;

    // --- Nombres de Etiquetas Conocidos ---

    /** Nombre de etiqueta para el nombre del cliente. */
    public static final byte CT_NAME = 0x01;
    
    /** Nombre de etiqueta para el puerto del cliente. */
    public static final byte CT_PORT = 0x02;
    
    /** Nombre de etiqueta para la versión del cliente. */
    public static final byte CT_VERSION = 0x11;
    
    /** Nombre de etiqueta para versión secundaria (ID 0x01). */
    public static final byte CT_VERSION_01 = 0x01;
    
    /** Nombre de etiqueta para versión eMule (ID 0xFB). */
    public static final byte CT_EMULE_VERSION = (byte) 0xFB;
    
    /** Nombre de etiqueta para las banderas de capacidades del servidor. */
    public static final byte CT_SERVER_FLAGS = 0x20;

    // --- Puertos ---

    /** Puerto TCP por defecto para clientes eD2K. */
    public static final int DEFAULT_CLIENT_TCP_PORT = 4662;
    
    /** Puerto UDP por defecto para Kademlia. */
    public static final int DEFAULT_KAD_UDP_PORT = 4672;
    
    /** Puerto TCP por defecto para servidores eD2K. */
    public static final int DEFAULT_SERVER_TCP_PORT = 4661;
}

