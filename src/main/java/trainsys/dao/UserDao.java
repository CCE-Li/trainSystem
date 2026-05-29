package trainsys.dao;

import trainsys.model.LoginRequest;
import trainsys.model.RegisterRequest;
import trainsys.model.UserInfo;
import trainsys.util.TrainSystem;
import trainsys.util.Types;
import org.springframework.stereotype.Repository;

@Repository
/**
 * 用户数据访问层。
 * 负责调用底层 TrainSystem 完成登录、注册和登出相关操作。
 */
public class UserDao {

    private final SystemContextDao systemContextDao;

    public UserDao(SystemContextDao systemContextDao) {
        this.systemContextDao = systemContextDao;
    }

    /**
     * 执行登录，并返回底层系统识别出的当前用户。
     * 登录前后都会重置内存登录态，避免无状态 HTTP 请求之间互相污染。
     */
    public UserInfo login(LoginRequest request) {
        TrainSystem trainSystem = systemContextDao.getTrainSystem();
        // The HTTP layer is stateless, so each login attempt must start from a
        // clean in-memory user context instead of reusing the last request state.
        trainSystem.setCurrentUser(new UserInfo(new Types.UserID(-1L), "", "", 0));
        trainSystem.login(request.getAccount(), request.getPassword());
        UserInfo currentUser = trainSystem.getCurrentUser();
        trainSystem.setCurrentUser(new UserInfo(new Types.UserID(-1L), "", "", 0));
        return currentUser;
    }

    /**
     * 注册新用户，并返回系统分配的用户 ID。
     */
    public long register(RegisterRequest request) {
        return systemContextDao.getTrainSystem().addUser(
                request.getUsername(),
                request.getPassword()
        );
    }

    /**
     * 调用底层系统执行登出。
     */
    public void logout() {
        systemContextDao.getTrainSystem().logout();
    }
}
