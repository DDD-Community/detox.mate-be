package com.detoxmate.upload.service;

import com.detoxmate.upload.dto.PresignedUrlRequest;
import com.detoxmate.upload.dto.PresignedUrlResponse;
import com.detoxmate.upload.dto.UploadPurpose;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GcsPhotoUploadTest {

    private static final String BUCKET = "photo-test-bucket";
    private static final String LEGACY_URL = "https://legacy-images.example";
    private static final long MIB = 1024 * 1024;

    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    private UploadService uploads;
    private ImageReadUrlBuilder reads;

    @BeforeAll
    void configureUploadContext(@TempDir Path directory) throws Exception {
        Path credentials = directory.resolve("signer.json");
        var keys = KeyPairGenerator.getInstance("RSA");
        keys.initialize(2048);
        String privateKey = Base64.getEncoder().encodeToString(keys.generateKeyPair().getPrivate().getEncoded());
        Files.writeString(credentials, """
                {
                  "type": "service_account",
                  "project_id": "photo-test",
                  "private_key_id": "test-key-id",
                  "private_key": "-----BEGIN PRIVATE KEY-----\\n%s\\n-----END PRIVATE KEY-----\\n",
                  "client_email": "photo-test@photo-test.iam.gserviceaccount.com",
                  "client_id": "123456789",
                  "token_uri": "https://oauth2.googleapis.com/token"
                }
                """.formatted(privateKey));

        context.getEnvironment().setActiveProfiles("prod");
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                "app.storage.gcs.bucket-name=" + BUCKET,
                "app.storage.gcs.presigned-url-expires-in=420",
                "app.storage.image-read-base-url=" + LEGACY_URL,
                "firebase.service-account-path=" + credentials.toUri());
        context.registerBean(Clock.class, () -> Clock.fixed(Instant.parse("2026-04-28T00:00:00Z"), ZoneOffset.UTC));
        context.scan("com.detoxmate.upload");
        context.refresh();
        uploads = context.getBean(UploadService.class);
        reads = context.getBean(ImageReadUrlBuilder.class);
    }

    @AfterAll
    void closeContext() {
        context.close();
    }

    @ParameterizedTest
    @EnumSource(UploadPurpose.class)
    void issuesSignedUploadAndReadUrlsForTheSameObject(UploadPurpose purpose) {
        long limit = purpose == UploadPurpose.PROFILE_IMAGE ? 5 * MIB : 10 * MIB;
        PresignedUrlRequest request = new PresignedUrlRequest("phone photo.png", "image/png", limit, purpose);
        PresignedUrlResponse response = uploads.issuePresignedUrl(7L, request);

        String key = response.objectKey();
        String directory = switch (purpose) {
            case PROFILE_IMAGE -> "profile-images/7/";
            case ACTIVITY_RECORD_IMAGE -> "activity-records/7/2026/04/";
            case SCREEN_TIME_OCR_REPORT_IMAGE -> "screen-time-ocr-reports/7/2026/04/";
        };
        assertThat(key).startsWith(directory).endsWith("-phone-photo.png");
        assertThat(response.expiresInSeconds()).isEqualTo(420);

        URI upload = URI.create(response.uploadUrl());
        assertGcsObject(upload, key);
        assertThat(upload.getQuery()).contains("X-Goog-Algorithm=GOOG4-RSA-SHA256", "X-Goog-Expires=420",
                "X-Goog-SignedHeaders=content-type;host", "X-Goog-Signature=");

        URI read = URI.create(reads.build(key));
        assertGcsObject(read, key);
        assertThat(read.getQuery()).contains("X-Goog-Algorithm=GOOG4-RSA-SHA256",
                "X-Goog-SignedHeaders=host", "X-Goog-Signature=");
    }

    @Test
    void keepsPersistedImageKeysReadableAtTheirOriginalLocation() {
        String key = "profile-images/7/d9052ef5-ca9a-473f-a7aa-ed6c89c51cb5-avatar.png";
        assertThat(reads.build(key)).isEqualTo(LEGACY_URL + "/" + key);
        assertThat(reads.build(null)).isNull();
    }

    @Test
    void rejectsOversizedOrUnsupportedImagesBeforeSigning() {
        var tooLarge = new PresignedUrlRequest("avatar.png", "image/png", 5 * MIB + 1, UploadPurpose.PROFILE_IMAGE);
        var unsupported = new PresignedUrlRequest("avatar.gif", "image/gif", 1L, UploadPurpose.PROFILE_IMAGE);
        for (var invalid : new PresignedUrlRequest[]{tooLarge, unsupported}) {
            assertThatThrownBy(() -> uploads.issuePresignedUrl(7L, invalid))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            failure -> assertThat(failure.getStatusCode().value()).isEqualTo(400));
        }
    }

    private void assertGcsObject(URI url, String key) {
        assertThat(url.getScheme()).isEqualTo("https");
        assertThat(url.getHost() + url.getPath()).isIn(
                "storage.googleapis.com/" + BUCKET + "/" + key,
                BUCKET + ".storage.googleapis.com/" + key);
    }
}
