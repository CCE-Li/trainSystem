package trainsys.control;

import trainsys.model.ApiResponse;
import trainsys.model.LoginRequest;
import trainsys.model.RegisterRequest;
import trainsys.model.RegisterResponse;
import trainsys.model.UserInfoDTO;
import trainsys.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户认证相关接口。
 * 负责登录、注册、会话校验和登出，供前端页面直接调用。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 登录接口。
     * 支持用用户 ID 或用户名作为登录标识，成功后返回会话 ID 和用户信息。
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return userService.login(request);
    }

    /**
     * 注册接口。
     * 用户只需提交用户名和密码，系统自动分配 6 位用户 ID。
     */
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    /**
     * 登出接口。
     * 前端通过 Bearer Token 传入当前会话 ID，服务端负责清理会话。
     */
    @PostMapping("/logout")
    public ApiResponse<String> logout(@RequestHeader(value = "Authorization", required = false) String sessionId) {
        if (sessionId == null || !sessionId.startsWith("Bearer ")) {
            return ApiResponse.error(401, "未登录");
        }
        return userService.logout(sessionId.substring(7));
    }

    /**
     * 会话校验接口。
     * 页面刷新后前端会调用它恢复登录状态，并重新拿到最新用户信息。
     */
    @GetMapping("/validate")
    public ApiResponse<UserInfoDTO> validate(
            @RequestHeader(value = "Authorization", required = false) String sessionId) {
        if (sessionId == null || !sessionId.startsWith("Bearer ")) {
            return ApiResponse.error(401, "未登录");
        }
        return userService.validateSession(sessionId.substring(7));
    }
}
