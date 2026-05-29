package trainsys.model;

import trainsys.util.Time;
import trainsys.util.Types.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 购票排队信息。
 * 用于购票和退票请求在等待队列中的传递。
 */
@Getter
@Setter
public class PurchaseInfo implements Comparable<PurchaseInfo> {
    private UserID userID;
    private TrainID trainID;
    private Time departureTime;
    private StationID departureStation;
    private StationID arrivalStation;
    private int type;

    public PurchaseInfo() {
    }

    public PurchaseInfo(UserID userID, TrainID trainID, Time departureTime,
                        StationID departureStation, StationID arrivalStation, int type) {
        this.userID = userID;
        this.trainID = trainID;
        this.departureTime = departureTime;
        this.departureStation = departureStation;
        this.arrivalStation = arrivalStation;
        this.type = type;
    }

    public boolean isOrdering() {
        return type > 0;
    }

    public boolean isRefunding() {
        return type < 0;
    }

    @Override
    public int compareTo(PurchaseInfo other) {
        int cmp = Long.compare(this.userID.value(), other.userID.value());
        if (cmp != 0) {
            return cmp;
        }

        cmp = this.trainID.compareTo(other.trainID);
        if (cmp != 0) {
            return cmp;
        }

        cmp = this.departureTime.compareTo(other.departureTime);
        if (cmp != 0) {
            return cmp;
        }

        cmp = Integer.compare(this.departureStation.value(), other.departureStation.value());
        if (cmp != 0) {
            return cmp;
        }

        cmp = Integer.compare(this.arrivalStation.value(), other.arrivalStation.value());
        if (cmp != 0) {
            return cmp;
        }

        return Integer.compare(this.type, other.type);
    }

    @Deprecated
    public Time getDate() {
        return departureTime;
    }

    @Deprecated
    public void setDate(Time time) {
        this.departureTime = time;
    }
}
