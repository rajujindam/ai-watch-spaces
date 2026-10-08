package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.dto.CreateTimelineMetadataRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.TimelineEventResponse;
import com.aiwatchspaces.ai_watch_spaces.exception.InvalidTimelineRequestException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TimelineMetadataUploadService {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final TimelineEventService timelineEventService;
    private final TimelineMetadataSchemaValidator schemaValidator;

    public List<TimelineEventResponse> upload(
            String filename,
            String content
    ) {
        CreateTimelineMetadataRequest request =
                parseFile(filename, content);

        validateRequest(request);

        schemaValidator.validate(request);

        return timelineEventService.createMetadata(request);
    }

    private CreateTimelineMetadataRequest parseFile(
            String filename,
            String content
    ) {
        try {
            String lowerCaseFilename = filename.toLowerCase();

            if (lowerCaseFilename.endsWith(".json")) {
                return objectMapper.readValue(
                        content,
                        CreateTimelineMetadataRequest.class
                );
            }

            if (lowerCaseFilename.endsWith(".yaml")
                    || lowerCaseFilename.endsWith(".yml")) {

                Yaml yaml = new Yaml();

                Object yamlData = yaml.load(content);

                if (!(yamlData instanceof Map)) {
                    throw new InvalidTimelineRequestException(
                            "Timeline metadata must contain a valid YAML object"
                    );
                }

                return objectMapper.convertValue(
                        yamlData,
                        CreateTimelineMetadataRequest.class
                );
            }

            throw new InvalidTimelineRequestException(
                    "Unsupported file format. Use .json, .yaml or .yml"
            );

        } catch (InvalidTimelineRequestException e) {
            throw e;

        } catch (Exception e) {
            throw new InvalidTimelineRequestException(
                    "Malformed timeline metadata file: " + e.getMessage()
            );
        }
    }

    private void validateRequest(
            CreateTimelineMetadataRequest request
    ) {
        Set<ConstraintViolation<CreateTimelineMetadataRequest>> violations =
                validator.validate(request);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .findFirst()
                    .orElse("Invalid timeline metadata");

            throw new InvalidTimelineRequestException(message);
        }
    }
}