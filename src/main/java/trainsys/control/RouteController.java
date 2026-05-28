package trainsys.control;

import trainsys.model.ApiResponse;
import trainsys.model.RouteQueryRequest;
import trainsys.service.RouteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/route")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/findAll")
    public ApiResponse<String> findAllRoute(@RequestBody RouteQueryRequest request) {
        return routeService.findAllRoute(request);
    }

    @PostMapping("/best")
    public ApiResponse<String> findBestRoute(@RequestBody RouteQueryRequest request) {
        return routeService.findBestRoute(request);
    }

    @PostMapping("/accessibility")
    public ApiResponse<Boolean> queryAccessibility(@RequestBody RouteQueryRequest request) {
        return routeService.queryAccessibility(request);
    }

    @GetMapping("/stations")
    public ApiResponse<List<String>> getAllStations() {
        return routeService.getAllStations();
    }
}
