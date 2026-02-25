package hotel.controller;

import hotel.service.HotelState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/hotel")
public class HotelController {
    private final HotelState hotelState;

    @Autowired
    public HotelController(HotelState hotelState) {
        this.hotelState = hotelState;
    }

    @GetMapping("/date")
    public Map<String, LocalDate> getCurrentDate() {
        return Map.of("currentDay", hotelState.getCurrentDay());
    }

    @PostMapping("/next-day")
    public Map<String, LocalDate> nextDay() {
        LocalDate newDay = hotelState.nextDay();
        return Map.of("currentDay", newDay);
    }

    @PostMapping("/save")
    public ResponseEntity<Map<String, String>> saveState() {
        hotelState.save();
        return ResponseEntity.ok(Map.of("message", "Состояние сохранено"));
    }
}
