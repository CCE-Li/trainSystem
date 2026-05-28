package trainsys.control;

import trainsys.model.ApiResponse;
import trainsys.model.BuyTicketRequest;
import trainsys.model.RefundTicketRequest;
import trainsys.model.TicketInfoDTO;
import trainsys.model.TicketQueryRequest;
import trainsys.model.TripInfoDTO;
import trainsys.service.TicketService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 票务接口入口。
 * 所有接口都要求前端携带 Bearer Token，用于恢复当前登录用户后再执行业务。
 */
@RestController
@RequestMapping("/api/ticket")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * 管理员发售某个车次在指定发车时间的票。
     */
    @PostMapping("/release")
    public ApiResponse<String> releaseTicket(
            @RequestHeader(value = "Authorization", required = false) String sessionId,
            @RequestBody TicketQueryRequest request) {
        if (sessionId == null || !sessionId.startsWith("Bearer ")) {
            return ApiResponse.error(401, "未登录");
        }
        return ticketService.releaseTicket(sessionId.substring(7), request);
    }

    /**
     * 管理员停止某个车次在指定发车时间的售票。
     */
    @PostMapping("/expire")
    public ApiResponse<String> expireTicket(
            @RequestHeader(value = "Authorization", required = false) String sessionId,
            @RequestBody TicketQueryRequest request) {
        if (sessionId == null || !sessionId.startsWith("Bearer ")) {
            return ApiResponse.error(401, "未登录");
        }
        return ticketService.expireTicket(sessionId.substring(7), request);
    }

    /**
     * 查询指定车次、出发站和发车时间下的剩余票数。
     */
    @PostMapping("/remaining")
    public ApiResponse<Integer> queryRemainingTicket(
            @RequestHeader(value = "Authorization", required = false) String sessionId,
            @RequestBody TicketQueryRequest request) {
        if (sessionId == null || !sessionId.startsWith("Bearer ")) {
            return ApiResponse.error(401, "未登录");
        }
        return ticketService.queryRemainingTicket(sessionId.substring(7), request);
    }

    /**
     * 购票接口。
     * 这里会进入后端排队/余票判定逻辑，而不是单纯的数据库插入。
     */
    @PostMapping("/buy")
    public ApiResponse<String> buyTicket(
            @RequestHeader(value = "Authorization", required = false) String sessionId,
            @RequestBody BuyTicketRequest request) {
        if (sessionId == null || !sessionId.startsWith("Bearer ")) {
            return ApiResponse.error(401, "未登录");
        }
        return ticketService.buyTicket(sessionId.substring(7), request);
    }

    /**
     * 退票接口。
     */
    @PostMapping("/refund")
    public ApiResponse<String> refundTicket(
            @RequestHeader(value = "Authorization", required = false) String sessionId,
            @RequestBody RefundTicketRequest request) {
        if (sessionId == null || !sessionId.startsWith("Bearer ")) {
            return ApiResponse.error(401, "未登录");
        }
        return ticketService.refundTicket(sessionId.substring(7), request);
    }

    /**
     * 查询当前登录用户的订单列表。
     */
    @GetMapping("/orders")
    public ApiResponse<List<TripInfoDTO>> queryMyOrders(
            @RequestHeader(value = "Authorization", required = false) String sessionId) {
        if (sessionId == null || !sessionId.startsWith("Bearer ")) {
            return ApiResponse.error(401, "未登录");
        }
        return ticketService.queryMyOrders(sessionId.substring(7));
    }

    /**
     * 查询当前系统中已经发售的票务列表，供管理员查看。
     */
    @GetMapping("/list")
    public ApiResponse<List<TicketInfoDTO>> getTicketList(
            @RequestHeader(value = "Authorization", required = false) String sessionId) {
        if (sessionId == null || !sessionId.startsWith("Bearer ")) {
            return ApiResponse.error(401, "未登录");
        }
        return ticketService.getTicketList(sessionId.substring(7));
    }
}
