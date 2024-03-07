package et.com.gebeya.parkinglotservice.service;

import et.com.gebeya.parkinglotservice.dto.requestdto.*;
import et.com.gebeya.parkinglotservice.dto.responsedto.BalanceResponseDto;
import et.com.gebeya.parkinglotservice.dto.responsedto.ReservationResponseDto;
import et.com.gebeya.parkinglotservice.exception.InsufficientBalance;
import et.com.gebeya.parkinglotservice.exception.ParkingLotAvailabilityException;
import et.com.gebeya.parkinglotservice.model.Driver;
import et.com.gebeya.parkinglotservice.model.ParkingLot;
import et.com.gebeya.parkinglotservice.model.ParkingLotProvider;
import et.com.gebeya.parkinglotservice.model.Reservation;
import et.com.gebeya.parkinglotservice.repository.ReservationRepository;
import et.com.gebeya.parkinglotservice.repository.specification.ReservationSpecification;
import et.com.gebeya.parkinglotservice.util.MappingUtil;
import et.com.gebeya.parkinglotservice.util.MessagingUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final ParkingLotService parkingLotService;
    private final PricingService pricingService;
    private final DriverService driverService;
    private final WebClient.Builder webClientBuilder;
    private final MessageService messageService;

    @Transactional
    public Map<String, String> book(Integer parkingLotId, ReservationRequestDto dto) {
        Integer driverId = (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Driver driver = driverService.getDriver(driverId);
        ParkingLot parkingLot = parkingLotService.getParkingLot(parkingLotId);
        if (parkingLot.getAvailableSlot() <= 0)
            throw new ParkingLotAvailabilityException("the parking lot that you selected is full at the moment");
        ParkingLotProvider provider = parkingLot.getParkingLotProvider();
        PriceRequestDto requestDto = PriceRequestDto.builder().duration(dto.getStayingDuration()).build();
        BigDecimal price = pricingService.dynamicPricing(requestDto, parkingLotId);
        Reservation reservation = Reservation.builder().price(price).parkingLot(parkingLot).driver(driver).stayingDuration(dto.getStayingDuration()).isReservationAccepted(true).build();
//        TransferBalanceRequestDto transferBalanceRequestDto = TransferBalanceRequestDto.builder().providerId(provider.getId()).driverId(driverId).amount(price).build();
//        Mono<BalanceResponseDto> balanceResponseDtoMono = transferBalance(transferBalanceRequestDto);
//        BalanceResponseDto balanceResponseDto = balanceResponseDtoMono.block();
//        log.info("Response from the paymentService==> {}", balanceResponseDto);
        checkBalanceForDriver(driverId,price);
        reservationRepository.save(reservation);
        notifyBooking(provider.getId(), driverId, parkingLot.getName(), dto.getStayingDuration());
        return Map.of("message", "you have reserved a parking lot successfully");

    }


    public Mono<BalanceResponseDto> transferBalance(TransferBalanceRequestDto transferBalanceRequestDto) {
        return webClientBuilder.build().post()
                .uri("http://PAYMENT-SERVICE/api/v1/payment/transfer")
                .bodyValue(transferBalanceRequestDto)
                .retrieve()
                .bodyToMono(BalanceResponseDto.class);
    }


    private Mono<BalanceResponseDto> checkBalance(Integer driverId) {
        return webClientBuilder.build().post()
                .uri("http://PAYMENT-SERVICE/api/v1/payment/driver_balance/"+driverId)
                .retrieve()
                .bodyToMono(BalanceResponseDto.class);
    }

    private void checkBalanceForDriver(Integer driverId, BigDecimal desiredAmount){
        BalanceResponseDto balanceResponseDto = checkBalance(driverId).block();
        assert balanceResponseDto != null;
        if(balanceResponseDto.getBalance().compareTo(desiredAmount)<0)
            throw new InsufficientBalance("You have insufficient balance. please purchase coupons");
    }

    public void updateAvailableSlotOfParkingLot(Integer parkingLotId) {
        ParkingLot parkingLot = parkingLotService.getParkingLot(parkingLotId);
        UpdateParkingLotDto updateParkingLotDto = UpdateParkingLotDto.builder().availableSlot(parkingLot.getAvailableSlot() - 1).build();
        parkingLotService.updateParkingLot(updateParkingLotDto, parkingLotId);
    }

    private void notifyBooking(Integer providerId, Integer driverId, String parkingLotName, LocalTime duration) {
        messageService.sendPushNotificationForDriver(driverId, MessagingUtil.driverBookNotification(parkingLotName, duration));
        messageService.sendPushNotificationForProvider(providerId, MessagingUtil.providerWithdrawalNotification());
    }

    public List<ReservationResponseDto> getReservationByProviderId(){
        Integer providerId = (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Reservation> reservationList = reservationRepository.findAll(ReservationSpecification.getReservationByProviderId(providerId));
        return MappingUtil.mapListOfReservationToReservationResponseDto(reservationList);
    }

}
