package et.com.gebeya.parkinglotservice.controller;


import et.com.gebeya.parkinglotservice.dto.requestdto.PriceRequestDto;
import et.com.gebeya.parkinglotservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/parking-lot")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/lots/{parkingLotId}/pricing")
    public ResponseEntity<BigDecimal> getPricing(@ModelAttribute PriceRequestDto request, @PathVariable("parkingLotId")Integer parkingLotId){
        return ResponseEntity.ok(paymentService.dynamicPricing(request,parkingLotId));
    }
}