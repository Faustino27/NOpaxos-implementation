package models;

public enum DataSize {
    ZERO(0),
    HUNDRED_BYTES(100),
    ONE_KB(1024),
    FOUR_KB(4096);

    private final int size;

    DataSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}

