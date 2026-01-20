package com.smartshelfx.smartshelfx_backend.scheduler;

import com.smartshelfx.smartshelfx_backend.model.Alert;
import com.smartshelfx.smartshelfx_backend.model.Product;
import com.smartshelfx.smartshelfx_backend.Repository.AlertRepository;
import com.smartshelfx.smartshelfx_backend.Repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ExpiryScheduler {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AlertRepository alertRepository;

    // Runs at 12:00 AM every day
    @Scheduled(cron = "0 0 0 * * *")
    public void checkProductExpiry() {
        LocalDate sevenDaysFromNow = LocalDate.now().plusDays(7);
        List<Product> expiringSoon = productRepository.findByExpiryDateBefore(sevenDaysFromNow);

        for (Product p : expiringSoon) {
            Alert alert = new Alert();
            alert.setType("EXPIRY");
            alert.setPriority("HIGH");
            alert.setUserRole("MANAGER");
            alert.setProductSku(p.getSku());
            alert.setMessage("URGENT: " + p.getName() + " expires on " + p.getExpiryDate());
            alertRepository.save(alert);
        }
    }
}