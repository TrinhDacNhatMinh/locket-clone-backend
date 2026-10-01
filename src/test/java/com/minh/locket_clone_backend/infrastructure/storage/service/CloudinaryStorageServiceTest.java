package com.minh.locket_clone_backend.infrastructure.storage.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryStorageServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryStorageService storageService;

    // Helper method to create a valid JPEG file for testing magic bytes
    private MultipartFile createValidJpegFile(long sizeBytes) {
        byte[] content = new byte[(int) sizeBytes];
        content[0] = (byte) 0xFF;
        content[1] = (byte) 0xD8;
        content[2] = (byte) 0xFF;
        return new MockMultipartFile("file", "test.jpg", "image/jpeg", content);
    }

    @Nested
    @DisplayName("uploadImage()")
    class UploadImageTests {

        @Test
        void uploadImage_validImage_returnsUrl() throws IOException {
            // given
            MultipartFile file = createValidJpegFile(1024); // 1KB
            String expectedUrl = "https://res.cloudinary.com/demo/image/upload/v1/test.jpg";

            when(cloudinary.uploader()).thenReturn(uploader);
            when(uploader.upload(any(byte[].class), anyMap()))
                    .thenReturn(Map.of(
                            "secure_url", expectedUrl,
                            "public_id", "test_id"
                    ));

            // when
            String url = storageService.uploadImage(file);

            // then
            assertThat(url).isEqualTo(expectedUrl);
            verify(cloudinary, times(1)).uploader();
            verify(uploader, times(1)).upload(any(byte[].class), anyMap());
        }

        @Test
        void uploadImage_emptyFile_throwsException() {
            // given
            MultipartFile file = new MockMultipartFile("file", new byte[0]);

            // when and then
            BusinessException ex = assertThrows(BusinessException.class, () -> storageService.uploadImage(file));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_FILE_FORMAT);
            verifyNoInteractions(cloudinary);
        }

        @Test
        void uploadImage_fileTooLarge_throwsException() {
            // given
            long oversize = 21L * 1024 * 1024; // 21MB
            MultipartFile file = createValidJpegFile(oversize);

            // when and then
            BusinessException ex = assertThrows(BusinessException.class, () -> storageService.uploadImage(file));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FILE_TOO_LARGE);
            verifyNoInteractions(cloudinary);
        }

        @Test
        void uploadImage_invalidMagicBytes_throwsException() {
            // given
            byte[] invalidContent = {0x00, 0x00, 0x00, 0x00};
            MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", invalidContent);

            // when and then
            BusinessException ex = assertThrows(BusinessException.class, () -> storageService.uploadImage(file));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_FILE_FORMAT);
            verifyNoInteractions(cloudinary);
        }

        @Test
        void uploadImage_cloudinaryThrowsIOException_throwsException() throws IOException {
            // given
            MultipartFile file = createValidJpegFile(1024);

            when(cloudinary.uploader()).thenReturn(uploader);
            when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("Cloudinary error"));

            // when and then
            BusinessException ex = assertThrows(BusinessException.class, () -> storageService.uploadImage(file));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.STORAGE_UPLOAD_FAILED);
        }
    }
}
