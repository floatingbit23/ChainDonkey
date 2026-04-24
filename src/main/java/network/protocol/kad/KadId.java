package network.protocol.kad;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Identificador Kademlia de 128 bits (16 bytes).
 * Compatible con el espacio de IDs de eMule Kad.
 */
public class KadId implements Comparable<KadId> {

    private final byte[] data; // Array de 16 bytes que representa el ID
    private static final SecureRandom random = new SecureRandom(); // Generador de IDs aleatorios

    /**
     * Constructor de KadId a partir de un array de bytes.
     * @param data Array de bytes que representa el ID.
     * @throws IllegalArgumentException si el array no tiene 16 bytes.
     */
    public KadId(byte[] data) {

        if (data.length != 16) {
            throw new IllegalArgumentException("KadId debe tener exactamente 16 bytes");
        }

        this.data = Arrays.copyOf(data, 16); // Copia el array de 16 bytes para evitar modificaciones externas
    }

    /**
     * Genera un KadId aleatorio.
     */
    public static KadId random() {
        byte[] bytes = new byte[16]; // Reserva 16 bytes para el ID
        random.nextBytes(bytes); // Genera 16 bytes aleatorios
        return new KadId(bytes); // Crea un nuevo objeto KadId con los bytes aleatorios
    }

    /**
     * Calcula la distancia XOR entre este ID y otro.
     * d(x, y) = x XOR y
     */
    public KadId xor(KadId other) {

        byte[] result = new byte[16]; // Reserva 16 bytes para el resultado

        for (int i = 0; i < 16; i++) { // Recorre los 16 bytes
            result[i] = (byte) (this.data[i] ^ other.data[i]); // Calcula el XOR de cada byte con el XOR de los dos IDs
        }

        return new KadId(result); // Crea un nuevo objeto KadId con el resultado del XOR
    }

    /**
     * Retorna el bit en la posición dada (0-127).
     * El bit 0 es el MSB (Most Significant Bit).
     * Este método es útil para obtener el bit más significativo de un ID
     * y se utiliza en los algoritmos de Kademlia para encontrar el k-vecino más cercano.
     * 
     * @param position Posición del bit a obtener (0-127).
     * @return Bit en la posición dada (0 o 1).
     */
    public int getBit(int position) {
        int byteIndex = position / 8; // Índice del byte: 0-15 
        int bitIndex = 7 - (position % 8); // Índice del bit dentro del byte: 0-7
        return (data[byteIndex] >> bitIndex) & 0x01; // Retorna el bit en la posición dada
    }
    /*
    Ejemplo: Recibimos posición de bit 10
    10 / 8 = 1 -> El bit 10 está en el byte 1
    10 % 8 = 2 -> bitIndex = 7 - 2 = 5 -> El bit 10 está en la posición 5 del byte 1
    Return (data[1] >> 5) & 0x01 -> Retorna el bit en la posición 5 del byte 1 (0 o 1)
    */


    /**
     * Compara dos IDs basándose en su valor numérico (útil para distancias XOR).
     */
    @Override
    public int compareTo(KadId other) {

        for (int i = 0; i < 16; i++) { // Recorre los 16 bytes

            int b1 = this.data[i] & 0xFF; // Obtiene el valor entero del byte (0-255) del ID actual
            int b2 = other.data[i] & 0xFF; // Obtiene el valor entero del byte (0-255) del ID comparado
            
            if (b1 != b2) { // Si los bytes son diferentes
                return Integer.compare(b1, b2); // Retorna la diferencia (positiva si b1 > b2, negativa si b1 < b2)
            }
        }

        return 0; // Si los IDs son iguales (imposible), retorna 0
    }


    // Métodos hashCode() y equals() para usar KadId en Sets y Maps
    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KadId kadId = (KadId) o; // Convierte el objeto a KadId
        
        // Retorna true si los arrays de bytes son iguales
        return Arrays.equals(data, kadId.data); 
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data); // Genera un hashCode basado en los bytes del ID
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        for (byte b : data) {
            sb.append(String.format("%02x", b)); // Formatea cada byte como un número hexadecimal de 2 dígitos
        }

        return sb.toString(); // Retorna el KadId como una cadena hexadecimal
    }


    /**
     * Getter que retorna una copia del array de bytes que representa el KadId.
     * @return Copia del array de bytes del KadId.
     */
    public byte[] getBytes() {
        return Arrays.copyOf(data, 16); // Copia defensiva
    }

}
