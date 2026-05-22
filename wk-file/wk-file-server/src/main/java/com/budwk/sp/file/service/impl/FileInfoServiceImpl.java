package com.budwk.sp.file.service.impl;

import com.budwk.sp.file.dto.FileInfoQueryDTO;
import com.budwk.sp.file.dto.FileUploadResultDTO;
import com.budwk.sp.file.entity.File_info;
import com.budwk.sp.file.enums.FileStorageType;
import com.budwk.sp.file.service.FileInfoService;
import com.budwk.sp.file.storage.FileStorageManager;
import com.budwk.sp.file.storage.FileStorageProvider;
import com.budwk.sp.file.storage.StoredFile;
import com.budwk.sp.file.util.FileStoragePathHelper;
import com.budwk.sp.file.util.FileTypeHelper;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.page.Pagination;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.dao.util.Daos;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileInfoServiceImpl extends BaseServiceImpl<File_info> implements FileInfoService {
    private final Map<String, Dao> tenantDaos = new HashMap<>();

    @Autowired
    private FileStorageManager fileStorageManager;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public FileInfoServiceImpl(Dao dao) {
        super(dao);
    }

    @Override
    public List<FileUploadResultDTO> upload(MultipartFile[] files, String category, String subPath, boolean publicFlag,
                                            String operatorId, String operatorLoginname, String operatorUsername, String tenantId) {
        if (files == null || files.length == 0) {
            throw new BaseException("请选择上传文件");
        }
        String normalizedSubPath = FileStoragePathHelper.normalizeSubPath(subPath);
        List<FileUploadResultDTO> results = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            results.add(doUpload(file, category, normalizedSubPath, publicFlag, operatorId, operatorLoginname, operatorUsername, tenantId));
        }
        if (results.isEmpty()) {
            throw new BaseException("请选择上传文件");
        }
        return results;
    }

    @Override
    public Pagination list(FileInfoQueryDTO dto, String tenantId) {
        FileInfoQueryDTO query = dto == null ? new FileInfoQueryDTO() : dto;
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("delFlag", "=", false);
        if (Strings.isNotBlank(query.getFileName())) {
            cnd.and("fileName", "like", "%" + query.getFileName().trim() + "%");
        }
        if (Strings.isNotBlank(query.getCategory())) {
            cnd.and("category", "=", query.getCategory().trim());
        }
        if (Strings.isNotBlank(query.getStorageType())) {
            cnd.and("storageType", "=", FileStorageType.fromValue(query.getStorageType()).name());
        }
        if (query.getBeginTime() != null) {
            cnd.and("createdAt", ">=", query.getBeginTime());
        }
        if (query.getEndTime() != null) {
            cnd.and("createdAt", "<=", query.getEndTime());
        }
        if (Strings.isNotBlank(query.getPageOrderName()) && Strings.isNotBlank(query.getPageOrderBy())) {
            cnd.orderBy(query.getPageOrderName(), PageUtil.getOrder(query.getPageOrderBy()));
        } else {
            cnd.desc("createdAt");
        }
        return this.listPage(fileInfoDao(tenantId), query.getPageNo(), query.getPageSize(), cnd);
    }

    @Override
    public File_info getFileInfo(String id, String tenantId) {
        File_info fileInfo = fileInfoDao(tenantId).fetch(getEntityClass(),
                Cnd.where("id", "=", id).and("tenantId", "=", tenantId).and("delFlag", "=", false));
        if (fileInfo == null) {
            throw new BaseException("文件不存在");
        }
        return fileInfo;
    }

    @Override
    public void preview(String id, String tenantId, HttpServletResponse response, boolean requirePublic) {
        File_info fileInfo = requirePublic ? getPublicFile(id, tenantId) : getFileInfo(id, tenantId);
        streamFile(fileInfo, response, false);
    }

    @Override
    public void download(String id, String tenantId, HttpServletResponse response) {
        File_info fileInfo = getFileInfo(id, tenantId);
        streamFile(fileInfo, response, true);
    }

    @Override
    public void updatePublicFlag(String id, boolean publicFlag, String operatorId, String tenantId) {
        File_info fileInfo = getFileInfo(id, tenantId);
        org.nutz.dao.Chain chain = org.nutz.dao.Chain.make("publicFlag", publicFlag)
                .add("updatedBy", operatorId)
                .add("updatedAt", System.currentTimeMillis());
        if (publicFlag && Strings.isBlank(fileInfo.getAccessPath())) {
            chain.add("accessPath", buildPublicAccessPath(tenantId, id));
        }
        fileInfoDao(tenantId).update(getEntityClass(), chain, Cnd.where("id", "=", id).and("tenantId", "=", tenantId));
    }

    @Override
    public void deleteFile(String id, String tenantId) {
        File_info fileInfo = getFileInfo(id, tenantId);
        try {
            fileStorageManager.getProvider(fileInfo.getStorageType()).delete(fileInfo.getStorageKey());
        } catch (IOException e) {
            throw new IllegalStateException("删除文件失败", e);
        }
        fileInfoDao(tenantId).update(getEntityClass(),
                org.nutz.dao.Chain.make("delFlag", true)
                        .add("updatedAt", System.currentTimeMillis())
                        .add("updatedBy", fileInfo.getUpdatedBy()),
                Cnd.where("id", "=", id).and("tenantId", "=", tenantId));
    }

    private FileUploadResultDTO doUpload(MultipartFile file, String category, String subPath, boolean publicFlag,
                                         String operatorId, String operatorLoginname, String operatorUsername, String tenantId) {
        String contentType = FileTypeHelper.resolveContentType(file);
        if (!FileTypeHelper.isAllowed(category, contentType)) {
            throw new BaseException("文件类型与上传分类不匹配");
        }
        String fileName = FileTypeHelper.resolveFileName(file, category);
        String suffix = FileTypeHelper.resolveSuffix(file);
        FileStorageType storageType = fileStorageManager.getDefaultStorageType();
        FileStorageProvider provider = fileStorageManager.getProvider(storageType);
        try {
            StoredFile storedFile = provider.store(tenantId, file, suffix, subPath);
            File_info entity = new File_info();
            entity.setTenantId(tenantId);
            entity.setFileName(fileName);
            entity.setSuffix(suffix);
            entity.setContentType(contentType);
            entity.setSize(file.getSize());
            entity.setCategory(Strings.sBlank(category, "file"));
            entity.setStorageType(storageType);
            entity.setStorageKey(storedFile.storageKey());
            entity.setPublicFlag(publicFlag);
            entity.setImageFlag(FileTypeHelper.isImage(contentType));
            entity.setCreatedBy(operatorId);
            entity.setCreatedByLoginname(operatorLoginname);
            entity.setCreatedByUsername(operatorUsername);
            Dao dao = fileInfoDao(tenantId);
            dao.insert(entity);
            entity.setAccessPath(buildPublicAccessPath(tenantId, entity.getId()));
            dao.updateIgnoreNull(entity);
            FileUploadResultDTO result = new FileUploadResultDTO();
            result.setId(entity.getId());
            result.setFilename(entity.getFileName());
            result.setUrl(entity.getAccessPath());
            result.setContentType(entity.getContentType());
            result.setSize(entity.getSize());
            result.setImage(entity.isImageFlag());
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("文件上传失败", e);
        }
    }

    private void streamFile(File_info fileInfo, HttpServletResponse response, boolean attachment) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(Strings.sBlank(fileInfo.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE));
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader("download-filename", urlEncode(fileInfo.getFileName()));
        String dispositionType = attachment ? "attachment" : "inline";
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                dispositionType + "; filename*=UTF-8''" + urlEncode(FileTypeHelper.formatDownloadName(fileInfo.getFileName())));
        response.setContentLengthLong(fileInfo.getSize());
        try {
            fileStorageManager.getProvider(fileInfo.getStorageType())
                    .writeTo(fileInfo.getStorageKey(), response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            throw new IllegalStateException("文件流输出失败", e);
        }
    }

    private File_info getPublicFile(String id, String tenantId) {
        File_info fileInfo = fileInfoDao(tenantId).fetch(getEntityClass(),
                Cnd.where("id", "=", id)
                        .and("tenantId", "=", tenantId)
                        .and("publicFlag", "=", true)
                        .and("delFlag", "=", false));
        if (fileInfo == null) {
            throw new BaseException("文件不存在");
        }
        return fileInfo;
    }

    private String buildPublicAccessPath(String tenantId, String id) {
        return "/platform/pub/file/view/" + Strings.sBlank(tenantId, "platform") + "/" + id;
    }

    private Dao fileInfoDao(String tenantId) {
        String tenantKey = sanitizeTenantKey(tenantId);
        Dao dao = tenantDaos.get(tenantKey);
        if (dao == null) {
            synchronized (this) {
                dao = tenantDaos.get(tenantKey);
                if (dao == null) {
                    dao = Daos.ext(this.dao(), tenantKey);
                    dao.create(File_info.class, false);
                    ensureColumns(getTableName(tenantKey));
                    ensureIndexes(getTableName(tenantKey));
                    tenantDaos.put(tenantKey, dao);
                }
            }
        }
        return dao;
    }

    private String getTableName(String tenantKey) {
        return "file_info_" + tenantKey;
    }

    private void ensureColumns(String tableName) {
        addColumnIfAbsent(tableName, "createdByLoginname", "VARCHAR(120)");
        addColumnIfAbsent(tableName, "createdByUsername", "VARCHAR(100)");
    }

    private void addColumnIfAbsent(String tableName, String columnName, String definition) {
        if (columnExists(tableName, columnName)) {
            return;
        }
        Sql sql = Sqls.create("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        this.dao().execute(sql);
    }

    private boolean columnExists(String tableName, String columnName) {
        final boolean[] exists = {false};
        this.dao().run(conn -> {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet resultSet = metaData.getColumns(conn.getCatalog(), conn.getSchema(), tableName, null)) {
                while (resultSet.next()) {
                    String current = resultSet.getString("COLUMN_NAME");
                    if (current != null && current.equalsIgnoreCase(columnName)) {
                        exists[0] = true;
                        break;
                    }
                }
            }
        });
        return exists[0];
    }

    private String sanitizeTenantKey(String tenantId) {
        String value = Strings.sBlank(tenantId, "platform").trim();
        return value.replaceAll("[^0-9A-Za-z_]", "_").toLowerCase();
    }

    private void ensureIndexes(String tableName) {
        createIndexIfAbsent(tableName + "_created_idx", tableName, "createdAt");
        createIndexIfAbsent(tableName + "_filename_idx", tableName, "fileName");
    }

    private void createIndexIfAbsent(String indexName, String tableName, String... fields) {
        if (indexExists(tableName, indexName)) {
            return;
        }
        Sql sql = Sqls.create("CREATE INDEX " + indexName + " ON " + tableName + "(" + String.join(",", fields) + ")");
        this.dao().execute(sql);
    }

    private boolean indexExists(String tableName, String indexName) {
        final boolean[] exists = {false};
        this.dao().run(conn -> {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet resultSet = metaData.getIndexInfo(conn.getCatalog(), conn.getSchema(), tableName, false, false)) {
                while (resultSet.next()) {
                    String current = resultSet.getString("INDEX_NAME");
                    if (current != null && indexName.equalsIgnoreCase(current)) {
                        exists[0] = true;
                        break;
                    }
                }
            }
        });
        return exists[0];
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(Strings.sBlank(value, "download.bin"), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
    }
}
