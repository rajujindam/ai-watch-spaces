package com.aiwatchspaces.ai_watch_spaces.controller;

import com.aiwatchspaces.ai_watch_spaces.dto.TimelineEventResponse;
import com.aiwatchspaces.ai_watch_spaces.service.TimelineMetadataUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminTimelineMetadataController {

    private static final int MAX_UPLOAD_BYTES = 1_048_576; // 1 MiB

    private final TimelineMetadataUploadService uploadService;

    @PostMapping(
            value = "/timeline-metadata",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Mono<ResponseEntity<List<TimelineEventResponse>>> uploadMetadata(
            @RequestPart("file") FilePart file
    ) {

        return DataBufferUtils.join(file.content(), MAX_UPLOAD_BYTES)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .flatMap(content ->
                        Mono.fromCallable(() ->
                                        uploadService.upload(
                                                file.filename(),
                                                content
                                        )
                                )
                                .subscribeOn(Schedulers.boundedElastic())
                )
                .map(ResponseEntity::ok);
    }
}