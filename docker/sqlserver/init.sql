-- Sample database and metadata for the SQL Server Metadata Viewer
IF DB_ID('SampleDB') IS NOT NULL
BEGIN
    ALTER DATABASE SampleDB SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE SampleDB;
END
GO

CREATE DATABASE SampleDB;
GO

USE SampleDB;
GO

CREATE SCHEMA app;
GO

CREATE TABLE app.T_ORDER (
    ORDER_ID INT IDENTITY(1,1) PRIMARY KEY,
    ORDER_NO VARCHAR(50) NOT NULL,
    CUSTOMER_NAME NVARCHAR(100) NOT NULL,
    ORDER_DATE DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    TOTAL_AMOUNT DECIMAL(18,2) NOT NULL
);
GO

CREATE TABLE app.T_ORDER_ITEM (
    ORDER_ITEM_ID INT IDENTITY(1,1) PRIMARY KEY,
    ORDER_ID INT NOT NULL,
    PRODUCT_CODE VARCHAR(30) NOT NULL,
    PRODUCT_NAME NVARCHAR(100) NOT NULL,
    QUANTITY INT NOT NULL,
    UNIT_PRICE DECIMAL(18,2) NOT NULL,
    CONSTRAINT FK_ORDER_ITEM_ORDER FOREIGN KEY (ORDER_ID) REFERENCES app.T_ORDER(ORDER_ID)
);
GO

INSERT INTO app.T_ORDER (ORDER_NO, CUSTOMER_NAME, ORDER_DATE, TOTAL_AMOUNT) VALUES
('ORD-001', N'山田太郎', SYSDATETIME(), 12000.50),
('ORD-002', N'佐藤花子', SYSDATETIME(), 5400.00);
GO

INSERT INTO app.T_ORDER_ITEM (ORDER_ID, PRODUCT_CODE, PRODUCT_NAME, QUANTITY, UNIT_PRICE) VALUES
(1, 'P-100', N'ノートPC', 1, 12000.50),
(2, 'P-200', N'マウス', 2, 2000.00),
(2, 'P-201', N'キーボード', 1, 1400.00);
GO

-- 追加サンプル: 顧客マスタ
CREATE TABLE app.M_CUSTOMER (
    CUSTOMER_ID INT IDENTITY(1,1) PRIMARY KEY,
    CUSTOMER_NO VARCHAR(20) NOT NULL UNIQUE,
    CUSTOMER_NAME NVARCHAR(100) NOT NULL,
    PREF_CODE CHAR(2) NULL,
    TEL VARCHAR(20) NULL
);
GO

INSERT INTO app.M_CUSTOMER (CUSTOMER_NO, CUSTOMER_NAME, PREF_CODE, TEL) VALUES
('CUST-001', N'山田太郎', '13', '03-1111-1111'),
('CUST-002', N'佐藤花子', '27', '06-2222-2222'),
('CUST-003', N'鈴木次郎', '14', '045-333-3333');
GO

-- 追加サンプル: 商品マスタ
CREATE TABLE app.M_PRODUCT (
    PRODUCT_CODE VARCHAR(30) PRIMARY KEY,
    PRODUCT_NAME NVARCHAR(100) NOT NULL,
    CATEGORY NVARCHAR(50) NOT NULL,
    UNIT_PRICE DECIMAL(18,2) NOT NULL,
    ACTIVE_FLG BIT NOT NULL DEFAULT 1
);
GO

INSERT INTO app.M_PRODUCT (PRODUCT_CODE, PRODUCT_NAME, CATEGORY, UNIT_PRICE, ACTIVE_FLG) VALUES
('P-100', N'ノートPC', N'PC', 12000.50, 1),
('P-200', N'マウス', N'周辺機器', 2000.00, 1),
('P-201', N'キーボード', N'周辺機器', 1400.00, 1),
('P-300', N'外付けHDD', N'ストレージ', 8800.00, 1);
GO

-- Extended properties for table descriptions
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'受注テーブル', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'T_ORDER';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'受注明細テーブル', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'T_ORDER_ITEM';
GO

-- Extended properties for column descriptions (logical names)
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'受注ID', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'T_ORDER', @level2type=N'COLUMN',@level2name=N'ORDER_ID';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'受注番号', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'T_ORDER', @level2type=N'COLUMN',@level2name=N'ORDER_NO';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'顧客名', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'T_ORDER', @level2type=N'COLUMN',@level2name=N'CUSTOMER_NAME';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'受注日', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'T_ORDER', @level2type=N'COLUMN',@level2name=N'ORDER_DATE';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'合計金額', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'T_ORDER', @level2type=N'COLUMN',@level2name=N'TOTAL_AMOUNT';

EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'受注明細ID', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'T_ORDER_ITEM', @level2type=N'COLUMN',@level2name=N'ORDER_ITEM_ID';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'受注ID', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'T_ORDER_ITEM', @level2type=N'COLUMN',@level2name=N'ORDER_ID';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'商品コード', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'T_ORDER_ITEM', @level2type=N'COLUMN',@level2name=N'PRODUCT_CODE';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'商品名', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'T_ORDER_ITEM', @level2type=N'COLUMN',@level2name=N'PRODUCT_NAME';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'数量', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'T_ORDER_ITEM', @level2type=N'COLUMN',@level2name=N'QUANTITY';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'単価', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'T_ORDER_ITEM', @level2type=N'COLUMN',@level2name=N'UNIT_PRICE';
GO

-- 顧客マスタ (M_CUSTOMER) 拡張プロパティ
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'顧客マスタ', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'M_CUSTOMER';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'顧客ID', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'M_CUSTOMER', @level2type=N'COLUMN',@level2name=N'CUSTOMER_ID';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'顧客番号', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'M_CUSTOMER', @level2type=N'COLUMN',@level2name=N'CUSTOMER_NO';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'顧客名', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'M_CUSTOMER', @level2type=N'COLUMN',@level2name=N'CUSTOMER_NAME';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'都道府県コード', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'M_CUSTOMER', @level2type=N'COLUMN',@level2name=N'PREF_CODE';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'電話番号', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'M_CUSTOMER', @level2type=N'COLUMN',@level2name=N'TEL';
GO

-- 商品マスタ (M_PRODUCT) 拡張プロパティ
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'商品マスタ', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'M_PRODUCT';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'商品コード', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'M_PRODUCT', @level2type=N'COLUMN',@level2name=N'PRODUCT_CODE';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'商品名', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'M_PRODUCT', @level2type=N'COLUMN',@level2name=N'PRODUCT_NAME';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'カテゴリ', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'M_PRODUCT', @level2type=N'COLUMN',@level2name=N'CATEGORY';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'単価', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'M_PRODUCT', @level2type=N'COLUMN',@level2name=N'UNIT_PRICE';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'有効フラグ', @level0type=N'SCHEMA',@level0name=N'app', @level1type=N'TABLE',@level1name=N'M_PRODUCT', @level2type=N'COLUMN',@level2name=N'ACTIVE_FLG';
GO
