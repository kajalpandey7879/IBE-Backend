package com.example.ibe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppliedFilterInput {

    @NotBlank(message = "filterId must not be blank")
    private String filterId;

    @Valid
    private RangeFilterInput selectedRange;
    private List<String> selectedValues;
}