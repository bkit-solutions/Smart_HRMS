

CREATE DATABASE employeemanagementsystem;

USE employeemanagementsystem;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('HR', 'Employee') NOT NULL,
    department ENUM('IT', 'NON-IT', 'HR') NOT NULL DEFAULT 'IT'
);

CREATE TABLE payroll (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    salary DECIMAL(10,2) NOT NULL,
    bonuses DECIMAL(10,2) DEFAULT 0.00,
    deductions DECIMAL(10,2) DEFAULT 0.00,
    net_salary DECIMAL(10,2) GENERATED ALWAYS AS (salary + bonuses - deductions) STORED,
    month VARCHAR(10) NOT NULL,
    year INT NOT NULL,
    paid_status ENUM('Paid', 'Pending') DEFAULT 'Pending',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE leave_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    leave_type ENUM('Sick', 'Casual', 'Paid') NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT,
    status ENUM('Pending', 'Approved', 'Rejected') DEFAULT 'Pending',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE attendance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    date DATE NOT NULL,
    check_in DATETIME DEFAULT NULL,
    check_out DATETIME DEFAULT NULL,
    status ENUM('Present', 'Absent', 'Leave') NOT NULL DEFAULT 'Absent',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

ALTER TABLE attendance
ADD COLUMN work_hours DOUBLE DEFAULT 0,
ADD COLUMN productivity_score DOUBLE DEFAULT 0;

CREATE TABLE performance_reports (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    month VARCHAR(20),
    year INT,

    avg_work_hours DOUBLE,
    avg_productivity DOUBLE,
    leave_count INT,

    performance_score DOUBLE,
    performance_grade VARCHAR(5),
    risk_level VARCHAR(20),
    recommendation TEXT,

    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_perf_user ON performance_reports(user_id);
CREATE INDEX idx_perf_year ON performance_reports(year);

ALTER TABLE payroll
ADD COLUMN ai_bonus DOUBLE DEFAULT 0;

ALTER TABLE leave_requests
ADD COLUMN impact_score DOUBLE DEFAULT 0;

ALTER TABLE attendance
ADD COLUMN warning_level VARCHAR(20) DEFAULT 'Normal';

CREATE TABLE IF NOT EXISTS performance_reports (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    score INT NOT NULL,
    grade VARCHAR(2) NOT NULL,
    risk VARCHAR(20),
    recommendation TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id)
);

ALTER TABLE performance_reports
ADD COLUMN score INT NOT NULL AFTER user_id;

ALTER TABLE performance_reports
ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;






