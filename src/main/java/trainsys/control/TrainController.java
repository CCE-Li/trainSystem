package trainsys.control;

import trainsys.model.dto.AddTrainRequest;
import trainsys.model.ApiResponse;
import trainsys.model.dto.TrainSchedulerDTO;
import trainsys.service.TrainService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 车次管理接口。
 * 提供新增车次、查询单个车次以及查看全部车次列表的能力。
 */
@RestController
@RequestMapping("/api/train")
public class TrainController {

    private final TrainService trainService;

    public TrainController(TrainService trainService) {
        this.trainService = trainService;
    }

    /**
     * 新增车次。
     * 权限校验在服务层完成，仅管理员允许执行该操作。
     */
    @PostMapping("/add")
    public ApiResponse<String> addTrain(
            @RequestHeader(value = "Authorization", required = false) String sessionId,
            @RequestBody AddTrainRequest request) {
        if (sessionId == null || !sessionId.startsWith("Bearer ")) {
            return ApiResponse.error(401, "未登录");
        }
        return trainService.addTrain(sessionId.substring(7), request);
    }

    /**
     * 按车次 ID 查询车次详情。
     */
    @GetMapping("/query/{trainId}")
    public ApiResponse<TrainSchedulerDTO> queryTrain(
            @RequestHeader(value = "Authorization", required = false) String sessionId,
            @PathVariable("trainId") String trainId) {
        if (sessionId == null || !sessionId.startsWith("Bearer ")) {
            return ApiResponse.error(401, "未登录");
        }
        return trainService.queryTrain(sessionId.substring(7), trainId);
    }

    /**
     * 获取全部车次列表，供前端展示总览页面。
     */
    @GetMapping("/list")
    public ApiResponse<List<TrainSchedulerDTO>> getAllTrains(
            @RequestHeader(value = "Authorization", required = false) String sessionId) {
        if (sessionId == null || !sessionId.startsWith("Bearer ")) {
            return ApiResponse.error(401, "未登录");
        }
        return trainService.getAllTrainSchedulers(sessionId.substring(7));
    }
}
