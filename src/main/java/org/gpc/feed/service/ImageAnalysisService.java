package org.gpc.feed.service;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImageAnalysisService {

    private final ImageAnnotatorClient visionClient;
    private final Storage storage;

    public ImageAnalysisService() {
        // Initialize GCP Vision API client
        this.visionClient = createVisionClient();

        // Initialize GCP Storage client
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    private ImageAnnotatorClient createVisionClient() {
        try {
            return ImageAnnotatorClient.create();
        } catch (Exception e) {
            throw new RuntimeException("Error creating Vision API client: " + e.getMessage());
        }
    }

    public List<String> analyzeImages(String bucketName, String folderName) {
        List<String> imageNotAvailableList = new ArrayList<>();
        // Logic to get a list of image content from GCS bucket and folder
        for (Map.Entry<String,ByteString> entry : getImageContent(bucketName, folderName).entrySet()) {
            String content = analyzeImage(entry.getValue(), entry.getKey());
            if(StringUtils.hasText(content)) {
                imageNotAvailableList.add(content);
            }
        }

        System.out.println("Analysis completed. Check logs for details.");
        return imageNotAvailableList;
    }

    private String analyzeImage(ByteString imageContent,String imageName) {
        try {
            // Use Vision API to analyze image
            Image image = Image.newBuilder().setContent(imageContent).build();

            // Perform label detection
            AnnotateImageRequest request =
                    AnnotateImageRequest.newBuilder()
                            .setImage(image)
                            .addFeatures(Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build())
                            .build();

            BatchAnnotateImagesResponse response = visionClient.batchAnnotateImages(List.of(request));
            List<EntityAnnotation> labelAnnotations = response.getResponses(0).getTextAnnotationsList();

            // Process label annotations as needed
            for (EntityAnnotation labelAnnotation : labelAnnotations) {
                String label = labelAnnotation.getDescription().replace("\n", "");
                if(!"ImageNotAvailable".equalsIgnoreCase(label)){
                    return imageName;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error analyzing image: " + e.getMessage());
        }
        return "";
    }

    private Map<String,ByteString> getImageContent(String bucketName, String folderName) {
        // Logic to list objects in the specified GCS bucket and folder
        List<ByteString> imageContentList = new ArrayList<>();
        Map<String,ByteString> imageMap = new HashMap<>();
        Page<Blob> blobs = storage.list(bucketName, Storage.BlobListOption.prefix(folderName));
        for (Blob blob : blobs.iterateAll()) {
            // Filter objects by type (image/jpeg)
            if ("image/jpeg".equals(blob.getContentType())) {
                // Get image content directly from Blob
                System.out.println(blob.getBlobId().getName());
                ByteString imageContent = ByteString.copyFrom(blob.getContent());
                imageMap.put(blob.getName(),imageContent);
            }
        }

        return imageMap;
    }
}
