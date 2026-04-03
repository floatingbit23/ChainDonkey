# ChainDonkey 🫏🔗

ChainDonkey es una infraestructura de almacenamiento descentralizado desarrollada en Java que fusiona la inmutabilidad de la tecnología **Blockchain** con la eficiencia histórica del protocolo **eDonkey2000 (ED2K)**.

![logo](/images/ChainDonkey_logo.png)

## Características principales
- **Motor de Consenso**: Implementación de una cadena de bloques básica con prueba de trabajo (Proof of Work).
- **Integridad de Datos**: Generación de hashes mediante el algoritmo SHA-256.
- **Ecosistema Moderno**: Basado en **Java 25 (LTS)** y gestionado con **Maven**.
- **Gestión de Datos**: Uso de **Google Gson** para la serialización y visualización del blockchain.

## Tecnologías utilizadas
| Tecnología | Versión | Propósito |
| :--- | :--- | :--- |
| **Java** | 25 (LTS) | Lenguaje base y runtime. |
| **Maven** | 3.x+ | Gestión de dependencias y ciclo de vida. |
| **Gson** | 2.11.0 | Conversión de objetos a JSON. |

## Estructura del proyecto
Siguiendo las convenciones estándar de Maven:
```text
ChainDonkey/
├── pom.xml                # Definición del proyecto y dependencias
├── README.md              # Documentación principal
└── src/
    └── main/
        └── java/
            └── simpleblockchain/   # Código fuente
                ├── Block.java            # Estructura del bloque
                ├── SimpleBlockchain.java # Lógica principal del blockchain
                └── StringUtil.java       # Utilidades de hashing y JSON
```

## Instalación y Ejecución

### Requisitos previos
- Tener instalado **Java 25** o superior.
- (Opcional) **Maven** para la gestión de dependencias.

### Cómo ejecutar (desde la terminal)
Si tienes Maven configurado en tu `PATH`:
```bash
mvn compile exec:java -Dexec.mainClass="simpleblockchain.SimpleBlockchain"
```

Si prefieres ejecutarlo manualmente con las librerías cacheadas:
```bash
java -cp "target/classes;%USERPROFILE%/.m2/repository/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar" simpleblockchain.SimpleBlockchain
```

