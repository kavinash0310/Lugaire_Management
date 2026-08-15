create table settings (
    id bigserial primary key,
    setting_key varchar(100) not null unique,
    setting_value text not null,
    setting_type varchar(20) not null,
    description text,
    updated_at timestamptz not null default current_timestamp
);

insert into settings (setting_key, setting_value, setting_type, description, updated_at) values
('business_name', 'LUGAIRE', 'STRING', 'Business/Store Name', current_timestamp),
('brand_name', 'LUGAIRE', 'STRING', 'Brand Name', current_timestamp),
('business_email', '', 'STRING', 'Business Email', current_timestamp),
('business_phone', '', 'STRING', 'Business Phone', current_timestamp),
('business_address', '', 'STRING', 'Business Address', current_timestamp),
('currency', 'INR', 'STRING', 'Currency', current_timestamp),
('currency_symbol', '₹', 'STRING', 'Currency Symbol', current_timestamp),
('date_format', 'dd/MM/yyyy', 'STRING', 'Date Format', current_timestamp),
('default_low_stock_threshold', '5', 'NUMBER', 'Default Low Stock Threshold', current_timestamp),
('allow_negative_stock', 'false', 'BOOLEAN', 'Allow Negative Stock', current_timestamp),
('default_order_status', 'PENDING', 'STRING', 'Default Order Status', current_timestamp),
('default_payment_status', 'PENDING', 'STRING', 'Default Payment Status', current_timestamp),
('default_tax_rate', '0', 'NUMBER', 'Default Tax Rate', current_timestamp);
