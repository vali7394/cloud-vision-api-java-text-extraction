package org.gpc.feed.controller;

import org.gpc.feed.service.ImageAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ImageAnalysisController {

    private final ImageAnalysisService imageAnalysisService;

    @Autowired
    public ImageAnalysisController(ImageAnalysisService imageAnalysisService) {
        this.imageAnalysisService = imageAnalysisService;
    }

    @GetMapping("/analyze-images")
    public List<String> analyzeImages(@RequestParam String bucketName, @RequestParam String folderName) {
        return imageAnalysisService.analyzeImages(bucketName, folderName);
    }
}

