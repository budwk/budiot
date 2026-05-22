package com.budwk.sp.file.providers;

import com.budwk.sp.file.dto.FileUploadRequestDTO;
import com.budwk.sp.file.dto.FileUploadResultDTO;
import com.budwk.sp.file.service.FileInfoService;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import org.apache.dubbo.config.annotation.DubboService;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@DubboService(interfaceClass = IFileUploadProvider.class)
public class FileUploadProvider implements IFileUploadProvider {
    private final FileInfoService fileInfoService;

    public FileUploadProvider(FileInfoService fileInfoService) {
        this.fileInfoService = fileInfoService;
    }

    @Override
    public FileUploadResultDTO upload(FileUploadRequestDTO request) {
        MultipartFile file = new InMemoryMultipartFile(
                Strings.sBlank(request.getFileName(), "upload.bin"),
                Strings.sBlank(request.getContentType(), "application/octet-stream"),
                request.getBytes()
        );
        return fileInfoService.upload(
                new MultipartFile[]{file},
                Strings.sBlank(request.getCategory(), "file"),
                Strings.sNull(request.getSubPath()).trim(),
                request.isPublicFlag(),
                Strings.sBlank(request.getOperatorId(), GlobalConstant.TENANT_ID_DEFAULT),
                Strings.sNull(request.getOperatorLoginname()).trim(),
                Strings.sNull(request.getOperatorUsername()).trim(),
                Strings.sBlank(request.getTenantId(), GlobalConstant.TENANT_ID_DEFAULT)
        ).getFirst();
    }

    @Override
    public void delete(String id, String tenantId) {
        fileInfoService.deleteFile(Strings.sNull(id).trim(), Strings.sBlank(tenantId, GlobalConstant.TENANT_ID_DEFAULT));
    }

    private record InMemoryMultipartFile(String originalFilename, String contentType, byte[] bytes) implements MultipartFile {
        @Override
        public String getName() {
            return originalFilename;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes == null || bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes == null ? 0 : bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes == null ? new byte[0] : bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(getBytes());
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            java.nio.file.Files.write(dest.toPath(), getBytes());
        }
    }
}
