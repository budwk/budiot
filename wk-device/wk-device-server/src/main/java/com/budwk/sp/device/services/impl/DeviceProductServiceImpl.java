package com.budwk.sp.device.services.impl;

import com.budwk.sp.device.dto.DeviceProductDTO;
import com.budwk.sp.device.dto.DeviceProductThingModelDTO;
import com.budwk.sp.device.entity.Device_gateway;
import com.budwk.sp.device.entity.Device_product;
import com.budwk.sp.device.services.DeviceGatewayService;
import com.budwk.sp.device.services.DeviceProductService;
import com.budwk.sp.device.services.support.DeviceDataLogSchemaService;
import com.budwk.sp.device.services.support.DeviceEntityRedisCacheService;
import com.budwk.sp.device.support.DeviceThingPropertySupport;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DeviceProductServiceImpl extends BaseServiceImpl<Device_product> implements DeviceProductService {
    private final DeviceGatewayService deviceGatewayService;
    private final DeviceEntityRedisCacheService deviceEntityRedisCacheService;
    private final DeviceDataLogSchemaService deviceDataLogSchemaService;
    private final ObjectMapper objectMapper;

    public DeviceProductServiceImpl(Dao dao,
                                    DeviceGatewayService deviceGatewayService,
                                    DeviceEntityRedisCacheService deviceEntityRedisCacheService,
                                    DeviceDataLogSchemaService deviceDataLogSchemaService,
                                    ObjectMapper objectMapper) {
        super(dao);
        this.deviceGatewayService = deviceGatewayService;
        this.deviceEntityRedisCacheService = deviceEntityRedisCacheService;
        this.deviceDataLogSchemaService = deviceDataLogSchemaService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Device_product createProduct(DeviceProductDTO dto, String operatorId, String tenantId) {
        validateProductKey(null, tenantId, dto.getProductKey());
        Device_product product = buildBasic(dto, new Device_product(), operatorId, tenantId);
        product.setThingPropertyJson("[]");
        product.setThingServiceJson("[]");
        product.setThingEventJson("[]");
        product.setCreatedBy(operatorId);
        this.insert(product);
        deviceEntityRedisCacheService.cacheProduct(product);
        return product;
    }

    @Override
    public Device_product updateProduct(DeviceProductDTO dto, String operatorId, String tenantId) {
        Device_product product = getProduct(dto.getId(), tenantId);
        validateProductKey(product.getId(), tenantId, dto.getProductKey());
        buildBasic(dto, product, operatorId, tenantId);
        this.updateIgnoreNull(product);
        deviceEntityRedisCacheService.cacheProduct(product);
        return product;
    }

    @Override
    public Device_product updateThingModel(DeviceProductThingModelDTO dto, String operatorId, String tenantId) {
        Device_product product = getProduct(dto.getId(), tenantId);
        String thingPropertyJson = normalizeThingJson(dto.getThingPropertyJson());
        validateThingPropertyColumns(thingPropertyJson);
        product.setThingPropertyJson(thingPropertyJson);
        product.setThingServiceJson(normalizeThingJson(dto.getThingServiceJson()));
        product.setThingEventJson(normalizeThingJson(dto.getThingEventJson()));
        product.setUpdatedBy(operatorId);
        this.updateIgnoreNull(product);
        deviceEntityRedisCacheService.cacheProduct(product);
        deviceDataLogSchemaService.syncProduct(product);
        return product;
    }

    @Override
    public void deleteProduct(String id, String tenantId) {
        Device_product product = getProduct(id, tenantId);
        this.delete(product.getId());
        deviceEntityRedisCacheService.evictProduct(tenantId, product.getId());
    }

    @Override
    public Device_product getProduct(String id, String tenantId) {
        Device_product cached = deviceEntityRedisCacheService.getProduct(tenantId, id);
        if (cached != null && !Boolean.TRUE.equals(cached.getDelFlag())) {
            return cached;
        }
        Device_product product = this.fetch(Cnd.where("id", "=", id).and("tenantId", "=", tenantId).and("delFlag", "=", false));
        if (product == null) throw new BaseException("设备产品不存在");
        deviceEntityRedisCacheService.cacheProduct(product);
        return product;
    }

    @Override
    public Device_product getByProductKey(String productKey, String tenantId) {
        Device_product product = this.fetch(Cnd.where("tenantId", "=", tenantId).and("productKey", "=", Strings.sNull(productKey).trim()).and("delFlag", "=", false));
        deviceEntityRedisCacheService.cacheProduct(product);
        return product;
    }

    @Override
    public List<Device_product> listEnabled(String tenantId) {
        return this.query(Cnd.where("tenantId", "=", tenantId).and("disabled", "=", false).and("delFlag", "=", false).asc("createdAt"));
    }

    @Override
    public Map<String, Integer> countDeviceByProductIds(List<String> productIds, String tenantId) {
        Map<String, Integer> map = new HashMap<>();
        if (productIds == null || productIds.isEmpty()) return map;
        Sql sql = Sqls.create("select productId, count(1) as count from device_info where tenantId=@tenantId and delFlag=false and productId in (@ids) group by productId");
        sql.params().set("tenantId", tenantId);
        sql.params().set("ids", productIds);
        sql.setCallback((conn, rs, sql1) -> {
            while (rs.next()) {
                map.put(rs.getString("productId"), rs.getInt("count"));
            }
            return null;
        });
        dao().execute(sql);
        return map;
    }

    private Device_product buildBasic(DeviceProductDTO dto, Device_product product, String operatorId, String tenantId) {
        Device_gateway gateway = null;
        if (Strings.isNotBlank(dto.getGatewayNodeId())) {
            gateway = deviceGatewayService.getGateway(dto.getGatewayNodeId(), tenantId);
        }
        product.setTenantId(tenantId);
        product.setProductKey(Strings.sNull(dto.getProductKey()).trim());
        product.setName(Strings.sNull(dto.getName()).trim());
        product.setCategoryId(Strings.sNull(dto.getCategoryId()).trim());
        product.setVendorId(Strings.sNull(dto.getVendorId()).trim());
        product.setProductType(dto.getProductType());
        product.setNetworkProtocol(gateway == null ? dto.getNetworkProtocol() : gateway.getNetworkProtocol());
        product.setProtocolId(gateway == null ? Strings.sNull(dto.getProtocolId()).trim() : gateway.getProtocolId());
        product.setGatewayNodeId(gateway == null ? Strings.sNull(dto.getGatewayNodeId()).trim() : gateway.getId());
        product.setGatewayPort(gateway == null ? dto.getGatewayPort() : gateway.getPort());
        product.setDescription(Strings.sNull(dto.getDescription()).trim());
        product.setDisabled(dto.isDisabled());
        product.setUpdatedBy(operatorId);
        return product;
    }

    private String normalizeThingJson(String json) {
        String resolved = Strings.sNull(json).trim();
        return Strings.isBlank(resolved) ? "[]" : resolved;
    }

    private void validateProductKey(String id, String tenantId, String productKey) {
        String resolved = Strings.sNull(productKey).trim();
        if (!resolved.matches("^[a-z][a-z0-9]{1,31}$")) throw new BaseException("ProductKey格式不正确");
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("productKey", "=", resolved).and("delFlag", "=", false);
        if (Strings.isNotBlank(id)) cnd.and("id", "<>", id);
        if (this.count(cnd) > 0) throw new BaseException("ProductKey已存在");
    }

    private void validateThingPropertyColumns(String thingPropertyJson) {
        Set<String> columns = new HashSet<>();
        for (Map<String, Object> property : DeviceThingPropertySupport.parseProperties(thingPropertyJson, objectMapper)) {
            String identifier = DeviceThingPropertySupport.readIdentifier(property);
            if (Strings.isBlank(identifier)) {
                continue;
            }
            String columnName = DeviceThingPropertySupport.toColumnName(identifier);
            if (!columns.add(columnName)) {
                throw new BaseException("物模型属性标识规范化后重复，请调整 identifier");
            }
        }
    }
}
