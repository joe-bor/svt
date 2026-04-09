-- Seed all 13 locations with fixed IDs so route/detour references remain stable.
INSERT INTO location (
    id,
    name,
    description,
    route_order,
    is_detour,
    branches_from_id,
    latitude,
    longitude,
    detour_bonus_stat,
    detour_bonus_value
) VALUES
    (1, 'San Jose', 'The garage where it all begins', 1, FALSE, NULL, 37.3382, -121.8863, NULL, NULL),
    (2, 'Santa Clara', 'Intel country, data centers', 2, FALSE, NULL, 37.3541, -121.9552, NULL, NULL),
    (3, 'Sunnyvale', 'LinkedIn Campus', 3, FALSE, NULL, 37.3688, -122.0363, NULL, NULL),
    (4, 'Mountain View', 'Googleplex, big tech', 4, FALSE, NULL, 37.3861, -122.0839, NULL, NULL),
    (5, 'Palo Alto', 'Stanford, startup birthplace', 5, FALSE, NULL, 37.4419, -122.1430, NULL, NULL),
    (6, 'Menlo Park', 'Sand Hill Road, VC capital', 6, FALSE, NULL, 37.4530, -122.1817, NULL, NULL),
    (7, 'Redwood City', 'Mid-peninsula gateway', 7, FALSE, NULL, 37.4852, -122.2364, NULL, NULL),
    (8, 'San Mateo', 'Peninsula hub', 8, FALSE, NULL, 37.5630, -122.3255, NULL, NULL),
    (9, 'South San Francisco', 'Biotech corridor, the Industrial City', 9, FALSE, NULL, 37.6547, -122.4077, NULL, NULL),
    (10, 'San Francisco', 'Destination: Demo Day', 10, FALSE, NULL, 37.7749, -122.4194, NULL, NULL),
    (11, 'Cupertino', 'Apple Park territory', NULL, TRUE, 2, 37.3230, -122.0322, 'BUGS', -3),
    (12, 'Woodside', 'Billionaire enclave', NULL, TRUE, 6, 37.4299, -122.2540, 'CASH', 2500),
    -- Half Moon Bay intentionally has no detour auto-bonus; reward comes from the Beach Team Retreat event flow.
    (13, 'Half Moon Bay', 'Coast retreat on Hwy 92', NULL, TRUE, 7, 37.4636, -122.4286, NULL, NULL);
