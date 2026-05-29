package trainsys.dao;

import trainsys.util.Types.StationID;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
/**
 * 路线数据访问层。
 * 负责站点名称映射以及底层路径查询能力的封装。
 */
public class RouteDao {

    private final SystemContextDao systemContextDao;

    public RouteDao(SystemContextDao systemContextDao) {
        this.systemContextDao = systemContextDao;
    }

    /**
     * 将站点名称转换为站点 ID。
     */
    public Integer stationNameToId(String stationName) {
        return systemContextDao.getStationManager().nameToID(stationName);
    }

    /**
     * 将站点 ID 转换为站点名称。
     */
    public String stationIdToName(int stationId) {
        return systemContextDao.getStationManager().idToName(stationId);
    }

    /**
     * 获取系统中的全部站点名称。
     */
    public List<String> getAllStations() {
        return new ArrayList<>(systemContextDao.getStationManager().getAllStationNames());
    }

    /**
     * 查询两个站点之间的全部可行路线。
     */
    public String findAllRoute(int depId, int arrId) {
        return systemContextDao.getTrainSystem().findAllRoute(new StationID(depId), new StationID(arrId));
    }

    /**
     * 查询两个站点之间的最优路线。
     */
    public String findBestRoute(int depId, int arrId, int preference) {
        return systemContextDao.getTrainSystem().findBestRoute(new StationID(depId), new StationID(arrId), preference);
    }

    /**
     * 查询两个站点之间是否连通。
     */
    public boolean queryAccessibility(int depId, int arrId) {
        return systemContextDao.getTrainSystem().getRailwayGraph().checkStationAccessibility(depId, arrId);
    }
}
