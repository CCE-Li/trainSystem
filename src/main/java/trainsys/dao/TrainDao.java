package trainsys.dao;

import trainsys.model.dto.AddTrainRequest;
import trainsys.util.FixedString;
import trainsys.util.TrainScheduler;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * 车次数据访问层。
 * 负责车次调度信息的写入和读取。
 */
public class TrainDao {

    private final SystemContextDao systemContextDao;

    public TrainDao(SystemContextDao systemContextDao) {
        this.systemContextDao = systemContextDao;
    }

    /**
     * 将新增车次请求转换为底层调度数据并写入系统。
     */
    public void addTrain(AddTrainRequest request, int[] stationIds, int[] durations, int[] prices) {
        systemContextDao.getTrainSystem().addTrainScheduler(
                new FixedString(request.getTrainId()),
                request.getSeatNum(),
                request.getStartTime().trim(),
                request.getStations().size(),
                stationIds,
                durations,
                prices
        );
    }

    /**
     * 按车次 ID 查询调度对象。
     */
    public TrainScheduler getScheduler(String trainId) {
        return systemContextDao.getTrainSystem().getSchedulerManager().getScheduler(new FixedString(trainId.trim()));
    }

    /**
     * 获取系统中的全部车次调度信息。
     */
    public List<TrainScheduler> getAllSchedulers() {
        return systemContextDao.getTrainSystem().getSchedulerManager().getAllSchedulers();
    }
}
