BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS btree_gist;


DROP TABLE IF EXISTS room_type_images CASCADE;

DROP TABLE IF EXISTS room_assignments CASCADE;

DROP TABLE IF EXISTS bookings CASCADE;

DROP TABLE IF EXISTS rooms CASCADE;

DROP TABLE IF EXISTS payment_info CASCADE;

DROP TABLE IF EXISTS billing_info CASCADE;

DROP TABLE IF EXISTS traveller_info CASCADE;

DROP TABLE IF EXISTS room_type_amenities CASCADE;

DROP TABLE IF EXISTS amenities CASCADE;

DROP TABLE IF EXISTS property_filters CASCADE;

DROP TABLE IF EXISTS room_types CASCADE;

DROP TABLE IF EXISTS promocodes CASCADE;

DROP TABLE IF EXISTS packages CASCADE;

DROP TABLE IF EXISTS properties CASCADE;

DROP TABLE IF EXISTS tenants CASCADE;


CREATE TABLE tenants (
    tenant_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_name varchar(120) NOT NULL,
    domain varchar(255) NOT NULL,
    config jsonb NOT NULL DEFAULT '{}' :: jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE properties (
    property_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL REFERENCES tenants(tenant_id),
    property_name varchar(160) NOT NULL,
    address varchar(500) NOT NULL,
    helpline_number varchar(30) NOT NULL,
    occupancy_tax_percentage numeric(5, 2) NOT NULL,
    due_percentage numeric(5, 2) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE packages (
    package_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    discount_percentage float4 NOT NULL,
    package_description varchar(255) NOT NULL,
    package_name varchar(255) NOT NULL,
    property_id uuid NOT NULL REFERENCES properties(property_id) ON DELETE CASCADE
);

CREATE TABLE room_types (
    room_type_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id uuid NOT NULL REFERENCES properties(property_id) ON DELETE CASCADE,
    room_type_name varchar(120) NOT NULL,
    total_rooms int NOT NULL,
    base_price numeric(10, 2) NOT NULL,
    max_occupancy int NOT NULL,
    area int NOT NULL,
    description text,
    bed_types jsonb NOT NULL DEFAULT '{}' :: jsonb,
    meal_plan varchar(20) CHECK (meal_plan IN ('ALL_INCLUSIVE', 'FULL_BOARD', 'HALF_BOARD', 'ROOM_ONLY'))
);

CREATE TABLE traveller_info (
    traveller_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name varchar(100) NOT NULL,
    last_name varchar(100),
    phone_number varchar(30) NOT NULL,
    email varchar(255) NOT NULL
);

CREATE TABLE billing_info (
    billing_info_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name varchar(100) NOT NULL,
    last_name varchar(100),
    mailing_address_1 varchar(255) NOT NULL,
    mailing_address_2 varchar(255),
    country varchar(100) NOT NULL,
    city varchar(100) NOT NULL,
    state varchar(100) NOT NULL,
    zip_code varchar(20) NOT NULL,
    phone_number varchar(30) NOT NULL,
    email varchar(255) NOT NULL
);

CREATE TABLE payment_info (
    payment_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_status varchar(20) NOT NULL CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED')),
    payment_method varchar(30) NOT NULL CHECK (payment_method IN ('CREDIT_CARD')),
    last_four_digits varchar(4) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    paid_at timestamptz,
    failed_at timestamptz
);

CREATE TABLE rooms (
    room_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    room_type_id uuid NOT NULL REFERENCES room_types(room_type_id) ON DELETE CASCADE,
    hotel_room_no varchar(50) NOT NULL,
    CONSTRAINT uk_room_type_room_number UNIQUE (room_type_id, hotel_room_no)
);

CREATE TABLE bookings (
    booking_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    traveller_id uuid NOT NULL REFERENCES traveller_info(traveller_id),
    property_id uuid NOT NULL REFERENCES properties(property_id) ON DELETE CASCADE,
    room_type_id uuid NOT NULL REFERENCES room_types(room_type_id) ON DELETE CASCADE,
    check_in_date date NOT NULL,
    check_out_date date NOT NULL,
    no_of_rooms int NOT NULL,
    no_of_guests int NOT NULL,
    guests_json jsonb,
    total_price numeric(10, 2) NOT NULL,
    tax_amount numeric(10, 2),
    final_amount numeric(10, 2) NOT NULL,
    booking_status varchar(20) NOT NULL CHECK (booking_status IN ('LOCKED', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    billing_info_id uuid REFERENCES billing_info(billing_info_id),
    payment_id uuid REFERENCES payment_info(payment_id),
    locked_at timestamptz,
    confirmed_at timestamptz,
    expires_at timestamptz
);

CREATE TABLE room_assignments (
    room_assignment_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id uuid NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
    room_id uuid NOT NULL REFERENCES rooms(room_id) ON DELETE CASCADE,
    start_date date NOT NULL,
    end_date date NOT NULL
);

ALTER TABLE room_assignments
ADD CONSTRAINT no_overlap_assignments
EXCLUDE USING gist (
    room_id WITH =,
    daterange(start_date, end_date) WITH &&
) WHERE (booking_id IS NOT NULL);

CREATE TABLE room_type_images (
    room_type_image_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    room_type_id uuid NOT NULL REFERENCES room_types(room_type_id) ON DELETE CASCADE,
    image_url varchar(2000) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE property_filters (
    filter_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id uuid NOT NULL REFERENCES properties(property_id) ON DELETE CASCADE,
    filter_name varchar(100) NOT NULL,
    filter_type varchar(50) NOT NULL,
    config jsonb NOT NULL DEFAULT '{}' :: jsonb,
    UNIQUE(property_id, filter_name)
);

CREATE TABLE amenities (
    amenity_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    amenity_name varchar(120) NOT NULL UNIQUE
);

CREATE TABLE room_type_amenities (
    room_type_id uuid NOT NULL REFERENCES room_types(room_type_id) ON DELETE CASCADE,
    amenity_id uuid NOT NULL REFERENCES amenities(amenity_id) ON DELETE CASCADE,
    PRIMARY KEY (room_type_id, amenity_id)
);

CREATE TABLE promocodes (
    promo_id uuid NOT NULL,
    promo_name varchar(255) NOT NULL,
    promo_description varchar(200) NOT NULL,
    promo_code varchar(5) NOT NULL,
    discount_percentage float NOT NULL,
    property_id uuid NOT NULL,
    PRIMARY KEY (promo_id),
    CONSTRAINT fk_property
        FOREIGN KEY (property_id)
        REFERENCES properties(property_id)
);

CREATE INDEX idx_room_type_property ON room_types(property_id);
CREATE INDEX idx_rooms_room_type ON rooms(room_type_id);
CREATE INDEX idx_bookings_room_type_dates ON bookings(room_type_id, check_in_date, check_out_date);
CREATE INDEX idx_bookings_property ON bookings(property_id);
CREATE INDEX idx_bookings_traveller ON bookings(traveller_id);
CREATE INDEX idx_bookings_status ON bookings(booking_status);
CREATE INDEX idx_bookings_active_property_dates ON bookings(property_id, check_in_date, check_out_date) WHERE booking_status IN ('CONFIRMED', 'LOCKED');
CREATE INDEX idx_room_assignments_booking ON room_assignments(booking_id);
CREATE INDEX idx_room_assignments_room ON room_assignments(room_id);


INSERT INTO
    tenants (
        tenant_id,
        tenant_name,
        domain,
        config,
        created_at,
        updated_at
    )
VALUES
    (
        '11111111-1111-1111-1111-111111111111',
        'radisson',
        'radisson.example.com',
        '{
        "tenantImage": "/images/radisson_banner_image.jpg",
        "tenantLogo": "/images/radisson_logo.jpg",
        "maxRooms": 5,
        "guests": {
            "adults": "18+",
            "teens": "13-17",
            "kids": "0-12"
        },
        "accessibility": true,
        "maxCapacityPerRoom": 4,
        "maxBookingRange": 14
    }' :: jsonb,
        now(),
        now()
    ),
    (
        '22222222-2222-2222-2222-222222222222',
        'taj',
        'taj.example.com',
        '{
        "tenantImage": "/images/taj_banner_image.jpg",
        "tenantLogo": "/images/taj_logo.png",
        "maxRooms": 3,
        "guests": {"adults": "18+","kids": "0-12"},
        "accessibility": false,
        "maxCapacityPerRoom": 3,
        "maxBookingRange": 7
    }' :: jsonb,
        now(),
        now()
    );


INSERT INTO
    properties (
        property_id,
        tenant_id,
        property_name,
        address,
        helpline_number,
        occupancy_tax_percentage,
        due_percentage,
        created_at,
        updated_at
    )
VALUES
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        '11111111-1111-1111-1111-111111111111',
        'Ocean View Resort',
        '123 Beach Avenue, Miami, FL',
        '+1-800-555-0101',
        12.00,
        25.00,
        now(),
        now()
    ),
    (
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        '11111111-1111-1111-1111-111111111111',
        'Mountain Escape Lodge',
        '456 Alpine Road, Aspen, CO',
        '+1-800-555-0102',
        10.00,
        25.00,
        now(),
        now()
    ),
    (
        'cccccccc-cccc-cccc-cccc-cccccccccccc',
        '22222222-2222-2222-2222-222222222222',
        'City Center Inn',
        '789 Downtown Street, Berlin, Germany',
        '+49-800-555-0103',
        18.00,
        30.00,
        now(),
        now()
    );


INSERT INTO
    packages (
        package_id,
        package_name,
        package_description,
        discount_percentage,
        property_id
    )
VALUES
    (
        'ba000001-0000-0000-0000-000000000001',
        'Senior Citizen Discount',
        'Exclusive senior citizen rates for a comfortable and worry-free stay.',
        15,
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
    ),
    (
        'ba000002-0000-0000-0000-000000000002',
        'Student Discount',
        'Get affordable student discount.',
        10,
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
    ),
    (
        'ba000007-0000-0000-0000-000000000007',
        'Long Weekend',
        'Get 12% off on Long weekend stay.',
        12,
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
    ),
    (
        'ba000003-0000-0000-0000-000000000003',
        'Senior Citizen Discount',
        'Exclusive senior citizen rates for a comfortable and worry-free stay.',
        12,
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
    ),
    (
        'ba000004-0000-0000-0000-000000000004',
        'Mountain Wellness Retreat',
        'Yoga sessions, hot spring access, and a healthy breakfast daily.',
        10,
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
    ),
    (
        'ba000008-0000-0000-0000-000000000008',
        'Long Weekend',
        'Get 12% off on Long weekend stay.',
        12,
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
    ),
    (
        'ba000005-0000-0000-0000-000000000005',
        'City Explorer',
        'Hop-on hop-off bus pass, museum entries, and a city walking tour.',
        8,
        'cccccccc-cccc-cccc-cccc-cccccccccccc'
    ),
    (
        'ba000006-0000-0000-0000-000000000006',
        'Business Traveller',
        'Airport transfer, express laundry, and access to the business lounge.',
        5,
        'cccccccc-cccc-cccc-cccc-cccccccccccc'
    ),
    (
        'ba000009-0000-0000-0000-000000000009',
        'Long Weekend',
        'Get 12% off on Long weekend stay.',
        12,
        'cccccccc-cccc-cccc-cccc-cccccccccccc'
    );


INSERT INTO
    room_types (
        room_type_id,
        property_id,
        room_type_name,
        total_rooms,
        base_price,
        max_occupancy,
        area,
        description,
        bed_types,
        meal_plan
    )
VALUES
    (
        'd1111111-1111-1111-1111-111111111111',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        'Deluxe Room',
        10,
        200,
        3,
        320,
        'Spacious deluxe room with ocean view and modern amenities.',
        '{"single": 2, "double": 1}' :: jsonb,
        'HALF_BOARD'
    ),
    (
        'd2222222-2222-2222-2222-222222222222',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        'Suite',
        5,
        350,
        4,
        450,
        'Luxurious suite with separate living area and premium furnishings.',
        '{"king": 1, "sofa": 1}' :: jsonb,
        'FULL_BOARD'
    ),
    (
        'd5555555-5555-5555-5555-555555555555',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        'Family Studio',
        7,
        260,
        4,
        380,
        'Family-friendly studio with extra living space and breakfast access.',
        '{"queen": 1, "single": 2}' :: jsonb,
        'ALL_INCLUSIVE'
    ),
    (
        'd6666666-6666-6666-6666-666666666666',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        'Premium Ocean Suite',
        3,
        620,
        5,
        580,
        'Premium suite with private balcony and elevated in-room amenities.',
        '{"king": 1, "sofa": 1, "single": 1}' :: jsonb,
        'ALL_INCLUSIVE'
    ),
    (
        'd3333333-3333-3333-3333-333333333333',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        'Mountain Deluxe',
        8,
        180,
        3,
        300,
        'Cozy mountain room with panoramic views and fireplace.',
        '{"single": 2}' :: jsonb,
        'ROOM_ONLY'
    ),
    (
        'db000001-0000-0000-0000-000000000001',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        'Alpine Suite',
        5,
        280,
        4,
        420,
        'Spacious alpine suite with a private terrace and mountain vistas.',
        '{"king": 1, "sofa": 1}' :: jsonb,
        'HALF_BOARD'
    ),
    (
        'db000002-0000-0000-0000-000000000002',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        'Cozy Cabin Room',
        6,
        150,
        2,
        260,
        'Intimate cabin-style room with wooden interiors and a warm fireplace.',
        '{"double": 1}' :: jsonb,
        'ROOM_ONLY'
    ),
    (
        'db000003-0000-0000-0000-000000000003',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        'Family Mountain Lodge',
        4,
        320,
        5,
        480,
        'Generous family room with bunk beds and a communal lounge area.',
        '{"queen": 1, "single": 2}' :: jsonb,
        'FULL_BOARD'
    ),
    (
        'd4444444-4444-4444-4444-444444444444',
        'cccccccc-cccc-cccc-cccc-cccccccccccc',
        'City Standard',
        12,
        120,
        2,
        250,
        'Comfortable city room in the heart of downtown.',
        '{"double": 1}' :: jsonb,
        'ROOM_ONLY'
    ),
    (
        'dc000001-0000-0000-0000-000000000001',
        'cccccccc-cccc-cccc-cccc-cccccccccccc',
        'Executive Room',
        8,
        200,
        2,
        300,
        'Modern executive room with a work desk and city skyline view.',
        '{"king": 1}' :: jsonb,
        'ROOM_ONLY'
    ),
    (
        'dc000002-0000-0000-0000-000000000002',
        'cccccccc-cccc-cccc-cccc-cccccccccccc',
        'Deluxe City Suite',
        5,
        310,
        3,
        400,
        'Upscale suite with a lounge area and panoramic city views.',
        '{"king": 1, "sofa": 1}' :: jsonb,
        'HALF_BOARD'
    ),
    (
        'dc000003-0000-0000-0000-000000000003',
        'cccccccc-cccc-cccc-cccc-cccccccccccc',
        'Family City Room',
        6,
        240,
        4,
        380,
        'Spacious family room with two queen beds close to city attractions.',
        '{"queen": 2}' :: jsonb,
        'FULL_BOARD'
    );


INSERT INTO
    amenities (amenity_id, amenity_name)
VALUES
    ('ae000001-0000-0000-0000-000000000001', 'WiFi'),
    ('ae000002-0000-0000-0000-000000000002', 'Air Conditioning'),
    ('ae000003-0000-0000-0000-000000000003', 'Mini Bar'),
    ('ae000004-0000-0000-0000-000000000004', 'Room Service'),
    ('ae000005-0000-0000-0000-000000000005', 'Mountain View'),
    ('ae000006-0000-0000-0000-000000000006', 'Balcony'),
    ('ae000007-0000-0000-0000-000000000007', 'Breakfast Included');


INSERT INTO
    room_type_amenities (room_type_id, amenity_id)
VALUES
    ('d1111111-1111-1111-1111-111111111111', 'ae000001-0000-0000-0000-000000000001'),
    ('d1111111-1111-1111-1111-111111111111', 'ae000002-0000-0000-0000-000000000002'),
    ('d1111111-1111-1111-1111-111111111111', 'ae000003-0000-0000-0000-000000000003'),
    ('d2222222-2222-2222-2222-222222222222', 'ae000001-0000-0000-0000-000000000001'),
    ('d2222222-2222-2222-2222-222222222222', 'ae000004-0000-0000-0000-000000000004'),
    ('d5555555-5555-5555-5555-555555555555', 'ae000001-0000-0000-0000-000000000001'),
    ('d5555555-5555-5555-5555-555555555555', 'ae000002-0000-0000-0000-000000000002'),
    ('d5555555-5555-5555-5555-555555555555', 'ae000007-0000-0000-0000-000000000007'),
    ('d6666666-6666-6666-6666-666666666666', 'ae000001-0000-0000-0000-000000000001'),
    ('d6666666-6666-6666-6666-666666666666', 'ae000003-0000-0000-0000-000000000003'),
    ('d6666666-6666-6666-6666-666666666666', 'ae000004-0000-0000-0000-000000000004'),
    ('d6666666-6666-6666-6666-666666666666', 'ae000006-0000-0000-0000-000000000006'),
    ('d3333333-3333-3333-3333-333333333333', 'ae000001-0000-0000-0000-000000000001'),
    ('d3333333-3333-3333-3333-333333333333', 'ae000005-0000-0000-0000-000000000005'),
    ('db000001-0000-0000-0000-000000000001', 'ae000001-0000-0000-0000-000000000001'),
    ('db000001-0000-0000-0000-000000000001', 'ae000005-0000-0000-0000-000000000005'),
    ('db000001-0000-0000-0000-000000000001', 'ae000006-0000-0000-0000-000000000006'),
    ('db000002-0000-0000-0000-000000000002', 'ae000001-0000-0000-0000-000000000001'),
    ('db000002-0000-0000-0000-000000000002', 'ae000002-0000-0000-0000-000000000002'),
    ('db000003-0000-0000-0000-000000000003', 'ae000001-0000-0000-0000-000000000001'),
    ('db000003-0000-0000-0000-000000000003', 'ae000007-0000-0000-0000-000000000007'),
    ('d4444444-4444-4444-4444-444444444444', 'ae000001-0000-0000-0000-000000000001'),
    ('dc000001-0000-0000-0000-000000000001', 'ae000001-0000-0000-0000-000000000001'),
    ('dc000001-0000-0000-0000-000000000001', 'ae000002-0000-0000-0000-000000000002'),
    ('dc000001-0000-0000-0000-000000000001', 'ae000004-0000-0000-0000-000000000004'),
    ('dc000002-0000-0000-0000-000000000002', 'ae000001-0000-0000-0000-000000000001'),
    ('dc000002-0000-0000-0000-000000000002', 'ae000003-0000-0000-0000-000000000003'),
    ('dc000002-0000-0000-0000-000000000002', 'ae000006-0000-0000-0000-000000000006'),
    ('dc000003-0000-0000-0000-000000000003', 'ae000001-0000-0000-0000-000000000001'),
    ('dc000003-0000-0000-0000-000000000003', 'ae000007-0000-0000-0000-000000000007');


INSERT INTO
    property_filters (
        filter_id,
        property_id,
        filter_name,
        filter_type,
        config
    )
VALUES
    (
        'ff000001-0000-0000-0000-000000000001',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        'Price',
        'RANGE',
        '{"currency":"USD","min":100,"max":700,"step":25}' :: jsonb
    ),
    (
        'ff000002-0000-0000-0000-000000000002',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        'Amenities',
        'CHECKBOX',
        '{"options":[{"label":"WiFi","value":"WiFi"},{"label":"Mini Bar","value":"Mini Bar"},{"label":"Room Service","value":"Room Service"},{"label":"Balcony","value":"Balcony"},{"label":"Breakfast Included","value":"Breakfast Included"}]}' :: jsonb
    ),
    (
        'ff000004-0000-0000-0000-000000000004',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        'Area',
        'RANGE',
        '{"unit":"sqft","min":200,"max":700,"step":20}' :: jsonb
    ),
    (
        'ff000005-0000-0000-0000-000000000005',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        'Max Occupancy',
        'RANGE',
        '{"min":1,"max":6,"step":1}' :: jsonb
    ),
    (
        'ff000006-0000-0000-0000-000000000006',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        'Bed Type',
        'CHECKBOX',
        '{"options":[{"label":"Single","value":"single"},{"label":"Double","value":"double"},{"label":"Queen","value":"queen"},{"label":"King","value":"king"},{"label":"Sofa","value":"sofa"}]}' :: jsonb
    ),
    (
        'ff000003-0000-0000-0000-000000000003',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        'Bed Type',
        'CHECKBOX',
        '{"options":[{"label":"Single","value":"single"},{"label":"Double","value":"double"},{"label":"Queen","value":"queen"},{"label":"King","value":"king"},{"label":"Sofa","value":"sofa"}]}' :: jsonb
    ),
    (
        'ff000007-0000-0000-0000-000000000007',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        'Price',
        'RANGE',
        '{"currency":"USD","min":100,"max":400,"step":25}' :: jsonb
    ),
    (
        'ff000008-0000-0000-0000-000000000008',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        'Amenities',
        'CHECKBOX',
        '{"options":[{"label":"WiFi","value":"WiFi"},{"label":"Mountain View","value":"Mountain View"},{"label":"Balcony","value":"Balcony"},{"label":"Breakfast Included","value":"Breakfast Included"}]}' :: jsonb
    ),
    (
        'ff000009-0000-0000-0000-000000000009',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        'Area',
        'RANGE',
        '{"unit":"sqft","min":200,"max":600,"step":20}' :: jsonb
    ),
    (
        'ff000010-0000-0000-0000-000000000010',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        'Max Occupancy',
        'RANGE',
        '{"min":1,"max":6,"step":1}' :: jsonb
    ),
    (
        'ff000011-0000-0000-0000-000000000011',
        'cccccccc-cccc-cccc-cccc-cccccccccccc',
        'Price',
        'RANGE',
        '{"currency":"USD","min":100,"max":400,"step":25}' :: jsonb
    ),
    (
        'ff000012-0000-0000-0000-000000000012',
        'cccccccc-cccc-cccc-cccc-cccccccccccc',
        'Amenities',
        'CHECKBOX',
        '{"options":[{"label":"WiFi","value":"WiFi"},{"label":"Air Conditioning","value":"Air Conditioning"},{"label":"Mini Bar","value":"Mini Bar"},{"label":"Room Service","value":"Room Service"},{"label":"Balcony","value":"Balcony"},{"label":"Breakfast Included","value":"Breakfast Included"}]}' :: jsonb
    ),
    (
        'ff000013-0000-0000-0000-000000000013',
        'cccccccc-cccc-cccc-cccc-cccccccccccc',
        'Area',
        'RANGE',
        '{"unit":"sqft","min":200,"max":500,"step":20}' :: jsonb
    ),
    (
        'ff000014-0000-0000-0000-000000000014',
        'cccccccc-cccc-cccc-cccc-cccccccccccc',
        'Max Occupancy',
        'RANGE',
        '{"min":1,"max":5,"step":1}' :: jsonb
    ),
    (
        'ff000015-0000-0000-0000-000000000015',
        'cccccccc-cccc-cccc-cccc-cccccccccccc',
        'Bed Type',
        'CHECKBOX',
        '{"options":[{"label":"Double","value":"double"},{"label":"Queen","value":"queen"},{"label":"King","value":"king"},{"label":"Sofa","value":"sofa"}]}' :: jsonb
    );


INSERT INTO
    room_type_images (
        room_type_image_id,
        room_type_id,
        image_url,
        created_at,
        updated_at
    )
VALUES
    ('f1111111-1111-1111-1111-111111111111', 'd1111111-1111-1111-1111-111111111111', 'https://cdn.pixabay.com/photo/2020/10/18/09/16/bedroom-5664221_1280.jpg', now(), now()),
    ('f1111111-1111-1111-1111-111111111112', 'd1111111-1111-1111-1111-111111111111', 'https://cdn.pixabay.com/photo/2018/06/14/21/15/bedroom-3475656_1280.jpg', now(), now()),
    ('f1111111-1111-1111-1111-111111111113', 'd1111111-1111-1111-1111-111111111111', 'https://cdn.pixabay.com/photo/2017/04/28/22/14/room-2269591_1280.jpg', now(), now()),
    ('f2222222-2222-2222-2222-222222222221', 'd2222222-2222-2222-2222-222222222222', 'https://cdn.pixabay.com/photo/2020/12/24/19/08/hotel-room-5858067_1280.jpg', now(), now()),
    ('f2222222-2222-2222-2222-222222222222', 'd2222222-2222-2222-2222-222222222222', 'https://cdn.pixabay.com/photo/2020/02/01/06/12/living-room-4809590_1280.jpg', now(), now()),
    ('f2222222-2222-2222-2222-222222222223', 'd2222222-2222-2222-2222-222222222222', 'https://cdn.pixabay.com/photo/2015/01/10/11/39/hotel-595121_1280.jpg', now(), now()),
    ('f5555555-5555-5555-5555-555555555551', 'd5555555-5555-5555-5555-555555555555', 'https://cdn.pixabay.com/photo/2020/01/15/18/02/room-4768554_1280.jpg', now(), now()),
    ('f5555555-5555-5555-5555-555555555552', 'd5555555-5555-5555-5555-555555555555', 'https://cdn.pixabay.com/photo/2020/10/18/09/16/bedroom-5664221_1280.jpg', now(), now()),
    ('f5555555-5555-5555-5555-555555555553', 'd5555555-5555-5555-5555-555555555555', 'https://cdn.pixabay.com/photo/2020/02/01/06/12/living-room-4809590_1280.jpg', now(), now()),
    ('f6666666-6666-6666-6666-666666666661', 'd6666666-6666-6666-6666-666666666666', 'https://cdn.pixabay.com/photo/2015/01/10/11/39/hotel-595121_1280.jpg', now(), now()),
    ('f6666666-6666-6666-6666-666666666662', 'd6666666-6666-6666-6666-666666666666', 'https://cdn.pixabay.com/photo/2020/12/24/19/08/hotel-room-5858067_1280.jpg', now(), now()),
    ('f6666666-6666-6666-6666-666666666663', 'd6666666-6666-6666-6666-666666666666', 'https://cdn.pixabay.com/photo/2018/06/14/21/15/bedroom-3475656_1280.jpg', now(), now()),
    ('f3333333-3333-3333-3333-333333333331', 'd3333333-3333-3333-3333-333333333333', 'https://cdn.pixabay.com/photo/2017/04/28/22/14/room-2269591_1280.jpg', now(), now()),
    ('f3333333-3333-3333-3333-333333333332', 'd3333333-3333-3333-3333-333333333333', 'https://cdn.pixabay.com/photo/2020/01/15/18/02/room-4768554_1280.jpg', now(), now()),
    ('f3333333-3333-3333-3333-333333333333', 'd3333333-3333-3333-3333-333333333333', 'https://cdn.pixabay.com/photo/2020/10/18/09/16/bedroom-5664221_1280.jpg', now(), now()),
    ('fb000001-0000-0000-0000-000000000001', 'db000001-0000-0000-0000-000000000001', 'https://cdn.pixabay.com/photo/2020/12/24/19/08/hotel-room-5858067_1280.jpg', now(), now()),
    ('fb000001-0000-0000-0000-000000000002', 'db000001-0000-0000-0000-000000000001', 'https://cdn.pixabay.com/photo/2018/06/14/21/15/bedroom-3475656_1280.jpg', now(), now()),
    ('fb000001-0000-0000-0000-000000000003', 'db000001-0000-0000-0000-000000000001', 'https://cdn.pixabay.com/photo/2015/01/10/11/39/hotel-595121_1280.jpg', now(), now()),
    ('fb000002-0000-0000-0000-000000000001', 'db000002-0000-0000-0000-000000000002', 'https://cdn.pixabay.com/photo/2020/02/01/06/12/living-room-4809590_1280.jpg', now(), now()),
    ('fb000002-0000-0000-0000-000000000002', 'db000002-0000-0000-0000-000000000002', 'https://cdn.pixabay.com/photo/2017/04/28/22/14/room-2269591_1280.jpg', now(), now()),
    ('fb000002-0000-0000-0000-000000000003', 'db000002-0000-0000-0000-000000000002', 'https://cdn.pixabay.com/photo/2020/10/18/09/16/bedroom-5664221_1280.jpg', now(), now()),
    ('fb000003-0000-0000-0000-000000000001', 'db000003-0000-0000-0000-000000000003', 'https://cdn.pixabay.com/photo/2020/01/15/18/02/room-4768554_1280.jpg', now(), now()),
    ('fb000003-0000-0000-0000-000000000002', 'db000003-0000-0000-0000-000000000003', 'https://cdn.pixabay.com/photo/2018/06/14/21/15/bedroom-3475656_1280.jpg', now(), now()),
    ('fb000003-0000-0000-0000-000000000003', 'db000003-0000-0000-0000-000000000003', 'https://cdn.pixabay.com/photo/2020/12/24/19/08/hotel-room-5858067_1280.jpg', now(), now()),
    ('f4444444-4444-4444-4444-444444444441', 'd4444444-4444-4444-4444-444444444444', 'https://cdn.pixabay.com/photo/2020/12/24/19/08/hotel-room-5858067_1280.jpg', now(), now()),
    ('f4444444-4444-4444-4444-444444444442', 'd4444444-4444-4444-4444-444444444444', 'https://cdn.pixabay.com/photo/2018/06/14/21/15/bedroom-3475656_1280.jpg', now(), now()),
    ('f4444444-4444-4444-4444-444444444443', 'd4444444-4444-4444-4444-444444444444', 'https://cdn.pixabay.com/photo/2015/01/10/11/39/hotel-595121_1280.jpg', now(), now()),
    ('fc000001-0000-0000-0000-000000000001', 'dc000001-0000-0000-0000-000000000001', 'https://cdn.pixabay.com/photo/2020/10/18/09/16/bedroom-5664221_1280.jpg', now(), now()),
    ('fc000001-0000-0000-0000-000000000002', 'dc000001-0000-0000-0000-000000000001', 'https://cdn.pixabay.com/photo/2020/02/01/06/12/living-room-4809590_1280.jpg', now(), now()),
    ('fc000001-0000-0000-0000-000000000003', 'dc000001-0000-0000-0000-000000000001', 'https://cdn.pixabay.com/photo/2017/04/28/22/14/room-2269591_1280.jpg', now(), now()),
    ('fc000002-0000-0000-0000-000000000001', 'dc000002-0000-0000-0000-000000000002', 'https://cdn.pixabay.com/photo/2015/01/10/11/39/hotel-595121_1280.jpg', now(), now()),
    ('fc000002-0000-0000-0000-000000000002', 'dc000002-0000-0000-0000-000000000002', 'https://cdn.pixabay.com/photo/2020/12/24/19/08/hotel-room-5858067_1280.jpg', now(), now()),
    ('fc000002-0000-0000-0000-000000000003', 'dc000002-0000-0000-0000-000000000002', 'https://cdn.pixabay.com/photo/2018/06/14/21/15/bedroom-3475656_1280.jpg', now(), now()),
    ('fc000003-0000-0000-0000-000000000001', 'dc000003-0000-0000-0000-000000000003', 'https://cdn.pixabay.com/photo/2020/01/15/18/02/room-4768554_1280.jpg', now(), now()),
    ('fc000003-0000-0000-0000-000000000002', 'dc000003-0000-0000-0000-000000000003', 'https://cdn.pixabay.com/photo/2020/10/18/09/16/bedroom-5664221_1280.jpg', now(), now()),
    ('fc000003-0000-0000-0000-000000000003', 'dc000003-0000-0000-0000-000000000003', 'https://cdn.pixabay.com/photo/2020/02/01/06/12/living-room-4809590_1280.jpg', now(), now());


INSERT INTO traveller_info (
    traveller_id,
    first_name,
    last_name,
    phone_number,
    email
)
VALUES
    ('70000000-0000-0000-0000-000000000001', 'Alex', 'Johnson', '+1-305-555-0101', 'alex.johnson@example.com'),
    ('70000000-0000-0000-0000-000000000002', 'Priya', 'Mehta', '+91-98765-43210', 'priya.mehta@example.com'),
    ('70000000-0000-0000-0000-000000000003', 'Liam', 'Carter', '+1-970-555-0145', 'liam.carter@example.com');

INSERT INTO billing_info (
    billing_info_id,
    first_name,
    last_name,
    mailing_address_1,
    mailing_address_2,
    country,
    city,
    state,
    zip_code,
    phone_number,
    email
)
VALUES
    ('71000000-0000-0000-0000-000000000001', 'Alex', 'Johnson', '123 Collins Ave', NULL, 'USA', 'Miami', 'FL', '33101', '+1-305-555-0101', 'alex.billing@example.com'),
    ('71000000-0000-0000-0000-000000000002', 'Priya', 'Mehta', '44 MG Road', 'Apt 6B', 'India', 'Pune', 'MH', '411001', '+91-98765-43210', 'priya.billing@example.com');

INSERT INTO payment_info (
    payment_id,
    payment_status,
    payment_method,
    last_four_digits,
    created_at,
    updated_at,
    paid_at,
    failed_at
)
VALUES
    ('71500000-0000-0000-0000-000000000001', 'PAID', 'CREDIT_CARD', '4242', now(), now(), now(), NULL),
    ('71500000-0000-0000-0000-000000000002', 'PENDING', 'CREDIT_CARD', '1111', now(), now(), NULL, NULL);

INSERT INTO rooms (
    room_id,
    room_type_id,
    hotel_room_no
)
SELECT
    gen_random_uuid(),
    rt.room_type_id,
    'R-' || upper(substr(replace(rt.room_type_id::text, '-', ''), 1, 6)) || '-' || lpad(gs::text, 3, '0')
FROM
    room_types rt
    CROSS JOIN LATERAL generate_series(1, rt.total_rooms) AS gs;

INSERT INTO bookings (
    booking_id,
    traveller_id,
    property_id,
    room_type_id,
    check_in_date,
    check_out_date,
    no_of_rooms,
    no_of_guests,
    guests_json,
    total_price,
    tax_amount,
    final_amount,
    booking_status,
    billing_info_id,
    payment_id,
    locked_at,
    confirmed_at,
    expires_at
)
VALUES
    (
        '72000000-0000-0000-0000-000000000001',
        '70000000-0000-0000-0000-000000000001',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        'd1111111-1111-1111-1111-111111111111',
        CURRENT_DATE + 2,
        CURRENT_DATE + 5,
        1,
        2,
        '[{"name":"Alex Johnson","age":32}]'::jsonb,
        600.00,
        72.00,
        672.00,
        'CONFIRMED',
        '71000000-0000-0000-0000-000000000001',
        '71500000-0000-0000-0000-000000000001',
        NULL,
        now(),
        NULL
    ),
    (
        '72000000-0000-0000-0000-000000000002',
        '70000000-0000-0000-0000-000000000002',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        'd3333333-3333-3333-3333-333333333333',
        CURRENT_DATE + 4,
        CURRENT_DATE + 6,
        1,
        2,
        '[{"name":"Priya Mehta","age":29}]'::jsonb,
        420.00,
        50.40,
        470.40,
        'LOCKED',
        '71000000-0000-0000-0000-000000000002',
        '71500000-0000-0000-0000-000000000002',
        now(),
        NULL,
        now() + INTERVAL '15 minutes'
    );

INSERT INTO room_assignments (
    room_assignment_id,
    booking_id,
    room_id,
    start_date,
    end_date
)
SELECT
    '73000000-0000-0000-0000-000000000001'::uuid,
    '72000000-0000-0000-0000-000000000001'::uuid,
    r.room_id,
    CURRENT_DATE + 2,
    CURRENT_DATE + 5
FROM rooms r
WHERE r.room_type_id = 'd1111111-1111-1111-1111-111111111111'
ORDER BY r.hotel_room_no
LIMIT 1;


INSERT INTO promocodes (
    promo_id,
    promo_name,
    promo_description,
    promo_code,
    discount_percentage,
    property_id
)
VALUES
    ('c0000001-0000-0000-0000-000000000001', 'Summer Sale',   'Flat 10% off on bookings',      'SUM10', 10, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('c0000002-0000-0000-0000-000000000002', 'Weekend Deal',  '15% off for weekend stays',     'WKD15', 15, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('c0000003-0000-0000-0000-000000000003', 'Early Bird',    'Book early and get 12% off',    'EAR12', 12, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
    ('c0000004-0000-0000-0000-000000000004', 'Festive Offer', 'Flat 18% festive discount',     'FST18', 18, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
    ('c0000005-0000-0000-0000-000000000005', 'New Year Sale', 'Celebrate with 20% off',        'NYR20', 20, 'cccccccc-cccc-cccc-cccc-cccccccccccc'),
    ('c0000006-0000-0000-0000-000000000006', 'Stay Longer',   'Stay 3 nights and get 8% off',  'STY08',  8, 'cccccccc-cccc-cccc-cccc-cccccccccccc');

COMMIT;
