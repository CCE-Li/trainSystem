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
        // Always keep a built-in admin account in storage, but never treat it as
        // the currently logged-in user during service startup.
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

    public int queryRemainingTicket(FixedString trainID, Time departureTime, StationID departureStation) {
        return ticketManager.querySeat(trainID, departureTime, departureStation.value());
    }

    private boolean trySatisfyOrder() {
        if (waitingList.isEmpty()) {
            return false;
        }
        PurchaseInfo purchaseInfo = waitingList.getFrontPurchaseInfo();
        waitingList.removeHeadFromWaitingList();

        System.out.println("Processing request from User " + purchaseInfo.getUserID().value());

        if (purchaseInfo.isOrdering()) {
            int remainingTickets = queryRemainingTicket(
                    new TrainID(purchaseInfo.getTrainID().toString()),
                    purchaseInfo.getDepartureTime(),
                    purchaseInfo.getDepartureStation());
            if (remainingTickets < purchaseInfo.getType()) {
                System.out.println("No enough tickets or scheduler not exists. Order failed.");
                return false;
            }

            TrainScheduler schedule = schedulerManager.getScheduler(new FixedString(purchaseInfo.getTrainID().toString()));
            int id = schedule.findStation(purchaseInfo.getDepartureStation());
            int duration = schedule.getDuration(id);
            int price = schedule.getPrice(id);
            StationID arrivalStation = schedule.getStation(id + 1);

            ticketManager.updateSeat(
                    new TrainID(purchaseInfo.getTrainID().toString()),
                    purchaseInfo.getDepartureTime(),
                    purchaseInfo.getDepartureStation().value(),
                    -purchaseInfo.getType());
            tripManager.addTrip(purchaseInfo.getUserID().value(), new TripInfo(
                    purchaseInfo.getTrainID(),
                    purchaseInfo.getDepartureStation(),
                    arrivalStation,
                    purchaseInfo.getType(),
                    duration,
                    price,
                    purchaseInfo.getDepartureTime()
            ));
            System.out.println("Order succeeded.");
            return true;
        }

        try {
            TrainScheduler schedule = schedulerManager.getScheduler(new FixedString(purchaseInfo.getTrainID().toString()));
            if (schedule == null) {
                log.warn("Refund failed: train not found {}", purchaseInfo.getTrainID());
                return false;
            }

            List<TripInfo> userTrips = tripManager.queryTrip(purchaseInfo.getUserID().value());
            TripInfo targetTrip = null;
            for (TripInfo trip : userTrips) {
                if (trip.getTrainID().equals(purchaseInfo.getTrainID())
                        && trip.getDepartureStation().equals(purchaseInfo.getDepartureStation())
                        && trip.getDepartureTime().equals(purchaseInfo.getDepartureTime())
                        && trip.getType() > 0) {
                    targetTrip = trip;
                    break;
                }
            }
            if (targetTrip == null) {
                log.warn("Refund failed: order not found for user {}", purchaseInfo.getUserID().value());
                return false;
            }

            ticketManager.updateSeat(
                    new TrainID(purchaseInfo.getTrainID().toString()),
                    purchaseInfo.getDepartureTime(),
                    purchaseInfo.getDepartureStation().value(),
                    -purchaseInfo.getType());
            tripManager.removeTrip(purchaseInfo.getUserID().value(), targetTrip);
            System.out.println("Refund succeeded.");
            return true;
        } catch (Exception e) {
            log.error("Refund failed", e);
            System.out.println("Refund failed: " + e.getMessage());
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

    public void orderTicket(FixedString trainID, Time departureTime, StationID departureStation, int quantity) {
        waitingList.addToWaitingList(new PurchaseInfo(
                currentUser.getUserID(),
                new TrainID(trainID.toString()),
                departureTime,
                departureStation,
                quantity));
        System.out.println("Ordering request has added to waiting list.");
        while (trySatisfyOrder()) {
        }
    }

    public void orderTicket(FixedString trainID, Time departureTime, StationID departureStation) {
        orderTicket(trainID, departureTime, departureStation, 1);
    }

    public void refundTicket(FixedString trainID, Time departureTime, StationID departureStation, int quantity) {
        while (waitingList.isBusy()) {
            trySatisfyOrder();
        }
        waitingList.addToWaitingList(new PurchaseInfo(
                currentUser.getUserID(),
                new TrainID(trainID.toString()),
                departureTime,
                departureStation,
                -quantity));
        System.out.println("Refunding request has added to waiting list.");
        while (trySatisfyOrder()) {
        }
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

        // New passwords are stored as hashes. Legacy plaintext records are still
        // accepted once and upgraded immediately after a successful login.
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

    // The web API allows users to log in with either a numeric ID or a username.
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

    // Registration now owns ID allocation so callers do not need to coordinate
    // uniqueness or understand internal ID constraints.
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
}
