package com.example.ibe.service;

import com.example.ibe.dto.AvailableRoomTypeSummary;
import com.example.ibe.dto.AppliedFilterInput;
import com.example.ibe.dto.AvailableRoomTypesRequest;
import com.example.ibe.dto.AvailableRoomTypesResponse;
import com.example.ibe.dto.AmenitySummary;
import com.example.ibe.dto.PageInfo;
import com.example.ibe.dto.PropertyFilterDefinition;
import com.example.ibe.dto.PropertyRangeFilterConfig;
import com.example.ibe.dto.RangeFilterInput;
import com.example.ibe.dto.RoomAvailabilitySearchCriteria;
import com.example.ibe.dto.enums.FilterType;
import com.example.ibe.entity.PropertyFilter;
import com.example.ibe.entity.RoomType;
import com.example.ibe.exception.BadRequestException;
import com.example.ibe.exception.DataProcessingException;
import com.example.ibe.repository.BookingAvailabilityRepository;
import com.example.ibe.repository.PropertyFilterRepository;
import com.example.ibe.repository.RoomTypeImageRepository;
import com.example.ibe.repository.RoomTypeRepository;
import com.example.ibe.service.filter.AndRoomTypeFilter;
import com.example.ibe.service.filter.AreaRangeRoomTypeFilter;
import com.example.ibe.service.filter.AmenitiesRoomTypeFilter;
import com.example.ibe.service.filter.BedTypeRoomTypeFilter;
import com.example.ibe.service.filter.MaxOccupancyRangeRoomTypeFilter;
import com.example.ibe.service.filter.PriceRangeRoomTypeFilter;
import com.example.ibe.service.filter.RoomTypeFilter;
import com.example.ibe.service.mapper.PropertyFilterConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomSearchService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 4;

    private static final String FILTER_NAME_AMENITIES = "amenities";
    private static final String FILTER_NAME_BED_TYPE = "bed type";
    private static final String FILTER_NAME_AREA = "area";
    private static final String FILTER_NAME_MAX_OCCUPANCY = "max occupancy";

    private final PropertyFilterRepository propertyFilterRepository;
    private final BookingAvailabilityRepository bookingAvailabilityRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypeImageRepository roomTypeImageRepository;
    private final ValidationService validationService;
    private final JsonParsingService jsonParsingService;

    /**
     * Fetches available room types for the requested stay, applies supported
     * filters and sorting, and returns a paginated search response.
     *
     * @param request the availability search request containing stay, guest, filter, and paging inputs
     * @return the paginated room type search result for the requested property
     */
    @Transactional(readOnly = true)
    public AvailableRoomTypesResponse getAvailableRoomTypes(AvailableRoomTypesRequest request) {
        validateRequest(request);
        int page = normalizedPage(request);
        int size = normalizedSize(request);
        UUID tenantId = validationService.parseUuid(request.getTenantId(), "tenantId");
        UUID propertyId = validationService.parseUuid(request.getPropertyId(), "propertyId");

        log.info(
                "Fetching available room types for tenantId={}, propertyId={}, checkInDate={}, checkOutDate={}, requiredRooms={}, requestedGuests={}, page={}, size={}",
                tenantId, propertyId, request.getCheckInDate(), request.getCheckOutDate(), request.getRequiredRooms(),
                request.getRequestedGuests(), page, size);

        validationService.validatePropertyBelongsToTenant(propertyId, tenantId);

        long nightCount = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());

        List<UUID> availableRoomTypeIds;
        List<RoomType> availableRoomTypes;
        List<PropertyFilter> propertyFilters;
        try {
            availableRoomTypeIds = bookingAvailabilityRepository.findAvailableRoomTypeIds(
                    propertyId,
                    request.getCheckInDate(),
                    request.getCheckOutDate(),
                    request.getRequiredRooms(),
                    nightCount);
            availableRoomTypes = availableRoomTypeIds.isEmpty()
                    ? List.of()
                    : roomTypeRepository.findAvailableRoomTypesByIds(
                            propertyId,
                            availableRoomTypeIds,
                            request.getRequiredRooms(),
                            request.getRequestedGuests());
            propertyFilters = propertyFilterRepository.findByProperty_PropertyIdOrderByFilterNameAsc(propertyId);
        } catch (DataAccessException ex) {
            throw new DataProcessingException("Failed to fetch available room types", ex);
        }

        int multiplier = (int) (nightCount * request.getRequiredRooms());
        List<AvailableRoomTypeSummary> allSummaries = availableRoomTypes.stream()
                .map(rt -> mapRoomTypeSummary(rt, multiplier))
                .toList();

        List<AvailableRoomTypeSummary> filteredSummaries = applyRequestedFilters(
                allSummaries,
                propertyFilters,
                request.getAppliedFilters());

        List<AvailableRoomTypeSummary> sortedSummaries = filteredSummaries;
        if (request.getSortType() != null) {
            switch (request.getSortType()) {
                case ASCE -> sortedSummaries = filteredSummaries.stream()
                        .sorted(java.util.Comparator.comparing(AvailableRoomTypeSummary::getBasePrice))
                        .toList();
                case DESC -> sortedSummaries = filteredSummaries.stream()
                        .sorted(java.util.Comparator.comparing(AvailableRoomTypeSummary::getArea).reversed())
                        .toList();
            }
        }

        List<AvailableRoomTypeSummary> pagedRoomTypes = paginate(sortedSummaries, page, size);

        return AvailableRoomTypesResponse.builder()
                .searchCriteria(buildSearchCriteria(request, propertyId))
                .filters(propertyFilters.stream().map(f -> mapFilterDefinition(f, multiplier)).toList())
                .roomTypes(pagedRoomTypes)
            .pageInfo(buildPageInfo(sortedSummaries.size(), page, size))
            .build();
    }

    /**
     * Validates the required search inputs before availability is queried.
     *
     * @param request the availability search request to validate
     */
    private void validateRequest(AvailableRoomTypesRequest request) {
        if (request == null) {
            throw new BadRequestException("request must not be null");
        }
        if (request.getRequiredRooms() == null) {
            throw new BadRequestException("requiredRooms must not be null");
        }
        if (request.getRequiredRooms() < 1) {
            throw new BadRequestException("requiredRooms must be at least 1");
        }
        if (request.getRequestedGuests() == null) {
            throw new BadRequestException("requestedGuests must not be null");
        }
        if (request.getRequestedGuests() < 1) {
            throw new BadRequestException("requestedGuests must be at least 1");
        }
        validationService.validateDateRangeStrict(
                request.getCheckInDate(),
                request.getCheckOutDate(),
                "checkInDate",
                "checkOutDate");
    }

    private int normalizedPage(AvailableRoomTypesRequest request) {
        return request.getPage() != null ? request.getPage() : DEFAULT_PAGE;
    }

    private int normalizedSize(AvailableRoomTypesRequest request) {
        return request.getSize() != null ? request.getSize() : DEFAULT_SIZE;
    }

    /**
     * Builds the response search criteria block from the validated request values.
     *
     * @param request the availability search request
     * @param propertyId the validated property identifier
     * @return the search criteria included in the response
     */
    private RoomAvailabilitySearchCriteria buildSearchCriteria(AvailableRoomTypesRequest request, UUID propertyId) {
        return RoomAvailabilitySearchCriteria.builder()
                .propertyId(propertyId)
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .requiredRooms(request.getRequiredRooms())
                .requestedGuests(request.getRequestedGuests())
                .build();
    }

    /**
     * Maps a room type entity into the summary model returned by room search.
     *
     * @param roomType the room type to map
     * @param multiplier the stay-based price multiplier for the requested search
     * @return the mapped room type summary
     */
    private AvailableRoomTypeSummary mapRoomTypeSummary(RoomType roomType, int multiplier) {
        return AvailableRoomTypeSummary.builder()
            .roomTypeId(roomType.getRoomTypeId())
            .roomTypeName(roomType.getRoomTypeName())
            .area(roomType.getArea())
                .maxOccupancy(roomType.getMaxOccupancy())
                .bedTypes(jsonParsingService.parseJsonNode(
                        roomType.getBedTypes(), "Invalid bed types configuration"))
                .basePrice(roomType.getBasePrice().multiply(java.math.BigDecimal.valueOf(multiplier)))
                .amenities(roomType.getAmenities() != null
                        ? roomType.getAmenities().stream()
                                .map(amenity -> AmenitySummary.builder()
                                        .amenityId(amenity.getAmenityId())
                                        .amenityName(amenity.getAmenityName())
                                        .build())
                                .toList()
                        : List.of())
                .imageUrls(roomTypeImageRepository.findByRoomType_RoomTypeId(roomType.getRoomTypeId())
                        .stream()
                        .map(image -> image.getImageUrl())
                        .toList())
            .mealPlan(roomType.getMealPlan())
            .build();
    }

    /**
     * Maps a property filter entity into the filter definition returned to clients.
     *
     * @param propertyFilter the property filter configuration
     * @param multiplier the stay-based price multiplier for price ranges
     * @return the mapped filter definition
     */
    private PropertyFilterDefinition mapFilterDefinition(PropertyFilter propertyFilter, int multiplier) {
        JsonNode config = jsonParsingService.parseJsonNode(
                propertyFilter.getConfigJson(), "Invalid property filter configuration");

        PropertyRangeFilterConfig rangeConfig = PropertyFilterConfigMapper
                .mapRangeConfig(propertyFilter.getFilterType(), config);

        if (rangeConfig != null && "price".equalsIgnoreCase(propertyFilter.getFilterName())) {
            BigDecimal multiplierBd = BigDecimal.valueOf(multiplier);
            if (rangeConfig.getMin() != null) {
                rangeConfig.setMin(rangeConfig.getMin().multiply(multiplierBd));
            }
            if (rangeConfig.getMax() != null) {
                rangeConfig.setMax(rangeConfig.getMax().multiply(multiplierBd));
            }
            if (rangeConfig.getStep() != null) {
                rangeConfig.setStep(rangeConfig.getStep().multiply(multiplierBd));
            }
        }

        return PropertyFilterDefinition.builder()
                .filterId(propertyFilter.getFilterId())
                .filterName(propertyFilter.getFilterName())
                .filterType(propertyFilter.getFilterType())
                .rangeConfig(rangeConfig)
                .options(PropertyFilterConfigMapper.mapFilterOptions(config))
                .build();
    }

    /**
     * Applies the requested client filters to the available room type summaries.
     *
     * @param roomTypes the room type summaries to filter
     * @param propertyFilters the supported property filters
     * @param appliedFilters the filters requested by the client
     * @return the filtered room type summaries
     */
    private List<AvailableRoomTypeSummary> applyRequestedFilters(
            List<AvailableRoomTypeSummary> roomTypes,
            List<PropertyFilter> propertyFilters,
            List<AppliedFilterInput> appliedFilters) {
        if (appliedFilters == null || appliedFilters.isEmpty()) {
            return roomTypes;
        }

        Map<UUID, PropertyFilter> availableFilterMap = new HashMap<>();
        for (PropertyFilter propertyFilter : propertyFilters) {
            availableFilterMap.put(propertyFilter.getFilterId(), propertyFilter);
        }

        List<RoomTypeFilter> activeFilters = new ArrayList<>();
        for (AppliedFilterInput appliedFilter : appliedFilters) {
            UUID filterId = validationService.parseUuid(appliedFilter.getFilterId(), "appliedFilters.filterId");
            PropertyFilter propertyFilter = availableFilterMap.get(filterId);
            if (propertyFilter == null) {
                throw new BadRequestException("Unsupported filterId: " + appliedFilter.getFilterId());
            }

            RoomTypeFilter roomTypeFilter = createFilter(propertyFilter, appliedFilter);
            if (roomTypeFilter != null) {
                activeFilters.add(roomTypeFilter);
            }
        }

        return new AndRoomTypeFilter(activeFilters).apply(roomTypes);
    }

    /**
     * Creates the concrete room type filter for an applied filter definition.
     *
     * @param propertyFilter the configured property filter
     * @param appliedFilter the client-provided filter value
     * @return the matching room type filter, or {@code null} when no filtering is needed
     */
    private RoomTypeFilter createFilter(PropertyFilter propertyFilter, AppliedFilterInput appliedFilter) {
        validateAppliedFilterValue(propertyFilter, appliedFilter);

        if (propertyFilter.getFilterType() == FilterType.CHECKBOX
                && FILTER_NAME_AMENITIES.equalsIgnoreCase(propertyFilter.getFilterName())) {
            return createAmenitiesFilter(appliedFilter.getSelectedValues());
        }

        if (propertyFilter.getFilterType() == FilterType.CHECKBOX
                && FILTER_NAME_BED_TYPE.equalsIgnoreCase(propertyFilter.getFilterName())) {
            return createBedTypeFilter(appliedFilter.getSelectedValues());
        }

        if (propertyFilter.getFilterType() == FilterType.RANGE) {
            if (FILTER_NAME_AREA.equalsIgnoreCase(propertyFilter.getFilterName())) {
                return createAreaRangeFilter(appliedFilter.getSelectedRange());
            }
            if (FILTER_NAME_MAX_OCCUPANCY.equalsIgnoreCase(propertyFilter.getFilterName())) {
                return createMaxOccupancyRangeFilter(appliedFilter.getSelectedRange());
            }
            return createPriceRangeFilter(appliedFilter.getSelectedRange());
        }

        throw new BadRequestException("No filter handler configured for filter: " + propertyFilter.getFilterName());
    }

    /**
     * Verifies that the applied filter value shape matches the configured filter type.
     *
     * @param propertyFilter the configured property filter
     * @param appliedFilter the client-provided filter value
     */
    private void validateAppliedFilterValue(PropertyFilter propertyFilter, AppliedFilterInput appliedFilter) {
        if (propertyFilter.getFilterType() == FilterType.RANGE && appliedFilter.getSelectedValues() != null) {
            throw new BadRequestException(
                    "Filter '" + propertyFilter.getFilterName() + "' expects selectedRange");
        }

        if (propertyFilter.getFilterType() != FilterType.RANGE && appliedFilter.getSelectedRange() != null) {
            throw new BadRequestException(
                    "Filter '" + propertyFilter.getFilterName() + "' expects selectedValues");
        }
    }

    /**
     * Creates an amenities filter from the selected amenity names.
     *
     * @param selectedValues the requested amenity values
     * @return the amenities filter, or {@code null} when no values are provided
     */
    private RoomTypeFilter createAmenitiesFilter(List<String> selectedValues) {
        if (selectedValues == null || selectedValues.isEmpty()) {
            return null;
        }

        Set<String> selectedAmenities = new HashSet<>(selectedValues);
        if (selectedAmenities.isEmpty()) {
            return null;
        }

        return new AmenitiesRoomTypeFilter(selectedAmenities);
    }

    /**
     * Creates a bed type filter from the selected bed type values.
     *
     * @param selectedValues the requested bed type values
     * @return the bed type filter, or {@code null} when no values are provided
     */
    private RoomTypeFilter createBedTypeFilter(List<String> selectedValues) {
        if (selectedValues == null || selectedValues.isEmpty()) {
            return null;
        }

        Set<String> selectedBedTypes = new HashSet<>(selectedValues);
        if (selectedBedTypes.isEmpty()) {
            return null;
        }

        return new BedTypeRoomTypeFilter(selectedBedTypes);
    }

    /**
     * Creates a price range filter from the selected minimum and maximum values.
     *
     * @param rangeValue the requested price range
     * @return the price range filter, or {@code null} when no bounds are provided
     */
    private RoomTypeFilter createPriceRangeFilter(RangeFilterInput rangeValue) {
        if (rangeValue == null) {
            return null;
        }

        BigDecimal minPrice = rangeValue.getMin();
        BigDecimal maxPrice = rangeValue.getMax();

        if (minPrice == null && maxPrice == null) {
            return null;
        }

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("priceRange min must be less than or equal to max");
        }

        return new PriceRangeRoomTypeFilter(minPrice, maxPrice);
    }

    /**
     * Creates an area range filter from the selected minimum and maximum values.
     *
     * @param rangeValue the requested area range
     * @return the area range filter, or {@code null} when no bounds are provided
     */
    private RoomTypeFilter createAreaRangeFilter(RangeFilterInput rangeValue) {
        if (rangeValue == null) {
            return null;
        }

        BigDecimal minArea = rangeValue.getMin();
        BigDecimal maxArea = rangeValue.getMax();

        if (minArea == null && maxArea == null) {
            return null;
        }

        if (minArea != null && maxArea != null && minArea.compareTo(maxArea) > 0) {
            throw new BadRequestException("areaRange min must be less than or equal to max");
        }

        return new AreaRangeRoomTypeFilter(minArea, maxArea);
    }

    /**
     * Creates a max occupancy filter from the selected minimum and maximum values.
     *
     * @param rangeValue the requested occupancy range
     * @return the max occupancy filter, or {@code null} when no bounds are provided
     */
    private RoomTypeFilter createMaxOccupancyRangeFilter(RangeFilterInput rangeValue) {
        if (rangeValue == null) {
            return null;
        }

        BigDecimal minOccupancy = rangeValue.getMin();
        BigDecimal maxOccupancy = rangeValue.getMax();

        if (minOccupancy == null && maxOccupancy == null) {
            return null;
        }

        if (minOccupancy != null && maxOccupancy != null && minOccupancy.compareTo(maxOccupancy) > 0) {
            throw new BadRequestException("maxOccupancyRange min must be less than or equal to max");
        }

        return new MaxOccupancyRangeRoomTypeFilter(minOccupancy, maxOccupancy);
    }

    /**
     * Returns the requested page slice from the filtered room type summaries.
     *
     * @param roomTypes the room type summaries to paginate
     * @param page the zero-based page number
     * @param size the page size
     * @return the paginated room type summaries
     */
    private List<AvailableRoomTypeSummary> paginate(List<AvailableRoomTypeSummary> roomTypes, int page, int size) {
        int fromIndex = page * size;
        if (fromIndex >= roomTypes.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + size, roomTypes.size());
        return new ArrayList<>(roomTypes.subList(fromIndex, toIndex));
    }

    /**
     * Builds pagination metadata for the search response.
     *
     * @param totalElements the total number of filtered room types
     * @param page the current zero-based page number
     * @param size the current page size
     * @return the response page metadata
     */
    private PageInfo buildPageInfo(int totalElements, int page, int size) {
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page + 1 < totalPages;
        boolean hasPrevious = page > 0 && totalPages > 0;

        return PageInfo.builder()
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
    }
}
