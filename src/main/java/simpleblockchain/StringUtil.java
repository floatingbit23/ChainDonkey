package simpleblockchain;

import java.security.MessageDigest; // Used for SHA-256 hashing

import com.google.gson.GsonBuilder; // Used to convert Objects to JSON strings

// Class to handle SHA-256 hashing and JSON conversion
public class StringUtil {

    // Public static method that applies SHA-256 to the received String, and returns the result
    public static String applySha256(String input) {

        try {

            // Create a MessageDigest object, using the SHA-256 algorithm
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Applies sha256 to our String
            byte[] hash = digest.digest(input.getBytes("UTF-8"));

            /*
            getBytes() converts the String to an array of bytes.
            digest() calculates the SHA-256 hash of the byte array.
            byte[] is an array of bytes. Each byte is a number between -128 and 127.
            */

            // Dynamic container for the hash as a hexidecimal string
            StringBuilder hexString = new StringBuilder(); 

            /*
            StringBuilder is a modern, mutable sequence of characters (faster than StringBuffer for single-threaded tasks).
            It is used to build strings in a more efficient way than using the "+" operator with Strings.
            */

            // Loop through the 32 hash bytes
            for (int i = 0; i < hash.length; i++) {

                String hex = Integer.toHexString(0xff & hash[i]);

                /*
                '0xff & hash[i]' ensures that the byte is treated as an unsigned integer (signed range (-128 to 127) -> unsigned range(0 to 255)).
                'toHexString()' converts the byte to a hexidecimal character.
                */

                // If the hexidecimal character is only one character long (e.g. 15 -> f), add a '0' to the beginning (f -> 0f)
                if (hex.length() == 1){
                    hexString.append('0');
                }

                // Appends the hexidecimal character to the end of the container
                hexString.append(hex); 
            }

            // Convert the container to a String (unmutable) and return it
            return hexString.toString(); // Returns 64 hexadecimal characters (256 bits)

        } catch (Exception e) { 
            throw new RuntimeException(e); //
        }
    }

    // Public static method that turns an Object into a JSON String
    public static String getJson(Object o) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(o);
    }

    // Public static method that returns the difficulty String target, to compare to hash.
    public static String getDifficultyString(int difficulty) {
        return new String(new char[difficulty]).replace('\0', '0');
    }

    /*
    Difficulty is the number of leading zeros required for a hash to be considered valid.
    The higher the difficulty, the more zeros are required, and the harder it is to mine a block.

    For example; a difficulty of 5 will return "00000"
    */

}