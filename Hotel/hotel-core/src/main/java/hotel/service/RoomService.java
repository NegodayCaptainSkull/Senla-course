package hotel.service;

import enums.RoomSort;
import enums.SortDirection;
import exceptions.DaoException;
import hotel.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import hotel.dao.RoomDao;
import hotel.dao.RoomGuestHistoryDao;
import hotel.dto.RoomWithGuestsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoomService {
    private final RoomDao roomDao;
    private final RoomGuestHistoryDao historyDao;
    private final HotelState hotelState;
    private final GuestService guestService;

    private HotelConfig config;

    @Autowired
    public RoomService(RoomDao roomDao, RoomGuestHistoryDao historyDao, HotelState hotelState, GuestService guestService, HotelConfig config) {
        this.roomDao = roomDao;
        this.hotelState = hotelState;
        this.guestService = guestService;
        this.historyDao = historyDao;
        this.config = config;
    }

    public List<Room> getAllRooms() {
        return roomDao.findAll();
    }

    public List<Room> getAvailableRooms() {
        return roomDao.findAvailable();
    }

    public Room getRoomByNumber(int roomNumber) {
        return roomDao.findById(roomNumber).orElseThrow(() -> new DaoException("Комната не найдена: " + roomNumber));
    }

    public boolean isRoomExists(int roomNumber) {
        return getRoomByNumber(roomNumber) != null;
    }

    public String getRoomInformation(int roomNumber) {
        Room room = getRoomByNumber(roomNumber);
        List<Guest> guests = guestService.getGuestsByRoom(roomNumber);

        StringBuilder info = new StringBuilder();
        info.append("Номер ").append(room.getNumber())
                .append(" тип: ").append(room.getType())
                .append("\nСтоимость: ").append(room.getPrice())
                .append(" вместимость: ").append(room.getCapacity())
                .append("\nСтатус: ").append(room.getStatus());

        if (!guests.isEmpty()) {
            info.append("\nГости:");
            for (Guest guest : guests) {
                info.append("\n  - ").append(guest.getInformation());
            }
        }

        return info.toString();
    }

    @Transactional
    public void updateRoomPrice(int roomNumber, int price) {
        try {
            Room room = getRoomByNumber(roomNumber);

            room.setPrice(price);
            roomDao.update(room);
        } catch (Exception e) {
            throw new DaoException("Ошибка обновления цены комнаты", e);
        }
    }

    @Transactional
    public void updateRoom(Room room) {
        try {
            roomDao.update(room);
        } catch (Exception e) {
            throw new DaoException("Ошибка обновления комнаты", e);
        }
    }

    @Transactional
    public boolean setRoomUnderMaintenance(int roomNumber, int days) {
        LocalDate date = hotelState.getCurrentDay();

        try {
            Room room = getRoomByNumber(roomNumber);

            if (room.setUnderMaintenance(date, days)) {
                roomDao.update(room);
                return true;
            }

            return false;
        } catch (Exception e) {
            throw new DaoException("Ошибка перевода комнаты на обслуживание", e);
        }
    }

    @Transactional
    public boolean setRoomCleaning(int roomNumber) {
        LocalDate date = hotelState.getCurrentDay();

        try {
            Room room = getRoomByNumber(roomNumber);

            if (room.setCleaning(date)) {
                roomDao.update(room);
                return true;
            }

            return false;
        } catch (Exception e) {
            throw new DaoException("Ошибка уборки комнаты", e);
        }
    }

    @Transactional
    public boolean setRoomAvailable(int roomNumber) {
        try {
            Room room = getRoomByNumber(roomNumber);

            if (room.setAvailable()) {
                roomDao.update(room);
                return true;
            }

            return false;
        } catch (Exception e) {
            throw new DaoException("Ошибка перевода комнаты в доступный режим");
        }
    }

    @Transactional
    public Room saveRoom(Room room) {
        try {
            Room savedRoom = roomDao.save(room);
            return savedRoom;
        } catch (Exception e) {
            throw new DaoException("Ошибка при сохранении комнаты", e);
        }
    }

    public Map<Integer, Room> getAvailableRoomsByDate(int days) {
        LocalDate currentDate = hotelState.getCurrentDay();
        LocalDate date = currentDate.plusDays(days);
        Map<Integer, Room> availableRoomsByDate = new HashMap<>();
        for (Room room : getAllRooms()) {
            LocalDate endDate = room.getEndDate();
            if (endDate == null || date.isAfter(endDate)) {
                availableRoomsByDate.put(room.getNumber(), room);
            }
        }
        return availableRoomsByDate;
    }

    public List<RoomWithGuestsDto> getSortedRooms(RoomSort sortBy, SortDirection direction) {
        List<Room> rooms = getAllRooms();
        Map<Integer, Room> sortedRooms = sortRooms(rooms, sortBy, direction);

        List<RoomWithGuestsDto> result = new ArrayList<>();
        for (Room room : sortedRooms.values()) {
            List<Guest> guests = guestService.getGuestsByRoom(room.getNumber());
            result.add(new RoomWithGuestsDto(room, guests));
        }

        return result;
    }

    public List<RoomWithGuestsDto> getSortedAvailableRooms(RoomSort sortBy, SortDirection direction) {
        List<Room> rooms = getAvailableRooms();
        Map<Integer, Room> sortedRooms = sortRooms(rooms, sortBy, direction);

        List<RoomWithGuestsDto> result = new ArrayList<>();
        for (Room room : sortedRooms.values()) {
            List<Guest> guests = guestService.getGuestsByRoom(room.getNumber());
            result.add(new RoomWithGuestsDto(room, guests));
        }

        return result;
    }

    public List<List<RoomGuestHistory>> getRoomHistory(int roomNumber) {
        return historyDao.getPreviousGuestGroups(roomNumber, config.getRoomHistorySize());
    }

    private Map<Integer, Room> sortRooms(List<Room> roomsToSort, RoomSort sortBy, SortDirection direction) {
        Comparator<Room> comparator = switch (sortBy) {
            case RoomSort.PRICE -> Comparator.comparingInt(Room::getPrice);
            case RoomSort.CAPACITY -> Comparator.comparingInt(Room::getCapacity);
            case RoomSort.TYPE -> Comparator.comparing(Room::getType);
        };

        if (direction == SortDirection.DESC) {
            comparator = comparator.reversed();
        }

        return roomsToSort.stream()
                .sorted(comparator)
                .collect(Collectors.toMap(
                        Room::getNumber,
                        room -> room,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
}
