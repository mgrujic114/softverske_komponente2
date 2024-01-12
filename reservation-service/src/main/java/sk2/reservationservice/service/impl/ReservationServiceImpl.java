package sk2.reservationservice.service.impl;

import sk2.reservationservice.client.trainingservice.dto.SessionDto;
import sk2.reservationservice.client.userservice.dto.DiscountDto;
import sk2.reservationservice.domain.Reservation;
import sk2.reservationservice.dto.ReservationCreateDto;
import sk2.reservationservice.exception.NotFoundException;
import sk2.reservationservice.repository.ReservationRepository;
import sk2.reservationservice.service.ReservationService;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service
public class ReservationServiceImpl implements ReservationService {

    private ReservationRepository reservationRepository;
    private RestTemplate trainingServiceRestTemplate;
    private RestTemplate userServiceRestTemplate;

    public ReservationServiceImpl(ReservationRepository reservationRepository, RestTemplate trainingServiceRestTemplate, RestTemplate userServiceRestTemplate) {
        this.reservationRepository = reservationRepository;
        this.trainingServiceRestTemplate = trainingServiceRestTemplate;
        this.userServiceRestTemplate = userServiceRestTemplate;
    }

    @Override
    public void addReservation(ReservationCreateDto reservationCreateDto) {
        //get projection from movie service
        ResponseEntity<SessionDto> projectionDtoResponseEntity = null;
        try {
            projectionDtoResponseEntity = trainingServiceRestTemplate.exchange("/session/"
                    + reservationCreateDto.getSessionId(), HttpMethod.GET, null, SessionDto.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND))
                throw new NotFoundException(String.format("Projection with id: %d not found.", reservationCreateDto.getSessionId()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        //get discount from user service
        ResponseEntity<DiscountDto> discountDtoResponseEntity = userServiceRestTemplate.exchange("/user/" +
                reservationCreateDto.getUserId() + "/discount", HttpMethod.GET, null, DiscountDto.class);
        //calculate price
        BigDecimal price = projectionDtoResponseEntity.getBody().getPrice().divide(BigDecimal.valueOf(100))
                .multiply(BigDecimal.valueOf(100 - discountDtoResponseEntity.getBody().getDiscount()));
        //save reservation
        Reservation reservation = new Reservation(reservationCreateDto.getUserId(), reservationCreateDto.getSessionId(), price);
        reservationRepository.save(reservation);
    }
}
