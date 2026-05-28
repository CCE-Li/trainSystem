package trainsys.dao;

import trainsys.model.BuyTicketRequest;
import trainsys.model.RefundTicketRequest;
import trainsys.model.TicketQueryRequest;
import trainsys.model.TripInfo;
import trainsys.util.FixedString;
import trainsys.util.Time;
import trainsys.util.TrainScheduler;
import trainsys.util.Types.StationID;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TicketDao {

    private final SystemContextDao systemContextDao;

    public TicketDao(SystemContextDao systemContextDao) {
        this.systemContextDao = systemContextDao;
    }

    public void releaseTicket(TrainScheduler scheduler, TicketQueryRequest request) {
        systemContextDao.getTrainSystem().releaseTicket(scheduler, new Time(request.getDepartureTime()));
    }

    public void expireTicket(TicketQueryRequest request) {
        systemContextDao.getTrainSystem().expireTicket(
                new FixedString(request.getTrainId()),
                new Time(request.getDepartureTime())
        );
    }

    public int queryRemainingTicket(String trainId, String departureTime, int stationId) {
        return systemContextDao.getTrainSystem().queryRemainingTicket(
                new FixedString(trainId),
                new Time(departureTime),
                new StationID(stationId)
        );
    }

    public void orderTicket(BuyTicketRequest request, int stationId) {
        systemContextDao.getTrainSystem().orderTicket(
                new FixedString(request.getTrainId()),
                new Time(request.getDepartureTime()),
                new StationID(stationId)
        );
    }

    public void refundTicket(RefundTicketRequest request, int stationId) {
        systemContextDao.getTrainSystem().refundTicket(
                new FixedString(request.getTrainId()),
                new Time(request.getDepartureTime()),
                new StationID(stationId)
        );
    }

    public List<TripInfo> queryUserTrips(long userId) {
        return systemContextDao.getTrainSystem().getTripManager().queryTrip(userId);
    }

    public List<Object[]> getAllTickets() {
        return systemContextDao.getTrainSystem().getTicketManager().getAllTickets();
    }

    public List<Object[]> getAllReleasedTickets() {
        return systemContextDao.getTrainSystem().getTicketManager().getAllReleasedTickets();
    }
}
