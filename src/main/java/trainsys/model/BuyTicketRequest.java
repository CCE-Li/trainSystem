package trainsys.model;

import lombok.Data;

/**
 * 购票请求 DTO。
 */
@Data
public class BuyTicketRequest {
    private String trainId;
    private String departureTime;
    private String departureStation;
    private String arrivalStation;
    private Integer quantity;
}
