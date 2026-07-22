"""
╔══════════════════════════════════════════════════════════════════╗
║  Rafeeq Kernel — Auto-Migration System v3.1.0                   ║
║  Flask + SQLAlchemy — dtr1-n                                    ║
║  نظام تحديث تلقائي + نظام التوكيلات التجارية + إشعارات       ║
╚══════════════════════════════════════════════════════════════════╝
"""

import os
import json
import hashlib
import logging
from datetime import datetime
from typing import Dict, List, Any, Optional
from dataclasses import dataclass, field
from enum import Enum

from sqlalchemy import create_engine, MetaData, Table, Column, inspect, text
from sqlalchemy.types import (
    Integer, String, Text, Boolean, DateTime, Float, 
    BigInteger, JSON, LargeBinary, Date, Time
)
from sqlalchemy.exc import SQLAlchemyError

logger = logging.getLogger('rafeeq.migrate')

DATABASE_URL = os.getenv('DATABASE_URL', 'sqlite:///rafeeq.db')
if DATABASE_URL.startswith('postgres://'):
    DATABASE_URL = DATABASE_URL.replace('postgres://', 'postgresql://', 1)

class FieldType(Enum):
    STRING = "string"
    TEXT = "text"
    INTEGER = "integer"
    BIGINT = "bigint"
    BOOLEAN = "boolean"
    DATETIME = "datetime"
    DATE = "date"
    TIME = "time"
    FLOAT = "float"
    JSON = "json"
    BINARY = "binary"

TYPE_MAP = {
    FieldType.STRING: String,
    FieldType.TEXT: Text,
    FieldType.INTEGER: Integer,
    FieldType.BIGINT: BigInteger,
    FieldType.BOOLEAN: Boolean,
    FieldType.DATETIME: DateTime,
    FieldType.DATE: Date,
    FieldType.TIME: Time,
    FieldType.FLOAT: Float,
    FieldType.JSON: JSON,
    FieldType.BINARY: LargeBinary,
}

@dataclass
class FieldDef:
    name: str
    type: FieldType
    length: Optional[int] = None
    nullable: bool = True
    default: Any = None
    primary_key: bool = False
    unique: bool = False
    index: bool = False
    auto_increment: bool = False

    def to_sqlalchemy(self):
        col_type = TYPE_MAP[self.type]
        if self.length and self.type == FieldType.STRING:
            col_type = col_type(self.length)
        kwargs = {}
        if self.primary_key:
            kwargs['primary_key'] = True
        if not self.nullable:
            kwargs['nullable'] = False
        if self.unique:
            kwargs['unique'] = True
        if self.index:
            kwargs['index'] = True
        if self.default is not None:
            kwargs['default'] = self.default
        if self.auto_increment:
            kwargs['autoincrement'] = True
        return Column(self.name, col_type, **kwargs)

@dataclass
class ModelDef:
    name: str
    fields: List[FieldDef]
    indexes: List[Dict[str, Any]] = field(default_factory=list)
    description: Optional[str] = None

    def to_sqlalchemy_table(self, metadata: MetaData) -> Table:
        columns = [f.to_sqlalchemy() for f in self.fields]
        return Table(self.name, metadata, *columns)

@dataclass
class SchemaDef:
    version: str
    models: List[ModelDef]
    created_at: datetime = field(default_factory=datetime.now)

    def get_model(self, name: str) -> Optional[ModelDef]:
        for m in self.models:
            if m.name == name:
                return m
        return None

    def to_hash(self) -> str:
        schema_str = json.dumps({
            "version": self.version,
            "models": [
                {"name": m.name, "fields": [{"name": f.name, "type": f.type.value} for f in m.fields]}
                for m in self.models
            ]
        }, sort_keys=True)
        return hashlib.sha256(schema_str.encode()).hexdigest()[:16]

def init_migration_tracking(engine):
    metadata = MetaData()
    Table('__migrations__', metadata,
        Column('id', Integer, primary_key=True, autoincrement=True),
        Column('version', String(50), nullable=False),
        Column('schema_hash', String(32), nullable=False),
        Column('description', Text),
        Column('sql_commands', Text, nullable=False),
        Column('tables_affected', Text),
        Column('executed_at', DateTime, default=datetime.utcnow),
        Column('duration_ms', Integer),
        Column('status', String(20), default='success'),
        Column('error_message', Text),
    )
    Table('__schema_version__', metadata,
        Column('id', Integer, primary_key=True, autoincrement=True),
        Column('version', String(50), nullable=False),
        Column('schema_hash', String(32), nullable=False),
        Column('tables', Text),
        Column('applied_at', DateTime, default=datetime.utcnow),
        Column('is_current', Boolean, default=True),
    )
    metadata.create_all(engine)
    logger.info("Migration tracking tables initialized")

class AutoMigrationEngine:
    def __init__(self, db_url: str = DATABASE_URL):
        self.engine = create_engine(db_url)
        self.inspector = inspect(self.engine)
        init_migration_tracking(self.engine)

    def get_current_version(self) -> Optional[Dict]:
        try:
            with self.engine.connect() as conn:
                result = conn.execute(text("""
                    SELECT * FROM __schema_version__ 
                    WHERE is_current = TRUE 
                    ORDER BY applied_at DESC 
                    LIMIT 1
                """))
                row = result.fetchone()
                if row:
                    return dict(row._mapping)
        except Exception as e:
            logger.warning(f"Could not get current version: {e}")
        return None

    def get_existing_tables(self) -> List[str]:
        return self.inspector.get_table_names()

    def get_existing_columns(self, table_name: str) -> List[Dict]:
        try:
            return self.inspector.get_columns(table_name)
        except Exception:
            return []

    def detect_changes(self, schema: SchemaDef) -> List[Dict]:
        changes = []
        existing_tables = self.get_existing_tables()
        for model in schema.models:
            if model.name not in existing_tables:
                changes.append({
                    "type": "CREATE_TABLE",
                    "table": model.name,
                    "model": model,
                    "description": f"Create new table: {model.name}"
                })
            else:
                existing_cols = {c['name']: c for c in self.get_existing_columns(model.name)}
                for field in model.fields:
                    if field.name not in existing_cols:
                        changes.append({
                            "type": "ADD_COLUMN",
                            "table": model.name,
                            "column": field.name,
                            "field": field,
                            "description": f"Add column {field.name} to {model.name}"
                        })
                model_fields = {f.name for f in model.fields}
                for col_name in existing_cols:
                    if col_name not in model_fields and not col_name.startswith('__'):
                        changes.append({
                            "type": "WARNING",
                            "table": model.name,
                            "column": col_name,
                            "description": f"Column '{col_name}' exists in DB but not in schema."
                        })
        return changes

    def execute_migration(self, schema: SchemaDef, description: str = "") -> Dict:
        start_time = datetime.now()
        changes = self.detect_changes(schema)
        if not changes:
            return {"success": True, "message": "No changes needed", "version": schema.version, "changes": [], "duration_ms": 0}
        actionable = [c for c in changes if c["type"] in ("CREATE_TABLE", "ADD_COLUMN")]
        warnings = [c for c in changes if c["type"] == "WARNING"]
        if not actionable:
            return {"success": True, "message": "No actionable changes", "version": schema.version, "changes": changes, "duration_ms": 0}

        sql_commands = []
        tables_affected = []
        with self.engine.connect() as conn:
            for change in actionable:
                if change["type"] == "CREATE_TABLE":
                    model = change["model"]
                    metadata = MetaData()
                    table = model.to_sqlalchemy_table(metadata)
                    from sqlalchemy.schema import CreateTable
                    create_sql = str(CreateTable(table).compile(self.engine))
                    conn.execute(text(create_sql))
                    sql_commands.append(create_sql)
                    tables_affected.append(model.name)
                    logger.info(f"Created table: {model.name}")
                elif change["type"] == "ADD_COLUMN":
                    table_name = change["table"]
                    field = change["field"]
                    col_def = field.to_sqlalchemy()
                    col_type_str = str(col_def.type.compile(self.engine))
                    nullable_str = "NULL" if field.nullable else "NOT NULL"
                    default_str = f"DEFAULT {field.default}" if field.default is not None else ""
                    alter_sql = f"ALTER TABLE {table_name} ADD COLUMN IF NOT EXISTS {field.name} {col_type_str} {nullable_str} {default_str}".strip()
                    conn.execute(text(alter_sql))
                    sql_commands.append(alter_sql)
                    if table_name not in tables_affected:
                        tables_affected.append(table_name)
                    logger.info(f"Added column: {field.name} to {table_name}")
            conn.commit()

        duration = int((datetime.now() - start_time).total_seconds() * 1000)
        schema_hash = schema.to_hash()
        with self.engine.connect() as conn:
            conn.execute(text("UPDATE __schema_version__ SET is_current = FALSE"))
            conn.execute(text(f"""
                INSERT INTO __schema_version__ (version, schema_hash, tables, is_current)
                VALUES ('{schema.version}', '{schema_hash}', '{json.dumps(tables_affected)}', TRUE)
            """))
            conn.execute(text(f"""
                INSERT INTO __migrations__ 
                (version, schema_hash, description, sql_commands, tables_affected, duration_ms, status)
                VALUES ('{schema.version}', '{schema_hash}', 
                        '{description or f"Auto-migration to {schema.version}"}',
                        '{json.dumps(sql_commands)}', '{json.dumps(tables_affected)}',
                        {duration}, 'success')
            """))
            conn.commit()

        return {
            "success": True, "message": f"Migration to {schema.version} completed",
            "version": schema.version, "schema_hash": schema_hash,
            "changes_count": len(actionable), "changes": actionable,
            "warnings": warnings, "tables_affected": tables_affected,
            "duration_ms": duration, "sql_executed": sql_commands
        }

    def get_migration_history(self, limit: int = 50) -> List[Dict]:
        try:
            with self.engine.connect() as conn:
                result = conn.execute(text(f"""
                    SELECT * FROM __migrations__ 
                    ORDER BY executed_at DESC 
                    LIMIT {limit}
                """))
                return [dict(row._mapping) for row in result.fetchall()]
        except Exception as e:
            logger.error(f"Error getting migration history: {e}")
            return []

class SchemaRegistry:
    @staticmethod
    def get_schema() -> SchemaDef:
        # ── Core Models ──
        users = ModelDef(name="users", description="بيانات المستخدمين",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="username", type=FieldType.STRING, length=80, nullable=False, unique=True, index=True),
                FieldDef(name="email", type=FieldType.STRING, length=120, nullable=False, unique=True, index=True),
                FieldDef(name="password_hash", type=FieldType.STRING, length=256, nullable=False),
                FieldDef(name="full_name", type=FieldType.STRING, length=100, nullable=True),
                FieldDef(name="avatar", type=FieldType.STRING, length=200, nullable=True, default="👤"),
                FieldDef(name="is_active", type=FieldType.BOOLEAN, default=True, nullable=False),
                FieldDef(name="is_admin", type=FieldType.BOOLEAN, default=False, nullable=False),
                FieldDef(name="is_premium", type=FieldType.BOOLEAN, default=False, nullable=False),
                FieldDef(name="role", type=FieldType.STRING, length=50, default="user", nullable=False),
                FieldDef(name="created_at", type=FieldType.DATETIME, default="now", nullable=False),
                FieldDef(name="last_login", type=FieldType.DATETIME, nullable=True),
                FieldDef(name="login_count", type=FieldType.INTEGER, default=0, nullable=False),
            ])

        sessions = ModelDef(name="sessions", description="جلسات تسجيل الدخول",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="user_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="session_token", type=FieldType.STRING, length=256, nullable=False, unique=True),
                FieldDef(name="ip_address", type=FieldType.STRING, length=45, nullable=True),
                FieldDef(name="user_agent", type=FieldType.STRING, length=500, nullable=True),
                FieldDef(name="created_at", type=FieldType.DATETIME, default="now", nullable=False),
                FieldDef(name="expires_at", type=FieldType.DATETIME, nullable=True),
                FieldDef(name="is_active", type=FieldType.BOOLEAN, default=True, nullable=False),
            ])

        activities = ModelDef(name="activities", description="سجل النشاطات",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="user_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="action", type=FieldType.STRING, length=100, nullable=False),
                FieldDef(name="details", type=FieldType.TEXT, nullable=True),
                FieldDef(name="ip_address", type=FieldType.STRING, length=45, nullable=True),
                FieldDef(name="created_at", type=FieldType.DATETIME, default="now", nullable=False),
            ])

        config = ModelDef(name="config", description="إعدادات النظام",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="key", type=FieldType.STRING, length=100, nullable=False, unique=True),
                FieldDef(name="value", type=FieldType.TEXT, nullable=True),
                FieldDef(name="updated_at", type=FieldType.DATETIME, default="now", nullable=False),
            ])

        # ── 🏪 STORE SYSTEM MODELS ──
        store_applications = ModelDef(name="store_applications", description="طلبات التوكيل",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="user_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="store_name", type=FieldType.STRING, length=200, nullable=False),
                FieldDef(name="store_slug", type=FieldType.STRING, length=200, nullable=False, unique=True),
                FieldDef(name="store_description", type=FieldType.TEXT, nullable=True),
                FieldDef(name="business_type", type=FieldType.STRING, length=100, nullable=False),
                FieldDef(name="requested_slots", type=FieldType.INTEGER, nullable=False),
                FieldDef(name="contact_phone", type=FieldType.STRING, length=50, nullable=True),
                FieldDef(name="contact_email", type=FieldType.STRING, length=120, nullable=True),
                FieldDef(name="business_license", type=FieldType.STRING, length=500, nullable=True),
                FieldDef(name="logo_url", type=FieldType.STRING, length=500, nullable=True),
                FieldDef(name="status", type=FieldType.STRING, length=50, default="pending", nullable=False),
                FieldDef(name="admin_notes", type=FieldType.TEXT, nullable=True),
                FieldDef(name="approved_slots", type=FieldType.INTEGER, default=0, nullable=False),
                FieldDef(name="monthly_fee", type=FieldType.FLOAT, default=0.0, nullable=False),
                FieldDef(name="commission_rate", type=FieldType.FLOAT, default=5.0, nullable=False),
                FieldDef(name="contract_start", type=FieldType.DATETIME, nullable=True),
                FieldDef(name="contract_end", type=FieldType.DATETIME, nullable=True),
                FieldDef(name="created_at", type=FieldType.DATETIME, default="now", nullable=False),
                FieldDef(name="updated_at", type=FieldType.DATETIME, default="now", nullable=False),
            ])

        store_slots = ModelDef(name="store_slots", description="مساحات العرض",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="slot_code", type=FieldType.STRING, length=50, nullable=False, unique=True),
                FieldDef(name="slot_name", type=FieldType.STRING, length=100, nullable=False),
                FieldDef(name="location", type=FieldType.STRING, length=200, nullable=True),
                FieldDef(name="size", type=FieldType.STRING, length=50, nullable=True),
                FieldDef(name="base_price", type=FieldType.FLOAT, default=0.0, nullable=False),
                FieldDef(name="features", type=FieldType.TEXT, nullable=True),
                FieldDef(name="is_available", type=FieldType.BOOLEAN, default=True, nullable=False),
                FieldDef(name="application_id", type=FieldType.INTEGER, nullable=True, index=True),
                FieldDef(name="created_at", type=FieldType.DATETIME, default="now", nullable=False),
            ])

        store_products = ModelDef(name="store_products", description="منتجات التجار",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="application_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="slot_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="product_name", type=FieldType.STRING, length=200, nullable=False),
                FieldDef(name="product_slug", type=FieldType.STRING, length=200, nullable=False, unique=True),
                FieldDef(name="product_description", type=FieldType.TEXT, nullable=True),
                FieldDef(name="category", type=FieldType.STRING, length=100, nullable=True),
                FieldDef(name="price", type=FieldType.FLOAT, default=0.0, nullable=False),
                FieldDef(name="old_price", type=FieldType.FLOAT, nullable=True),
                FieldDef(name="currency", type=FieldType.STRING, length=10, default="EGP", nullable=False),
                FieldDef(name="stock_quantity", type=FieldType.INTEGER, default=0, nullable=False),
                FieldDef(name="images", type=FieldType.TEXT, nullable=True),
                FieldDef(name="is_featured", type=FieldType.BOOLEAN, default=False, nullable=False),
                FieldDef(name="is_active", type=FieldType.BOOLEAN, default=True, nullable=False),
                FieldDef(name="display_order", type=FieldType.INTEGER, default=0, nullable=False),
                FieldDef(name="views_count", type=FieldType.INTEGER, default=0, nullable=False),
                FieldDef(name="sales_count", type=FieldType.INTEGER, default=0, nullable=False),
                FieldDef(name="created_at", type=FieldType.DATETIME, default="now", nullable=False),
                FieldDef(name="updated_at", type=FieldType.DATETIME, default="now", nullable=False),
            ])

        store_contracts = ModelDef(name="store_contracts", description="عقود التوكيل",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="application_id", type=FieldType.INTEGER, nullable=False, unique=True, index=True),
                FieldDef(name="contract_terms", type=FieldType.TEXT, nullable=False),
                FieldDef(name="commission_rate", type=FieldType.FLOAT, default=5.0, nullable=False),
                FieldDef(name="payment_terms", type=FieldType.STRING, length=200, nullable=True),
                FieldDef(name="cancellation_policy", type=FieldType.TEXT, nullable=True),
                FieldDef(name="special_conditions", type=FieldType.TEXT, nullable=True),
                FieldDef(name="status", type=FieldType.STRING, length=50, default="draft", nullable=False),
                FieldDef(name="signed_by_merchant", type=FieldType.BOOLEAN, default=False, nullable=False),
                FieldDef(name="signed_by_admin", type=FieldType.BOOLEAN, default=False, nullable=False),
                FieldDef(name="signed_at", type=FieldType.DATETIME, nullable=True),
                FieldDef(name="expires_at", type=FieldType.DATETIME, nullable=False),
                FieldDef(name="created_at", type=FieldType.DATETIME, default="now", nullable=False),
            ])

        store_payments = ModelDef(name="store_payments", description="مدفوعات التجار",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="application_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="amount", type=FieldType.FLOAT, nullable=False),
                FieldDef(name="payment_type", type=FieldType.STRING, length=50, nullable=False),
                FieldDef(name="period_start", type=FieldType.DATETIME, nullable=True),
                FieldDef(name="period_end", type=FieldType.DATETIME, nullable=True),
                FieldDef(name="status", type=FieldType.STRING, length=50, default="pending", nullable=False),
                FieldDef(name="transaction_id", type=FieldType.STRING, length=200, nullable=True),
                FieldDef(name="payment_method", type=FieldType.STRING, length=100, nullable=True),
                FieldDef(name="notes", type=FieldType.TEXT, nullable=True),
                FieldDef(name="created_at", type=FieldType.DATETIME, default="now", nullable=False),
            ])

        store_orders = ModelDef(name="store_orders", description="طلبات الشراء",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="product_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="buyer_name", type=FieldType.STRING, length=100, nullable=False),
                FieldDef(name="buyer_phone", type=FieldType.STRING, length=50, nullable=False),
                FieldDef(name="buyer_email", type=FieldType.STRING, length=120, nullable=True),
                FieldDef(name="buyer_address", type=FieldType.TEXT, nullable=True),
                FieldDef(name="quantity", type=FieldType.INTEGER, default=1, nullable=False),
                FieldDef(name="total_price", type=FieldType.FLOAT, nullable=False),
                FieldDef(name="status", type=FieldType.STRING, length=50, default="pending", nullable=False),
                FieldDef(name="notes", type=FieldType.TEXT, nullable=True),
                FieldDef(name="created_at", type=FieldType.DATETIME, default="now", nullable=False),
                FieldDef(name="updated_at", type=FieldType.DATETIME, default="now", nullable=False),
            ])

        # ── 🔔 NOTIFICATIONS ──
        notifications = ModelDef(name="notifications", description="إشعارات المستخدمين",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="user_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="title", type=FieldType.STRING, length=200, nullable=False),
                FieldDef(name="message", type=FieldType.TEXT, nullable=False),
                FieldDef(name="type", type=FieldType.STRING, length=50, default="info", nullable=False),
                FieldDef(name="is_read", type=FieldType.BOOLEAN, default=False, nullable=False),
                FieldDef(name="link", type=FieldType.STRING, length=500, nullable=True),
                FieldDef(name="created_at", type=FieldType.DATETIME, default="now", nullable=False),
            ])

        # ── 🌐 WEBSITE BUILDER MODELS ──
        websites = ModelDef(name="websites", description="المواقع الإلكترونية المنشأة",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="user_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="site_name", type=FieldType.STRING, length=200, nullable=False),
                FieldDef(name="site_slug", type=FieldType.STRING, length=200, nullable=False, unique=True, index=True),
                FieldDef(name="business_category", type=FieldType.STRING, length=100, nullable=True),
                FieldDef(name="site_title", type=FieldType.STRING, length=200, nullable=True),
                FieldDef(name="description", type=FieldType.TEXT, nullable=True),
                FieldDef(name="logo_url", type=FieldType.STRING, length=500, nullable=True),
                FieldDef(name="theme_color", type=FieldType.STRING, length=50, default="#0ea5e9", nullable=False),
                FieldDef(name="accent_color", type=FieldType.STRING, length=50, default="#6366f1", nullable=False),
                FieldDef(name="bg_style", type=FieldType.STRING, length=50, default="dark", nullable=False),
                FieldDef(name="font_family", type=FieldType.STRING, length=50, default="sans-serif", nullable=False),
                FieldDef(name="contact_email", type=FieldType.STRING, length=120, nullable=True),
                FieldDef(name="contact_phone", type=FieldType.STRING, length=50, nullable=True),
                FieldDef(name="contact_address", type=FieldType.STRING, length=200, nullable=True),
                FieldDef(name="social_links", type=FieldType.TEXT, nullable=True),
                FieldDef(name="is_published", type=FieldType.BOOLEAN, default=True, nullable=False),
                FieldDef(name="views_count", type=FieldType.INTEGER, default=0, nullable=False),
                FieldDef(name="created_at", type=FieldType.DATETIME, default="now", nullable=False),
                FieldDef(name="updated_at", type=FieldType.DATETIME, default="now", nullable=False),
            ])

        website_pages = ModelDef(name="website_pages", description="صفحات الموقع الإلكتروني",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="website_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="page_title", type=FieldType.STRING, length=200, nullable=False),
                FieldDef(name="page_slug", type=FieldType.STRING, length=200, nullable=False),
                FieldDef(name="is_home", type=FieldType.BOOLEAN, default=False, nullable=False),
                FieldDef(name="sections_data", type=FieldType.TEXT, nullable=False),
                FieldDef(name="display_order", type=FieldType.INTEGER, default=0, nullable=False),
                FieldDef(name="created_at", type=FieldType.DATETIME, default="now", nullable=False),
                FieldDef(name="updated_at", type=FieldType.DATETIME, default="now", nullable=False),
            ])

        website_messages = ModelDef(name="website_messages", description="رسائل التواصل بالموقع",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="website_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="sender_name", type=FieldType.STRING, length=100, nullable=False),
                FieldDef(name="sender_email", type=FieldType.STRING, length=120, nullable=False),
                FieldDef(name="sender_phone", type=FieldType.STRING, length=50, nullable=True),
                FieldDef(name="subject", type=FieldType.STRING, length=200, nullable=True),
                FieldDef(name="message", type=FieldType.TEXT, nullable=False),
                FieldDef(name="is_read", type=FieldType.BOOLEAN, default=False, nullable=False),
                FieldDef(name="created_at", type=FieldType.DATETIME, default="now", nullable=False),
            ])

        # ── 🐺 SOCIAL NETWORK MODELS ──
        social_posts = ModelDef(name="social_posts", description="منشورات التواصل الاجتماعي",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="user_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="content", type=FieldType.TEXT, nullable=False),
                FieldDef(name="media_type", type=FieldType.STRING, length=50, default="text", nullable=False),
                FieldDef(name="media_url", type=FieldType.STRING, length=500, nullable=True),
                FieldDef(name="likes_count", type=FieldType.INTEGER, default=0, nullable=False),
                FieldDef(name="comments_count", type=FieldType.INTEGER, default=0, nullable=False),
                FieldDef(name="shares_count", type=FieldType.INTEGER, default=0, nullable=False),
                FieldDef(name="created_at", type=FieldType.DATETIME, default="now", nullable=False),
                FieldDef(name="updated_at", type=FieldType.DATETIME, default="now", nullable=False),
            ])

        social_likes = ModelDef(name="social_likes", description="إعجابات المنشورات",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="post_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="user_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="created_at", type=FieldType.DATETIME, default="now", nullable=False),
            ])

        social_comments = ModelDef(name="social_comments", description="تعليقات المنشورات",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="post_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="user_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="comment_text", type=FieldType.TEXT, nullable=False),
                FieldDef(name="created_at", type=FieldType.DATETIME, default="now", nullable=False),
            ])

        social_followers = ModelDef(name="social_followers", description="متابعات شبكة التواصل",
            fields=[
                FieldDef(name="id", type=FieldType.INTEGER, primary_key=True, auto_increment=True, nullable=False),
                FieldDef(name="follower_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="followed_id", type=FieldType.INTEGER, nullable=False, index=True),
                FieldDef(name="created_at", type=FieldType.DATETIME, default="now", nullable=False),
            ])

        return SchemaDef(
            version="3.3.0",
            models=[users, sessions, activities, config,
                    store_applications, store_slots, store_products,
                    store_contracts, store_payments, store_orders,
                    notifications, websites, website_pages, website_messages,
                    social_posts, social_likes, social_comments, social_followers]
        )

def auto_migrate():
    engine = AutoMigrationEngine()
    schema = SchemaRegistry.get_schema()
    result = engine.execute_migration(schema, description="v3.1.0 — JWT Auth + Notifications + Mobile Dashboard")
    logger.info(f"Migration result: {result['message']}")
    return result

def get_migration_status():
    engine = AutoMigrationEngine()
    schema = SchemaRegistry.get_schema()
    changes = engine.detect_changes(schema)
    current = engine.get_current_version()
    return {
        "current_version": current.get("version") if current else None,
        "target_version": schema.version,
        "pending_changes": len([c for c in changes if c["type"] in ("CREATE_TABLE", "ADD_COLUMN")]),
        "warnings": len([c for c in changes if c["type"] == "WARNING"]),
        "changes": changes
    }

if __name__ == "__main__":
    result = auto_migrate()
    print(json.dumps(result, indent=2, default=str))
