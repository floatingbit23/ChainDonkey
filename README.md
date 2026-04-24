# ChainDonkey 🫏🔗

ChainDonkey es una infraestructura de almacenamiento descentralizado desarrollada en Java que fusiona la inmutabilidad de la tecnología **Blockchain** con la eficiencia lógica del protocolo **eDonkey2000 (ED2K)** y el descubrimiento de la red **Kademlia (Kad)**.

![logo](/images/ChainDonkey_logo.png)

## Características principales

- **Motor de Consenso**: Implementación de una cadena de bloques (_blockchain_) con **Proof of Upload (PoU)**.

- **Integridad de Datos (_Merkle Tree_)**: Árbol de Merkle binario para agrupar transacciones y garantizar la integridad de cada bloque.

- **Seguridad Criptográfica (_ECDSA_)**: Identidad digital basada en curvas elípticas (_Bouncy Castle_) para la firma y verificación de transacciones.

## 📖 Terminología de la Red

Para entender ChainDonkey, es importante familiarizarse con su ecosistema:

- **CDK**: Es el nombre de nuestra criptomoneda de reputación. Se genera únicamente mediante la **subida verificada de archivos legítimos** a la red (_Proof of Upload_).

- **Sherpas**: Son los nodos verificadores. Su misión es patrullar la red, validar que los archivos no son _fakes_ y auditar las subidas de otros nodos.

- **Sherpa-Challenge**: El desafío aleatorio que lanza un Sherpa para verificar la integridad de un fragmento de datos mediante el árbol AICH.

- **_The Stable_ (El Establo)**: Nuestra _Mempool_. Es el lugar donde las transacciones esperan pacientemente antes de ser incluidas en un bloque de la _blockchain_.

- **_The Braying_ (El Rebuzno)**: Nuestro protocolo de propagación (*Gossip*) de bloques y transacciones. Cuando ocurre algo importante, los nodos lo "rebuznan" para que toda la red se entere al instante.

- **_Proof of Upload_ (PoU)**: El mecanismo de consenso donde tu capacidad de minar bloques depende de cuánto hayas ayudado a la comunidad compartiendo datos legítimos.


## Arquitectura y Stack Tecnológico

- **Red P2P Híbrida (Netty 4.2)**: 
  - **Handshake eD2K**: Saludo inicial compatible con clientes eMule, incluyendo ofuscación de protocolo (RC4) y autenticación segura (RSA/_SecureIdent_).
  - **Motor Kademlia (Kad)**: DHT descentralizada con lógica de distancia XOR, k-buckets y búsqueda iterativa paralela ($\alpha=3$) para el descubrimiento de nodos.

- **Explorador Web (_Dashboard_)**: Interfaz visual interactiva construida con **Javalin** para auditar bloques, _wallets_ y el estado de la red P2P en tiempo real.

- **Ecosistema Moderno**: Basado en **Java 21** y gestionado con **Maven**.


### Tecnologías utilizadas
| Tecnología | Versión | Propósito |
| :--- | :--- | :--- |
| **Java** | 21 | Lenguaje base y runtime. |
| **Maven** | 3.x+ | Gestión de dependencias y ciclo de vida. |
| **Netty** | 4.2.x | Motor de red asíncrono para TCP (eD2K) y UDP (Kad). |
| **JUnit 5** | 5.10.x | Suite de pruebas unitarias y de integración. |
| **Bouncy Castle** | 1.83 | Librería criptográfica para firmas ECDSA y RSA. |
| **Javalin** | 6.1.x | Framework web ligero para el Dashboard. |
| **Gson** | 2.11.0 | Conversión de objetos a JSON. |

### Estructura del proyecto
```text
ChainDonkey/
├── pom.xml                # Configuración y dependencias
├── README.md              # Documentación principal
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── network/           # Infraestructura de red P2P
    │   │   │   ├── node/          # Identidad (KadId) y estado del nodo (NodeInfo)
    │   │   │   ├── protocol/      
    │   │   │   │   ├── ed2k/      # Protocolo eD2K (TCP, Ofuscación, Handshake)
    │   │   │   │   └── kad/       # Protocolo Kademlia (UDP, RoutingTable, Lookup)
    │   │   │   └── transport/     # Servidores y clientes Netty
    │   │   └── simpleblockchain/  # Lógica del motor Blockchain
    │   └── resources/
    │       └── public/            # Frontend del Dashboard (HTML, CSS, JS)
    └── test/                      # Suite de pruebas (Blockchain & Network)
```

## Instalación y Ejecución

### Requisitos previos
- **Java 21** instalado.
- **Maven** configurado en el `PATH`.

### Cómo ejecutar (Modo Nodo)
Para arrancar el nodo completo con soporte P2P y Dashboard:
```bash
mvn compile exec:java "-Dexec.mainClass=simpleblockchain.SimpleBlockchain"
```

### Cómo auditar (Running Tests)
Para ejecutar la suite completa de pruebas (Blockchain + Networking):
```bash
mvn test
```

## Seguridad y Auditoría
El proyecto incluye verificaciones automáticas para:
1. **Doble Gasto (_Double-Spending_)**: Protección contra el reuso de UTXOs (_Unspent Transaction Outputs_, es decir, transacciones pendientes de gastar).

2. **Alteración de Datos (_Anti-Tampering_)**: Detección de cambios mediante hashes Merkle.

3. **Protocolo Seguro**: Ofuscación de tráfico para evitar la detección por parte de los ISPs (Movistar, Vodafone, Orange...) y SecureIdent para validar identidades eMule (anti-botnet, anti-spoofing).

4. **Resiliencia Kad**: Tabla de enrutamiento tolerante a fallos basada en el algoritmo Kademlia original.

## Explorador de Red (Endpoints)
El nodo levanta automáticamente un servidor web en el puerto **7070**:
- **Dashboard**: `http://localhost:7070`
- **API Blockchain**: `http://localhost:7070/api/blockchain`
- **API Red P2P**: `http://localhost:7070/api/peers` (Próximamente)
