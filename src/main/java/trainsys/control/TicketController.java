package trainsys.control;

import trainsys.model.ApiResponse;
import trainsys.model.dto.BuyTicketRequest;
import trainsys.model.dto.RefundTicketRequest;
import trainsys.model.dto.TicketInfoDTO;
import trainsys.model.dto.TicketQueryRequest;
import trainsys.model.dto.TripInfoDTO;
import trainsys.service.TicketService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 票务接口入口。
 * 除站点查询外，所有操作都要求前端携带 Bearer Token 用于确认当前登录用户。
 */
@RestController
@RequestMapping("/api/ticket")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * 发布指定车次在某个发车时间的车票。
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
     * 停止销售指定车次在某个发车时间的车票。
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
     * 查询指定车次、出发站和发车时间下的余票数量。
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
     * 服务端会执行余票校验、排队和订单写入等完整流程，而不是简单插入记录。
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
     * 查询系统中已发布的车票列表，供管理员查看。
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
