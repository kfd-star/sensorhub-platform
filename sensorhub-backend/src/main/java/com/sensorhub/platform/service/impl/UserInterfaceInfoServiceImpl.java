package com.sensorhub.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sensorhub.platform.common.ErrorCode;
import com.sensorhub.platform.exception.BusinessException;
import com.sensorhub.platform.mapper.UserInterfaceInfoMapper;
import com.sensorhub.platform.service.UserInterfaceInfoService;
import com.sensorhub.common.model.entity.UserInterfaceInfo;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User interface relation service implementation.
 */
@Service
public class UserInterfaceInfoServiceImpl extends ServiceImpl<UserInterfaceInfoMapper, UserInterfaceInfo>
        implements UserInterfaceInfoService {

    private static final int DEFAULT_LEFT_NUM = 999999;

    private static final Map<String, Object> INVOKE_RELATION_LOCK_MAP = new ConcurrentHashMap<>();

    @Override
    public void validUserInterfaceInfo(UserInterfaceInfo userInterfaceInfo, boolean add) {
        if (userInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (add) {
            if (userInterfaceInfo.getInterfaceInfoId() <= 0 || userInterfaceInfo.getUserId() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "interface or user does not exist");
            }
        }
        if (userInterfaceInfo.getLeftNum() != null && userInterfaceInfo.getLeftNum() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "left count must be greater than or equal to 0");
        }
    }

    @Override
    public boolean invokeCount(long interfaceInfoId, long userId) {
        if (interfaceInfoId <= 0 || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean updated = updateInvokeCount(interfaceInfoId, userId);
        if (updated) {
            return true;
        }
        ensureInvokeRelationExists(interfaceInfoId, userId);
        return updateInvokeCount(interfaceInfoId, userId);
    }

    private boolean updateInvokeCount(long interfaceInfoId, long userId) {
        UpdateWrapper<UserInterfaceInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("interfaceInfoId", interfaceInfoId);
        updateWrapper.eq("userId", userId);
        updateWrapper.eq("isDelete", 0);
        updateWrapper.setSql("leftNum = leftNum - 1, totalNum = totalNum + 1");
        return this.update(updateWrapper);
    }

    private void ensureInvokeRelationExists(long interfaceInfoId, long userId) {
        String lockKey = userId + ":" + interfaceInfoId;
        Object lock = INVOKE_RELATION_LOCK_MAP.computeIfAbsent(lockKey, key -> new Object());
        synchronized (lock) {
            QueryWrapper<UserInterfaceInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userId", userId);
            queryWrapper.eq("interfaceInfoId", interfaceInfoId);
            queryWrapper.eq("isDelete", 0);
            queryWrapper.last("limit 1");
            UserInterfaceInfo existingRelation = this.getOne(queryWrapper, false);
            if (existingRelation == null) {
                UserInterfaceInfo defaultRelation = new UserInterfaceInfo();
                defaultRelation.setInterfaceInfoId(interfaceInfoId);
                defaultRelation.setUserId(userId);
                defaultRelation.setTotalNum(0);
                defaultRelation.setLeftNum(DEFAULT_LEFT_NUM);
                defaultRelation.setStatus(0);
                this.save(defaultRelation);
            }
        }
        INVOKE_RELATION_LOCK_MAP.remove(lockKey, lock);
    }
}
