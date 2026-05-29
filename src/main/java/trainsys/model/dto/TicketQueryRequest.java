package trainsys.model.dto;

import lombok.Data;

/**
 * 余票查询请求 DTO。
 */
@Data
public class TicketQueryRequest {
    private String trainId;
    private String departureTime;
    private String departureStation;
    private String arrivalStation;
}
