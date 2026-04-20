# ChainDonkey 🫏🔗

ChainDonkey es una infraestructura de almacenamiento descentralizado desarrollada en Java que fusiona la inmutabilidad de la tecnología **Blockchain** con la eficiencia lógica del protocolo **eDonkey2000 (ED2K)**.

![logo](/images/ChainDonkey_logo.png)

## Características principales

- **Motor de Consenso**: Implementación de una cadena de bloques con **Proof of Work (Prueba de Trabajo)** y dificultad ajustable.

- **Integridad de Datos (_Merkle Tree_)**: Implementación de un Árbol de Merkle binario para agrupar transacciones y garantizar la integridad total de cada bloque.

- **Seguridad Criptográfica (_ECDSA_)**: Identidad digital basada en curvas elípticas (_Bouncy Castle_) para la firma y verificación de transacciones.
- **Ecosistema Moderno**: Basado en **Java 21** y gestionado con **Maven**.

- **Red P2P Híbrida**: Capa de transporte basada en **Netty 4.2**, compatible con los protocolos de ofuscación y transferencia de la red eD2K.
- **Explorador Web (Dashboard)**: Interfaz visual interactiva construida con **Javalin** para auditar bloques y wallets en tiempo real.
- **Logs Universales**: Sistema de registros basado en texto (`[ OK ]`, `[ FAIL ]`, `[ MINER ]`) compatible con cualquier terminal.

## Tecnologías utilizadas
| Tecnología | Versión | Propósito |
| :--- | :--- | :--- |
| **Java** | 21 | Lenguaje base y runtime. |
| **Maven** | 3.x+ | Gestión de dependencias y ciclo de vida. |
| **JUnit 5** | 5.10.x | Suite de pruebas unitarias y de integración. |
| **Gson** | 2.11.0 | Conversión de objetos a JSON. |
| **Bouncy Castle** | 1.83 | Librería criptográfica para firmas ECDSA. |
| **Netty** | 4.2.x | Motor de red asíncrono para P2P. |
| **Javalin** | 6.1.x | Framework web ligero para el Dashboard. |

## Estructura del proyecto
Siguiendo las convenciones estándar de Maven:
```text
ChainDonkey/
├── pom.xml                # Configuración y dependencias (Netty, Javalin, JUnit 5, GSON, BC)
├── README.md              # Documentación principal
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── network/           # Infraestructura de red P2P (Netty 4.2)
    │   │   │   ├── node/          # Gestión de identidad y estado del nodo
    │   │   │   ├── protocol/      # Lógica de protocolos (eD2K Codec, Handshake...)
    │   │   │   └── transport/     # Servidores y clientes TCP/UDP
    │   │   └── simpleblockchain/  # Lógica del motor de la Blockchain
    │   │       ├── Block.java            # Estructura del bloque y minado
    │   │       ├── BlockchainServer.java # Servidor web y API del Dashboard
    │   │       ├── Transaction.java      # Lógica de pagos y firmas criptográficas
    │   │       ├── Wallet.java           # Generación de llaves y balances de Wallets
    │   │       ├── SimpleBlockchain.java # Orquestador y validación global
    │   │       └── StringUtil.java       # Utilidades (Hashing, Merkle Tree, JSON...)
    │   └── resources/
    │       └── public/            # Frontend del Dashboard (HTML, CSS, app.js)
    └── test/                      # Suite de Pruebas unitarias y de integración (JUnit 5)
```

## Instalación y Ejecución

### Requisitos previos

- Tener instalado **Java 21** o superior.
- **Maven** instalado y configurado en el `PATH`.

### Cómo ejecutar (Playground)
Para ejecutar la simulación principal de la blockchain:

**Desde PowerShell (Recomendado en Windows):**
```powershell
mvn compile exec:java "-Dexec.mainClass=simpleblockchain.SimpleBlockchain"
```

**Desde Bash / Git Bash:**
```bash
mvn compile exec:java -Dexec.mainClass="simpleblockchain.SimpleBlockchain"
```

### Cómo auditar (Running Tests)
Para ejecutar la auditoría de seguridad automatizada y verificar que la lógica de la blockchain es correcta:
```bash
mvn test
```

## Seguridad y Auditoría
El proyecto incluye verificaciones automáticas para:
1. **Doble Gasto (_Double-Spending_)**: Evita que un mismo "billete" (_UTXO_, _Unspent Transaction Output_) sea gastado dos veces.

2. **Alteración de Datos (_Anti-Tampering_)**: El _Merkle Root_ (un hash que representa todas las transacciones) detectará si alguien cambia un solo bit de una transacción histórica.

3. **Consistencia de la Cadena**: Verifica que todos los bloques estén correctamente enlazados mediante sus hashes previos.

## Explorador de Red
El nodo levanta automáticamente un servidor web en el puerto **7070**:
- **Dashboard**: `http://localhost:7070`
- **API Blockchain**: `http://localhost:7070/api/blockchain`
