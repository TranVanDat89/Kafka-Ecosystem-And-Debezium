-- Tạo bảng cities
CREATE TABLE cities (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    region VARCHAR(100),
    population INTEGER,
    area_km2 DECIMAL(10,2),
    latitude DECIMAL(10,6),
    longitude DECIMAL(10,6),
    timezone VARCHAR(50),
    is_capital BOOLEAN DEFAULT false,
    founded_year INTEGER,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tạo function để tự động update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Tạo trigger cho auto-update timestamp
CREATE TRIGGER update_cities_updated_at BEFORE UPDATE ON cities 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert dữ liệu các thành phố Việt Nam
INSERT INTO cities (name, country, region, population, area_km2, latitude, longitude, timezone, is_capital, founded_year, description) VALUES
('Hà Nội', 'Vietnam', 'Northern Vietnam', 8330000, 3358.90, 21.028511, 105.804817, 'Asia/Ho_Chi_Minh', true, 1010, 'Thủ đô của Việt Nam, trung tâm chính trị và văn hóa'),
('Hồ Chí Minh', 'Vietnam', 'Southern Vietnam', 9000000, 2061.00, 10.823099, 106.629664, 'Asia/Ho_Chi_Minh', false, 1698, 'Thành phố lớn nhất Việt Nam, trung tâm kinh tế'),
('Đà Nẵng', 'Vietnam', 'Central Vietnam', 1134000, 1285.40, 16.047079, 108.206230, 'Asia/Ho_Chi_Minh', false, 1858, 'Thành phố trung tâm miền Trung, cảng biển quan trọng'),
('Hải Phòng', 'Vietnam', 'Northern Vietnam', 2028000, 1561.82, 20.844721, 106.687972, 'Asia/Ho_Chi_Minh', false, 1888, 'Thành phố cảng lớn thứ 3 của Việt Nam'),
('Cần Thơ', 'Vietnam', 'Mekong Delta', 1235000, 1409.00, 10.045162, 105.746857, 'Asia/Ho_Chi_Minh', false, 1739, 'Trung tâm vùng Đồng bằng sông Cửu Long'),
('Huế', 'Vietnam', 'Central Vietnam', 455000, 83.30, 16.461900, 107.594100, 'Asia/Ho_Chi_Minh', false, 1687, 'Cố đô Việt Nam, di sản văn hóa thế giới'),
('Nha Trang', 'Vietnam', 'Central Vietnam', 423000, 251.00, 12.238791, 109.196749, 'Asia/Ho_Chi_Minh', false, 1653, 'Thành phố biển du lịch nổi tiếng'),
('Vũng Tàu', 'Vietnam', 'Southern Vietnam', 327000, 140.00, 10.345596, 107.084503, 'Asia/Ho_Chi_Minh', false, 1898, 'Thành phố du lích biển gần TP.HCM'),
('Quy Nhon', 'Vietnam', 'Central Vietnam', 457000, 284.28, 13.777600, 109.173800, 'Asia/Ho_Chi_Minh', false, 1024, 'Thành phố cảng miền Trung'),
('Buôn Ma Thuột', 'Vietnam', 'Central Highlands', 340000, 369.06, 12.667800, 108.041700, 'Asia/Ho_Chi_Minh', false, 1904, 'Thủ phủ cà phê Việt Nam');

-- Insert thêm một số thành phố quốc tế
INSERT INTO cities (name, country, region, population, area_km2, latitude, longitude, timezone, is_capital, founded_year, description) VALUES
('Tokyo', 'Japan', 'Kanto', 13960000, 2194.07, 35.676098, 139.650311, 'Asia/Tokyo', true, 1457, 'Thủ đô và thành phố lớn nhất Nhật Bản'),
('Seoul', 'South Korea', 'Seoul Capital Area', 9720000, 605.21, 37.566535, 126.977969, 'Asia/Seoul', true, 18, 'Thủ đô và trung tâm kinh tế Hàn Quốc'),
('Bangkok', 'Thailand', 'Central Thailand', 8280000, 1568.70, 13.756331, 100.501765, 'Asia/Bangkok', true, 1782, 'Thủ đô và thành phố lớn nhất Thái Lan'),
('Singapore', 'Singapore', 'Southeast Asia', 5640000, 719.80, 1.352083, 103.819836, 'Asia/Singapore', true, 1819, 'Đảo quốc và thành phố thông minh'),
('Kuala Lumpur', 'Malaysia', 'Klang Valley', 1780000, 243.65, 3.139003, 101.686855, 'Asia/Kuala_Lumpur', true, 1857, 'Thủ đô Malaysia, trung tâm kinh tế');