

package com.smartshelfx.smartshelfx_backend.Service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.smartshelfx.smartshelfx_backend.model.WarehouseStock;
import com.smartshelfx.smartshelfx_backend.Repository.StockTransactionRepository;
import com.smartshelfx.smartshelfx_backend.Repository.WarehouseStockRepository;
import com.smartshelfx.smartshelfx_backend.dto.InventoryTrendDTO;
import com.smartshelfx.smartshelfx_backend.dto.MonthlyComparisonDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private WarehouseStockRepository warehouseStockRepo; // Use WarehouseStock to see all locations

    private final StockTransactionRepository transactionRepository;

    // Spring will automatically inject the repository here
    public ReportService(StockTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<InventoryTrendDTO> getInventoryTrends() {
        List<Object[]> results = transactionRepository.findDailyStockChanges();
        List<InventoryTrendDTO> trendData = new ArrayList<>();
        
        double runningTotal = 0.0;

        for (Object[] result : results) {
            String date = result[0].toString();
            // result[1] is the SUM(quantity) from the query
            double dailyChange = ((Number) result[1]).doubleValue();
            
            runningTotal += dailyChange;
            trendData.add(new InventoryTrendDTO(date, runningTotal));
        }
        
        return trendData;
    }

    public void generateInventoryPdf(HttpServletResponse response) throws IOException {
        // Fetch all stock entries across all warehouses
        List<WarehouseStock> stocks = warehouseStockRepo.findAll();

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();
        
        // 1. Title Section
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("SmartShelfX - Global Warehouse Inventory", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        document.add(new Paragraph("Report Generated on: " + java.time.LocalDate.now()));
        document.add(new Paragraph(" ")); // Spacer

        // 2. Create Table (6 Columns now to include Warehouse ID)
        PdfPTable table = new PdfPTable(6); 
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        
        // 3. Table Headers
        String[] headers = {"WH-ID", "SKU", "Product", "Stock", "Threshold", "Status"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }

        // 4. Fill Data from WarehouseStock
        for (WarehouseStock ws : stocks) {
            // WH-ID column
            table.addCell(String.valueOf(ws.getWarehouseId()));
            
            // Product info from the linked Product object
            table.addCell(ws.getProduct().getSku());
            table.addCell(ws.getProduct().getName());
            
            // Stock levels from the specific warehouse entry
            Integer currentStock = (ws.getCurrentStock() != null) ? ws.getCurrentStock() : 0;
            Integer reorderLevel = (ws.getReorderLevel() != null) ? ws.getReorderLevel() : 0;
            
            table.addCell(String.valueOf(currentStock));
            table.addCell(String.valueOf(reorderLevel));
            
            // Status Logic
            boolean isLow = currentStock <= reorderLevel;
            String statusText = isLow ? "LOW STOCK" : "HEALTHY";
            
            PdfPCell statusCell = new PdfPCell(new Phrase(statusText));
            statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            if (isLow) {
                statusCell.setBackgroundColor(new java.awt.Color(255, 204, 203)); // Light Red
            } else {
                statusCell.setBackgroundColor(new java.awt.Color(204, 255, 204)); // Light Green
            }
            table.addCell(statusCell);
        }

        document.add(table);
        document.close();
    }

    public List<MonthlyComparisonDTO> getMonthlyComparison() {
    List<Object[]> results = transactionRepository.findMonthlyComparison();
    return results.stream().map(result -> new MonthlyComparisonDTO(
        (String) result[0], 
        ((Number) result[1]).doubleValue(), 
        ((Number) result[2]).doubleValue()
    )).collect(Collectors.toList());
}
}