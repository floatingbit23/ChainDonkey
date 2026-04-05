package simpleblockchain;

import java.security.PublicKey;

public class TransactionOutput {
    
    public String id; // Reference to the transaction (transactionId) this output belongs to
    public PublicKey recipient; // The address that will receive the funds
    public float value; // The amount of funds being sent
    public String parentTransactionId; // Reference to the parent transaction (id of the transaction that created this output)

    // Constructor
    public TransactionOutput(PublicKey recipient, float value, String parentTransactionId) {
        this.recipient = recipient; // To
        this.value = value;
        this.parentTransactionId = parentTransactionId;

        // Every TransactionOutput object MUST have a unique ID (hash) to be correctly tracked as an unspent "bill" in the ledger
        this.id = StringUtil.applySha256(StringUtil.getStringFromKey(recipient) + Float.toString(value) + parentTransactionId);
    }

    // Boolean method to check if the Output belongs to a specific address (Wallet)
    public boolean isMine(PublicKey publicKey) {
        return (publicKey == recipient); 
        // if the PK of the transaction output is equal to the PK of the address that is checking
        // then the output belongs to that address (True)

        // otherwise, it is not mine (False)
    }

}
