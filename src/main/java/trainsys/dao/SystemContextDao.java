package trainsys.dao;

import trainsys.util.TrainSystem;
import org.springframework.stereotype.Component;

@Component
/**
 * 系统上下文访问层。
 * 统一暴露底层 TrainSystem 及其常用管理器，供其他 DAO 复用。
 */
public class SystemContextDao {

    private final TrainSystem trainSystem;
    private final StationManager stationManager;

    public SystemContextDao(TrainSystem trainSystem) {
        this.trainSystem = trainSystem;
        this.stationManager = trainSystem.getStationManager();
    }

    /**
     * 获取全局唯一的 TrainSystem 实例。
     */
    public TrainSystem getTrainSystem() {
        return trainSystem;
    }

    /**
     * 获取站点管理器。
     */
    public StationManager getStationManager() {
        return stationManager;
    }
}
