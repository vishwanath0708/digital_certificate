package com.certchain.blockchain.controller;

import com.certchain.blockchain.model.Block;
import com.certchain.blockchain.service.BlockchainService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class CertificateController {

    private final BlockchainService blockchainService;

    public CertificateController(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("difficulty", blockchainService.getDifficulty());
        return "index";
    }

    // API: get full chain (for debugging)
    @GetMapping("/api/chain")
    @ResponseBody
    public List<Block> getChain() {
        return blockchainService.getChain();
    }

    // Issue certificate
    @PostMapping("/api/issue")
    @ResponseBody
    public ResponseEntity<?> issueCertificate(@RequestBody Map<String, String> request) {
        try {
            Block block = blockchainService.issueCertificate(
                request.get("studentId"),
                request.get("studentName"),
                request.get("degree"),
                request.get("certificateId")
            );
            return ResponseEntity.ok(block);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Revoke certificate
    @PostMapping("/api/revoke")
    @ResponseBody
    public ResponseEntity<?> revokeCertificate(@RequestBody Map<String, String> request) {
        try {
            Block block = blockchainService.revokeCertificate(
                request.get("certificateId"),
                request.get("reason")
            );
            return ResponseEntity.ok(block);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Verify certificate - real-time lookup
    @GetMapping("/api/verify/{certificateId}")
    @ResponseBody
    public ResponseEntity<?> verifyCertificate(@PathVariable String certificateId) {
        BlockchainService.CertificateStatus status = blockchainService.verifyCertificate(certificateId);
        if (status == null) {
            return ResponseEntity.ok(Map.of("exists", false, "message", "Certificate not found"));
        }
        return ResponseEntity.ok(status);
    }
}
