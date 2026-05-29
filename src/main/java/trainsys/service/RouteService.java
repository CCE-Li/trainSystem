package trainsys.service;

import trainsys.dao.RouteDao;
import trainsys.model.ApiResponse;
import trainsys.model.RouteQueryRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 路线服务。
 * 负责站点名称解析以及路径、连通性相关查询。
 */
@Service
public class RouteService {

    private final RouteDao routeDao;

    public RouteService(RouteDao routeDao) {
        this.routeDao = routeDao;
    }

    /**
     * 查询两个站点之间的全部可行路线。
     */
    public ApiResponse<String> findAllRoute(RouteQueryRequest request) {
        try {
            Integer depId = routeDao.stationNameToId(request.getDepartureStation());
            Integer arrId = routeDao.stationNameToId(request.getArrivalStation());
            if (depId == null || arrId == null) {
                return ApiResponse.error("站点不存在");
            }
            return ApiResponse.success(routeDao.findAllRoute(depId, arrId));
        } catch (Exception e) {
            return ApiResponse.error("查询路线失败: " + e.getMessage());
        }
    }

    /**
     * 查询两个站点之间的最优路线。
     * 当偏好为 price 时按价格优化，否则按时间优化。
     */
    public ApiResponse<String> findBestRoute(RouteQueryRequest request) {
        try {
            Integer depId = routeDao.stationNameToId(request.getDepartureStation());
            Integer arrId = routeDao.stationNameToId(request.getArrivalStation());
            if (depId == null || arrId == null) {
                return ApiResponse.error("站点不存在");
            }
            int preference = "price".equals(request.getPreference()) ? 0 : 1;
            return ApiResponse.success(routeDao.findBestRoute(depId, arrId, preference));
        } catch (Exception e) {
            return ApiResponse.error("查询最优路线失败: " + e.getMessage());
        }
    }

    /**
     * 查询两个站点之间是否连通。
     */
    public ApiResponse<Boolean> queryAccessibility(RouteQueryRequest request) {
        try {
            Integer depId = routeDao.stationNameToId(request.getDepartureStation());
            Integer arrId = routeDao.stationNameToId(request.getArrivalStation());
            if (depId == null || arrId == null) {
                return ApiResponse.error("站点不存在");
            }
            return ApiResponse.success(routeDao.queryAccessibility(depId, arrId));
        } catch (Exception e) {
            return ApiResponse.error("查询连通性失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统中的全部站点名称。
     */
    public ApiResponse<List<String>> getAllStations() {
        try {
            return ApiResponse.success(routeDao.getAllStations());
        } catch (Exception e) {
            return ApiResponse.error("获取站点列表失败: " + e.getMessage());
        }
    }
}
