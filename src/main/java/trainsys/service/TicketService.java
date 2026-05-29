package trainsys.service;

import trainsys.config.StaticConfig;
import trainsys.dao.RouteDao;
import trainsys.dao.TicketDao;
import trainsys.dao.TrainDao;
import trainsys.model.ApiResponse;
import trainsys.model.TripInfo;
import trainsys.model.UserInfo;
import trainsys.model.dto.BuyTicketRequest;
import trainsys.model.dto.RefundTicketRequest;
import trainsys.model.dto.TicketInfoDTO;
import trainsys.model.dto.TicketQueryRequest;
import trainsys.model.dto.TripInfoDTO;
import trainsys.util.TrainScheduler;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 票务服务。
 * 负责车票发布、停售、余票查询、购票、退票和订单查询等核心票务流程。
 */
@Service
public class TicketService {

    private final TicketDao ticketDao;
    private final TrainDao trainDao;
    private final RouteDao routeDao;
    private final UserService userService;

    public TicketService(TicketDao ticketDao, TrainDao trainDao, RouteDao routeDao, UserService userService) {
        this.ticketDao = ticketDao;
        this.trainDao = trainDao;
        this.routeDao = routeDao;
        this.userService = userService;
    }

    public ApiResponse<String> releaseTicket(String sessionId, TicketQueryRequest request) {
        UserInfo user = userService.getCurrentUser(sessionId);
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }
        userService.bindCurrentUser(sessionId);

        try {
            TrainScheduler scheduler = trainDao.getScheduler(request.getTrainId());
            if (scheduler == null) {
                return ApiResponse.error("车次不存在");
            }
            ticketDao.releaseTicket(scheduler, request);
            return ApiResponse.success("车票发售成功", null);
        } catch (Exception e) {
            return ApiResponse.error("车票发售失败: " + e.getMessage());
        }
    }

    public ApiResponse<String> expireTicket(String sessionId, TicketQueryRequest request) {
        if (userService.getCurrentUser(sessionId) == null) {
            return ApiResponse.error(401, "未登录");
        }
        userService.bindCurrentUser(sessionId);

        try {
            ticketDao.expireTicket(request);
            return ApiResponse.success("车票停售成功", null);
        } catch (Exception e) {
            return ApiResponse.error("车票停售失败: " + e.getMessage());
        }
    }

    public ApiResponse<Integer> queryRemainingTicket(String sessionId, TicketQueryRequest request) {
        if (userService.getCurrentUser(sessionId) == null) {
            return ApiResponse.error(401, "未登录");
        }
        userService.bindCurrentUser(sessionId);

        try {
            Integer departureStationId = routeDao.stationNameToId(request.getDepartureStation());
            if (departureStationId == null) {
                return ApiResponse.error("站点不存在: " + request.getDepartureStation());
            }

            Integer arrivalStationId = routeDao.stationNameToId(request.getArrivalStation());
            if (arrivalStationId == null) {
                return ApiResponse.error("站点不存在: " + request.getArrivalStation());
            }

            return ApiResponse.success(
                    ticketDao.queryRemainingTicket(
                            request.getTrainId(),
                            request.getDepartureTime(),
                            departureStationId,
                            arrivalStationId
                    )
            );
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("查询余票失败: " + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("查询余票失败: " + e.getMessage());
        }
    }

    public ApiResponse<String> buyTicket(String sessionId, BuyTicketRequest request) {
        if (userService.getCurrentUser(sessionId) == null) {
            return ApiResponse.error(401, "未登录");
        }
        userService.bindCurrentUser(sessionId);

        try {
            int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
            if (quantity <= 0) {
                return ApiResponse.error("购票数量必须大于 0");
            }

            Integer departureStationId = routeDao.stationNameToId(request.getDepartureStation());
            if (departureStationId == null) {
                return ApiResponse.error("站点不存在: " + request.getDepartureStation());
            }

            Integer arrivalStationId = routeDao.stationNameToId(request.getArrivalStation());
            if (arrivalStationId == null) {
                return ApiResponse.error("站点不存在: " + request.getArrivalStation());
            }

            TrainScheduler scheduler = trainDao.getScheduler(request.getTrainId());
            if (scheduler == null) {
                return ApiResponse.error("车次不存在: " + request.getTrainId());
            }

            int remaining = ticketDao.queryRemainingTicket(
                    request.getTrainId(),
                    request.getDepartureTime(),
                    departureStationId,
                    arrivalStationId
            );
            if (remaining < 0) {
                return ApiResponse.error("该车次该区间的车票尚未发售，请先发布车票");
            }
            if (remaining < quantity) {
                return ApiResponse.error("余票不足，无法购票");
            }

            ticketDao.orderTicket(request, departureStationId, arrivalStationId, quantity);
            return ApiResponse.success("购票成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("购票失败: " + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("购票失败: " + e.getMessage());
        }
    }

    public ApiResponse<String> refundTicket(String sessionId, RefundTicketRequest request) {
        UserInfo user = userService.getCurrentUser(sessionId);
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }
        userService.bindCurrentUser(sessionId);

        try {
            int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
            if (quantity <= 0) {
                return ApiResponse.error("退票数量必须大于 0");
            }

            Integer departureStationId = routeDao.stationNameToId(request.getDepartureStation());
            if (departureStationId == null) {
                return ApiResponse.error("站点不存在: " + request.getDepartureStation());
            }

            Integer arrivalStationId = routeDao.stationNameToId(request.getArrivalStation());
            if (arrivalStationId == null) {
                return ApiResponse.error("站点不存在: " + request.getArrivalStation());
            }

            int ownedCount = 0;
            for (TripInfo trip : ticketDao.queryUserTrips(user.getUserID().value())) {
                if (trip.getTrainID().toString().equals(request.getTrainId())
                        && trip.getDepartureStation().value() == departureStationId
                        && trip.getArrivalStation().value() == arrivalStationId
                        && trip.getDepartureTime().toString().equals(request.getDepartureTime())
                        && trip.getType() > 0) {
                    ownedCount += trip.getType();
                }
            }

            if (ownedCount <= 0) {
                return ApiResponse.error("您没有该订单，无法退票");
            }
            if (ownedCount < quantity) {
                return ApiResponse.error("可退票数量不足");
            }

            if (trainDao.getScheduler(request.getTrainId()) == null) {
                return ApiResponse.error("车次不存在: " + request.getTrainId());
            }

            ticketDao.refundTicket(request, departureStationId, arrivalStationId, quantity);
            return ApiResponse.success("退票成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("退票失败: " + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("退票失败: " + e.getMessage());
        }
    }

    public ApiResponse<List<TripInfoDTO>> queryMyOrders(String sessionId) {
        UserInfo user = userService.getCurrentUser(sessionId);
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }
        userService.bindCurrentUser(sessionId);

        try {
            Map<String, TripInfoDTO> grouped = new LinkedHashMap<>();
            for (TripInfo trip : ticketDao.queryUserTrips(user.getUserID().value())) {
                String key = trip.getTrainID() + "|" + trip.getDepartureStation().value() + "|" + trip.getArrivalStation().value()
                        + "|" + trip.getDepartureTime() + "|" + trip.getArrivalTime();
                TripInfoDTO dto = grouped.get(key);
                if (dto == null) {
                    dto = new TripInfoDTO();
                    dto.setTrainId(trip.getTrainID().toString());
                    dto.setDepartureStation(routeDao.stationIdToName(trip.getDepartureStation().value()));
                    dto.setArrivalStation(routeDao.stationIdToName(trip.getArrivalStation().value()));
                    dto.setTicketNumber(0);
                    dto.setRefundableCount(0);
                    dto.setDuration(trip.getDuration());
                    dto.setPrice(trip.getPrice());
                    dto.setDepartureTime(trip.getDepartureTime().toString());
                    dto.setArrivalTime(trip.getArrivalTime().toString());
                    grouped.put(key, dto);
                }
                dto.setTicketNumber((dto.getTicketNumber() == null ? 0 : dto.getTicketNumber()) + trip.getTicketNumber());
                if (trip.getTicketNumber() > 0) {
                    dto.setRefundableCount((dto.getRefundableCount() == null ? 0 : dto.getRefundableCount()) + trip.getTicketNumber());
                }
            }
            return ApiResponse.success(new ArrayList<>(grouped.values()));
        } catch (Exception e) {
            return ApiResponse.error("查询订单失败: " + e.getMessage());
        }
    }

    public ApiResponse<List<TicketInfoDTO>> getTicketList(String sessionId) {
        UserInfo user = userService.getCurrentUser(sessionId);
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }
        userService.bindCurrentUser(sessionId);

        try {
            List<Object[]> tickets = user.getPrivilege() >= StaticConfig.ADMIN_PRIVILEGE
                    ? ticketDao.getAllTickets()
                    : ticketDao.getAllReleasedTickets();

            List<TicketInfoDTO> dtos = new ArrayList<>();
            for (Object[] ticket : tickets) {
                TicketInfoDTO dto = new TicketInfoDTO();
                dto.setTrainId((String) ticket[0]);
                dto.setDepartureTime((String) ticket[1]);
                dto.setDepartureStation(routeDao.stationIdToName((Integer) ticket[2]));
                dto.setArrivalStation(routeDao.stationIdToName((Integer) ticket[3]));
                dto.setSeatNum((Integer) ticket[4]);
                dto.setPrice((Integer) ticket[5]);
                dto.setDuration((Integer) ticket[6]);
                dtos.add(dto);
            }
            return ApiResponse.success(dtos);
        } catch (Exception e) {
            return ApiResponse.error("获取车票列表失败: " + e.getMessage());
        }
    }
}
