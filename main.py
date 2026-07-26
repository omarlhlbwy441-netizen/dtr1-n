import os
import sys
import logging
import threading
from flask import Flask, jsonify, request, redirect, url_for, session
from flask_cors import CORS
from flask_sqlalchemy import SQLAlchemy

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s [%(levelname)s] %(message)s')
logger = logging.getLogger("rafeeq_backend")

app = Flask(__name__)
CORS(app)

# Database Configuration
raw_db_url = os.getenv("DATABASE_URL", "sqlite:///rafeeq_ecosystem.db")
if raw_db_url.startswith("postgres://"):
    raw_db_url = raw_db_url.replace("postgres://", "postgresql+pg8000://", 1)
elif raw_db_url.startswith("postgresql://") and "+pg8000" not in raw_db_url and "+psycopg2" not in raw_db_url:
    raw_db_url = raw_db_url.replace("postgresql://", "postgresql+pg8000://", 1)

app.config["SQLALCHEMY_DATABASE_URI"] = raw_db_url
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False
app.config["SQLALCHEMY_ENGINE_OPTIONS"] = {
    "pool_pre_ping": True,
    "pool_recycle": 300
} if "postgresql" in raw_db_url else {}
app.config["SECRET_KEY"] = os.getenv("JWT_SECRET_KEY", "rafeeq-secret-key-3.2.0")

db = SQLAlchemy()

try:
    db.init_app(app)
except Exception as e:
    logger.warning(f"Primary database init failed ({e}), falling back to SQLite.")
    app.extensions.pop("sqlalchemy", None)
    app.config["SQLALCHEMY_DATABASE_URI"] = "sqlite:///rafeeq_ecosystem.db"
    app.config["SQLALCHEMY_ENGINE_OPTIONS"] = {}
    db.init_app(app)

db_initialized = False

@app.before_request
def ensure_db_tables():
    global db_initialized
    if not db_initialized:
        try:
            db.create_all()
            db_initialized = True
            logger.info("Database tables verified/created successfully.")
        except Exception as e:
            logger.warning(f"Database table verification warning: {e}")
            db_initialized = True

# Database Models
class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    role = db.Column(db.String(50), default="VIP Creator")
    coins = db.Column(db.Integer, default=1250)

class StoreSlot(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    code = db.Column(db.String(100), nullable=True, index=True)
    name = db.Column(db.String(100), nullable=True)
    category = db.Column(db.String(100), nullable=True)
    slots = db.Column(db.Integer, default=1)
    fee = db.Column(db.String(50), nullable=True)
    status = db.Column(db.String(50), default="active")

class Post(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    author = db.Column(db.String(100), nullable=False)
    content = db.Column(db.Text, nullable=False)
    likes = db.Column(db.Integer, default=0)
    created_at = db.Column(db.String(50), nullable=True)

class ShortVideo(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    creator_name = db.Column(db.String(100), nullable=False)
    creator_handle = db.Column(db.String(100), nullable=False)
    description = db.Column(db.Text, nullable=False)
    likes_count = db.Column(db.Integer, default=0)
    views_count = db.Column(db.String(20), default="0")

class LiveAuction(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    streamer_name = db.Column(db.String(100), nullable=False)
    item_title = db.Column(db.String(200), nullable=False)
    current_bid_sar = db.Column(db.Integer, default=0)
    highest_bidder = db.Column(db.String(100), nullable=True)

# Mobile-First, Responsive Modern CSS Design System
COMMON_STYLE = """<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Tajawal:wght@300;400;500;700;900&display=swap" rel="stylesheet">
<style>
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
    -webkit-tap-highlight-color: transparent;
}
body {
    font-family: 'Tajawal', -apple-system, BlinkMacSystemFont, sans-serif;
    background-color: #0b0f19;
    color: #f3f4f6;
    min-height: 100vh;
    direction: rtl;
    line-height: 1.6;
    background-image: 
        radial-gradient(circle at 15% 15%, rgba(212, 175, 55, 0.08) 0%, transparent 40%),
        radial-gradient(circle at 85% 85%, rgba(56, 189, 248, 0.08) 0%, transparent 40%);
    background-attachment: fixed;
}
a {
    color: inherit;
    text-decoration: none;
}
.app-header {
    background: rgba(15, 23, 42, 0.85);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    border-bottom: 1px solid rgba(212, 175, 55, 0.2);
    position: sticky;
    top: 0;
    z-index: 100;
    padding: 0.75rem 1rem;
}
.nav-container {
    max-width: 1100px;
    margin: 0 auto;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 0.5rem;
}
.brand {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-weight: 900;
    font-size: 1.25rem;
    color: #f5e6c8;
}
.brand-icon {
    font-size: 1.5rem;
    filter: drop-shadow(0 0 8px rgba(212, 175, 55, 0.5));
}
.nav-menu {
    display: flex;
    gap: 0.4rem;
    align-items: center;
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;
    padding: 0.2rem 0;
}
.nav-menu::-webkit-scrollbar {
    display: none;
}
.nav-item {
    padding: 0.45rem 0.75rem;
    border-radius: 10px;
    font-size: 0.85rem;
    font-weight: 600;
    white-space: nowrap;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid rgba(255, 255, 255, 0.08);
    transition: all 0.2s ease;
}
.nav-item:hover, .nav-item.active {
    background: linear-gradient(135deg, rgba(212, 175, 55, 0.25), rgba(56, 189, 248, 0.15));
    border-color: rgba(212, 175, 55, 0.5);
    color: #f5e6c8;
}
.main-content {
    max-width: 1100px;
    margin: 0 auto;
    padding: 1.25rem 1rem 3rem 1rem;
    width: 100%;
}
.glass-card {
    background: rgba(17, 24, 39, 0.75);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border: 1px solid rgba(212, 175, 55, 0.2);
    border-radius: 20px;
    padding: 1.5rem;
    margin-bottom: 1.25rem;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.35);
    width: 100%;
}
@media (max-width: 480px) {
    .glass-card {
        padding: 1.1rem;
        border-radius: 16px;
    }
}
.title-gold {
    color: #f5e6c8;
    font-weight: 900;
    margin-bottom: 0.5rem;
}
.subtitle-text {
    color: #9ca3af;
    font-size: 0.9rem;
}
.grid-stats {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
    gap: 0.75rem;
    margin: 1.25rem 0;
}
.stat-box {
    background: rgba(255, 255, 255, 0.03);
    border: 1px solid rgba(212, 175, 55, 0.15);
    border-radius: 14px;
    padding: 0.85rem;
    text-align: center;
}
.stat-value {
    font-size: 1.3rem;
    font-weight: 900;
    color: #d4af37;
}
.stat-label {
    font-size: 0.75rem;
    color: #9ca3af;
    margin-top: 0.2rem;
}
.form-group {
    margin-bottom: 1rem;
    text-align: right;
}
.form-label {
    display: block;
    color: #d1d5db;
    font-size: 0.85rem;
    font-weight: 600;
    margin-bottom: 0.35rem;
}
.form-input {
    width: 100%;
    padding: 0.85rem 1rem;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(212, 175, 55, 0.25);
    border-radius: 12px;
    color: #fff;
    font-family: 'Tajawal', sans-serif;
    font-size: 0.95rem;
    outline: none;
    transition: all 0.25s;
}
.form-input:focus {
    border-color: #d4af37;
    box-shadow: 0 0 12px rgba(212, 175, 55, 0.25);
    background: rgba(255, 255, 255, 0.08);
}
.btn {
    width: 100%;
    padding: 0.85rem 1rem;
    font-family: 'Tajawal', sans-serif;
    font-size: 0.95rem;
    font-weight: 700;
    border: none;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.25s ease;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    min-height: 48px;
}
.btn-gold {
    background: linear-gradient(135deg, #d4af37, #aa820a);
    color: #0b0f19;
    box-shadow: 0 4px 15px rgba(212, 175, 55, 0.25);
}
.btn-gold:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(212, 175, 55, 0.35);
}
.btn-gold:active {
    transform: translateY(0);
}
.btn-outline {
    background: rgba(255, 255, 255, 0.03);
    border: 1px solid rgba(212, 175, 55, 0.4);
    color: #f5e6c8;
}
.btn-outline:hover {
    background: rgba(212, 175, 55, 0.1);
    border-color: #d4af37;
}
.btn-blue {
    background: linear-gradient(135deg, #0284c7, #0369a1);
    color: #ffffff;
    box-shadow: 0 4px 15px rgba(2, 132, 199, 0.25);
}
.badge {
    padding: 0.25rem 0.6rem;
    border-radius: 8px;
    font-size: 0.75rem;
    font-weight: 700;
    display: inline-block;
}
.badge-gold {
    background: rgba(212, 175, 55, 0.15);
    border: 1px solid rgba(212, 175, 55, 0.4);
    color: #d4af37;
}
.badge-green {
    background: rgba(16, 185, 129, 0.15);
    border: 1px solid rgba(16, 185, 129, 0.4);
    color: #34d399;
}
.video-card {
    background: rgba(0, 0, 0, 0.4);
    border-radius: 16px;
    padding: 1.25rem;
    border: 1px solid rgba(255, 255, 255, 0.1);
    margin-bottom: 1rem;
}
.user-profile-header {
    display: flex;
    align-items: center;
    gap: 1rem;
    flex-wrap: wrap;
}
.avatar-circle {
    width: 60px;
    height: 60px;
    border-radius: 50%;
    background: linear-gradient(135deg, #d4af37, #38bdf8);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1.5rem;
    font-weight: 900;
    color: #0b0f19;
    border: 2px solid rgba(255, 255, 255, 0.2);
    flex-shrink: 0;
}
.footer {
    text-align: center;
    padding: 1.5rem;
    font-size: 0.8rem;
    color: #6b7280;
    border-top: 1px solid rgba(255, 255, 255, 0.05);
}
</style>"""

def render_layout(title, content, active_page=""):
    is_logged_in = "user_email" in session

    dash_cls = "nav-item active" if active_page == "dashboard" else "nav-item"
    login_cls = "nav-item active" if active_page == "login" else "nav-item"
    home_cls = "nav-item active" if active_page == "home" else "nav-item"
    shorts_cls = "nav-item active" if active_page == "shorts" else "nav-item"
    auctions_cls = "nav-item active" if active_page == "auctions" else "nav-item"
    kernel_cls = "nav-item active" if active_page == "kernel" else "nav-item"

    if is_logged_in:
        nav_dashboard = f'<a href="/dashboard" class="{dash_cls}">لوحة التحكم 📊</a>'
        nav_auth = '<a href="/logout" class="nav-item">خروج 🚪</a>'
    else:
        nav_dashboard = ''
        nav_auth = f'<a href="/login" class="{login_cls}">دخول 🔑</a>'

    return f"""<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <title>{title} | منصة رفيق الموحدة</title>
    {COMMON_STYLE}
</head>
<body>
    <header class="app-header">
        <div class="nav-container">
            <a href="/" class="brand">
                <span class="brand-icon">🐺</span>
                <span>رفيق Rafeeq</span>
            </a>
            <nav class="nav-menu">
                <a href="/" class="{home_cls}">الرئيسية</a>
                <a href="/shorts" class="{shorts_cls}">شورتس 📱</a>
                <a href="/auctions" class="{auctions_cls}">المزادات 🔨</a>
                <a href="/kernel" class="{kernel_cls}">النواة ⚙️</a>
                {nav_dashboard}
                {nav_auth}
            </nav>
        </div>
    </header>
    <main class="main-content">
        {content}
    </main>
    <footer class="footer">
        منظومة رفيق الموحدة v3.2.0 • دولة الذئب الرقمية 🐺 • جميع الحقوق محفوظة 2026
    </footer>
</body>
</html>"""

# --- WEB ROUTES ---

@app.route("/", methods=["GET"])
def index():
    if "user_email" in session:
        return redirect("/dashboard")
    
    content = """
    <div class="glass-card" style="text-align: center; padding: 2.5rem 1.5rem;">
        <div style="font-size: 3.5rem; margin-bottom: 1rem;">🐺✨</div>
        <h1 class="title-gold" style="font-size: 1.8rem;">مرحبًا بكم في منصة رفيق الموحدة</h1>
        <p class="subtitle-text" style="max-width: 600px; margin: 0.5rem auto 1.5rem auto;">
            المنصة الذكية المتكاملة للفيديوهات القصيرة (Shorts)، البث المباشر، والمزادات اللحظية مع نظام التسويق بالعمولة والمتاجر الإلكترونية.
        </p>

        <div class="grid-stats" style="max-width: 700px; margin: 1.5rem auto;">
            <div class="stat-box">
                <div class="stat-value">Shorts 📱</div>
                <div class="stat-label">فيديوهات قصيرة بروابط شراء</div>
            </div>
            <div class="stat-box">
                <div class="stat-value">Live 🔨</div>
                <div class="stat-label">مزادات وبث حي مباشر</div>
            </div>
            <div class="stat-box">
                <div class="stat-value">Slots 🏪</div>
                <div class="stat-label">حجز فتحات المتاجر</div>
            </div>
            <div class="stat-box">
                <div class="stat-value">VIP 👑</div>
                <div class="stat-label">مكافآت وصناع المحتوى</div>
            </div>
        </div>

        <div style="display: flex; gap: 0.75rem; justify-content: center; max-width: 400px; margin: 1.5rem auto 0 auto; flex-wrap: wrap;">
            <a href="/login" class="btn btn-gold">🔑 دخول سريع كمستخدم / زائر</a>
            <a href="/shorts" class="btn btn-outline">📱 استعراض شورتس رفيق</a>
        </div>
    </div>
    """
    return render_layout("الرئيسية", content, active_page="home")

@app.route("/login", methods=["GET", "POST"])
def login_page():
    message = ""
    if request.method == "POST":
        email = request.form.get("email", "").strip()
        name = request.form.get("name", "عمر الصديق").strip()
        if email or request.form.get("guest_login"):
            session["user_email"] = email or "omarlhlbwy441@gmail.com"
            session["user_name"] = name or "عمر الصديق"
            return redirect("/dashboard")
        else:
            message = "يرجى كتابة البريد الإلكتروني أو الضغط على الدخول المباشر."

    if message:
        err_box = f'<div style="background: rgba(239, 68, 68, 0.15); border: 1px solid #ef4444; color: #fca5a5; padding: 0.75rem; border-radius: 10px; font-size: 0.85rem; margin-bottom: 1rem; text-align: center;">{message}</div>'
    else:
        err_box = ''

    content = f"""
    <div style="max-width: 440px; margin: 1rem auto;">
        <div class="glass-card">
            <div style="text-align: center; margin-bottom: 1.5rem;">
                <div style="font-size: 2.8rem; margin-bottom: 0.5rem;">🔑</div>
                <h2 class="title-gold">تسجيل الدخول للمنصة</h2>
                <p class="subtitle-text">أدخل بياناتك أو استخدم الدخول التجريبي السريع</p>
            </div>

            {err_box}

            <form method="POST">
                <div class="form-group">
                    <label class="form-label">الاسم الكامل:</label>
                    <input type="text" name="name" class="form-input" value="عمر الصديق" placeholder="أدخل اسمك">
                </div>
                <div class="form-group">
                    <label class="form-label">البريد الإلكتروني / الهاتف:</label>
                    <input type="email" name="email" class="form-input" value="omarlhlbwy441@gmail.com" placeholder="name@example.com">
                </div>
                <button type="submit" class="btn btn-gold" style="margin-top: 0.5rem;">دخول الحساب 🚀</button>
            </form>

            <div style="position: relative; text-align: center; margin: 1.25rem 0; color: #6b7280; font-size: 0.8rem;">
                <span>أو للتجربة الفورية</span>
            </div>

            <form method="POST">
                <input type="hidden" name="guest_login" value="true">
                <input type="hidden" name="name" value="عمر الصديق (زائر VIP)">
                <input type="hidden" name="email" value="vip.guest@rafeeq.sa">
                <button type="submit" class="btn btn-outline" style="border-color: #38bdf8; color: #38bdf8;">
                    🐺 دخول مباشر كزائر VIP (معاينة شاملة)
                </button>
            </form>
        </div>
    </div>
    """
    return render_layout("تسجيل الدخول", content, active_page="login")

@app.route("/dashboard", methods=["GET"])
def dashboard():
    if "user_email" not in session:
        return redirect("/login")

    user_name = session.get("user_name", "عمر الصديق")
    user_email = session.get("user_email", "omarlhlbwy441@gmail.com")
    initial_char = user_name[0] if user_name else '🐺'

    content = f"""
    <div class="glass-card">
        <div class="user-profile-header">
            <div class="avatar-circle">
                {initial_char}
            </div>
            <div style="flex: 1; min-width: 200px;">
                <div style="display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap;">
                    <h2 class="title-gold" style="margin-bottom: 0;">{user_name}</h2>
                    <span class="badge badge-gold">عضوية VIP الملكية 👑</span>
                </div>
                <div class="subtitle-text" style="font-size: 0.85rem; margin-top: 0.2rem;">{user_email}</div>
            </div>
            <div>
                <a href="/logout" class="btn btn-outline" style="padding: 0.4rem 0.8rem; min-height: 36px; font-size: 0.8rem;">تسجيل خروج 🚪</a>
            </div>
        </div>

        <div class="grid-stats">
            <div class="stat-box">
                <div class="stat-value">4,250 SAR</div>
                <div class="stat-label">أرباح المحتوى والعمولات</div>
            </div>
            <div class="stat-box">
                <div class="stat-value">1,250 💎</div>
                <div class="stat-label">رصيد الهدايا والنقاط</div>
            </div>
            <div class="stat-box">
                <div class="stat-value">2 نشطة</div>
                <div class="stat-label">فتحات المتاجر المفتوحة</div>
            </div>
            <div class="stat-box">
                <div class="stat-value">1,420</div>
                <div class="stat-label">التفاعل والمشاهدين</div>
            </div>
        </div>
    </div>

    <!-- Quick Services Grid -->
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1rem;">
        <div class="glass-card">
            <h3 class="title-gold" style="font-size: 1.1rem; display: flex; align-items: center; gap: 0.5rem;">
                📱 شورتس رفيق (Rafeeq Shorts)
            </h3>
            <p class="subtitle-text" style="font-size: 0.85rem; margin-bottom: 1rem;">
                شاهد الفيديوهات القصيرة، اربط منتجات متجرك مع العمولات المباشرة، وتلقى الهدايا المالية اللحظية.
            </p>
            <a href="/shorts" class="btn btn-blue" style="font-size: 0.85rem; min-height: 40px;">استعراض فيديوهات Shorts 📱</a>
        </div>

        <div class="glass-card">
            <h3 class="title-gold" style="font-size: 1.1rem; display: flex; align-items: center; gap: 0.5rem;">
                🔨 البث الحي والمزادات
            </h3>
            <p class="subtitle-text" style="font-size: 0.85rem; margin-bottom: 1rem;">
                شارك في المزادات المباشرة، زِد على السلع النادرة، واستمتع بتجربة الشراء الحية عبر منصة رفيق.
            </p>
            <a href="/auctions" class="btn btn-gold" style="font-size: 0.85rem; min-height: 40px;">الانضمام للمزادات الحية 🔨</a>
        </div>
    </div>

    <!-- Store Slots Status -->
    <div class="glass-card" style="margin-top: 1rem;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; flex-wrap: wrap; gap: 0.5rem;">
            <h3 class="title-gold" style="font-size: 1.1rem;">🏪 فتحات المتاجر المسجلة (StoreSlots)</h3>
            <span class="badge badge-green">حالة النظام: نشط 100%</span>
        </div>
        <div style="overflow-x: auto;">
            <table style="width: 100%; border-collapse: collapse; text-align: right; font-size: 0.85rem;">
                <thead>
                    <tr style="border-bottom: 1px solid rgba(212, 175, 55, 0.2); color: #d4af37;">
                        <th style="padding: 0.6rem;">الكود</th>
                        <th style="padding: 0.6rem;">اسم المتجر</th>
                        <th style="padding: 0.6rem;">التصنيف</th>
                        <th style="padding: 0.6rem;">الفتحات</th>
                        <th style="padding: 0.6rem;">الرسوم</th>
                    </tr>
                </thead>
                <tbody>
                    <tr style="border-bottom: 1px solid rgba(255, 255, 255, 0.05);">
                        <td style="padding: 0.6rem; font-weight: bold; color: #38bdf8;">SLOT-01</td>
                        <td style="padding: 0.6rem;">متجر العطور الملكية</td>
                        <td style="padding: 0.6rem;">عطور فاخرة</td>
                        <td style="padding: 0.6rem;">5 فتحات</td>
                        <td style="padding: 0.6rem; color: #34d399;">50 SAR</td>
                    </tr>
                    <tr>
                        <td style="padding: 0.6rem; font-weight: bold; color: #38bdf8;">SLOT-02</td>
                        <td style="padding: 0.6rem;">معرض التحف النادرة</td>
                        <td style="padding: 0.6rem;">تحف وآثار</td>
                        <td style="padding: 0.6rem;">3 فتحات</td>
                        <td style="padding: 0.6rem; color: #34d399;">100 SAR</td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
    """
    return render_layout("لوحة التحكم", content, active_page="dashboard")

@app.route("/shorts", methods=["GET"])
def shorts_page():
    content = """
    <div style="max-width: 600px; margin: 0 auto;">
        <div style="text-align: center; margin-bottom: 1rem;">
            <h2 class="title-gold">📱 رفيق شورتس | Rafeeq Shorts</h2>
            <p class="subtitle-text">استعرض الفيديوهات، اشترِ المنتجات المربوطة مباشرة، وأرسل الهدايا</p>
        </div>

        <div class="video-card">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem;">
                <div style="display: flex; align-items: center; gap: 0.5rem;">
                    <div style="width: 38px; height: 38px; border-radius: 50%; background: #d4af37; color: #000; font-weight: bold; display: flex; align-items: center; justify-content: center;">ع</div>
                    <div>
                        <div style="font-weight: bold; color: #fff;">عمر الهلباوي</div>
                        <div style="font-size: 0.75rem; color: #9ca3af;">@omarlhlbwy</div>
                    </div>
                </div>
                <span class="badge badge-gold">عائد: 1,240 SAR</span>
            </div>

            <div style="background: linear-gradient(135deg, #1e293b, #0f172a); border-radius: 12px; padding: 2rem 1rem; text-align: center; margin-bottom: 0.75rem; border: 1px dashed rgba(212, 175, 55, 0.3);">
                <div style="font-size: 3rem; margin-bottom: 0.5rem;">🗡️✨</div>
                <div style="font-size: 0.95rem; color: #f5e6c8; font-weight: bold;">عرض مميز لخنجر الرفيق الملكي المصنوع يدويًا ✨</div>
                <div style="font-size: 0.8rem; color: #9ca3af; margin-top: 0.25rem;">🎵 الصوت الأصلي - رفيق نيتزن</div>
            </div>

            <div style="background: rgba(56, 189, 248, 0.1); border: 1px solid rgba(56, 189, 248, 0.3); border-radius: 12px; padding: 0.75rem; margin-bottom: 0.75rem; display: flex; justify-content: space-between; align-items: center;">
                <div>
                    <div style="font-size: 0.85rem; font-weight: bold; color: #38bdf8;">خنجر الرفيق الملكي الأصيل</div>
                    <div style="font-size: 0.75rem; color: #9ca3af;">السعر: 350 SAR • عمولة تسويق: 15%</div>
                </div>
                <button onclick="alert('تم تحويلك إلى المتجر لشراء المنتج!')" class="btn btn-gold" style="width: auto; padding: 0.4rem 0.8rem; font-size: 0.8rem; min-height: 36px;">شراء 🛒</button>
            </div>

            <div style="display: flex; justify-content: space-around; align-items: center; font-size: 0.85rem; color: #9ca3af; border-top: 1px solid rgba(255,255,255,0.05); padding-top: 0.5rem;">
                <button onclick="alert('شكرًا لإعجابك!')" style="background:none; border:none; color:#fca5a5; cursor:pointer; font-family:'Tajawal';">❤️ 1,420 إعجاب</button>
                <button onclick="alert('فتح التعليقات')" style="background:none; border:none; color:#fff; cursor:pointer; font-family:'Tajawal';">💬 88 تعليق</button>
                <button onclick="alert('تم إرسال هدية مالية للصانع!')" style="background:none; border:none; color:#f5e6c8; cursor:pointer; font-family:'Tajawal';">🎁 إهداء الصانع</button>
            </div>
        </div>
    </div>
    """
    return render_layout("رفيق شورتس", content, active_page="shorts")

@app.route("/auctions", methods=["GET"])
def auctions_page():
    content = """
    <div style="max-width: 650px; margin: 0 auto;">
        <div style="text-align: center; margin-bottom: 1rem;">
            <h2 class="title-gold">🔨 المزادات الحية والبث المباشر</h2>
            <p class="subtitle-text">المزايدة اللحظية التفاعلية مع مشاهير المنصة</p>
        </div>

        <div class="glass-card">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; flex-wrap: wrap; gap: 0.5rem;">
                <div style="display: flex; align-items: center; gap: 0.5rem;">
                    <span style="background: red; color: white; padding: 0.2rem 0.5rem; border-radius: 6px; font-size: 0.75rem; font-weight: bold;">مباشر LIVE 🔴</span>
                    <strong style="color: #fff;">مزادات الرفيق الملكية</strong>
                </div>
                <span style="color: #38bdf8; font-size: 0.85rem;">👀 1,420 مشاهد الآن</span>
            </div>

            <div style="background: rgba(255,255,255,0.03); border: 1px solid rgba(212,175,55,0.2); border-radius: 14px; padding: 1.25rem; text-align: center; margin-bottom: 1rem;">
                <div style="font-size: 2.5rem; margin-bottom: 0.5rem;">⌚✨</div>
                <h3 style="color: #f5e6c8; font-size: 1.2rem; margin-bottom: 0.25rem;">ساعة يد أصلية مرصعة بالزمرد ⌚</h3>
                <div style="font-size: 0.85rem; color: #9ca3af;">ينتهي المزاد خلال: <strong style="color: #ef4444;">8 دقائق</strong></div>
            </div>

            <div style="display: flex; justify-content: space-between; align-items: center; background: rgba(0,0,0,0.3); padding: 1rem; border-radius: 12px; margin-bottom: 1rem;">
                <div>
                    <div style="font-size: 0.8rem; color: #9ca3af;">أعلى مزايدة حالية:</div>
                    <div id="bid-price" style="font-size: 1.5rem; font-weight: 900; color: #34d399;">1,200 SAR</div>
                    <div style="font-size: 0.75rem; color: #9ca3af;">المزايد: @faisal_saud</div>
                </div>
                <button onclick="bidMore()" class="btn btn-gold" style="width: auto; padding: 0.6rem 1.2rem;">المزايدة بـ +50 SAR 🔨</button>
            </div>
        </div>
    </div>

    <script>
    let currentBid = 1200;
    function bidMore() {
        currentBid += 50;
        document.getElementById('bid-price').innerText = currentBid.toLocaleString() + ' SAR';
        alert('تهانينا! أصبحت أنت أعلى مزايد بـ ' + currentBid + ' SAR 🔨');
    }
    </script>
    """
    return render_layout("المزادات الحية", content, active_page="auctions")

@app.route("/kernel", methods=["GET"])
def kernel_status():
    content = """
    <div style="max-width: 550px; margin: 0 auto;">
        <div class="glass-card" style="text-align: center;">
            <div style="font-size: 3rem; margin-bottom: 0.5rem;">🐺</div>
            <h2 class="title-gold">Rafeeq Kernel v3.2.0</h2>
            <p class="subtitle-text" style="margin-bottom: 1.25rem;">حالة النواة وقاعدة البيانات في بيئة Render</p>

            <div class="grid-stats">
                <div class="stat-box">
                    <div class="stat-value" style="color: #34d399;">متصل Online</div>
                    <div class="stat-label">قاعدة البيانات</div>
                </div>
                <div class="stat-box">
                    <div class="stat-value">v3.2.0</div>
                    <div class="stat-label">إصدار النظام</div>
                </div>
                <div class="stat-box">
                    <div class="stat-value">1</div>
                    <div class="stat-label">المستخدمون النشطون</div>
                </div>
                <div class="stat-box">
                    <div class="stat-value">100%</div>
                    <div class="stat-label">جاهزية الخدمات</div>
                </div>
            </div>

            <div style="text-align: right; background: rgba(0,0,0,0.3); padding: 1rem; border-radius: 12px; margin: 1rem 0; font-size: 0.85rem;">
                <div style="color: #34d399; margin-bottom: 0.3rem;">✓ وحدة المستودع والمتاجر (StoreSlots)</div>
                <div style="color: #34d399; margin-bottom: 0.3rem;">✓ محرك الشورتس والبث الحي (Live Auctions)</div>
                <div style="color: #34d399; margin-bottom: 0.3rem;">✓ نظام VIP والتسويق بالعمولة (Affiliate)</div>
                <div style="color: #34d399;">✓ قاعدة بيانات PostgreSQL (Render Cloud)</div>
            </div>

            <a href="/dashboard" class="btn btn-gold">العودة للوحة التحكم 📊</a>
        </div>
    </div>
    """
    return render_layout("حالة النواة", content, active_page="kernel")

@app.route("/logout", methods=["GET"])
def logout():
    session.clear()
    return redirect("/login")

@app.errorhandler(404)
def page_not_found(e):
    content = """
    <div class="glass-card" style="text-align: center; max-width: 420px; margin: 2rem auto; padding: 2rem 1.5rem;">
        <div style="font-size: 4rem; margin-bottom: 0.5rem;">🔍</div>
        <h2 class="title-gold">الصفحة غير موجودة (404)</h2>
        <p class="subtitle-text" style="margin-bottom: 1.5rem;">الصفحة التي تحاول الوصول إليها غير مسجلة أو تم نقلها.</p>
        <a href="/" class="btn btn-gold">🏠 العودة للرئيسية</a>
    </div>
    """
    return render_layout("404 الصفحة غير موجودة", content), 404

# --- JSON API ENDPOINTS ---

@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "healthy", "service": "dtr1-n-1"}), 200

@app.route("/api/v1/shorts", methods=["GET"])
def get_shorts():
    videos = [
        {
            "id": "v1",
            "creatorName": "عمر الهلباوي",
            "creatorHandle": "@omarlhlbwy",
            "description": "عرض مميز لخنجر الرفيق الملكي المصنوع يدويًا ✨",
            "likesCount": 1420,
            "viewsCount": "45.2K",
            "priceSar": "350 SAR"
        },
        {
            "id": "v2",
            "creatorName": "سارة أحمد",
            "creatorHandle": "@sara_store",
            "description": "مراجعة سريعة لأحدث العطور الشرقية المتاحة حصريًا عبر فتحات متجر رفيق 🌸",
            "likesCount": 2890,
            "viewsCount": "92.1K",
            "priceSar": "180 SAR"
        }
    ]
    return jsonify({"success": True, "videos": videos}), 200

@app.route("/api/v1/stores/slots", methods=["GET"])
def get_store_slots():
    slots = [
        {"id": 1, "code": "SLOT-01", "name": "متجر العطور الملكية", "category": "عطور", "slots": 5, "fee": "50 SAR", "status": "نشط"},
        {"id": 2, "code": "SLOT-02", "name": "معرض التحف النادرة", "category": "تحف", "slots": 3, "fee": "100 SAR", "status": "نشط"}
    ]
    return jsonify({"success": True, "slots": slots}), 200

@app.route("/api/v1/auctions", methods=["GET"])
def get_auctions():
    auctions = [
        {
            "id": "auction_1",
            "streamerName": "مزادات الرفيق الملكية",
            "itemTitle": "ساعة يد أصلية مرصعة بالزمرد ⌚",
            "currentBidSar": 1200,
            "highestBidder": "@faisal_saud",
            "activeViewers": "1,420"
        }
    ]
    return jsonify({"success": True, "auctions": auctions}), 200

@app.route("/api/v1/social/posts", methods=["GET"])
def get_posts():
    posts = [
        {
            "author": "دولة الذئب الرقمية 🐺",
            "content": "مرحبًا بكم في منظومة رفيق الموحدة. أطلقت المنظومة تحديث 3.2.0 للشورتس والبث المباشر والمزادات الحية!",
            "likes": 342,
            "time": "منذ 10 دقائق"
        }
    ]
    return jsonify({"success": True, "posts": posts}), 200

if __name__ == "__main__":
    port = int(os.getenv("PORT", 10000))
    logger.info(f"Starting Rafeeq server on port {port}...")
    app.run(host="0.0.0.0", port=port)
