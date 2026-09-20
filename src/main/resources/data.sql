-- Seed data matching the original static BuildPro Construction page.
-- Idempotent: safe to re-run on every app start (spring.sql.init.mode=always).

INSERT INTO services (id, title, description, display_order) VALUES
  (1, 'Residential Construction', 'Luxury homes, villas and apartments.', 1),
  (2, 'Commercial Buildings', 'Office spaces, malls and shopping complexes.', 2),
  (3, 'Interior Design', 'Modern interiors with premium finishing.', 3),
  (4, 'Renovation', 'House renovation and remodeling solutions.', 4),
  (5, 'Architecture', 'Creative architectural planning.', 5),
  (6, 'Project Management', 'End-to-end construction management.', 6)
ON CONFLICT (id) DO NOTHING;

INSERT INTO stats (id, label, target_value, display_order) VALUES
  (1, 'Projects Completed', 120, 1),
  (2, 'Engineers', 55, 2),
  (3, 'Years Experience', 25, 3),
  (4, 'Happy Clients', 100, 4)
ON CONFLICT (id) DO NOTHING;

INSERT INTO projects (id, image_url, title, display_order) VALUES
  (1, 'https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=800&q=80', 'Project 1', 1),
  (2, 'https://images.unsplash.com/photo-1460317442991-0ec209397118?auto=format&fit=crop&w=800&q=80', 'Project 2', 2),
  (3, 'https://images.unsplash.com/photo-1511818966892-d7d671e672a2?auto=format&fit=crop&w=800&q=80', 'Project 3', 3)
ON CONFLICT (id) DO NOTHING;

INSERT INTO testimonials (id, client_name, message, rating, display_order) VALUES
  (1, 'David', 'Excellent workmanship and completed our home before deadline.', 5, 1),
  (2, 'Jennifer', 'Professional engineers and outstanding quality.', 5, 2),
  (3, 'Michael', 'Highly recommended construction company.', 5, 3)
ON CONFLICT (id) DO NOTHING;

INSERT INTO company_info (id, company_name, address, phone, email) VALUES
  (1, 'BuildPro Construction', '123 Business Street', '+91 9876543210', 'info@buildpro.com')
ON CONFLICT (id) DO NOTHING;

-- Keep the identity sequences ahead of the manually-inserted ids above,
-- so the next app-generated insert doesn't collide with seed rows.
SELECT setval(pg_get_serial_sequence('services', 'id'), COALESCE((SELECT MAX(id) FROM services), 1));
SELECT setval(pg_get_serial_sequence('stats', 'id'), COALESCE((SELECT MAX(id) FROM stats), 1));
SELECT setval(pg_get_serial_sequence('projects', 'id'), COALESCE((SELECT MAX(id) FROM projects), 1));
SELECT setval(pg_get_serial_sequence('testimonials', 'id'), COALESCE((SELECT MAX(id) FROM testimonials), 1));
SELECT setval(pg_get_serial_sequence('company_info', 'id'), COALESCE((SELECT MAX(id) FROM company_info), 1));
