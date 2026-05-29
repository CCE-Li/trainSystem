package trainsys.dao;

import trainsys.dao.entity.TicketInfoEntity;
import trainsys.dao.mapper.TicketInfoMapper;
import trainsys.util.FixedString;
import trainsys.util.Time;
import trainsys.util.TrainScheduler;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
/**
 * 车票持久化管理器。
 * 负责车票库存记录的查询、更新、发布和列表读取。
 */
public class TicketManager {
    private final TicketInfoMapper ticketInfoMapper;

    public TicketManager(TicketInfoMapper ticketInfoMapper) {
        this.ticketInfoMapper = ticketInfoMapper;
    }

    /**
     * 查询指定区段的余票数量。
     */
    public int querySeat(FixedString trainID, Time departureTime, int stationID) {
        TicketInfoEntity entity = ticketInfoMapper.selectOne(new QueryWrapper<TicketInfoEntity>()
                .eq("train_id", trainID.toString())
                .eq("departure_time", departureTime.toString())
                .eq("departure_station", stationID)
                .last("LIMIT 1"));
        return entity == null || entity.getSeatNum() == null ? -1 : entity.getSeatNum();
    }

    /**
     * 按增量方式更新余票，并返回对应票价。
     */
    public int updateSeat(FixedString trainID, Time departureTime, int stationID, int delta) {
        TicketInfoEntity entity = ticketInfoMapper.selectOne(new QueryWrapper<TicketInfoEntity>()
                .eq("train_id", trainID.toString())
                .eq("departure_time", departureTime.toString())
                .eq("departure_station", stationID)
                .last("LIMIT 1"));
        if (entity == null) {
            return -1;
        }
        int oldSeatNum = entity.getSeatNum() == null ? 0 : entity.getSeatNum();
        entity.setSeatNum(oldSeatNum + delta);
        ticketInfoMapper.updateById(entity);
        return entity.getPrice() == null ? -1 : entity.getPrice();
    }

    /**
     * 根据车次调度信息发布整趟列车各区段的票务记录。
     */
    public void releaseTicket(TrainScheduler scheduler, Time baseTime) {
        int passingStationNum = scheduler.getPassingStationNum();
        for (int i = 0; i + 1 < passingStationNum; i++) {
            Time departureTime = scheduler.getDepartureTimeAt(i, baseTime);
            QueryWrapper<TicketInfoEntity> query = new QueryWrapper<TicketInfoEntity>()
                    .eq("train_id", scheduler.getTrainID().toString())
                    .eq("departure_time", departureTime.toString())
                    .eq("departure_station", scheduler.getStation(i).value())
                    .last("LIMIT 1");
            TicketInfoEntity entity = ticketInfoMapper.selectOne(query);
            if (entity == null) {
                entity = new TicketInfoEntity();
            }
            entity.setTrainId(scheduler.getTrainID().toString());
            entity.setDepartureTime(departureTime.toString());
            entity.setDepartureStation(scheduler.getStation(i).value());
            entity.setArrivalStation(scheduler.getStation(i + 1).value());
            entity.setSeatNum(scheduler.getSeatNum());
            entity.setPrice(scheduler.getPrice(i));
            entity.setDuration(scheduler.getDuration(i));

            if (entity.getId() == null) {
                ticketInfoMapper.insert(entity);
            } else {
                ticketInfoMapper.updateById(entity);
            }
        }
    }

    /**
     * 删除指定车次和发车时间下的全部票务记录。
     */
    public void expireTicket(FixedString trainID, Time departureTime) {
        ticketInfoMapper.delete(new QueryWrapper<TicketInfoEntity>()
                .eq("train_id", trainID.toString())
                .eq("departure_time", departureTime.toString()));
    }

    /**
     * 获取所有已发布且余票合法的车票记录。
     */
    public List<Object[]> getAllReleasedTickets() {
        List<TicketInfoEntity> entities = ticketInfoMapper.selectList(new QueryWrapper<TicketInfoEntity>()
                .ge("seat_num", 0)
                .orderByAsc("train_id", "departure_time", "departure_station"));
        return toTicketRows(entities);
    }

    /**
     * 获取全部车票记录。
     */
    public List<Object[]> getAllTickets() {
        List<TicketInfoEntity> entities = ticketInfoMapper.selectList(new QueryWrapper<TicketInfoEntity>()
                .orderByAsc("train_id", "departure_time", "departure_station"));
        return toTicketRows(entities);
    }

    /**
     * 将实体列表转换为统一的数组结构，供上层组装 DTO。
     */
    private List<Object[]> toTicketRows(List<TicketInfoEntity> entities) {
        List<Object[]> tickets = new ArrayList<>();
        for (TicketInfoEntity entity : entities) {
            Object[] ticket = new Object[7];
            ticket[0] = entity.getTrainId();
            ticket[1] = entity.getDepartureTime();
            ticket[2] = entity.getDepartureStation();
            ticket[3] = entity.getArrivalStation();
            ticket[4] = entity.getSeatNum();
            ticket[5] = entity.getPrice();
            ticket[6] = entity.getDuration();
            tickets.add(ticket);
        }
        return tickets;
    }
}
