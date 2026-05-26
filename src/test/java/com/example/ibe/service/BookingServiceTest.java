package com.example.ibe.service;

import com.example.ibe.dto.StayPricingResult;
import com.example.ibe.dto.booking.*;
import com.example.ibe.entity.Property;
import com.example.ibe.entity.RoomType;
import com.example.ibe.entity.Booking;
import com.example.ibe.entity.PaymentInfo;
import com.example.ibe.entity.RoomTypeImage;
import com.example.ibe.entity.Tenant;
import com.example.ibe.dto.enums.PaymentStatus;
import com.example.ibe.dto.enums.PaymentMethod;
import com.example.ibe.exception.BookingNotFoundException;
import com.example.ibe.exception.PriceChangedException;
import com.example.ibe.exception.RoomNotAvailableException;
import com.example.ibe.repository.BookingRepository;
import com.example.ibe.repository.RoomTypeImageRepository;
import com.example.ibe.service.PricingService.BookingPriceCalculation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class BookingServiceTest {

    @Mock
    private ValidationService validationService;
    
    @Mock
    private RoomTypeService roomTypeService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private com.example.ibe.repository.RoomRepository roomRepository;

    @Mock
    private RoomTypeImageRepository roomTypeImageRepository;

    @Spy
    private PricingService pricingService = new PricingService();

    @Mock
    private StayPricingService stayPricingService;

    @Mock
    private PaymentProcessorService paymentProcessorService;

    @Mock
    private BookingEmailService bookingEmailService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private JsonParsingService jsonParsingService = new JsonParsingService(objectMapper);

    @InjectMocks
    private BookingService bookingService;

    private CreateBookingRequest baseRequest;
    private Property property;
    private RoomType roomType;
    private UUID tenantId = UUID.randomUUID();
    private UUID propertyId = UUID.randomUUID();
    private UUID roomTypeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        baseRequest = CreateBookingRequest.builder()
                .tenantId(tenantId.toString())
                .propertyId(propertyId.toString())
                .roomTypeId(roomTypeId.toString())
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(3))
                .roomCount(1)
                .packageChosen("Standard Rate")
                .guests(java.util.Map.of("adults", 2))
                .traveler(TravelerInputDTO.builder().firstName("John").lastName("Doe").email("j@test.com").phone("123").build())
                .payment(PaymentInputDTO.builder().cardNumber("1111222233334444").expMonth("12").expYear("2030").build())
                .pricing(BookingPricingInputDTO.builder()
                        .subtotal(new BigDecimal("200.00"))
                        .taxesAndCharges(new BigDecimal("30.00"))
                        .dueAtResort(BigDecimal.ZERO)
                        .dueNow(new BigDecimal("230.00"))
                        .currency("USD")
                        .build())
                .build();

        property = new Property();
        property.setPropertyId(propertyId);
        property.setPropertyName("Radisson Blu Bengaluru");
        property.setDuePercentage(BigDecimal.ZERO);

        roomType = new RoomType();
        roomType.setRoomTypeId(roomTypeId);
        roomType.setRoomTypeName("Deluxe");
        roomType.setBasePrice(new BigDecimal("100.00"));
        roomType.setProperty(property);
    }

    @Test
    void createBooking_Success() {
        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        when(validationService.parseUuid(roomTypeId.toString(), "roomTypeId")).thenReturn(roomTypeId);
        
        when(validationService.validateRoomTypeBelongsToProperty(roomTypeId, propertyId)).thenReturn(roomType);

        when(roomTypeService.isRoomTypeAvailable(eq(propertyId), eq(roomTypeId), any(), any(), eq(1), eq(2L)))
                .thenReturn(true);

        StayPricingResult calc = StayPricingResult.builder()
                .subtotal(new BigDecimal("200.00"))
                .taxesAndCharges(new BigDecimal("30.00"))
                .dueAtResort(BigDecimal.ZERO)
                .dueNow(new BigDecimal("230.00"))
                .build();
        when(stayPricingService.calculateStayPricing(eq(property), eq(roomType.getBasePrice()), any(), any(), eq(1), eq("Standard Rate")))
                .thenReturn(calc);

        when(roomRepository.findAndLockAvailablePhysicalRooms(any(), any(), any(), eq(1)))
                .thenReturn(java.util.List.of(UUID.randomUUID()));

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentId(UUID.randomUUID());
        paymentInfo.setPaymentStatus(PaymentStatus.PAID);
        paymentInfo.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentInfo.setLastFourDigits("4444");
        when(paymentProcessorService.processPayment(any(), any(), any())).thenReturn(paymentInfo);

        RoomTypeImage roomTypeImage = new RoomTypeImage();
        roomTypeImage.setImageUrl("https://example.com/rooms/deluxe-1.jpg");
        when(roomTypeImageRepository.findByRoomType_RoomTypeId(roomTypeId))
                .thenReturn(java.util.List.of(roomTypeImage));

        Booking mockBooking = new Booking();
        mockBooking.setBookingId(UUID.randomUUID());
        mockBooking.setTravellerInfo(new com.example.ibe.entity.TravellerInfo());
        mockBooking.getTravellerInfo().setEmail("j@test.com");
        mockBooking.setPaymentInfo(paymentInfo);
        mockBooking.setBookingStatus(com.example.ibe.dto.enums.BookingStatus.CONFIRMED);
        mockBooking.setRoomType(roomType);
        mockBooking.setProperty(property);
        mockBooking.setCheckInDate(baseRequest.getCheckInDate());
        mockBooking.setCheckOutDate(baseRequest.getCheckOutDate());
        mockBooking.setNoOfGuests(2);
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);

        BookingConfirmationResponse res = bookingService.createBooking(baseRequest);

        assertNotNull(res);
        assertEquals("Deluxe", res.getRoomTypeName());
        assertEquals(java.util.List.of("https://example.com/rooms/deluxe-1.jpg"), res.getImageUrls());
        assertEquals("PAID", res.getPayment().getStatus());
        
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        
        Booking saved = bookingCaptor.getValue();
        assertEquals(new BigDecimal("200.00"), saved.getTotalPrice());
        assertEquals(2, saved.getNoOfGuests());
        assertEquals(2, saved.getGuestsJson().get("adults").asInt());
        verify(bookingEmailService).sendBookingConfirmation(any());
    }

    @Test
    void createBooking_ThrowsWhenRoomNotAvailable() {
        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        when(validationService.parseUuid(roomTypeId.toString(), "roomTypeId")).thenReturn(roomTypeId);
        
        when(validationService.validateRoomTypeBelongsToProperty(roomTypeId, propertyId)).thenReturn(roomType);

        when(roomTypeService.isRoomTypeAvailable(eq(propertyId), eq(roomTypeId), any(), any(), eq(1), eq(2L)))
                .thenReturn(false); // Not available!

        RoomNotAvailableException ex = assertThrows(RoomNotAvailableException.class, () -> bookingService.createBooking(baseRequest));
        assertTrue(ex.getMessage().contains("ROOM_NOT_AVAILABLE"));
        
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_ThrowsWhenPriceMismatched() {
        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        when(validationService.parseUuid(roomTypeId.toString(), "roomTypeId")).thenReturn(roomTypeId);
        
        when(validationService.validateRoomTypeBelongsToProperty(roomTypeId, propertyId)).thenReturn(roomType);

        when(roomTypeService.isRoomTypeAvailable(eq(propertyId), eq(roomTypeId), any(), any(), eq(1), eq(2L)))
                .thenReturn(true);

        // Backend says price is 250! Max difference
        StayPricingResult calc = StayPricingResult.builder()
                .subtotal(new BigDecimal("250.00"))
                .taxesAndCharges(new BigDecimal("30.00"))
                .dueAtResort(BigDecimal.ZERO)
                .dueNow(new BigDecimal("280.00"))
                .build();
        when(stayPricingService.calculateStayPricing(eq(roomType.getProperty()), eq(roomType.getBasePrice()), any(), any(), eq(1), eq("Standard Rate")))
                .thenReturn(calc);

        PriceChangedException ex = assertThrows(PriceChangedException.class, () -> bookingService.createBooking(baseRequest));
        assertTrue(ex.getMessage().contains("PRICE_CHANGED"));
        
        verify(paymentProcessorService, never()).processPayment(any(), any(), any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_AllowsMissingTravelerLastName() {
        baseRequest.setTraveler(TravelerInputDTO.builder()
                .firstName("John")
                .email("j@test.com")
                .phone("123")
                .build());

        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        when(validationService.parseUuid(roomTypeId.toString(), "roomTypeId")).thenReturn(roomTypeId);
        when(validationService.validateRoomTypeBelongsToProperty(roomTypeId, propertyId)).thenReturn(roomType);
        when(roomTypeService.isRoomTypeAvailable(eq(propertyId), eq(roomTypeId), any(), any(), eq(1), eq(2L)))
                .thenReturn(true);

        StayPricingResult calc = StayPricingResult.builder()
                .subtotal(new BigDecimal("200.00"))
                .taxesAndCharges(new BigDecimal("30.00"))
                .dueAtResort(BigDecimal.ZERO)
                .dueNow(new BigDecimal("230.00"))
                .build();
        when(stayPricingService.calculateStayPricing(eq(property), eq(roomType.getBasePrice()), any(), any(), eq(1), eq("Standard Rate")))
                .thenReturn(calc);
        when(roomRepository.findAndLockAvailablePhysicalRooms(any(), any(), any(), eq(1)))
                .thenReturn(java.util.List.of(UUID.randomUUID()));

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentId(UUID.randomUUID());
        paymentInfo.setPaymentStatus(PaymentStatus.PAID);
        paymentInfo.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentInfo.setLastFourDigits("4444");
        when(paymentProcessorService.processPayment(any(), any(), any())).thenReturn(paymentInfo);

        RoomTypeImage roomTypeImage = new RoomTypeImage();
        roomTypeImage.setImageUrl("https://example.com/rooms/deluxe-1.jpg");
        when(roomTypeImageRepository.findByRoomType_RoomTypeId(roomTypeId))
                .thenReturn(java.util.List.of(roomTypeImage));

        Booking mockBooking = new Booking();
        mockBooking.setBookingId(UUID.randomUUID());
        mockBooking.setTravellerInfo(new com.example.ibe.entity.TravellerInfo());
        mockBooking.getTravellerInfo().setFirstName("John");
        mockBooking.getTravellerInfo().setPhoneNumber("123");
        mockBooking.getTravellerInfo().setEmail("j@test.com");
        mockBooking.setPaymentInfo(paymentInfo);
        mockBooking.setBookingStatus(com.example.ibe.dto.enums.BookingStatus.CONFIRMED);
        mockBooking.setRoomType(roomType);
        mockBooking.setProperty(property);
        mockBooking.setCheckInDate(baseRequest.getCheckInDate());
        mockBooking.setCheckOutDate(baseRequest.getCheckOutDate());
        mockBooking.setNoOfGuests(2);
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);

        BookingConfirmationResponse res = bookingService.createBooking(baseRequest);

        assertNotNull(res);
        assertNull(res.getTraveler().getLastName());

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertNull(bookingCaptor.getValue().getTravellerInfo().getLastName());
    }

    @Test
    void createBooking_EmailFailureDoesNotBreakBookingFlow(CapturedOutput output) {
        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(propertyId.toString(), "propertyId")).thenReturn(propertyId);
        when(validationService.parseUuid(roomTypeId.toString(), "roomTypeId")).thenReturn(roomTypeId);
        when(validationService.validateRoomTypeBelongsToProperty(roomTypeId, propertyId)).thenReturn(roomType);
        when(roomTypeService.isRoomTypeAvailable(eq(propertyId), eq(roomTypeId), any(), any(), eq(1), eq(2L)))
                .thenReturn(true);

        StayPricingResult calc = StayPricingResult.builder()
                .subtotal(new BigDecimal("200.00"))
                .taxesAndCharges(new BigDecimal("30.00"))
                .dueAtResort(BigDecimal.ZERO)
                .dueNow(new BigDecimal("230.00"))
                .build();
        when(stayPricingService.calculateStayPricing(eq(property), eq(roomType.getBasePrice()), any(), any(), eq(1),
                eq("Standard Rate"))).thenReturn(calc);
        when(roomRepository.findAndLockAvailablePhysicalRooms(any(), any(), any(), eq(1)))
                .thenReturn(java.util.List.of(UUID.randomUUID()));

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentId(UUID.randomUUID());
        paymentInfo.setPaymentStatus(PaymentStatus.PAID);
        paymentInfo.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentInfo.setLastFourDigits("4444");
        when(paymentProcessorService.processPayment(any(), any(), any())).thenReturn(paymentInfo);

        RoomTypeImage roomTypeImage = new RoomTypeImage();
        roomTypeImage.setImageUrl("https://example.com/rooms/deluxe-1.jpg");
        when(roomTypeImageRepository.findByRoomType_RoomTypeId(roomTypeId))
                .thenReturn(java.util.List.of(roomTypeImage));

        Booking mockBooking = new Booking();
        mockBooking.setBookingId(UUID.randomUUID());
        mockBooking.setTravellerInfo(new com.example.ibe.entity.TravellerInfo());
        mockBooking.getTravellerInfo().setEmail("j@test.com");
        mockBooking.setPaymentInfo(paymentInfo);
        mockBooking.setBookingStatus(com.example.ibe.dto.enums.BookingStatus.CONFIRMED);
        mockBooking.setRoomType(roomType);
        mockBooking.setProperty(property);
        mockBooking.setCheckInDate(baseRequest.getCheckInDate());
        mockBooking.setCheckOutDate(baseRequest.getCheckOutDate());
        mockBooking.setNoOfGuests(2);
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);

        doThrow(new RuntimeException("Email address is not verified"))
                .when(bookingEmailService).sendBookingConfirmation(any());

        BookingConfirmationResponse response = bookingService.createBooking(baseRequest);

        assertNotNull(response);
        assertEquals("Deluxe", response.getRoomTypeName());
        assertTrue(output.getAll().contains("confirmation email failed"));
        verify(bookingRepository).save(any(Booking.class));
        verify(bookingEmailService).sendBookingConfirmation(any());
    }

    @Test
    void getBooking_ReturnsBookingOnlyForMatchingTenant() {
        UUID bookingId = UUID.randomUUID();

        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);
        property.setTenant(tenant);

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentId(UUID.randomUUID());
        paymentInfo.setPaymentStatus(PaymentStatus.PAID);
        paymentInfo.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentInfo.setLastFourDigits("4444");

        RoomTypeImage roomTypeImage = new RoomTypeImage();
        roomTypeImage.setImageUrl("https://example.com/rooms/deluxe-1.jpg");

        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setProperty(property);
        booking.setRoomType(roomType);
        booking.setGuestsJson(objectMapper.valueToTree(java.util.Map.of("adults", 2)));
        booking.setCheckInDate(baseRequest.getCheckInDate());
        booking.setCheckOutDate(baseRequest.getCheckOutDate());
        booking.setTotalPrice(new BigDecimal("200.00"));
        booking.setTaxAmount(new BigDecimal("30.00"));
        booking.setFinalAmount(new BigDecimal("230.00"));
        booking.setBookingStatus(com.example.ibe.dto.enums.BookingStatus.CONFIRMED);
        booking.setTravellerInfo(new com.example.ibe.entity.TravellerInfo());
        booking.getTravellerInfo().setFirstName("John");
        booking.getTravellerInfo().setLastName("Doe");
        booking.getTravellerInfo().setEmail("j@test.com");
        booking.getTravellerInfo().setPhoneNumber("123");
        booking.setPaymentInfo(paymentInfo);

        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(bookingId.toString(), "bookingId")).thenReturn(bookingId);
        doNothing().when(validationService).validateTenantExists(tenantId);
        when(bookingRepository.findByBookingIdAndProperty_Tenant_TenantId(bookingId, tenantId))
                .thenReturn(Optional.of(booking));
        when(roomTypeImageRepository.findByRoomType_RoomTypeId(roomTypeId))
                .thenReturn(java.util.List.of(roomTypeImage));
        doReturn(new BookingPriceCalculation(
                        new BigDecimal("200.00"),
                        new BigDecimal("30.00"),
                        new BigDecimal("230.00"),
                        BigDecimal.ZERO,
                        "USD"))
                .when(pricingService)
                .reconstructPricing(new BigDecimal("200.00"), new BigDecimal("30.00"), new BigDecimal("230.00"), BigDecimal.ZERO);

        BookingConfirmationResponse response = bookingService.getBooking(tenantId.toString(), bookingId.toString());

        assertEquals("Deluxe", response.getRoomTypeName());
        assertEquals(java.util.List.of("https://example.com/rooms/deluxe-1.jpg"), response.getImageUrls());
        verify(bookingRepository).findByBookingIdAndProperty_Tenant_TenantId(bookingId, tenantId);
    }

    @Test
    void getBooking_ThrowsWhenBookingDoesNotBelongToTenant() {
        UUID bookingId = UUID.randomUUID();

        when(validationService.parseUuid(tenantId.toString(), "tenantId")).thenReturn(tenantId);
        when(validationService.parseUuid(bookingId.toString(), "bookingId")).thenReturn(bookingId);
        doNothing().when(validationService).validateTenantExists(tenantId);
        when(bookingRepository.findByBookingIdAndProperty_Tenant_TenantId(bookingId, tenantId))
                .thenReturn(Optional.empty());

        BookingNotFoundException ex = assertThrows(
                BookingNotFoundException.class,
                () -> bookingService.getBooking(tenantId.toString(), bookingId.toString()));

        assertTrue(ex.getMessage().contains("BOOKING_NOT_FOUND"));
        verify(bookingRepository).findByBookingIdAndProperty_Tenant_TenantId(bookingId, tenantId);
    }
}
