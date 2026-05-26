package com.example.ibe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageInfo {

    private Integer page;
    private Integer size;
    private Integer totalElements;
    private Integer totalPages;
    private Boolean hasNext;
    private Boolean hasPrevious;
}