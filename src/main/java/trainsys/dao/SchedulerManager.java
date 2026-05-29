package trainsys.dao;

import trainsys.dao.entity.TrainSchedulerEntity;
import trainsys.dao.mapper.TrainSchedulerMapper;
import trainsys.dao.support.DbCodec;
import trainsys.util.FixedString;
import trainsys.util.TrainScheduler;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
/**
 * 车次调度持久化管理器。
 * 负责车次调度信息的存储、查询和删除。
 */
public class SchedulerManager {
    private final TrainSchedulerMapper trainSchedulerMapper;

    public SchedulerManager(TrainSchedulerMapper trainSchedulerMapper) {
        this.trainSchedulerMapper = trainSchedulerMapper;
    }

    /**
     * 新增或更新一条车次调度记录。
     */
    public void addScheduler(FixedString trainID, int seatNum, String startTime, int passingStationNumber, int[] stations, int[] duration, int[] price) {
        TrainSchedulerEntity entity = new TrainSchedulerEntity();
        entity.setTrainId(trainID.toString());
        entity.setSeatNum(seatNum);
        entity.setStartTime(startTime);
        entity.setPassingNum(passingStationNumber);
        entity.setStations(DbCodec.joinIntArray(stations, passingStationNumber));
        entity.setDuration(DbCodec.joinIntArray(duration, passingStationNumber - 1));
        entity.setPrice(DbCodec.joinIntArray(price, passingStationNumber - 1));
        if (trainSchedulerMapper.selectById(entity.getTrainId()) == null) {
            trainSchedulerMapper.insert(entity);
        } else {
            trainSchedulerMapper.updateById(entity);
        }
    }

    /**
     * 判断指定车次调度是否存在。
     */
    public boolean existScheduler(FixedString trainID) {
        return trainSchedulerMapper.selectById(trainID.toString()) != null;
    }

    /**
     * 查询单个车次调度，并转换为领域对象。
     */
    public TrainScheduler getScheduler(FixedString trainID) {
        TrainSchedulerEntity entity = trainSchedulerMapper.selectById(trainID.toString());
        if (entity == null) {
            return null;
        }
        return DbCodec.toTrainScheduler(entity.getTrainId(), entity.getSeatNum(), entity.getStartTime(), entity.getStations(), entity.getDuration(), entity.getPrice());
    }

    /**
     * 删除指定车次调度。
     */
    public void removeScheduler(FixedString trainID) {
        trainSchedulerMapper.deleteById(trainID.toString());
    }

    /**
     * 获取全部车次调度记录。
     */
    public List<TrainScheduler> getAllSchedulers() {
        List<TrainSchedulerEntity> entities = trainSchedulerMapper.selectList(new QueryWrapper<TrainSchedulerEntity>().orderByAsc("train_id"));
        List<TrainScheduler> schedulers = new ArrayList<>();
        for (TrainSchedulerEntity entity : entities) {
            schedulers.add(DbCodec.toTrainScheduler(entity.getTrainId(), entity.getSeatNum(), entity.getStartTime(), entity.getStations(), entity.getDuration(), entity.getPrice()));
        }
        return schedulers;
    }
}
