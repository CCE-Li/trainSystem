package trainsys.service;

import trainsys.dao.SessionDao;
import trainsys.dao.SystemContextDao;
import trainsys.dao.UserDao;
import trainsys.model.ApiResponse;
import trainsys.model.LoginRequest;
import trainsys.model.RegisterRequest;
import trainsys.model.RegisterResponse;
import trainsys.model.UserInfoDTO;
import trainsys.model.UserInfo;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户认证服务。
 * 统一处理登录、注册、会话映射和当前用户绑定逻辑。
 */
@Service
public class UserService {

    private final UserDao userDao;
    private final SessionDao sessionDao;
    private final SystemContextDao systemContextDao;

    public UserService(UserDao userDao, SessionDao sessionDao, SystemContextDao systemContextDao) {
        this.userDao = userDao;
        this.sessionDao = sessionDao;
        this.systemContextDao = systemContextDao;
    }

    /**
     * 校验账号密码并创建新的会话记录。
     * 成功后返回会话 ID 和当前登录用户信息。
     */
    public ApiResponse<Map<String, Object>> login(LoginRequest request) {
        try {
            UserInfo currentUser = userDao.login(request);
            if (currentUser == null || currentUser.getUserID().value() == -1) {
                return ApiResponse.error("登录失败，用户名、用户 ID 或密码错误");
            }

            String sessionId = "session_" + currentUser.getUserID().value() + "_" + System.currentTimeMillis();
            sessionDao.save(sessionId, currentUser);

            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", sessionId);
            result.put("user", toUserDTO(currentUser));
            return ApiResponse.success("登录成功", result);
        } catch (Exception e) {
            return ApiResponse.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 注册新用户并返回系统生成的用户标识。
     */
    public ApiResponse<RegisterResponse> register(RegisterRequest request) {
        try {
            long userId = userDao.register(request);
            return ApiResponse.success("注册成功", new RegisterResponse(userId, request.getUsername()));
        } catch (Exception e) {
            return ApiResponse.error("注册失败: " + e.getMessage());
        }
    }

    /**
     * 删除会话记录，并清理底层系统保存的当前登录态。
     */
    public ApiResponse<String> logout(String sessionId) {
        sessionDao.remove(sessionId);
        userDao.logout();
        return ApiResponse.success("退出登录成功", null);
    }

    /**
     * 根据会话 ID 获取当前用户。
     */
    public UserInfo getCurrentUser(String sessionId) {
        return sessionDao.find(sessionId);
    }

    /**
     * 将会话对应的用户重新绑定到底层 TrainSystem 上下文。
     * 旧核心逻辑依赖 currentUser，因此每次业务调用前都需要显式恢复。
     */
    public void bindCurrentUser(String sessionId) {
        UserInfo user = getCurrentUser(sessionId);
        systemContextDao.getTrainSystem().setCurrentUser(user);
    }

    /**
     * 校验会话是否仍然有效，并返回最新用户信息。
     */
    public ApiResponse<UserInfoDTO> validateSession(String sessionId) {
        UserInfo user = getCurrentUser(sessionId);
        if (user == null) {
            return ApiResponse.error(401, "Session expired");
        }
        return ApiResponse.success(toUserDTO(user));
    }

    /**
     * 将内部用户对象转换为前端使用的数据传输对象。
     */
    private UserInfoDTO toUserDTO(UserInfo user) {
        UserInfoDTO userDTO = new UserInfoDTO();
        userDTO.setUserId(user.getUserID().value());
        userDTO.setUsername(user.getUsername());
        userDTO.setPrivilege(user.getPrivilege());
        return userDTO;
    }
}
