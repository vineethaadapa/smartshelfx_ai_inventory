package com.smartshelfx.smartshelfx_backend.Service;

import com.smartshelfx.smartshelfx_backend.Repository.UserRepository;
import com.smartshelfx.smartshelfx_backend.model.Prediction;
import com.smartshelfx.smartshelfx_backend.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class PredictionService {

    @Autowired
    private UserRepository userRepository;

    public List<Prediction> getFilteredDemand(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return Collections.emptyList();

        RestTemplate restTemplate = new RestTemplate();
        String flaskApiUrl;

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            flaskApiUrl = "http://localhost:5001/api/predict_all";
        } else if ("MANAGER".equalsIgnoreCase(user.getRole())) {
            flaskApiUrl = "http://localhost:5001/api/predict_for_manager/" + user.getWarehouseId();
        } else {
            flaskApiUrl = "http://localhost:5001/api/predict_for_vendor/" + user.getId();
        }

        try {
            System.out.println("DEBUG: Requesting Flask URL: " + flaskApiUrl);
            Prediction[] results = restTemplate.getForObject(flaskApiUrl, Prediction[].class);
            
            if (results != null) {
                System.out.println("DEBUG: Successfully mapped " + results.length + " items.");
            }
            return results != null ? Arrays.asList(results) : Collections.emptyList();
        } catch (Exception e) {
            System.err.println("DEBUG: Mapping/Connection Error: " + e.getMessage());
            e.printStackTrace(); // This will show you exactly why mapping failed in the console
            return Collections.emptyList();
        }
    }
}