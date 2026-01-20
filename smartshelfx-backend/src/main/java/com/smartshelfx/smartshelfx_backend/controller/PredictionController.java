package com.smartshelfx.smartshelfx_backend.controller;

import com.smartshelfx.smartshelfx_backend.model.Prediction;
import com.smartshelfx.smartshelfx_backend.Service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/predictions")
@CrossOrigin(origins = "http://localhost:4200")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    @GetMapping("/demand-data") 
    public ResponseEntity<List<Prediction>> getDemand(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        // The service logic handles the filtering based on the user's role
        return ResponseEntity.ok(predictionService.getFilteredDemand(principal));
    }
 }