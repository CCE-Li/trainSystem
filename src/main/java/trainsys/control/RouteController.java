package trainsys.control;

import trainsys.model.ApiResponse;
import trainsys.model.dto.RouteQueryRequest;
import trainsys.service.RouteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 路线查询接口。
 * 提供可达性校验、全路径查询、最优路径查询以及站点列表查询能力。
 */
@RestController
@RequestMapping("/api/route")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    /**
     * 查询两个站点之间的全部可行路线。
     */
    @PostMapping("/findAll")
    public ApiResponse<String> findAllRoute(@RequestBody RouteQueryRequest request) {
        return routeService.findAllRoute(request);
    }

    /**
     * 查询两个站点之间的最优路线。
     */
    @PostMapping("/best")
    public ApiResponse<String> findBestRoute(@RequestBody RouteQueryRequest request) {
        return routeService.findBestRoute(request);
    }

    /**
     * 查询两个站点之间是否连通。
     */
    @PostMapping("/accessibility")
    public ApiResponse<Boolean> queryAccessibility(@RequestBody RouteQueryRequest request) {
        return routeService.queryAccessibility(request);
    }

    /**
     * 获取系统中的全部站点名称列表。
     */
    @GetMapping("/stations")
    public ApiResponse<List<String>> getAllStations() {
        return routeService.getAllStations();
    }
}
