package hotel.dto;

import java.util.List;

public class CheckInRequest {

    private int roomNumber;
    private int days;
    private List<GuestDto> guests;

    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }

    public int getDays() { return days; }
    public void setDays(int days) { this.days = days; }

    public List<GuestDto> getGuests() { return guests; }
    public void setGuests(List<GuestDto> guests) { this.guests = guests; }
}