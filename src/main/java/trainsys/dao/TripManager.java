package trainsys.dao;

import trainsys.dao.entity.TripInfoEntity;
import trainsys.dao.mapper.TripInfoMapper;
import trainsys.dao.support.DbCodec;
import trainsys.model.TripInfo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
/**
 * 行程持久化管理器。
 * 负责订单行程记录的新增、查询和删除。
 */
public class TripManager {
    private final TripInfoMapper tripInfoMapper;

    public TripManager(TripInfoMapper tripInfoMapper) {
        this.tripInfoMapper = tripInfoMapper;
    }

    /**
     * 为指定用户新增一条行程记录。
     */
    public void addTrip(long userID, TripInfo trip) {
        TripInfoEntity entity = new TripInfoEntity();
        entity.setUserId(userID);
        entity.setTrainId(trip.getTrainID().toString());
        entity.setDepartureStation(trip.getDepartureStation().value());
        entity.setArrivalStation(trip.getArrivalStation().value());
        entity.setType(trip.getType());
        entity.setDuration(trip.getDuration());
        entity.setPrice(trip.getPrice());
        entity.setDepartureTime(trip.getDepartureTime().toString());
        entity.setArrivalTime(trip.getArrivalTime().toString());
        tripInfoMapper.insert(entity);
    }

    /**
     * 查询指定用户的全部行程记录。
     */
    public List<TripInfo> queryTrip(long userID) {
        List<TripInfoEntity> entities = tripInfoMapper.selectList(new QueryWrapper<TripInfoEntity>().eq("user_id", userID));
        List<TripInfo> trips = new ArrayList<>();
        for (TripInfoEntity entity : entities) {
            trips.add(DbCodec.toTripInfo(
                    entity.getTrainId(),
                    entity.getDepartureStation(),
                    entity.getArrivalStation(),
                    entity.getType(),
                    entity.getDuration(),
                    entity.getPrice(),
                    entity.getDepartureTime(),
                    entity.getArrivalTime()
            ));
        }
        return trips;
    }

    /**
     * 删除与给定行程完全匹配的一条订单记录。
     */
    public void removeTrip(long userID, TripInfo trip) {
        tripInfoMapper.delete(new QueryWrapper<TripInfoEntity>()
                .eq("user_id", userID)
                .eq("train_id", trip.getTrainID().toString())
                .eq("departure_station", trip.getDepartureStation().value())
                .eq("arrival_station", trip.getArrivalStation().value())
                .eq("type", trip.getType())
                .eq("departure_time", trip.getDepartureTime().toString()));
    }
}
