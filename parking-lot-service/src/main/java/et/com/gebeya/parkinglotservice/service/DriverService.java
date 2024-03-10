package et.com.gebeya.parkinglotservice.service;

import et.com.gebeya.parkinglotservice.dto.requestdto.AddDriverRequestDto;
import et.com.gebeya.parkinglotservice.dto.requestdto.AddUserRequest;
import et.com.gebeya.parkinglotservice.dto.requestdto.BalanceRequestDto;
import et.com.gebeya.parkinglotservice.dto.requestdto.UpdateDriverRequestDto;
import et.com.gebeya.parkinglotservice.dto.responsedto.AddUserResponse;
import et.com.gebeya.parkinglotservice.dto.responsedto.BalanceResponseDto;
import et.com.gebeya.parkinglotservice.dto.responsedto.DriverResponseDto;
import et.com.gebeya.parkinglotservice.exception.AuthException;
import et.com.gebeya.parkinglotservice.exception.DriverIdNotFound;
import et.com.gebeya.parkinglotservice.model.Driver;
import et.com.gebeya.parkinglotservice.repository.DriverRepository;
import et.com.gebeya.parkinglotservice.repository.specification.DriverSpecification;
import et.com.gebeya.parkinglotservice.util.MappingUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {
    private final BalanceService balanceService;
    private final DriverRepository driverRepository;
    private final AuthService authService;
    @Transactional
    public AddUserResponse registerDriver(AddDriverRequestDto dto) {
        Integer dId = null;
        try{
            Driver driver = MappingUtil.mapAddDiverRequestDtoToDriver(dto);
            driver = driverRepository.save(driver);
            dId = driver.getId();
            AddUserRequest addUserRequest = MappingUtil.mapCustomerToAddUserRequest(driver);
            Mono<ResponseEntity<AddUserResponse>> responseMono = authService.getAuthServiceResponseEntityMono(addUserRequest);
            BalanceRequestDto requestDto = BalanceRequestDto.builder().userId(driver.getId()).amount(BigDecimal.valueOf(0.0)).build();
            BalanceResponseDto responseDto = balanceService.createBalanceForDriver(requestDto);
            log.info("Response from Payment micro service==> {}", responseDto);
            return responseMono.blockOptional()
                    .map(ResponseEntity::getBody)
                    .orElseThrow(() -> new AuthException("Error occurred during generating of token"));
        }
        catch (Exception e){
            if(dId!=null)
            {
                log.info("delete the coupon balance ==>{}",dId);
                balanceService.deleteBalanceForDriver(dId);
            }
            throw e;
        }

    }

    public DriverResponseDto updateDriver(UpdateDriverRequestDto dto, Integer id) {
        Driver driver = getDriver(id);
        driver = MappingUtil.mapUpdateDiverRequestDtoToDriver(dto, driver);
        return MappingUtil.mapDriverToDriverResponseDto(driver);
    }

    public DriverResponseDto getDriverById(Integer id) {
        Driver driver = getDriver(id);
        return MappingUtil.mapDriverToDriverResponseDto(driver);
    }

    public DriverResponseDto getMyDriverProfile(){
        Integer driverId = (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return getDriverById(driverId);
    }

    Driver getDriver(Integer id) {
        List<Driver> driver = driverRepository.findAll(DriverSpecification.getDriverById(id));
        if (driver.isEmpty())
            throw new DriverIdNotFound("Driver is not found");
        return driver.get(0);
    }

    public List<DriverResponseDto> getAllDrivers(Pageable pageable){
        List<Driver> driverList = driverRepository.findAll(DriverSpecification.getAllDrivers(),pageable).stream().toList();
        return MappingUtil.listOfReservationToListOfDriverResponseDto(driverList);
    }
}
