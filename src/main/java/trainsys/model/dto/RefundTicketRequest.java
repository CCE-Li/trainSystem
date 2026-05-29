package trainsys.model.dto;

import lombok.Data;

/**
 * 退票请求 DTO。
 */
@Data
public class RefundTicketRequest {
    private String trainId;
    private String departureTime;
    private String departureStation;
    private String arrivalStation;
    private Integer quantity;
}
