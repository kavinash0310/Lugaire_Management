CREATE TABLE expense_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_expense_categories_code UNIQUE (code)
);

INSERT INTO expense_categories (name, code, active, created_at, updated_at) VALUES
('Packaging', 'PKG', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Courier', 'CUR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Printing', 'PRN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Advertising', 'ADV', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Marketing', 'MKT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Rent', 'RNT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Electricity', 'ELEC', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Internet', 'INET', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Software', 'SFTW', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Salaries', 'SAL', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Labour', 'LAB', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Transportation', 'TRAN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Office Supplies', 'OFF', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Equipment', 'EQU', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Maintenance', 'MNT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Miscellaneous', 'MSC', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

CREATE TABLE expenses (
    id BIGSERIAL PRIMARY KEY,
    expense_number VARCHAR(50) NOT NULL,
    expense_date DATE NOT NULL,
    category_id BIGINT NOT NULL,
    supplier_id BIGINT,
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    tax_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (tax_amount >= 0),
    total_amount NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
    payment_method VARCHAR(30) NOT NULL,
    payment_status VARCHAR(30) NOT NULL,
    reference_number VARCHAR(100),
    remarks TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_expenses_expense_number UNIQUE (expense_number),
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES expense_categories(id),
    CONSTRAINT fk_expenses_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT chk_expenses_total CHECK (total_amount = amount + tax_amount),
    CONSTRAINT chk_expenses_payment_method CHECK (payment_method IN ('CASH', 'BANK_TRANSFER', 'UPI', 'CARD', 'OTHER')),
    CONSTRAINT chk_expenses_payment_status CHECK (payment_status IN ('PENDING', 'PAID'))
);

CREATE INDEX idx_expenses_expense_number ON expenses (expense_number);
CREATE INDEX idx_expenses_date ON expenses (expense_date);
CREATE INDEX idx_expenses_category ON expenses (category_id);
CREATE INDEX idx_expenses_supplier ON expenses (supplier_id);
CREATE INDEX idx_expenses_payment_status ON expenses (payment_status);
CREATE INDEX idx_expenses_payment_method ON expenses (payment_method);
