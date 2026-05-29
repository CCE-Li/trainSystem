package trainsys.dao;

import trainsys.dao.entity.UserEntity;
import trainsys.dao.mapper.UserMapper;
import trainsys.dao.support.DbCodec;
import trainsys.model.UserInfo;
import trainsys.util.PasswordHasher;
import trainsys.util.Types.UserID;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
/**
 * 用户持久化管理器。
 * 基于 MyBatis-Plus 负责用户表的增删改查以及对象转换。
 */
public class UserManager {
    private final UserMapper userMapper;

    public UserManager(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 新增用户记录，并在入库前完成密码哈希。
     */
    public void insertUser(UserID userID, String username, String password, int privilege) {
        UserEntity entity = new UserEntity();
        entity.setUserId(userID.value());
        entity.setUsername(username);
        entity.setPassword(PasswordHasher.hash(password));
        entity.setPrivilege(privilege);
        userMapper.insert(entity);
    }

    /**
     * 判断指定用户是否存在。
     */
    public boolean existUser(UserID userID) {
        return userMapper.selectById(userID.value()) != null;
    }

    /**
     * 按用户 ID 查询用户信息。
     */
    public UserInfo findUser(UserID userID) {
        UserEntity entity = userMapper.selectById(userID.value());
        if (entity == null) {
            return null;
        }
        return DbCodec.toUserInfo(entity.getUserId(), entity.getUsername(), entity.getPassword(), entity.getPrivilege());
    }

    /**
     * 按用户名查询用户信息。
     */
    public UserInfo findUserByUsername(String username) {
        List<UserEntity> entities = userMapper.selectList(null);
        for (UserEntity entity : entities) {
            if (entity.getUsername() != null && entity.getUsername().equals(username)) {
                return DbCodec.toUserInfo(entity.getUserId(), entity.getUsername(), entity.getPassword(), entity.getPrivilege());
            }
        }
        return null;
    }

    /**
     * 删除指定用户。
     */
    public void removeUser(UserID userID) {
        userMapper.deleteById(userID.value());
    }

    /**
     * 修改用户权限等级。
     */
    public void modifyUserPrivilege(UserID userID, int newPrivilege) {
        UserEntity entity = userMapper.selectById(userID.value());
        if (entity != null) {
            entity.setPrivilege(newPrivilege);
            userMapper.updateById(entity);
        }
    }

    /**
     * 修改用户密码，并重新进行哈希。
     */
    public void modifyUserPassword(UserID userID, String newPassword) {
        UserEntity entity = userMapper.selectById(userID.value());
        if (entity != null) {
            entity.setPassword(PasswordHasher.hash(newPassword));
            userMapper.updateById(entity);
        }
    }

    /**
     * 预留关闭钩子，当前无需额外清理。
     */
    public void close() {
    }
}
