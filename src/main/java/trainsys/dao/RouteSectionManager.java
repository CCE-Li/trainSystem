package trainsys.dao;

import trainsys.dao.entity.RouteSectionEntity;
import trainsys.dao.entity.StationComponentEntity;
import trainsys.dao.mapper.RouteSectionMapper;
import trainsys.dao.mapper.StationComponentMapper;
import trainsys.util.Types.TrainID;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
/**
 * 路线区段持久化管理器。
 * 负责列车区段信息和站点连通分量信息的存取。
 */
public class RouteSectionManager {
    private final RouteSectionMapper routeSectionMapper;
    private final StationComponentMapper stationComponentMapper;

    public RouteSectionManager(RouteSectionMapper routeSectionMapper, StationComponentMapper stationComponentMapper) {
        this.routeSectionMapper = routeSectionMapper;
        this.stationComponentMapper = stationComponentMapper;
    }

    /**
     * 路线区段的轻量数据载体，供上层重建图结构时使用。
     */
    public static class RouteSectionData {
        public final TrainID trainID;
        public final int departureID;
        public final int arrivalID;
        public final int price;
        public final int duration;

        public RouteSectionData(TrainID trainID, int departureID, int arrivalID, int price, int duration) {
            this.trainID = trainID;
            this.departureID = departureID;
            this.arrivalID = arrivalID;
            this.price = price;
            this.duration = duration;
        }
    }

    /**
     * 保存一条列车运行区段记录。
     */
    public void saveSection(TrainID trainID, int departureID, int arrivalID, int duration, int price) {
        RouteSectionEntity entity = new RouteSectionEntity();
        entity.setTrainId(trainID.toString());
        entity.setDepartureId(departureID);
        entity.setArrivalId(arrivalID);
        entity.setDuration(duration);
        entity.setPrice(price);
        routeSectionMapper.insert(entity);
    }

    /**
     * 加载全部列车区段记录。
     */
    public List<RouteSectionData> loadAllSections() {
        List<RouteSectionEntity> entities = routeSectionMapper.selectList(new QueryWrapper<>());
        List<RouteSectionData> result = new ArrayList<>();
        for (RouteSectionEntity entity : entities) {
            result.add(new RouteSectionData(
                    new TrainID(entity.getTrainId()),
                    entity.getDepartureId(),
                    entity.getArrivalId(),
                    entity.getPrice(),
                    entity.getDuration()
            ));
        }
        return result;
    }

    /**
     * 保存站点所属的连通分量编号。
     */
    public void saveStationComponent(int stationId, int componentId) {
        StationComponentEntity entity = new StationComponentEntity();
        entity.setStationId(stationId);
        entity.setComponentId(componentId);
        if (stationComponentMapper.selectById(stationId) == null) {
            stationComponentMapper.insert(entity);
        } else {
            stationComponentMapper.updateById(entity);
        }
    }

    /**
     * 清空全部站点连通分量记录。
     */
    public void clearStationComponents() {
        stationComponentMapper.delete(new QueryWrapper<>());
    }

    /**
     * 预留关闭钩子，当前无需额外清理。
     */
    public void close() {
    }
}
