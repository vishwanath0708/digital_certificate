package com.certchain.blockchain.service;

import com.certchain.blockchain.model.Block;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BlockchainService {

    private final List<Block> blockchain = new ArrayList<>();
    private static final int DIFFICULTY = 2;
    private static final String DIFFICULTY_PREFIX = "0".repeat(DIFFICULTY);
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Fast cache for certificate status (real-time)
    private final Map<String, CertificateStatus> certificateCache = new ConcurrentHashMap<>();

    public BlockchainService() {
        createGenesisBlock();
    }

    private void createGenesisBlock() {
        String genesisData = "{\"type\":\"GENESIS\",\"message\":\"Certificate Authority Blockchain - Genesis\"}";
        Block genesis = mineBlock(0, genesisData, "0");
        blockchain.add(genesis);
    }

    private Block mineBlock(int index, String data, String previousHash) {
        long timestamp = System.currentTimeMillis();
        int nonce = 0;
        String hash = calculateHash(index, timestamp, data, previousHash, nonce);
        while (!hash.startsWith(DIFFICULTY_PREFIX)) {
            nonce++;
            hash = calculateHash(index, timestamp, data, previousHash, nonce);
        }
        return new Block(index, timestamp, data, previousHash, hash, nonce);
    }

    public String calculateHash(int index, long timestamp, String data, String previousHash, int nonce) {
        String input = index + timestamp + data + previousHash + nonce;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not found", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }

    public synchronized Block issueCertificate(String studentId, String studentName, String degree, String certificateId) throws Exception {
        if (certificateCache.containsKey(certificateId)) {
            throw new IllegalArgumentException("Certificate ID already exists: " + certificateId);
        }
        Map<String, Object> certData = new LinkedHashMap<>();
        certData.put("type", "CERTIFICATE");
        certData.put("certificateId", certificateId);
        certData.put("studentId", studentId);
        certData.put("studentName", studentName);
        certData.put("degree", degree);
        certData.put("issueDate", System.currentTimeMillis());
        String dataJson = objectMapper.writeValueAsString(certData);
        Block latest = getLatestBlock();
        Block newBlock = mineBlock(latest.getIndex() + 1, dataJson, latest.getHash());
        blockchain.add(newBlock);
        updateCertificateCache(newBlock);
        return newBlock;
    }

    public synchronized Block revokeCertificate(String certificateId, String reason) throws Exception {
        CertificateStatus status = certificateCache.get(certificateId);
        if (status == null) throw new IllegalArgumentException("Certificate ID not found");
        if (status.revoked) throw new IllegalStateException("Certificate already revoked");
        Map<String, Object> revokeData = new LinkedHashMap<>();
        revokeData.put("type", "REVOCATION");
        revokeData.put("certificateId", certificateId);
        revokeData.put("reason", reason);
        revokeData.put("revokedAt", System.currentTimeMillis());
        String dataJson = objectMapper.writeValueAsString(revokeData);
        Block latest = getLatestBlock();
        Block newBlock = mineBlock(latest.getIndex() + 1, dataJson, latest.getHash());
        blockchain.add(newBlock);
        updateCertificateCache(newBlock);
        return newBlock;
    }

    public synchronized CertificateStatus verifyCertificate(String certificateId) {
        return certificateCache.get(certificateId);
    }

    private void updateCertificateCache(Block block) {
        try {
            if (block.getBlockType() == null) return;
            if ("CERTIFICATE".equals(block.getBlockType())) {
                Map<String, Object> data = objectMapper.readValue(block.getData(), Map.class);
                String certId = (String) data.get("certificateId");
                CertificateStatus status = new CertificateStatus();
                status.certificateId = certId;
                status.studentName = (String) data.get("studentName");
                status.degree = (String) data.get("degree");
                status.issueDate = (Long) data.get("issueDate");
                status.revoked = false;
                certificateCache.put(certId, status);
            } else if ("REVOCATION".equals(block.getBlockType())) {
                Map<String, Object> data = objectMapper.readValue(block.getData(), Map.class);
                String certId = (String) data.get("certificateId");
                CertificateStatus existing = certificateCache.get(certId);
                if (existing != null) {
                    existing.revoked = true;
                    existing.revocationReason = (String) data.get("reason");
                    existing.revokedAt = (Long) data.get("revokedAt");
                    certificateCache.put(certId, existing);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized List<Block> getChain() {
        return new ArrayList<>(blockchain);
    }

    public synchronized Block getLatestBlock() {
        return blockchain.get(blockchain.size() - 1);
    }

    public int getDifficulty() {
        return DIFFICULTY;
    }

    public static class CertificateStatus {
        public String certificateId;
        public String studentName;
        public String degree;
        public long issueDate;
        public boolean revoked;
        public String revocationReason;
        public Long revokedAt;

        public boolean isValid() { return !revoked; }
        public String getFormattedIssueDate() {
            return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(issueDate));
        }
        public String getFormattedRevokedAt() {
            if (revokedAt == null) return "";
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(revokedAt));
        }
    }
}
