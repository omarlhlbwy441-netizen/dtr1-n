"""
Rafeeq Database Migration Script
Adds missing columns to existing tables
"""
import os
import psycopg2

DATABASE_URL = os.getenv('DATABASE_URL', 'postgresql://dtr_no_user:GRtFA4nVLhnELSi8xTookZyKasr8XoME@dpg-d9dlnlv7f7vs738ugbe0-a/dtr_no')

# Fix URL for psycopg2
if DATABASE_URL.startswith('postgres://'):
    DATABASE_URL = DATABASE_URL.replace('postgres://', 'postgresql://', 1)

def migrate():
    conn = psycopg2.connect(DATABASE_URL)
    cursor = conn.cursor()

    # Check if avatar column exists in users table
    cursor.execute("""
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'avatar'
    """)

    if not cursor.fetchone():
        print("Adding avatar column to users table...")
        cursor.execute("ALTER TABLE users ADD COLUMN avatar VARCHAR(200) DEFAULT '👤'")
        conn.commit()
        print("✅ avatar column added")
    else:
        print("✅ avatar column already exists")

    # Check if full_name column exists
    cursor.execute("""
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'full_name'
    """)

    if not cursor.fetchone():
        print("Adding full_name column to users table...")
        cursor.execute("ALTER TABLE users ADD COLUMN full_name VARCHAR(100) DEFAULT NULL")
        conn.commit()
        print("✅ full_name column added")
    else:
        print("✅ full_name column already exists")

    # Check if last_login column exists
    cursor.execute("""
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'last_login'
    """)

    if not cursor.fetchone():
        print("Adding last_login column to users table...")
        cursor.execute("ALTER TABLE users ADD COLUMN last_login TIMESTAMP DEFAULT NULL")
        conn.commit()
        print("✅ last_login column added")
    else:
        print("✅ last_login column already exists")

    # Check if login_count column exists
    cursor.execute("""
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'login_count'
    """)

    if not cursor.fetchone():
        print("Adding login_count column to users table...")
        cursor.execute("ALTER TABLE users ADD COLUMN login_count INTEGER DEFAULT 0")
        conn.commit()
        print("✅ login_count column added")
    else:
        print("✅ login_count column already exists")

    # Check if is_premium column exists
    cursor.execute("""
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'is_premium'
    """)

    if not cursor.fetchone():
        print("Adding is_premium column to users table...")
        cursor.execute("ALTER TABLE users ADD COLUMN is_premium BOOLEAN DEFAULT FALSE")
        conn.commit()
        print("✅ is_premium column added")
    else:
        print("✅ is_premium column already exists")

    cursor.close()
    conn.close()
    print("\n🎉 Migration completed!")

if __name__ == "__main__":
    migrate()
