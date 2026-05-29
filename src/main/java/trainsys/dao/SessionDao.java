package trainsys.dao;

import trainsys.model.UserInfo;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
/**
 * 会话数据访问层。
 * 使用内存 Map 维护 sessionId 到用户信息的映射。
 */
public class SessionDao {

    private final Map<String, UserInfo> sessionMap = new ConcurrentHashMap<>();

    /**
     * 保存会话与用户的映射关系。
     */
    public void save(String sessionId, UserInfo userInfo) {
        sessionMap.put(sessionId, userInfo);
    }

    /**
     * 根据会话 ID 查询对应用户。
     */
    public UserInfo find(String sessionId) {
        return sessionMap.get(sessionId);
    }

    /**
     * 删除指定会话。
     */
    public void remove(String sessionId) {
        sessionMap.remove(sessionId);
    }
}
