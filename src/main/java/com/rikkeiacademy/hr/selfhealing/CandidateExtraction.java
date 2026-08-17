package com.rikkeiacademy.hr.selfhealing;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "fullName",
        "phone",
        "email",
        "skills",
        "yearsExperience"
})
public record CandidateExtraction(
        String fullName,
        String phone,
        String email,
        List<String> skills,
        int yearsExperience
) {

    public static CandidateExtraction fallback() {
        return new CandidateExtraction(
                "UNKNOWN",
                "",
                "",
                List.of(),
                0
        );
    }
}
