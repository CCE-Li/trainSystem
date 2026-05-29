package trainsys.util;

import trainsys.config.StaticConfig;
import trainsys.dao.RouteSectionManager;
import trainsys.dao.SchedulerManager;
import trainsys.dao.StationManager;
import trainsys.dao.TicketManager;
import trainsys.dao.TripManager;
import trainsys.dao.UserManager;
import trainsys.model.PurchaseInfo;
import trainsys.model.TripInfo;
import trainsys.model.UserInfo;
import trainsys.util.Types.StationID;
import trainsys.util.Types.TrainID;
import trainsys.util.Types.UserID;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Data
@Slf4j
@Component
public class TrainSystem {

    private UserInfo currentUser;
    private final UserManager userManager;
    private final RouteSectionManager routeSectionManager;
    private final RailwayGraph railwayGraph;
    private final SchedulerManager schedulerManager;
    private final TicketManager ticketManager;
    private final PrioritizedWaitingList waitingList;
    private final TripManager tripManager;
    private final StationManager stationManager;

    public TrainSystem(UserManager userManager,
                       RouteSectionManager routeSectionManager,
                       SchedulerManager schedulerManager,
                       TicketManager ticketManager,
                       TripManager tripManager,
                       StationManager stationManager) {
        this.stationManager = stationManager;
        this.userManager = userManager;
        this.routeSectionManager = routeSectionManager;
        this.railwayGraph = new RailwayGraph(routeSectionManager);
        this.schedulerManager = schedulerManager;
        this.ticketManager = ticketManager;
        this.waitingList = new PrioritizedWaitingList();
        this.tripManager = tripManager;

        railwayGraph.loadFromDB();
        log.info("Loaded route sections from database");

        UserID adminID = new UserID(0L);
        if (!userManager.existUser(adminID)) {
            userManager.insertUser(adminID, "admin", "admin", StaticConfig.ADMIN_PRIVILEGE);
        }
        currentUser = new UserInfo(new UserID(-1L), "", "", 0);
    }

    public void addTrainScheduler(FixedString trainID, int seatNum, String startTime, int passingStationNumber,
                                  int[] stations, int[] duration, int[] price) {
        if (currentUser == null || currentUser.getPrivilege() < StaticConfig.ADMIN_PRIVILEGE) {
            log.warn("Permission denied for addTrainScheduler, user={}", currentUser != null ? currentUser.getUserID().value() : "null");
            throw new RuntimeException("Permission denied");
        }

        for (int i = 0; i < passingStationNumber; i++) {
            if (stations[i] < 0 || stations[i] >= StaticConfig.MAX_STATIONID) {
                throw new RuntimeException("Invalid station ID: " + stations[i]);
            }
        }
        if (duration.length != passingStationNumber - 1) {
            throw new RuntimeException("Invalid duration array length");
        }
        if (price.length != passingStationNumber - 1) {
            throw new RuntimeException("Invalid price array length");
        }
        if (schedulerManager.existScheduler(trainID)) {
            throw new RuntimeException("Train ID already exists: " + trainID);
        }

        schedulerManager.addScheduler(trainID, seatNum, startTime, passingStationNumber, stations, duration, price);
        for (int i = 0; i + 1 < passingStationNumber; i++) {
            railwayGraph.addRoute(stations[i], stations[i + 1], duration[i], price[i], new TrainID(trainID.toString()));
        }
        log.info("Train added successfully: {}", trainID);
    }

    public void queryTrainScheduler(FixedString trainID) {
        if (currentUser == null || currentUser.getPrivilege() < StaticConfig.ADMIN_PRIVILEGE) {
            System.out.println("Permission denied.");
            return;
        }
        TrainScheduler relatedInfo = schedulerManager.getScheduler(trainID);
        if (relatedInfo == null) {
            System.out.println("Train not found.");
            return;
        }
        System.out.println(relatedInfo);
    }

    public void releaseTicket(TrainScheduler scheduler, Time departureTime) {
        if (currentUser == null || currentUser.getPrivilege() < StaticConfig.ADMIN_PRIVILEGE) {
            System.out.println("Permission denied.");
            return;
        }
        if (scheduler == null) {
            System.out.println("Train not found. Please add train first.");
            return;
        }
        ticketManager.releaseTicket(scheduler, departureTime);
        System.out.println("Ticket released.");
    }

    public void expireTicket(FixedString trainID, Time departureTime) {
        if (currentUser == null || currentUser.getPrivilege() < StaticConfig.ADMIN_PRIVILEGE) {
            System.out.println("Permission denied.");
            return;
        }
        ticketManager.expireTicket(trainID, departureTime);
        System.out.println("Ticket expired.");
    }

    public int queryRemainingTicket(FixedString trainID, Time departureTime, StationID departureStation, StationID arrivalStation) {
        TrainScheduler scheduler = schedulerManager.getScheduler(trainID);
        SegmentRange range = resolveSegmentRange(scheduler, departureStation, arrivalStation);
        Time baseTime = resolveBaseDepartureTime(scheduler, departureTime, range.departureIndex);

        int remaining = Integer.MAX_VALUE;
        for (int i = range.departureIndex; i < range.arrivalIndex; i++) {
            int seat = ticketManager.querySeat(
                    trainID,
                    scheduler.getDepartureTimeAt(i, baseTime),
                    scheduler.getStation(i).value()
            );
            if (seat < 0) {
                return -1;
            }
            remaining = Math.min(remaining, seat);
        }
        return remaining == Integer.MAX_VALUE ? -1 : remaining;
    }

    public int queryRemainingTicket(FixedString trainID, Time departureTime, StationID departureStation) {
        TrainScheduler scheduler = schedulerManager.getScheduler(trainID);
        int departureIndex = resolveStationIndex(scheduler, departureStation);
        if (departureIndex + 1 >= scheduler.getPassingStationNum()) {
            throw new IllegalArgumentException("Departure station must not be the terminal station");
        }
        return queryRemainingTicket(trainID, departureTime, departureStation, scheduler.getStation(departureIndex + 1));
    }

    private boolean trySatisfyOrder() {
        if (waitingList.isEmpty()) {
            return false;
        }
        PurchaseInfo purchaseInfo = waitingList.getFrontPurchaseInfo();
        waitingList.removeHeadFromWaitingList();

        System.out.println("Processing request from User " + purchaseInfo.getUserID().value());

        try {
            TrainScheduler scheduler = schedulerManager.getScheduler(new FixedString(purchaseInfo.getTrainID().toString()));
            SegmentRange range = resolveSegmentRange(scheduler, purchaseInfo.getDepartureStation(), purchaseInfo.getArrivalStation());
            int quantity = Math.abs(purchaseInfo.getType());

            if (purchaseInfo.isOrdering()) {
                int remainingTickets = queryRemainingTicket(
                        new TrainID(purchaseInfo.getTrainID().toString()),
                        purchaseInfo.getDepartureTime(),
                        purchaseInfo.getDepartureStation(),
                        purchaseInfo.getArrivalStation()
                );
                if (remainingTickets < quantity) {
                    System.out.println("No enough tickets or scheduler not exists. Order failed.");
                    return false;
                }

                updateInventoryForRange(scheduler, purchaseInfo.getDepartureTime(), range, -quantity);
                tripManager.addTrip(purchaseInfo.getUserID().value(), buildTripInfo(scheduler, purchaseInfo, range, quantity));
                System.out.println("Order succeeded.");
                return true;
            }

            List<TripInfo> userTrips = ticketTripMatches(purchaseInfo);
            if (userTrips.isEmpty()) {
                log.warn("Refund failed: order not found for user {}", purchaseInfo.getUserID().value());
                return false;
            }

            consumeTripsForRefund(purchaseInfo.getUserID().value(), userTrips, quantity);
            updateInventoryForRange(scheduler, purchaseInfo.getDepartureTime(), range, quantity);
            System.out.println("Refund succeeded.");
            return true;
        } catch (Exception e) {
            log.error("Order handling failed", e);
            System.out.println("Order handling failed: " + e.getMessage());
            return false;
        }
    }

    public void queryMyTicket() {
        while (waitingList.isBusy()) {
            trySatisfyOrder();
        }
        List<TripInfo> tripInfo = tripManager.queryTrip(currentUser.getUserID().value());
        for (TripInfo t : tripInfo) {
            System.out.println(t);
        }
    }

    public void orderTicket(FixedString trainID, Time departureTime, StationID departureStation, StationID arrivalStation, int quantity) {
        waitingList.addToWaitingList(new PurchaseInfo(
                currentUser.getUserID(),
                new TrainID(trainID.toString()),
                departureTime,
                departureStation,
                arrivalStation,
                quantity));
        System.out.println("Ordering request has added to waiting list.");
        while (trySatisfyOrder()) {
        }
    }

    public void orderTicket(FixedString trainID, Time departureTime, StationID departureStation, int quantity) {
        TrainScheduler scheduler = schedulerManager.getScheduler(trainID);
        int departureIndex = resolveStationIndex(scheduler, departureStation);
        orderTicket(trainID, departureTime, departureStation, scheduler.getStation(departureIndex + 1), quantity);
    }

    public void orderTicket(FixedString trainID, Time departureTime, StationID departureStation) {
        orderTicket(trainID, departureTime, departureStation, 1);
    }

    public void refundTicket(FixedString trainID, Time departureTime, StationID departureStation, StationID arrivalStation, int quantity) {
        while (waitingList.isBusy()) {
            trySatisfyOrder();
        }
        waitingList.addToWaitingList(new PurchaseInfo(
                currentUser.getUserID(),
                new TrainID(trainID.toString()),
                departureTime,
                departureStation,
                arrivalStation,
                -quantity));
        System.out.println("Refunding request has added to waiting list.");
        while (trySatisfyOrder()) {
        }
    }

    public void refundTicket(FixedString trainID, Time departureTime, StationID departureStation, int quantity) {
        TrainScheduler scheduler = schedulerManager.getScheduler(trainID);
        int departureIndex = resolveStationIndex(scheduler, departureStation);
        refundTicket(trainID, departureTime, departureStation, scheduler.getStation(departureIndex + 1), quantity);
    }

    public void refundTicket(FixedString trainID, Time departureTime, StationID departureStation) {
        refundTicket(trainID, departureTime, departureStation, 1);
    }

    public String findAllRoute(StationID departureID, StationID arrivalID) {
        return railwayGraph.displayRoute(departureID.value(), arrivalID.value());
    }

    public String findBestRoute(StationID departureID, StationID arrivalID, int preference) {
        return railwayGraph.shortestPath(departureID.value(), arrivalID.value(), preference);
    }

    public void login(String account, String password) {
        if (currentUser != null && currentUser.getUserID().value() != -1) {
            System.out.println("Only one user can login in at the same time.");
            return;
        }

        UserInfo userInfo = findUserByAccount(account);
        if (userInfo == null) {
            System.out.println("User not found. Login failed.");
            return;
        }

        if (!PasswordHasher.matches(password, userInfo.getPassword())) {
            System.out.println("Wrong password. Login failed.");
            return;
        }
        if (PasswordHasher.needsUpgrade(userInfo.getPassword())) {
            userManager.modifyUserPassword(userInfo.getUserID(), password);
            userInfo = userManager.findUser(userInfo.getUserID());
        }
        currentUser = userInfo;
        System.out.println("Login succeeded.");
    }

    public void logout() {
        if (currentUser == null || currentUser.getUserID().value() == -1) {
            System.out.println("No user logined.");
            return;
        }
        currentUser = new UserInfo(new UserID(-1L), "", "", 0);
        System.out.println("Logout succeeded.");
    }

    public long addUser(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Username is required.");
        }
        if (userManager.findUserByUsername(username) != null) {
            throw new RuntimeException("Username already exists.");
        }

        UserID uid = generateUserId();
        userManager.insertUser(uid, username, password, 0);
        System.out.println("User added.");
        return uid.value();
    }

    private UserInfo findUserByAccount(String account) {
        if (account == null || account.isBlank()) {
            return null;
        }
        String normalized = account.trim();
        if (normalized.chars().allMatch(Character::isDigit)) {
            long userId = Long.parseLong(normalized);
            UserInfo byId = userManager.findUser(new UserID(userId));
            if (byId != null) {
                return byId;
            }
        }
        return userManager.findUserByUsername(normalized);
    }

    private UserID generateUserId() {
        for (int attempt = 0; attempt < 1000; attempt++) {
            long candidate = ThreadLocalRandom.current().nextLong(100000L, 1000000L);
            UserID userID = new UserID(candidate);
            if (!userManager.existUser(userID)) {
                return userID;
            }
        }
        throw new RuntimeException("Failed to allocate user ID.");
    }

    public void findUserInfoByUserID(long userID) {
        UserID uid = new UserID(userID);
        if (!userManager.existUser(uid)) {
            System.out.println("User not found.");
            return;
        }
        UserInfo userInfo = userManager.findUser(uid);
        if (currentUser == null || currentUser.getUserID().value() == -1 || currentUser.getPrivilege() <= userInfo.getPrivilege()) {
            System.out.println("Permission denied.");
            return;
        }
        System.out.println("UserID: " + userInfo.getUserID().value());
        System.out.println("UserName: " + userInfo.getUsername());
        System.out.println("Password: [PROTECTED]");
        System.out.println("Privilege: " + userInfo.getPrivilege());
    }

    public void modifyUserPassword(long userID, String newPassword) {
        UserID uid = new UserID(userID);
        if (!userManager.existUser(uid)) {
            System.out.println("User not found.");
            return;
        }
        UserInfo userInfo = userManager.findUser(uid);
        if (currentUser == null || currentUser.getUserID().value() == -1 || currentUser.getPrivilege() <= userInfo.getPrivilege()) {
            System.out.println("Modification forbidden.");
            return;
        }
        userManager.modifyUserPassword(uid, newPassword);
        System.out.println("Modification succeeded.");
    }

    public void modifyUserPrivilege(long userID, int newPrivilege) {
        UserID uid = new UserID(userID);
        if (!userManager.existUser(uid)) {
            System.out.println("User not found.");
            return;
        }
        UserInfo userInfo = userManager.findUser(uid);
        if (currentUser == null || currentUser.getUserID().value() == -1 || currentUser.getPrivilege() <= userInfo.getPrivilege()) {
            System.out.println("Modification forbidden.");
            return;
        }
        userManager.modifyUserPrivilege(uid, newPrivilege);
        System.out.println("Modifiaction succeeded.");
    }

    private SegmentRange resolveSegmentRange(TrainScheduler scheduler, StationID departureStation, StationID arrivalStation) {
        if (scheduler == null) {
            throw new IllegalArgumentException("Train not found");
        }

        int departureIndex = resolveStationIndex(scheduler, departureStation);
        int arrivalIndex = resolveStationIndexIncludingTerminal(scheduler, arrivalStation);
        if (departureIndex < 0 || arrivalIndex < 0) {
            throw new IllegalArgumentException("Station not found on the train route");
        }
        if (arrivalIndex <= departureIndex) {
            throw new IllegalArgumentException("Arrival station must be after departure station");
        }
        return new SegmentRange(departureIndex, arrivalIndex);
    }

    private int resolveStationIndex(TrainScheduler scheduler, StationID station) {
        if (scheduler == null) {
            throw new IllegalArgumentException("Train not found");
        }
        for (int i = 0; i + 1 < scheduler.getPassingStationNum(); i++) {
            if (scheduler.getStation(i).equals(station)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Station not found on the train route");
    }

    private int resolveStationIndexIncludingTerminal(TrainScheduler scheduler, StationID station) {
        if (scheduler == null) {
            throw new IllegalArgumentException("Train not found");
        }
        for (int i = 0; i < scheduler.getPassingStationNum(); i++) {
            if (scheduler.getStation(i).equals(station)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Station not found on the train route");
    }

    private Time resolveBaseDepartureTime(TrainScheduler scheduler, Time departureTimeAtStation, int departureIndex) {
        int minutesBeforeDeparture = 0;
        for (int i = 0; i < departureIndex; i++) {
            minutesBeforeDeparture += scheduler.getDuration(i);
        }
        return departureTimeAtStation.subtractMinutes(minutesBeforeDeparture);
    }

    private void updateInventoryForRange(TrainScheduler scheduler, Time departureTimeAtStation, SegmentRange range, int delta) {
        Time baseTime = resolveBaseDepartureTime(scheduler, departureTimeAtStation, range.departureIndex);
        for (int i = range.departureIndex; i < range.arrivalIndex; i++) {
            int price = ticketManager.updateSeat(
                    scheduler.getTrainID(),
                    scheduler.getDepartureTimeAt(i, baseTime),
                    scheduler.getStation(i).value(),
                    delta
            );
            if (price < 0) {
                throw new IllegalStateException("Ticket segment not found");
            }
        }
    }

    private TripInfo buildTripInfo(TrainScheduler scheduler, PurchaseInfo purchaseInfo, SegmentRange range, int quantity) {
        int totalDuration = 0;
        int totalPrice = 0;
        for (int i = range.departureIndex; i < range.arrivalIndex; i++) {
            totalDuration += scheduler.getDuration(i);
            totalPrice += scheduler.getPrice(i);
        }
        return new TripInfo(
                purchaseInfo.getTrainID(),
                purchaseInfo.getDepartureStation(),
                purchaseInfo.getArrivalStation(),
                quantity,
                totalDuration,
                totalPrice,
                purchaseInfo.getDepartureTime()
        );
    }

    private List<TripInfo> ticketTripMatches(PurchaseInfo purchaseInfo) {
        List<TripInfo> matches = new ArrayList<>();
        for (TripInfo trip : tripManager.queryTrip(purchaseInfo.getUserID().value())) {
            if (trip.getTrainID().equals(purchaseInfo.getTrainID())
                    && trip.getDepartureStation().equals(purchaseInfo.getDepartureStation())
                    && trip.getArrivalStation().equals(purchaseInfo.getArrivalStation())
                    && trip.getDepartureTime().equals(purchaseInfo.getDepartureTime())
                    && trip.getType() > 0) {
                matches.add(trip);
            }
        }
        return matches;
    }

    private void consumeTripsForRefund(long userId, List<TripInfo> trips, int quantity) {
        int remaining = quantity;
        for (TripInfo trip : trips) {
            if (remaining <= 0) {
                break;
            }

            tripManager.removeTrip(userId, trip);
            int consumed = Math.min(trip.getTicketNumber(), remaining);
            int leftover = trip.getTicketNumber() - consumed;
            if (leftover > 0) {
                TripInfo residual = new TripInfo(
                        trip.getTrainID(),
                        trip.getDepartureStation(),
                        trip.getArrivalStation(),
                        leftover,
                        trip.getDuration(),
                        trip.getPrice(),
                        trip.getDepartureTime(),
                        trip.getArrivalTime()
                );
                tripManager.addTrip(userId, residual);
            }
            remaining -= consumed;
        }

        if (remaining > 0) {
            throw new IllegalStateException("Refund quantity exceeds owned tickets");
        }
    }

    private static final class SegmentRange {
        private final int departureIndex;
        private final int arrivalIndex;

        private SegmentRange(int departureIndex, int arrivalIndex) {
            this.departureIndex = departureIndex;
            this.arrivalIndex = arrivalIndex;
        }
    }
}
