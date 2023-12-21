package com.chenming.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenming.usercenter.common.ErrorCode;
import com.chenming.usercenter.common.ResultUtils;
import com.chenming.usercenter.exception.BusinessException;
import com.chenming.usercenter.model.domain.User;
import com.chenming.usercenter.service.UserService;
import com.chenming.usercenter.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.chenming.usercenter.constant.userConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
 *
 * @author chenming
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    /**
     * 盐值
     */
    private final String SALT = "chenming";


    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        //1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户密码过短");
        }
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "星球编号过长");
        }


        //账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "包含特殊字符");
        }

        //密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次密码不一致");
        }

        //用户账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户账号重复");
        }

        //星期编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "星球编号重复");
        }

        //2 加密

        String encrytPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        //3 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encrytPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);

        if (!saveResult) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "注册失败");
        }
        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        if (userAccount.length() < 4) {
            return null;
        }
        if (userPassword.length() < 8) {
            return null;
        }


        //账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return null;
        }

        String encrytPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());   //md5DigestAsHex() 方法接受的是一个字节数组作为输入参数，而不是直接接受字符串
        //查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encrytPassword);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            return null;
        }

        //先用户脱敏

        User safetyUser = getSafetyUser(user);
        //再记录用户登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);

        return safetyUser;


    }

    /**
     * 用户脱敏
     *
     * @param orignUser
     * @return
     */
    @Override
    public User getSafetyUser(User orignUser) {
        if (orignUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(orignUser.getId());
        safetyUser.setUsername(orignUser.getUsername());
        safetyUser.setUserAccount(orignUser.getUserAccount());
        safetyUser.setAvatarUrl(orignUser.getAvatarUrl());
        safetyUser.setGender(orignUser.getGender());
        safetyUser.setPhone(orignUser.getPhone());
        safetyUser.setEmail(orignUser.getEmail());
        safetyUser.setUserRole(orignUser.getUserRole());
        safetyUser.setUserStatus(orignUser.getUserStatus());
        safetyUser.setCreateTime(orignUser.getCreateTime());
        safetyUser.setPlanetCode(orignUser.getPlanetCode());
        return safetyUser;
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        //移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }
}




