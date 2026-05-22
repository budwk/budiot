package com.budwk.sp.device.services.impl;

import com.budwk.sp.device.entity.Device_category;
import com.budwk.sp.device.services.DeviceCategoryService;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceCategoryServiceImpl extends BaseServiceImpl<Device_category> implements DeviceCategoryService {
    public DeviceCategoryServiceImpl(Dao dao) { super(dao); }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void save(Device_category category, String parentId, String tenantId) {
        checkCodeUnique(null, tenantId, category.getCode());
        String path = "";
        if (Strings.isNotBlank(parentId)) {
            Device_category parent = getCategory(parentId, tenantId);
            path = parent.getPath();
        }
        category.setTenantId(tenantId);
        category.setParentId(Strings.sNull(parentId));
        category.setPath(getSubPath("device_category", "path", path));
        dao().insert(category);
        if (Strings.isNotBlank(parentId)) {
            this.update(Chain.make("hasChildren", true), Cnd.where("id", "=", parentId));
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class)
    public void deleteAndChild(Device_category category, String tenantId) {
        getCategory(category.getId(), tenantId);
        dao().execute(Sqls.create("delete from device_category where tenantId=@tenantId and path like @path").setParam("tenantId", tenantId).setParam("path", category.getPath() + "%"));
        if (Strings.isNotBlank(category.getParentId())) {
            int count = count(Cnd.where("tenantId", "=", tenantId).and("parentId", "=", category.getParentId()).and("delFlag", "=", false));
            if (count < 1) this.update(Chain.make("hasChildren", false), Cnd.where("id", "=", category.getParentId()));
        }
    }

    @Override
    public Device_category getCategory(String id, String tenantId) {
        Device_category category = this.fetch(Cnd.where("id", "=", id).and("tenantId", "=", tenantId).and("delFlag", "=", false));
        if (category == null) throw new BaseException("设备分类不存在");
        return category;
    }

    private void checkCodeUnique(String id, String tenantId, String code) {
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("code", "=", Strings.sNull(code).trim()).and("delFlag", "=", false);
        if (Strings.isNotBlank(id)) cnd.and("id", "<>", id);
        if (this.count(cnd) > 0) throw new BaseException("分类编码已存在");
    }
}
