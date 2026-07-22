"""
╔══════════════════════════════════════════════════════════════╗
║  رفيق | Rafeeq — Store System v3.1.0                        ║
║  JWT Auth + HTTP-Only Cookies + Mobile-First Dashboard      ║
╚══════════════════════════════════════════════════════════════╝
"""

import os
import sys
import json
import logging
import subprocess
import re
import secrets
import hashlib
from datetime import datetime, timedelta
from functools import wraps

# ═══════════════════════════════════════════════════════════════
# SECTION 0: DATABASE MIGRATION
# ═══════════════════════════════════════════════════════════════

def run_db_migration():
    try:
        result = subprocess.run([sys.executable, 'auto_migrate.py'], capture_output=True, text=True, timeout=60)
        if result.returncode == 0:
            print("✅ DB Migration completed")
        else:
            print(f"⚠️ Migration warning: {result.stderr[:500]}")
    except Exception as e:
        print(f"⚠️ Migration error: {e}")

run_db_migration()

# ═══════════════════════════════════════════════════════════════
# SECTION 1: FLASK APP + JWT AUTH SETUP
# ═══════════════════════════════════════════════════════════════

from flask import Flask, request, jsonify, session, render_template, redirect, url_for, flash, abort, make_response
from flask_sqlalchemy import SQLAlchemy
from flask_bcrypt import Bcrypt
from sqlalchemy import text, func
import jwt

app = Flask(__name__, template_folder='templates', static_folder='static')
app.secret_key = os.getenv('SECRET_KEY', secrets.token_hex(32))

# JWT Config
JWT_SECRET = os.getenv('JWT_SECRET', app.secret_key)
JWT_ALGORITHM = 'HS256'
JWT_ACCESS_EXPIRY = timedelta(hours=24)
JWT_REFRESH_EXPIRY = timedelta(days=30)

# Database
DATABASE_URL = os.getenv('DATABASE_URL', 'sqlite:///rafeeq.db')
if DATABASE_URL.startswith('postgres://'):
    DATABASE_URL = DATABASE_URL.replace('postgres://', 'postgresql://', 1)
app.config['SQLALCHEMY_DATABASE_URI'] = DATABASE_URL
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['SQLALCHEMY_ENGINE_OPTIONS'] = {'pool_pre_ping': True, 'pool_recycle': 300}

db = SQLAlchemy(app)
bcrypt = Bcrypt(app)

@app.after_request
def add_cache_control_headers(response):
    if request.path.startswith('/api/'):
        response.headers['Cache-Control'] = 'no-cache, no-store, must-revalidate'
        response.headers['Pragma'] = 'no-cache'
        response.headers['Expires'] = '0'
    return response

# ═══════════════════════════════════════════════════════════════
# SECTION 2: MODELS (same as before + notifications)
# ═══════════════════════════════════════════════════════════════

class User(db.Model):
    __tablename__ = 'users'
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    password_hash = db.Column(db.String(256), nullable=False)
    full_name = db.Column(db.String(100))
    avatar = db.Column(db.String(200), default="👤")
    is_active = db.Column(db.Boolean, default=True)
    is_admin = db.Column(db.Boolean, default=False)
    is_premium = db.Column(db.Boolean, default=False)
    role = db.Column(db.String(50), default="user")
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    last_login = db.Column(db.DateTime)
    login_count = db.Column(db.Integer, default=0)

class StoreApplication(db.Model):
    __tablename__ = 'store_applications'
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    store_name = db.Column(db.String(200), nullable=False)
    store_slug = db.Column(db.String(200), unique=True, nullable=False)
    store_description = db.Column(db.Text)
    business_type = db.Column(db.String(100), nullable=False)
    requested_slots = db.Column(db.Integer, nullable=False)
    contact_phone = db.Column(db.String(50))
    contact_email = db.Column(db.String(120))
    business_license = db.Column(db.String(500))
    logo_url = db.Column(db.String(500))
    status = db.Column(db.String(50), default="pending")
    admin_notes = db.Column(db.Text)
    approved_slots = db.Column(db.Integer, default=0)
    monthly_fee = db.Column(db.Float, default=0.0)
    commission_rate = db.Column(db.Float, default=5.0)
    contract_start = db.Column(db.DateTime)
    contract_end = db.Column(db.DateTime)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    user = db.relationship('User', backref='store_applications')

class StoreSlot(db.Model):
    __tablename__ = 'store_slots'
    id = db.Column(db.Integer, primary_key=True)
    slot_code = db.Column(db.String(50), unique=True, nullable=False)
    slot_name = db.Column(db.String(100), nullable=False)
    location = db.Column(db.String(200))
    size = db.Column(db.String(50))
    base_price = db.Column(db.Float, default=0.0)
    features = db.Column(db.Text)
    is_available = db.Column(db.Boolean, default=True)
    application_id = db.Column(db.Integer, db.ForeignKey('store_applications.id'))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    application = db.relationship('StoreApplication', backref='slots')

class StoreProduct(db.Model):
    __tablename__ = 'store_products'
    id = db.Column(db.Integer, primary_key=True)
    application_id = db.Column(db.Integer, db.ForeignKey('store_applications.id'), nullable=False)
    slot_id = db.Column(db.Integer, db.ForeignKey('store_slots.id'), nullable=False)
    product_name = db.Column(db.String(200), nullable=False)
    product_slug = db.Column(db.String(200), unique=True, nullable=False)
    product_description = db.Column(db.Text)
    category = db.Column(db.String(100))
    price = db.Column(db.Float, default=0.0)
    old_price = db.Column(db.Float)
    currency = db.Column(db.String(10), default="EGP")
    stock_quantity = db.Column(db.Integer, default=0)
    images = db.Column(db.Text)
    is_featured = db.Column(db.Boolean, default=False)
    is_active = db.Column(db.Boolean, default=True)
    display_order = db.Column(db.Integer, default=0)
    views_count = db.Column(db.Integer, default=0)
    sales_count = db.Column(db.Integer, default=0)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    application = db.relationship('StoreApplication', backref='products')
    slot = db.relationship('StoreSlot', backref='products')

class StoreContract(db.Model):
    __tablename__ = 'store_contracts'
    id = db.Column(db.Integer, primary_key=True)
    application_id = db.Column(db.Integer, db.ForeignKey('store_applications.id'), unique=True, nullable=False)
    contract_terms = db.Column(db.Text, nullable=False)
    commission_rate = db.Column(db.Float, default=5.0)
    payment_terms = db.Column(db.String(200))
    cancellation_policy = db.Column(db.Text)
    special_conditions = db.Column(db.Text)
    status = db.Column(db.String(50), default="draft")
    signed_by_merchant = db.Column(db.Boolean, default=False)
    signed_by_admin = db.Column(db.Boolean, default=False)
    signed_at = db.Column(db.DateTime)
    expires_at = db.Column(db.DateTime, nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    application = db.relationship('StoreApplication', backref='contract')

class StorePayment(db.Model):
    __tablename__ = 'store_payments'
    id = db.Column(db.Integer, primary_key=True)
    application_id = db.Column(db.Integer, db.ForeignKey('store_applications.id'), nullable=False)
    amount = db.Column(db.Float, nullable=False)
    payment_type = db.Column(db.String(50), nullable=False)
    period_start = db.Column(db.DateTime)
    period_end = db.Column(db.DateTime)
    status = db.Column(db.String(50), default="pending")
    transaction_id = db.Column(db.String(200))
    payment_method = db.Column(db.String(100))
    notes = db.Column(db.Text)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    application = db.relationship('StoreApplication', backref='payments')

class StoreOrder(db.Model):
    __tablename__ = 'store_orders'
    id = db.Column(db.Integer, primary_key=True)
    product_id = db.Column(db.Integer, db.ForeignKey('store_products.id'), nullable=False)
    buyer_name = db.Column(db.String(100), nullable=False)
    buyer_phone = db.Column(db.String(50), nullable=False)
    buyer_email = db.Column(db.String(120))
    buyer_address = db.Column(db.Text)
    quantity = db.Column(db.Integer, default=1)
    total_price = db.Column(db.Float, nullable=False)
    status = db.Column(db.String(50), default="pending")
    notes = db.Column(db.Text)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    product = db.relationship('StoreProduct', backref='orders')

class Activity(db.Model):
    __tablename__ = 'activities'
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    action = db.Column(db.String(100), nullable=False)
    details = db.Column(db.Text)
    ip_address = db.Column(db.String(45))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

class Notification(db.Model):
    __tablename__ = 'notifications'
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    title = db.Column(db.String(200), nullable=False)
    message = db.Column(db.Text, nullable=False)
    type = db.Column(db.String(50), default="info")  # info, success, warning, order
    is_read = db.Column(db.Boolean, default=False)
    link = db.Column(db.String(500))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

# ═══════════════════════════════════════════════════════════════
# SECTION 3: JWT AUTH HELPERS
# ═══════════════════════════════════════════════════════════════

def create_tokens(user_id):
    """Create access and refresh JWT tokens"""
    now = datetime.utcnow()
    access_payload = {
        'user_id': user_id,
        'type': 'access',
        'iat': now,
        'exp': now + JWT_ACCESS_EXPIRY
    }
    refresh_payload = {
        'user_id': user_id,
        'type': 'refresh',
        'iat': now,
        'exp': now + JWT_REFRESH_EXPIRY
    }
    access_token = jwt.encode(access_payload, JWT_SECRET, algorithm=JWT_ALGORITHM)
    refresh_token = jwt.encode(refresh_payload, JWT_SECRET, algorithm=JWT_ALGORITHM)
    return access_token, refresh_token

def verify_token(token, token_type='access'):
    """Verify JWT token from cookie"""
    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM])
        if payload.get('type') != token_type:
            return None
        return payload
    except jwt.ExpiredSignatureError:
        return {'error': 'Token expired'}
    except jwt.InvalidTokenError:
        return None

def get_current_user():
    """Get current user from JWT cookie"""
    token = request.cookies.get('access_token')
    if not token:
        return None
    payload = verify_token(token)
    if not payload or isinstance(payload, dict) and 'error' in payload:
        return None
    return User.query.get(payload.get('user_id'))

def login_required_jwt(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        user = get_current_user()
        if not user:
            if request.is_json:
                return jsonify({'error': 'Authentication required'}), 401
            return redirect('/login')
        request.current_user = user
        return f(*args, **kwargs)
    return decorated

def admin_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        user = get_current_user()
        if not user or not user.is_admin:
            return jsonify({'error': 'Admin access required'}), 403
        request.current_user = user
        return f(*args, **kwargs)
    return decorated

def merchant_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        user = get_current_user()
        if not user:
            return jsonify({'error': 'Authentication required'}), 401
        app_obj = StoreApplication.query.filter_by(user_id=user.id, status='approved').first()
        if not app_obj:
            return jsonify({'error': 'Active merchant account required'}), 403
        request.current_user = user
        request.merchant_app = app_obj
        return f(*args, **kwargs)
    return decorated

def slugify(text):
    text = text.lower().strip()
    text = re.sub(r'[^\w\s-]', '', text)
    text = re.sub(r'[\s]+', '-', text)
    return text[:200]

def log_activity(user_id, action, details=None):
    try:
        activity = Activity(user_id=user_id, action=action, details=details, ip_address=request.remote_addr)
        db.session.add(activity)
        db.session.commit()
    except:
        db.session.rollback()

# ═══════════════════════════════════════════════════════════════
# SECTION 4: AUTH ROUTES (JWT + HTTP-Only Cookies)
# ═══════════════════════════════════════════════════════════════

@app.route('/')
def index():
    user = get_current_user()
    featured = StoreProduct.query.filter_by(is_active=True, is_featured=True).order_by(StoreProduct.display_order).limit(8).all()
    stores = StoreApplication.query.filter_by(status='approved').limit(6).all()
    return render_template('index.html', featured_products=featured, stores=stores, user=user)

@app.route('/login', methods=['GET', 'POST'])
def login_page():
    if request.method == 'POST':
        data = request.get_json() or request.form
        username = data.get('username', '').strip()
        password = data.get('password', '')

        user = User.query.filter((User.username == username) | (User.email == username)).first()

        if user and bcrypt.check_password_hash(user.password_hash, password):
            if not user.is_active:
                return jsonify({'error': 'Account disabled'}), 403

            # Create JWT tokens
            access_token, refresh_token = create_tokens(user.id)

            # Update user stats
            user.last_login = datetime.utcnow()
            user.login_count += 1
            db.session.commit()
            log_activity(user.id, 'login', f"IP: {request.remote_addr}")

            # Create notification for user
            notif = Notification(
                user_id=user.id,
                title="تسجيل دخول ناجح",
                message=f"تم تسجيل دخولك من {request.remote_addr}",
                type="success"
            )
            db.session.add(notif)
            db.session.commit()

            # Set HTTP-Only cookies
            response = make_response(jsonify({'success': True, 'redirect': '/'}))

            is_secure_cookie = request.is_secure or request.headers.get('X-Forwarded-Proto', '') == 'https'

            # Access token - short lived
            response.set_cookie(
                'access_token', access_token,
                httponly=True,
                secure=is_secure_cookie,
                samesite='Lax',
                max_age=int(JWT_ACCESS_EXPIRY.total_seconds())
            )

            # Refresh token - long lived
            response.set_cookie(
                'refresh_token', refresh_token,
                httponly=True,
                secure=is_secure_cookie,
                samesite='Lax',
                max_age=int(JWT_REFRESH_EXPIRY.total_seconds())
            )

            return response

        return jsonify({'error': 'Invalid credentials'}), 401

    return render_template('login.html')

@app.route('/register', methods=['POST'])
def register():
    data = request.get_json() or request.form
    username = data.get('username', '').strip()
    email = data.get('email', '').strip().lower()
    password = data.get('password', '')
    full_name = data.get('full_name', '').strip()

    if not all([username, email, password]):
        return jsonify({'error': 'All fields required'}), 400

    if User.query.filter((User.username == username) | (User.email == email)).first():
        return jsonify({'error': 'Username or email exists'}), 409

    user = User(
        username=username, email=email,
        password_hash=bcrypt.generate_password_hash(password).decode('utf-8'),
        full_name=full_name
    )
    db.session.add(user)
    db.session.commit()
    log_activity(user.id, 'register')

    # Welcome notification
    notif = Notification(
        user_id=user.id,
        title="مرحباً بك في رفيق!",
        message="تم إنشاء حسابك بنجاح. يمكنك الآن التقدم بطلب توكيل تجاري.",
        type="success"
    )
    db.session.add(notif)
    db.session.commit()

    return jsonify({'success': True, 'message': 'Account created'})

@app.route('/logout')
def logout():
    user = get_current_user()
    if user:
        log_activity(user.id, 'logout')

    response = make_response(redirect('/'))
    response.delete_cookie('access_token')
    response.delete_cookie('refresh_token')
    return response

@app.route('/api/auth/refresh', methods=['POST'])
def refresh_token():
    """Refresh access token using refresh token"""
    refresh_token = request.cookies.get('refresh_token')
    if not refresh_token:
        return jsonify({'error': 'No refresh token'}), 401

    payload = verify_token(refresh_token, 'refresh')
    if not payload or isinstance(payload, dict) and 'error' in payload:
        return jsonify({'error': 'Invalid refresh token'}), 401

    user = User.query.get(payload['user_id'])
    if not user or not user.is_active:
        return jsonify({'error': 'User not found'}), 401

    access_token, new_refresh = create_tokens(user.id)
    is_secure_cookie = request.is_secure or request.headers.get('X-Forwarded-Proto', '') == 'https'

    response = make_response(jsonify({'success': True}))
    response.set_cookie('access_token', access_token, httponly=True, secure=is_secure_cookie, samesite='Lax', max_age=int(JWT_ACCESS_EXPIRY.total_seconds()))
    response.set_cookie('refresh_token', new_refresh, httponly=True, secure=is_secure_cookie, samesite='Lax', max_age=int(JWT_REFRESH_EXPIRY.total_seconds()))
    return response

@app.route('/api/auth/me')
def auth_me():
    """Get current authenticated user info"""
    user = get_current_user()
    if not user:
        return jsonify({'authenticated': False}), 401
    return jsonify({
        'authenticated': True,
        'user': {
            'id': user.id,
            'username': user.username,
            'email': user.email,
            'full_name': user.full_name,
            'avatar': user.avatar,
            'is_admin': user.is_admin,
            'role': user.role
        }
    })

# ═══════════════════════════════════════════════════════════════
# SECTION 5: NOTIFICATIONS API
# ═══════════════════════════════════════════════════════════════

@app.route('/api/notifications')
@login_required_jwt
def get_notifications():
    """Get user notifications"""
    user = request.current_user
    unread_only = request.args.get('unread', 'false').lower() == 'true'

    query = Notification.query.filter_by(user_id=user.id)
    if unread_only:
        query = query.filter_by(is_read=False)

    notifications = query.order_by(Notification.created_at.desc()).limit(50).all()
    unread_count = Notification.query.filter_by(user_id=user.id, is_read=False).count()

    return jsonify({
        'notifications': [{
            'id': n.id,
            'title': n.title,
            'message': n.message,
            'type': n.type,
            'is_read': n.is_read,
            'link': n.link,
            'created_at': n.created_at.isoformat()
        } for n in notifications],
        'unread_count': unread_count
    })

@app.route('/api/notifications/<int:notif_id>/read', methods=['POST'])
@login_required_jwt
def mark_notification_read(notif_id):
    user = request.current_user
    notif = Notification.query.filter_by(id=notif_id, user_id=user.id).first()
    if notif:
        notif.is_read = True
        db.session.commit()
    return jsonify({'success': True})

@app.route('/api/notifications/read-all', methods=['POST'])
@login_required_jwt
def mark_all_read():
    user = request.current_user
    Notification.query.filter_by(user_id=user.id, is_read=False).update({'is_read': True})
    db.session.commit()
    return jsonify({'success': True})

# ═══════════════════════════════════════════════════════════════
# SECTION 6: STORE SYSTEM API
# ═══════════════════════════════════════════════════════════════

@app.route('/api/store/slots')
def list_slots():
    slots = StoreSlot.query.filter_by(is_available=True).all()
    return jsonify([{'id': s.id, 'code': s.slot_code, 'name': s.slot_name, 'location': s.location, 'size': s.size, 'price': s.base_price, 'features': json.loads(s.features) if s.features else []} for s in slots])

@app.route('/api/store/apply', methods=['POST'])
@login_required_jwt
def apply_for_store():
    data = request.get_json() or request.form
    user = request.current_user

    existing = StoreApplication.query.filter(StoreApplication.user_id == user.id, StoreApplication.status.in_(['pending', 'approved'])).first()
    if existing:
        return jsonify({'error': 'You already have an application'}), 409

    store_name = data.get('store_name', '').strip()
    if not store_name:
        return jsonify({'error': 'Store name required'}), 400

    slug = slugify(store_name)
    base_slug = slug
    counter = 1
    while StoreApplication.query.filter_by(store_slug=slug).first():
        slug = f"{base_slug}-{counter}"
        counter += 1

    application = StoreApplication(
        user_id=user.id, store_name=store_name, store_slug=slug,
        store_description=data.get('store_description', ''),
        business_type=data.get('business_type', 'retail'),
        requested_slots=int(data.get('requested_slots', 1)),
        contact_phone=data.get('phone', ''),
        contact_email=data.get('email', user.email),
        business_license=data.get('license_url', '')
    )
    db.session.add(application)
    db.session.commit()

    # Notify admin (user_id=1 or any admin)
    admin = User.query.filter_by(is_admin=True).first()
    if admin:
        notif = Notification(
            user_id=admin.id,
            title="طلب توكيل جديد",
            message=f"تقدم {user.username} بطلب توكيل لمتجر '{store_name}'",
            type="info",
            link="/admin/stores"
        )
        db.session.add(notif)
        db.session.commit()

    log_activity(user.id, 'store_apply', f"Store: {store_name}")
    return jsonify({'success': True, 'application_id': application.id, 'store_slug': slug, 'message': 'Application submitted for review'})

@app.route('/api/store/merchant/status')
@login_required_jwt
def merchant_status():
    user = request.current_user
    app_obj = StoreApplication.query.filter_by(user_id=user.id).order_by(StoreApplication.created_at.desc()).first()
    if not app_obj:
        return jsonify({'has_store': False})
    return jsonify({'has_store': True, 'status': app_obj.status, 'store_name': app_obj.store_name, 'store_slug': app_obj.store_slug, 'approved_slots': app_obj.approved_slots, 'monthly_fee': app_obj.monthly_fee, 'contract_end': app_obj.contract_end.isoformat() if app_obj.contract_end else None})

@app.route('/api/store/merchant/products', methods=['GET', 'POST'])
@merchant_required
def merchant_products():
    app_obj = request.merchant_app
    if request.method == 'POST':
        data = request.get_json() or request.form
        current_count = StoreProduct.query.filter_by(application_id=app_obj.id).count()
        if current_count >= app_obj.approved_slots * 10:
            return jsonify({'error': 'Product limit reached'}), 403

        slug = slugify(data.get('product_name', ''))
        base_slug = slug
        counter = 1
        while StoreProduct.query.filter_by(product_slug=slug).first():
            slug = f"{base_slug}-{counter}"
            counter += 1

        slot = StoreSlot.query.filter_by(application_id=app_obj.id).first()
        if not slot:
            return jsonify({'error': 'No slot assigned'}), 400

        product = StoreProduct(
            application_id=app_obj.id, slot_id=slot.id,
            product_name=data.get('product_name', ''), product_slug=slug,
            product_description=data.get('description', ''),
            category=data.get('category', ''),
            price=float(data.get('price', 0)),
            old_price=float(data.get('old_price')) if data.get('old_price') else None,
            stock_quantity=int(data.get('stock', 0)),
            images=json.dumps(data.get('images', [])) if isinstance(data.get('images'), list) else data.get('images', '[]'),
            is_featured=data.get('is_featured', False)
        )
        db.session.add(product)
        db.session.commit()

        # Notify user
        notif = Notification(
            user_id=app_obj.user_id,
            title="منتج جديد مضاف",
            message=f"تم إضافة '{product.product_name}' إلى متجرك بنجاح",
            type="success"
        )
        db.session.add(notif)
        db.session.commit()

        log_activity(app_obj.user_id, 'product_add', f"Product: {product.product_name}")
        return jsonify({'success': True, 'product_id': product.id})

    products = StoreProduct.query.filter_by(application_id=app_obj.id).order_by(StoreProduct.display_order).all()
    return jsonify([{'id': p.id, 'name': p.product_name, 'slug': p.product_slug, 'price': p.price, 'old_price': p.old_price, 'stock': p.stock_quantity, 'category': p.category, 'is_featured': p.is_featured, 'is_active': p.is_active, 'views': p.views_count, 'sales': p.sales_count, 'images': json.loads(p.images) if p.images else []} for p in products])

@app.route('/api/store/merchant/products/<int:product_id>', methods=['PUT', 'DELETE'])
@merchant_required
def manage_product(product_id):
    app_obj = request.merchant_app
    product = StoreProduct.query.filter_by(id=product_id, application_id=app_obj.id).first()
    if not product:
        return jsonify({'error': 'Product not found'}), 404
    if request.method == 'DELETE':
        db.session.delete(product)
        db.session.commit()
        log_activity(app_obj.user_id, 'product_delete', f"ID: {product_id}")
        return jsonify({'success': True})

    data = request.get_json() or request.form
    if 'product_name' in data: product.product_name = data['product_name']
    if 'description' in data: product.product_description = data['description']
    if 'price' in data: product.price = float(data['price'])
    if 'stock' in data: product.stock_quantity = int(data['stock'])
    if 'is_featured' in data: product.is_featured = bool(data['is_featured'])
    if 'is_active' in data: product.is_active = bool(data['is_active'])
    db.session.commit()
    log_activity(app_obj.user_id, 'product_update', f"ID: {product_id}")
    return jsonify({'success': True})

@app.route('/api/store/merchant/orders')
@merchant_required
def merchant_orders():
    app_obj = request.merchant_app
    orders = db.session.query(StoreOrder).join(StoreProduct).filter(StoreProduct.application_id == app_obj.id).order_by(StoreOrder.created_at.desc()).all()
    return jsonify([{'id': o.id, 'product_name': o.product.product_name, 'buyer_name': o.buyer_name, 'buyer_phone': o.buyer_phone, 'quantity': o.quantity, 'total': o.total_price, 'status': o.status, 'created_at': o.created_at.isoformat()} for o in orders])

@app.route('/api/store/merchant/dashboard')
@merchant_required
def merchant_dashboard_api():
    app_obj = request.merchant_app
    user = request.current_user

    products_count = StoreProduct.query.filter_by(application_id=app_obj.id).count()
    active_products = StoreProduct.query.filter_by(application_id=app_obj.id, is_active=True).count()

    # Today's sales
    today = datetime.utcnow().replace(hour=0, minute=0, second=0, microsecond=0)
    today_sales = db.session.query(func.sum(StoreOrder.total_price)).join(StoreProduct).filter(
        StoreProduct.application_id == app_obj.id,
        StoreOrder.created_at >= today
    ).scalar() or 0

    total_sales = db.session.query(func.sum(StoreOrder.total_price)).join(StoreProduct).filter(StoreProduct.application_id == app_obj.id).scalar() or 0
    total_orders = db.session.query(StoreOrder).join(StoreProduct).filter(StoreProduct.application_id == app_obj.id).count()

    # Today's orders
    today_orders = db.session.query(StoreOrder).join(StoreProduct).filter(
        StoreProduct.application_id == app_obj.id,
        StoreOrder.created_at >= today
    ).count()

    # Total views
    total_views = db.session.query(func.sum(StoreProduct.views_count)).filter_by(application_id=app_obj.id).scalar() or 0

    # Sales chart data (last 7 days)
    chart_data = []
    for i in range(6, -1, -1):
        day = today - timedelta(days=i)
        next_day = day + timedelta(days=1)
        day_sales = db.session.query(func.sum(StoreOrder.total_price)).join(StoreProduct).filter(
            StoreProduct.application_id == app_obj.id,
            StoreOrder.created_at >= day,
            StoreOrder.created_at < next_day
        ).scalar() or 0
        chart_data.append({
            'date': day.strftime('%Y-%m-%d'),
            'label': day.strftime('%a'),
            'sales': float(day_sales)
        })

    contract = StoreContract.query.filter_by(application_id=app_obj.id).first()

    # Setup progress
    setup_steps = [
        {'name': 'إنشاء المتجر', 'done': True},
        {'name': 'تخصيص المتجر', 'done': bool(app_obj.logo_url)},
        {'name': 'إضافة منتج أول', 'done': products_count > 0},
        {'name': 'تفعيل الدفع', 'done': bool(contract and contract.signed_by_merchant)},
        {'name': 'إطلاق المتجر', 'done': active_products > 0}
    ]
    setup_progress = sum(1 for s in setup_steps if s['done'])

    return jsonify({
        'store': {
            'name': app_obj.store_name,
            'slug': app_obj.store_slug,
            'logo': app_obj.logo_url,
            'status': app_obj.status,
            'slots': app_obj.approved_slots,
            'monthly_fee': app_obj.monthly_fee,
            'contract_end': app_obj.contract_end.isoformat() if app_obj.contract_end else None
        },
        'user': {
            'full_name': user.full_name or user.username,
            'avatar': user.avatar
        },
        'stats': {
            'products_total': products_count,
            'products_active': active_products,
            'today_sales': float(today_sales),
            'total_sales': float(total_sales),
            'today_orders': today_orders,
            'total_orders': total_orders,
            'total_views': int(total_views),
            'commission_rate': contract.commission_rate if contract else 5.0
        },
        'chart': chart_data,
        'setup': {
            'steps': setup_steps,
            'progress': setup_progress,
            'total': len(setup_steps),
            'percentage': int((setup_progress / len(setup_steps)) * 100)
        },
        'contract': {
            'status': contract.status if contract else None,
            'signed': contract.signed_by_merchant if contract else False,
            'expires': contract.expires_at.isoformat() if contract else None
        }
    })

# ═══════════════════════════════════════════════════════════════
# SECTION 7: PUBLIC STORE API
# ═══════════════════════════════════════════════════════════════

@app.route('/api/store/stores')
def list_stores():
    stores = StoreApplication.query.filter_by(status='approved').all()
    return jsonify([{'id': s.id, 'name': s.store_name, 'slug': s.store_slug, 'description': s.store_description, 'type': s.business_type, 'logo': s.logo_url} for s in stores])

@app.route('/api/store/store/<slug>')
def store_detail(slug):
    store = StoreApplication.query.filter_by(store_slug=slug, status='approved').first()
    if not store:
        return jsonify({'error': 'Store not found'}), 404
    products = StoreProduct.query.filter_by(application_id=store.id, is_active=True).order_by(StoreProduct.display_order).all()
    return jsonify({'store': {'name': store.store_name, 'slug': store.store_slug, 'description': store.store_description, 'type': store.business_type, 'logo': store.logo_url}, 'products': [{'id': p.id, 'name': p.product_name, 'slug': p.product_slug, 'price': p.price, 'old_price': p.old_price, 'category': p.category, 'stock': p.stock_quantity, 'is_featured': p.is_featured, 'images': json.loads(p.images) if p.images else []} for p in products]})

@app.route('/api/store/product/<slug>')
def product_detail(slug):
    product = StoreProduct.query.filter_by(product_slug=slug, is_active=True).first()
    if not product:
        return jsonify({'error': 'Product not found'}), 404
    product.views_count += 1
    db.session.commit()
    store = StoreApplication.query.get(product.application_id)
    return jsonify({'product': {'id': product.id, 'name': product.product_name, 'slug': product.product_slug, 'description': product.product_description, 'price': product.price, 'old_price': product.old_price, 'currency': product.currency, 'stock': product.stock_quantity, 'category': product.category, 'images': json.loads(product.images) if product.images else [], 'views': product.views_count, 'sales': product.sales_count}, 'store': {'name': store.store_name, 'slug': store.store_slug}})

@app.route('/api/store/order', methods=['POST'])
def place_order():
    data = request.get_json() or request.form
    product_id = data.get('product_id')
    product = StoreProduct.query.get(product_id)
    if not product or not product.is_active:
        return jsonify({'error': 'Product not available'}), 404
    quantity = int(data.get('quantity', 1))
    if quantity > product.stock_quantity:
        return jsonify({'error': 'Insufficient stock'}), 400
    total = product.price * quantity
    order = StoreOrder(product_id=product_id, buyer_name=data.get('buyer_name', ''), buyer_phone=data.get('buyer_phone', ''), buyer_email=data.get('buyer_email', ''), buyer_address=data.get('buyer_address', ''), quantity=quantity, total_price=total, notes=data.get('notes', ''))
    db.session.add(order)
    product.stock_quantity -= quantity
    product.sales_count += quantity
    db.session.commit()

    # Notify merchant
    notif = Notification(
        user_id=product.application.user_id,
        title="طلب شراء جديد!",
        message=f"تم طلب '{product.product_name}' بكمية {quantity}",
        type="order",
        link="/store/merchant"
    )
    db.session.add(notif)
    db.session.commit()

    return jsonify({'success': True, 'order_id': order.id, 'total': total, 'status': 'pending'})

# ═══════════════════════════════════════════════════════════════
# SECTION 8: ADMIN API
# ═══════════════════════════════════════════════════════════════

@app.route('/api/admin/applications')
@admin_required
def admin_applications():
    status = request.args.get('status', 'pending')
    apps = StoreApplication.query.filter_by(status=status).all()
    return jsonify([{'id': a.id, 'store_name': a.store_name, 'slug': a.store_slug, 'business_type': a.business_type, 'requested_slots': a.requested_slots, 'contact_phone': a.contact_phone, 'contact_email': a.contact_email, 'created_at': a.created_at.isoformat(), 'user': {'username': a.user.username, 'email': a.user.email}} for a in apps])

@app.route('/api/admin/applications/<int:app_id>/approve', methods=['POST'])
@admin_required
def approve_application(app_id):
    data = request.get_json() or request.form
    application = StoreApplication.query.get_or_404(app_id)
    application.status = 'approved'
    application.approved_slots = int(data.get('approved_slots', application.requested_slots))
    application.monthly_fee = float(data.get('monthly_fee', 500))
    application.commission_rate = float(data.get('commission_rate', 5.0))
    application.contract_start = datetime.utcnow()
    application.contract_end = datetime.utcnow() + timedelta(days=180)
    slots = StoreSlot.query.filter_by(is_available=True).limit(application.approved_slots).all()
    for slot in slots:
        slot.is_available = False
        slot.application_id = application.id
    contract = StoreContract(application_id=application.id, contract_terms=data.get('contract_terms', 'Standard franchise agreement'), commission_rate=application.commission_rate, payment_terms=f"Monthly fee: {application.monthly_fee} EGP", expires_at=application.contract_end)
    db.session.add(contract)
    db.session.commit()

    # Notify merchant
    notif = Notification(
        user_id=application.user_id,
        title="تم اعتماد متجرك!",
        message=f"تهانينا! تم اعتماد متجر '{application.store_name}'. يمكنك البدء في إضافة منتجاتك الآن.",
        type="success",
        link="/store/merchant"
    )
    db.session.add(notif)
    db.session.commit()

    log_activity(request.current_user.id, 'application_approved', f"Store: {application.store_name}")
    return jsonify({'success': True, 'assigned_slots': len(slots)})

@app.route('/api/admin/applications/<int:app_id>/reject', methods=['POST'])
@admin_required
def reject_application(app_id):
    application = StoreApplication.query.get_or_404(app_id)
    application.status = 'rejected'
    application.admin_notes = request.get_json().get('reason', '')
    db.session.commit()

    notif = Notification(
        user_id=application.user_id,
        title="تحديث طلب التوكيل",
        message=f"تم رفض طلب توكيل متجر '{application.store_name}'",
        type="warning"
    )
    db.session.add(notif)
    db.session.commit()

    return jsonify({'success': True})

@app.route('/api/admin/stats')
@admin_required
def admin_stats():
    total_users = User.query.count()
    total_stores = StoreApplication.query.filter_by(status='approved').count()
    pending_apps = StoreApplication.query.filter_by(status='pending').count()
    total_products = StoreProduct.query.count()
    total_orders = StoreOrder.query.count()
    total_revenue = db.session.query(func.sum(StoreOrder.total_price)).scalar() or 0
    return jsonify({'users': total_users, 'stores': total_stores, 'pending_applications': pending_apps, 'products': total_products, 'orders': total_orders, 'revenue': float(total_revenue)})

# ═══════════════════════════════════════════════════════════════
# SECTION 9: MIGRATION API
# ═══════════════════════════════════════════════════════════════

@app.route('/api/migrate/status')
def migrate_status():
    from auto_migrate import get_migration_status
    return jsonify(get_migration_status())

@app.route('/api/migrate/run', methods=['POST'])
@admin_required
def migrate_run():
    from auto_migrate import auto_migrate
    result = auto_migrate()
    return jsonify(result)

# ═══════════════════════════════════════════════════════════════
# SECTION 10: PAGE ROUTES
# ═══════════════════════════════════════════════════════════════

@app.route('/store/apply')
@login_required_jwt
def store_apply_page():
    return render_template('store/apply.html')

@app.route('/store/merchant')
@login_required_jwt
def merchant_dashboard_page():
    return render_template('store/merchant_dashboard.html')

@app.route('/store/<slug>')
def store_public_page(slug):
    store = StoreApplication.query.filter_by(store_slug=slug, status='approved').first()
    if not store:
        abort(404)
    return render_template('store/store_public.html', store_slug=slug)

@app.route('/product/<slug>')
def product_public_page(slug):
    product = StoreProduct.query.filter_by(product_slug=slug, is_active=True).first()
    if not product:
        abort(404)
    return render_template('store/product_detail.html', product_slug=slug)

@app.route('/admin/stores')
@admin_required
def admin_stores_page():
    return render_template('store/admin.html')

# ═══════════════════════════════════════════════════════════════
# SECTION 11: ERROR HANDLERS
# ═══════════════════════════════════════════════════════════════

@app.errorhandler(404)
def not_found(e):
    if request.is_json or request.headers.get('Accept', '').startswith('application/json'):
        return jsonify({'error': 'Not found'}), 404
    return render_template('404.html'), 404

@app.errorhandler(500)
def server_error(e):
    db.session.rollback()
    if request.is_json or request.headers.get('Accept', '').startswith('application/json'):
        return jsonify({'error': 'Internal server error'}), 500
    return render_template('500.html'), 500

# ═══════════════════════════════════════════════════════════════
# SECTION 12: INIT DEFAULT SLOTS
# ═══════════════════════════════════════════════════════════════

def init_default_admin():
    admin = User.query.filter_by(is_admin=True).first()
    if not admin:
        admin_pass = os.environ.get('ADMIN_PASSWORD', 'admin123')
        admin = User(
            username='admin',
            email='admin@rafeeq.app',
            password_hash=bcrypt.generate_password_hash(admin_pass).decode('utf-8'),
            full_name='مدير النظام',
            is_admin=True,
            role='admin'
        )
        db.session.add(admin)
        db.session.commit()
        print("✅ Default admin created (admin / admin123)")

def init_default_slots():
    if StoreSlot.query.count() > 0:
        return
    slots = [
        {'code': 'A1', 'name': 'المساحة الذهبية A1', 'location': 'الصفحة الرئيسية - أعلى', 'size': 'xlarge', 'price': 2000, 'features': '["عرض مميز","إحصائيات متقدمة","دعم فني"]'},
        {'code': 'A2', 'name': 'المساحة الذهبية A2', 'location': 'الصفحة الرئيسية - أعلى', 'size': 'xlarge', 'price': 2000, 'features': '["عرض مميز","إحصائيات متقدمة","دعم فني"]'},
        {'code': 'B1', 'name': 'المساحة الفضية B1', 'location': 'الصفحة الرئيسية - وسط', 'size': 'large', 'price': 1200, 'features': '["عرض مميز","إحصائيات"]'},
        {'code': 'B2', 'name': 'المساحة الفضية B2', 'location': 'الصفحة الرئيسية - وسط', 'size': 'large', 'price': 1200, 'features': '["عرض مميز","إحصائيات"]'},
        {'code': 'B3', 'name': 'المساحة الفضية B3', 'location': 'الصفحة الرئيسية - وسط', 'size': 'large', 'price': 1200, 'features': '["عرض مميز","إحصائيات"]'},
        {'code': 'C1', 'name': 'المساحة البرونزية C1', 'location': 'صفحة المتاجر', 'size': 'medium', 'price': 700, 'features': '["إحصائيات أساسية"]'},
        {'code': 'C2', 'name': 'المساحة البرونزية C2', 'location': 'صفحة المتاجر', 'size': 'medium', 'price': 700, 'features': '["إحصائيات أساسية"]'},
        {'code': 'C3', 'name': 'المساحة البرونزية C3', 'location': 'صفحة المتاجر', 'size': 'medium', 'price': 700, 'features': '["إحصائيات أساسية"]'},
        {'code': 'C4', 'name': 'المساحة البرونزية C4', 'location': 'صفحة المتاجر', 'size': 'medium', 'price': 700, 'features': '["إحصائيات أساسية"]'},
        {'code': 'D1', 'name': 'المساحة الأساسية D1', 'location': 'قائمة المتاجر', 'size': 'small', 'price': 350, 'features': '["عرض عادي"]'},
        {'code': 'D2', 'name': 'المساحة الأساسية D2', 'location': 'قائمة المتاجر', 'size': 'small', 'price': 350, 'features': '["عرض عادي"]'},
        {'code': 'D3', 'name': 'المساحة الأساسية D3', 'location': 'قائمة المتاجر', 'size': 'small', 'price': 350, 'features': '["عرض عادي"]'},
    ]
    for s in slots:
        slot = StoreSlot(**s)
        db.session.add(slot)
    db.session.commit()
    print(f"✅ Created {len(slots)} default slots")

# ═══════════════════════════════════════════════════════════════
# SECTION 13: APP STARTUP
# ═══════════════════════════════════════════════════════════════

with app.app_context():
    db.create_all()
    init_default_admin()
    init_default_slots()
    print("✅ Database initialized")

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    app.run(host='0.0.0.0', port=port, debug=False)
