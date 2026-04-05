# ChainDonkey 🫏🔗

ChainDonkey es una infraestructura de almacenamiento descentralizado desarrollada en Java que fusiona la inmutabilidad de la tecnología **Blockchain** con la eficiencia lógica del protocolo **eDonkey2000 (ED2K)**.

![logo](/images/ChainDonkey_logo.png)

## Características principales

- **Motor de Consenso**: Implementación de una cadena de bloques con **Proof of Work (Prueba de Trabajo)** y dificultad ajustable.

- **Integridad de Datos (Merkle Tree)**: Implementación de un árbol de Merkle binario para agrupar transacciones y garantizar la integridad total de cada bloque.

- **Seguridad Criptográfica (ECDSA)**: Identidad digital basada en curvas elípticas (Bouncy Castle) para la firma y verificación de transacciones.
- **Ecosistema Moderno**: Basado en **Java 25 (LTS)** y gestionado con **Maven**.

- **Testing Suite**: Cobertura de pruebas automatizadas con **JUnit 5** para auditar el _ledger_ (libro contable de transacciones) y prevenir fraudes.

- **Logs Universales**: Sistema de registros basado en texto (`[ OK ]`, `[ FAIL ]`, `[ MINER ]`) compatible con cualquier terminal (PowerShell, Bash, CMD).

## Tecnologías utilizadas
| Tecnología | Versión | Propósito |
| :--- | :--- | :--- |
| **Java** | 25 (LTS) | Lenguaje base y runtime. |
| **Maven** | 3.x+ | Gestión de dependencias y ciclo de vida. |
| **JUnit 5** | 5.10.x | Suite de pruebas unitarias y de integración. |
| **Gson** | 2.11.0 | Conversión de objetos a JSON. |
| **Bouncy Castle** | 1.83 | Librería criptográfica para firmas ECDSA. |

## Estructura del proyecto
Siguiendo las convenciones estándar de Maven:
```text
ChainDonkey/
├── pom.xml                # Configuración y dependencias (JUnit 5, GSON, BC)
├── README.md              # Documentación principal
└── src/
    ├── main/java/simpleblockchain/   # Lógica de producción
    │   ├── Block.java            # Estructura del bloque y minado
    │   ├── Transaction.java      # Lógica de pagos y firmas
    │   ├── Wallet.java           # Generación de llaves y balances
    │   ├── SimpleBlockchain.java # Orquestador y validación global
    │   └── StringUtil.java       # Utilidades (Hashing, Merkle, JSON)
    └── test/java/simpleblockchain/   # Suite de Pruebas
        ├── BlockchainTest.java   # Tests de integridad y anti-tampering
        ├── TransactionTest.java  # Tests de firmas y procesamiento
        └── WalletTest.java       # Tests de llaves y balances
```

## Instalación y Ejecución

### Requisitos previos

- Tener instalado **Java 25** o superior.
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
