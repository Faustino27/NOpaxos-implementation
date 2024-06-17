package models;

public class SignatureCounterPair {
    private String signature;
    private int counter;

    public SignatureCounterPair(String signature, int counter) {
        this.signature = signature;
        this.counter = counter;
    }

    public String getSignature() {
        return signature;
    }

    public int getCounter() {
        return counter;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }
}
