package simpleblockchain;

// This claass will be used to reference TransactionOutputs that have not yet been spent
public class TransactionInput {

    public String transactionOutputId; // Reference to the previous transaction's output
    // Allows us to spend the output of a previous transaction

    // UTXO is the "Unspent Transaction Output" object (in spanish: objeto de "salida no gastada")
    public TransactionOutput UTXO; 
    
    // Constructor 
    public TransactionInput(String transactionOutputId) {
        this.transactionOutputId = transactionOutputId; 
    }
}
