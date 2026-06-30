package com.mingzhe.resumetailor.rag;

import com.mingzhe.resumetailor.education.Education;
import com.mingzhe.resumetailor.education.EducationMapper;
import com.mingzhe.resumetailor.experience.Experience;
import com.mingzhe.resumetailor.experience.ExperienceMapper;
import com.mingzhe.resumetailor.profile.Profile;
import com.mingzhe.resumetailor.profile.ProfileMapper;
import com.mingzhe.resumetailor.project.Project;
import com.mingzhe.resumetailor.project.ProjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
public class ResumeContextBuilderService {

    private static final DateTimeFormatter RESUME_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final ProfileMapper profileMapper;
    private final EducationMapper educationMapper;
    private final ExperienceMapper experienceMapper;
    private final ProjectMapper projectMapper;

    public ResumeContextBuilderService(
            ProfileMapper profileMapper,
            EducationMapper educationMapper,
            ExperienceMapper experienceMapper,
            ProjectMapper projectMapper
    ) {
        this.profileMapper = profileMapper;
        this.educationMapper = educationMapper;
        this.experienceMapper = experienceMapper;
        this.projectMapper = projectMapper;
    }

    public String buildResumeContext(Long userId, ResumeRetrievalResultDTO retrievalResult) {
        return buildResumeContext(userId, retrievalResult, false);
    }

    public String buildResumeContext(
            Long userId,
            ResumeRetrievalResultDTO retrievalResult,
            boolean includeDebugMetadata
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("User id cannot be null.");
        }

        StringBuilder builder = new StringBuilder();
        Profile profile = profileMapper.findByUserId(userId);

        appendCandidateProfile(builder, profile);
        appendEducation(builder, profile);
        appendExperienceAndProjects(
                builder,
                safeList(retrievalResult == null ? null : retrievalResult.getExperienceAndProjects()),
                includeDebugMetadata
        );
        appendSkills(
                builder,
                safeList(retrievalResult == null ? null : retrievalResult.getSkills()),
                includeDebugMetadata
        );

        return builder.toString().trim();
    }

    private void appendCandidateProfile(StringBuilder builder, Profile profile) {
        builder.append("CANDIDATE PROFILE\n");

        if (profile == null) {
            builder.append("Profile: Not found\n\n");
            return;
        }

        appendLabel(builder, "Name", profile.getFullName());
        appendLabel(builder, "Email", profile.getContactEmail());
        appendLabel(builder, "Phone", profile.getPhone());
        appendLabel(builder, "Location", profile.getLocation());
        appendLabel(builder, "LinkedIn", profile.getLinkedinUrl());
        appendLabel(builder, "GitHub", profile.getGithubUrl());
        appendLabel(builder, "Summary", profile.getSummary());
        builder.append('\n');
    }

    private void appendEducation(StringBuilder builder, Profile profile) {
        builder.append("EDUCATION\n");

        if (profile == null || profile.getId() == null) {
            builder.append("No education records available.\n\n");
            return;
        }

        List<Education> educations = educationMapper.findByProfileId(profile.getId());
        if (educations == null || educations.isEmpty()) {
            builder.append("No education records available.\n\n");
            return;
        }

        for (Education education : educations) {
            appendIfPresent(builder, education.getSchoolName());
            appendIfPresent(builder, joinNonBlank(", ", education.getDegree(), education.getMajor()));
            appendIfPresent(builder, formatDateRange(education.getStartDate(), education.getEndDate()));
            appendLabel(builder, "GPA", education.getGpa() == null ? null : education.getGpa().toPlainString());
            appendLabel(builder, "Relevant Coursework", formatDelimitedLine(education.getRelevantCoursework()));
            appendIfPresent(builder, education.getDescription());
            builder.append('\n');
        }
    }

    private void appendExperienceAndProjects(
            StringBuilder builder,
            List<RetrievedChunkDTO> chunks,
            boolean includeDebugMetadata
    ) {
        List<RetrievedChunkDTO> sortedChunks = chunks.stream()
                .sorted(Comparator.comparing(this::distanceValue))
                .toList();

        Map<Long, List<RetrievedChunkDTO>> experienceChunks = new LinkedHashMap<>();
        Map<Long, List<RetrievedChunkDTO>> projectChunks = new LinkedHashMap<>();

        for (RetrievedChunkDTO chunk : sortedChunks) {
            if (chunk.getSourceType() == EmbeddingSourceType.EXPERIENCE) {
                experienceChunks.computeIfAbsent(chunk.getSourceId(), ignored -> new ArrayList<>()).add(chunk);
            } else if (chunk.getSourceType() == EmbeddingSourceType.PROJECT) {
                projectChunks.computeIfAbsent(chunk.getSourceId(), ignored -> new ArrayList<>()).add(chunk);
            }
        }

        builder.append("RELEVANT EXPERIENCE\n");
        if (experienceChunks.isEmpty()) {
            builder.append("No relevant experience chunks retrieved.\n\n");
        } else {
            for (Map.Entry<Long, List<RetrievedChunkDTO>> entry : experienceChunks.entrySet()) {
                appendExperienceGroup(builder, entry.getKey(), entry.getValue(), includeDebugMetadata);
            }
        }

        builder.append("RELEVANT PROJECTS\n");
        if (projectChunks.isEmpty()) {
            builder.append("No relevant project chunks retrieved.\n\n");
        } else {
            for (Map.Entry<Long, List<RetrievedChunkDTO>> entry : projectChunks.entrySet()) {
                appendProjectGroup(builder, entry.getKey(), entry.getValue(), includeDebugMetadata);
            }
        }
    }

    private void appendExperienceGroup(
            StringBuilder builder,
            Long sourceId,
            List<RetrievedChunkDTO> chunks,
            boolean includeDebugMetadata
    ) {
        Experience experience = sourceId == null ? null : experienceMapper.findById(sourceId);
        if (includeDebugMetadata) {
            builder.append("[EXPERIENCE id=")
                    .append(sourceId)
                    .append(", distance=")
                    .append(formatDistance(bestDistance(chunks)))
                    .append("]\n");
        }

        if (experience == null) {
            builder.append("Unknown Experience");
            if (includeDebugMetadata) {
                builder.append(" sourceId=").append(sourceId);
            }
            builder.append('\n');
        } else {
            builder.append(joinNonBlank(" | ", experience.getCompanyName(), experience.getPosition())).append('\n');
            appendIfPresent(builder, joinNonBlank(" | ", experience.getLocation(), formatDateRange(experience.getStartDate(), experience.getEndDate())));
        }

        appendBullets(builder, chunks);
        builder.append('\n');
    }

    private void appendProjectGroup(
            StringBuilder builder,
            Long sourceId,
            List<RetrievedChunkDTO> chunks,
            boolean includeDebugMetadata
    ) {
        Project project = sourceId == null ? null : projectMapper.findById(sourceId);
        if (includeDebugMetadata) {
            builder.append("[PROJECT id=")
                    .append(sourceId)
                    .append(", distance=")
                    .append(formatDistance(bestDistance(chunks)))
                    .append("]\n");
        }

        if (project == null) {
            builder.append("Unknown Project");
            if (includeDebugMetadata) {
                builder.append(" sourceId=").append(sourceId);
            }
            builder.append('\n');
        } else {
            builder.append(project.getProjectName()).append('\n');
            appendIfPresent(builder, joinNonBlank(" | ", project.getTechStack(), formatDateRange(project.getStartDate(), project.getEndDate())));
        }

        appendBullets(builder, chunks);
        builder.append('\n');
    }

    private void appendSkills(StringBuilder builder, List<RetrievedChunkDTO> chunks, boolean includeDebugMetadata) {
        builder.append("RELEVANT SKILLS\n");

        List<RetrievedChunkDTO> sortedChunks = chunks.stream()
                .filter(chunk -> chunk.getSourceType() == EmbeddingSourceType.SKILL)
                .sorted(Comparator.comparing(this::distanceValue))
                .toList();

        if (sortedChunks.isEmpty()) {
            builder.append("No relevant skill chunks retrieved.\n");
            return;
        }

        for (RetrievedChunkDTO chunk : sortedChunks) {
            if (includeDebugMetadata) {
                builder.append("[SKILL id=")
                        .append(chunk.getSourceId())
                        .append(", distance=")
                        .append(formatDistance(chunk.getDistance()))
                        .append("]\n");
            }
            appendBullet(builder, chunk.getContentText());
        }
    }

    private void appendBullets(StringBuilder builder, List<RetrievedChunkDTO> chunks) {
        chunks.stream()
                .sorted(Comparator.comparing(this::distanceValue))
                .forEach(chunk -> appendBullet(builder, chunk.getContentText()));
    }

    private void appendBullet(StringBuilder builder, String value) {
        if (hasText(value)) {
            builder.append("- ").append(value.trim()).append('\n');
        }
    }

    private void appendLabel(StringBuilder builder, String label, String value) {
        if (hasText(value)) {
            builder.append(label).append(": ").append(value.trim()).append('\n');
        }
    }

    private void appendIfPresent(StringBuilder builder, String value) {
        if (hasText(value)) {
            builder.append(value.trim()).append('\n');
        }
    }

    private String joinNonBlank(String separator, String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (hasText(value)) {
                parts.add(value.trim());
            }
        }
        return String.join(separator, parts);
    }

    private String formatDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "";
        }

        String start = startDate == null ? "" : startDate.format(RESUME_DATE_FORMATTER);
        String end = endDate == null ? "Present" : endDate.format(RESUME_DATE_FORMATTER);
        if (start.isBlank()) {
            return end;
        }
        return start + " - " + end;
    }

    private String formatDelimitedLine(String value) {
        if (!hasText(value)) {
            return "";
        }

        String[] rawParts = value.split("[,;\\r\\n]+");
        List<String> parts = new ArrayList<>();
        for (String part : rawParts) {
            if (hasText(part)) {
                parts.add(part.trim());
            }
        }

        return String.join(", ", parts);
    }

    private Double bestDistance(List<RetrievedChunkDTO> chunks) {
        return chunks.stream()
                .map(RetrievedChunkDTO::getDistance)
                .filter(distance -> distance != null)
                .min(Double::compareTo)
                .orElse(null);
    }

    private Double distanceValue(RetrievedChunkDTO chunk) {
        return chunk.getDistance() == null ? Double.MAX_VALUE : chunk.getDistance();
    }

    private String formatDistance(Double distance) {
        if (distance == null) {
            return "null";
        }
        return String.format(Locale.ENGLISH, "%.4f", distance);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private List<RetrievedChunkDTO> safeList(List<RetrievedChunkDTO> chunks) {
        return chunks == null ? List.of() : chunks;
    }
}
