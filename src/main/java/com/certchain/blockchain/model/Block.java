package com.certchain.blockchain.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Block {
    private int index;
    private long timestamp;
    private String data;          // JSON string with certificate or revocation details
    private String previousHash;
    private String hash;
    private int nonce;

    // Metadata for easier UI display
    private String blockType;     // "CERTIFICATE", "REVOCATION", "GENESIS"
    private String certificateId;

    public Block() {}

    public Block(int index, long timestamp, String data, String previousHash, String hash, int nonce) {
        this.index = index;
        this.timestamp = timestamp;
        this.data = data;
        this.previousHash = previousHash;
        this.hash = hash;
        this.nonce = nonce;
        extractMetadata();
    }

    private void extractMetadata() {
        if (data.contains("\"type\":\"CERTIFICATE\"")) {
            blockType = "CERTIFICATE";
            int start = data.indexOf("\"certificateId\":\"") + 17;
            int end = data.indexOf("\"", start);
            if (start > 17 && end > start) certificateId = data.substring(start, end);
        } else if (data.contains("\"type\":\"REVOCATION\"")) {
            blockType = "REVOCATION";
            int start = data.indexOf("\"certificateId\":\"") + 17;
            int end = data.indexOf("\"", start);
            if (start > 17 && end > start) certificateId = data.substring(start, end);
        } else {
            blockType = "GENESIS";
        }
    }

    // Getters and setters
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; extractMetadata(); }
    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    public int getNonce() { return nonce; }
    public void setNonce(int nonce) { this.nonce = nonce; }
    public String getBlockType() { return blockType; }
    public String getCertificateId() { return certificateId; }

    public String getFormattedTimestamp() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}