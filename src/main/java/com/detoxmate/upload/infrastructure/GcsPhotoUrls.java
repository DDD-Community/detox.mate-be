package com.detoxmate.upload.infrastructure;

import com.detoxmate.upload.config.StorageGcsProperties;
import com.detoxmate.upload.config.StorageProperties;
import com.detoxmate.upload.dto.PresignedUrlResponse;
import com.detoxmate.upload.service.ImageReadUrlBuilder;
import com.detoxmate.upload.service.PhotoUploadSigner;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
@Profile("prod")
public class GcsPhotoUrls extends ImageReadUrlBuilder implements PhotoUploadSigner {

    // Previous uploads placed the UUID directly below the user or month directory.
    private static final String GCS_DIRECTORY = "gcs/";
    private static final Pattern GCS_PHOTO_KEY = Pattern.compile(
            "[a-z-]+/[0-9]+/(?:[0-9]{4}/[0-9]{2}/)?" + GCS_DIRECTORY + "[0-9a-f-]{36}-[a-zA-Z0-9._-]+");

    private final Storage storage;
    private final StorageGcsProperties properties;

    public GcsPhotoUrls(StorageProperties legacy, Storage storage, StorageGcsProperties properties) {
        super(legacy);
        this.storage = storage;
        this.properties = properties;
    }

    @Override
    public PresignedUrlResponse signUpload(String objectKey, String contentType) {
        int fileNameStart = objectKey.lastIndexOf('/') + 1;
        String key = objectKey.substring(0, fileNameStart) + GCS_DIRECTORY + objectKey.substring(fileNameStart);
        BlobInfo object = BlobInfo.newBuilder(properties.bucketName(), key).setContentType(contentType).build();
        String url = storage.signUrl(object, properties.presignedUrlExpiresIn(), TimeUnit.SECONDS,
                Storage.SignUrlOption.withV4Signature(), Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withExtHeaders(Map.of("Content-Type", contentType))).toString();
        return new PresignedUrlResponse(url, key, properties.presignedUrlExpiresIn());
    }

    @Override
    public String build(String objectKey) {
        return isGcsPhoto(objectKey) ? signedReadUrl(objectKey) : super.build(objectKey);
    }

    @Override
    public String buildForLegacyFeed(String objectKey) {
        return isGcsPhoto(objectKey) ? signedReadUrl(objectKey) : super.buildForLegacyFeed(objectKey);
    }

    private boolean isGcsPhoto(String objectKey) {
        return objectKey != null && GCS_PHOTO_KEY.matcher(objectKey).matches();
    }

    private String signedReadUrl(String objectKey) {
        BlobInfo object = BlobInfo.newBuilder(properties.bucketName(), objectKey).build();
        return storage.signUrl(object, 7, TimeUnit.DAYS, Storage.SignUrlOption.withV4Signature(),
                Storage.SignUrlOption.httpMethod(HttpMethod.GET)).toString();
    }
}
