package hotel.service;

import exceptions.DaoException;
import hotel.Guest;
import hotel.Room;
import hotel.RoomGuestHistory;
import hotel.dao.GuestDao;
import hotel.dao.RoomDao;
import hotel.dao.RoomGuestHistoryDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class HotelServiceFacade {
    private final RoomDao roomDao;
    private final GuestDao guestDao;
    private final RoomGuestHistoryDao historyDao;
    private final HotelState hotelState;

    @Autowired
    HotelServiceFacade(RoomDao roomDao, GuestDao guestDao, RoomGuestHistoryDao historyDao, HotelState hotelState) {
        this.roomDao = roomDao;
        this.guestDao = guestDao;
        this.historyDao = historyDao;
        this.hotelState = hotelState;
    }

    @Transactional
    public boolean checkIn(List<Guest> guests, int roomNumber, int days) {
        try {
            LocalDate currentDay = hotelState.getCurrentDay();

            Room room = roomDao.findById(roomNumber)
                    .orElseThrow(() -> new DaoException("Комната не найдена: " + roomNumber));

            if (!room.canCheckIn(guests.size())) {
                return false;
            }

            room.markAsOccupied(currentDay, days);
            roomDao.update(room);

            List<Guest> savedGuests = new ArrayList<>();
            for (Guest guest : guests) {
                guest.setRoomNumber(roomNumber);
                Guest saved = guestDao.save(guest);
                savedGuests.add(saved);
            }

            guests.clear();
            guests.addAll(savedGuests);
            return true;
        } catch (Exception e) {
            throw new DaoException("Ошибка заселения", e);
        }
    }

    @Transactional
    public boolean checkOut(int roomNumber) {
        try {
            Room room = roomDao.findById(roomNumber)
                    .orElseThrow(() -> new DaoException("Комната не найдена: " + roomNumber));

            if (!room.canCheckOut()) {
                return false;
            }

            List<Guest> guests = guestDao.findByRoomNumber(roomNumber);

            if (guests.isEmpty()) {
                return false;
            }

            int nextGroupId = historyDao.getNextGroupId(roomNumber);
            for (Guest guest : guests) {
                RoomGuestHistory history = RoomGuestHistory.fromGuest(guest, roomNumber, nextGroupId);
                historyDao.save(history);
                guestDao.delete(guest.getId());
            }

            room.markAsAvailable();
            roomDao.update(room);
            return true;
        } catch (Exception e) {
            throw new DaoException("Ошибка выселения", e);
        }
    }
}
