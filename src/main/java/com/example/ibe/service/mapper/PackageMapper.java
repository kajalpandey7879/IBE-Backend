package com.example.ibe.service.mapper;

import com.example.ibe.dto.PackageResponse;
import com.example.ibe.entity.Package;
import java.util.List;
import java.util.stream.Collectors;

public class PackageMapper {
    public static PackageResponse toResponse(Package pkg) {
        return new PackageResponse(
            pkg.getPackageName(),
            pkg.getPackageDesc(),
            java.math.BigDecimal.valueOf(pkg.getDiscountPercentage())
        );
    }

    public static List<PackageResponse> toResponseList(List<Package> packages) {
        return packages.stream().map(PackageMapper::toResponse).collect(Collectors.toList());
    }
}
