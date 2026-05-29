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
/**
 * 票务数据访问层。
 * 负责车票发布、停售、余票查询以及订单相关持久化操作。
 */
public class TicketDao {

    private final SystemContextDao systemContextDao;

    public TicketDao(SystemContextDao systemContextDao) {
        this.systemContextDao = systemContextDao;
    }

    /**
     * 发布指定车次在给定发车时间下的票务记录。
     */
    public void releaseTicket(TrainScheduler scheduler, TicketQueryRequest request) {
        systemContextDao.getTrainSystem().releaseTicket(scheduler, new Time(request.getDepartureTime()));
    }

    /**
     * 停止销售指定车次和发车时间下的车票。
     */
    public void expireTicket(TicketQueryRequest request) {
        systemContextDao.getTrainSystem().expireTicket(
                new FixedString(request.getTrainId()),
                new Time(request.getDepartureTime())
        );
    }

    /**
     * 查询指定区段的剩余票数。
     */
    public int queryRemainingTicket(String trainId, String departureTime, int stationId) {
        return systemContextDao.getTrainSystem().queryRemainingTicket(
                new FixedString(trainId),
                new Time(departureTime),
                new StationID(stationId)
        );
    }

    /**
     * 写入购票结果。
     */
    public void orderTicket(BuyTicketRequest request, int stationId) {
        systemContextDao.getTrainSystem().orderTicket(
                new FixedString(request.getTrainId()),
                new Time(request.getDepartureTime()),
                new StationID(stationId)
        );
    }

    /**
     * 写入退票结果。
     */
    public void refundTicket(RefundTicketRequest request, int stationId) {
        systemContextDao.getTrainSystem().refundTicket(
                new FixedString(request.getTrainId()),
                new Time(request.getDepartureTime()),
                new StationID(stationId)
        );
    }

    /**
     * 查询指定用户的全部行程订单。
     */
    public List<TripInfo> queryUserTrips(long userId) {
        return systemContextDao.getTrainSystem().getTripManager().queryTrip(userId);
    }

    /**
     * 获取系统中的全部车票记录。
     */
    public List<Object[]> getAllTickets() {
        return systemContextDao.getTrainSystem().getTicketManager().getAllTickets();
    }

    /**
     * 获取当前已发布的车票记录。
     */
    public List<Object[]> getAllReleasedTickets() {
        return systemContextDao.getTrainSystem().getTicketManager().getAllReleasedTickets();
    }
}
