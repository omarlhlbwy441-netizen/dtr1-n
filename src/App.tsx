import React, { useState, useEffect } from 'react';
import { 
  Store, ShoppingBag, LayoutDashboard, Shield, LogIn, LogOut, 
  User, CheckCircle, AlertCircle, Plus, Search, Star, TrendingUp,
  CreditCard, Package, Settings, Eye, ChevronRight, Filter, Clock,
  DollarSign, ShoppingCart, Bell, Check, X, Building, ArrowRight
} from 'lucide-react';

interface Product {
  id: number;
  name: string;
  slug: string;
  description: string;
  price: number;
  old_price?: number;
  category: string;
  stock: number;
  is_featured: boolean;
  store_name: string;
  store_slug: string;
  views?: number;
  sales?: number;
}

interface StoreItem {
  id: number;
  name: string;
  slug: string;
  description: string;
  type: string;
  slots: number;
  status: 'pending' | 'approved' | 'rejected';
}

interface Application {
  id: number;
  store_name: string;
  business_type: string;
  requested_slots: number;
  contact_phone: string;
  contact_email: string;
  created_at: string;
  username: string;
}

export default function App() {
  const [activeTab, setActiveTab] = useState<'home' | 'apply' | 'merchant' | 'admin' | 'product'>('home');
  const [currentUser, setCurrentUser] = useState<{ username: string; is_admin: boolean; role: string; is_merchant: boolean } | null>({
    username: 'admin',
    is_admin: true,
    role: 'admin',
    is_merchant: true
  });
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login');
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  
  // Auth Form State
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');

  // Search & Filter
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('all');

  // Sample Mock / Initial Data
  const [stores, setStores] = useState<StoreItem[]>([
    { id: 1, name: 'متجر الأناقة والجمال', slug: 'elegance-store', description: 'أرقى الملابس والإكسسوارات العصرية', type: 'fashion', slots: 2, status: 'approved' },
    { id: 2, name: 'تقنيات المستقبل', slug: 'future-tech', description: 'أحدث الأجهزة الذكية والإلكترونيات', type: 'tech', slots: 3, status: 'approved' },
    { id: 3, name: 'عالم المنزل العصري', slug: 'modern-home', description: 'مستلزمات المنزل والديكورات الحديثة', type: 'home', slots: 1, status: 'approved' }
  ]);

  const [products, setProducts] = useState<Product[]>([
    { id: 101, name: 'ساعة ذكية فاخرة V8', slug: 'smart-watch-v8', description: 'ساعة ذكية مقاومة للماء مع شاشة AMOLED ومراقبة نبضات القلب', price: 1850, old_price: 2200, category: 'tech', stock: 15, is_featured: true, store_name: 'تقنيات المستقبل', store_slug: 'future-tech', views: 340, sales: 28 },
    { id: 102, name: 'سماعات لاسلكية Pro', slug: 'airpods-pro', description: 'سماعات بلوتوث مع عزل ضوضاء نقي وبطارية تدوم 24 ساعة', price: 950, old_price: 1200, category: 'tech', stock: 22, is_featured: true, store_name: 'تقنيات المستقبل', store_slug: 'future-tech', views: 510, sales: 45 },
    { id: 103, name: 'حقيبة جلدية عصرية', slug: 'leather-bag', description: 'حقيبة يد جلدية فاخرة مصممة من أجود أنواع الجلد الطبيعي', price: 1450, old_price: 1700, category: 'fashion', stock: 8, is_featured: true, store_name: 'متجر الأناقة والجمال', store_slug: 'elegance-store', views: 210, sales: 12 },
    { id: 104, name: 'طقم إضاءة ذكية RGB', slug: 'rgb-light-set', description: 'شريط إضاءة ليد ذكي يعمل بالتحكم الصوتي وتطبيق الهاتف', price: 420, old_price: 550, category: 'home', stock: 30, is_featured: true, store_name: 'عالم المنزل العصري', store_slug: 'modern-home', views: 180, sales: 19 }
  ]);

  const [applications, setApplications] = useState<Application[]>([
    { id: 1, store_name: 'متجر العطور الملكية', business_type: 'beauty', requested_slots: 2, contact_phone: '01012345678', contact_email: 'royal@perfumes.com', created_at: '2026-07-21', username: 'merchant_royal' },
    { id: 2, store_name: 'عالم الألعاب الذكية', business_type: 'tech', requested_slots: 1, contact_phone: '01298765432', contact_email: 'games@world.com', created_at: '2026-07-22', username: 'games_master' }
  ]);

  const [orders, setOrders] = useState<any[]>([
    { id: 501, buyer_name: 'أحمد محمود', buyer_phone: '01122334455', product_name: 'ساعة ذكية فاخرة V8', quantity: 1, total: 1850, status: 'pending', date: '2026-07-22' },
    { id: 502, buyer_name: 'سارة خالد', buyer_phone: '01055667788', product_name: 'سماعات لاسلكية Pro', quantity: 2, total: 1900, status: 'completed', date: '2026-07-21' }
  ]);

  // Notifications
  const [notifications, setNotifications] = useState([
    { id: 1, title: 'مرحباً بك في رفيق', message: 'منصة التجارة الذكية جاهزة للاستخدام', time: 'منذ قليل', is_read: false },
    { id: 2, title: 'طلب جديد', message: 'تم استلام طلب جديد لشراء ساعة ذكية', time: 'منذ ساعة', is_read: false }
  ]);
  const [showNotifications, setShowNotifications] = useState(false);

  // New Product Form
  const [newProductName, setNewProductName] = useState('');
  const [newProductPrice, setNewProductPrice] = useState('');
  const [newProductCategory, setNewProductCategory] = useState('tech');
  const [newProductStock, setNewProductStock] = useState('10');
  const [newProductDesc, setNewProductDesc] = useState('');

  // New Application Form
  const [appStoreName, setAppStoreName] = useState('');
  const [appType, setAppType] = useState('retail');
  const [appSlots, setAppSlots] = useState(1);
  const [appPhone, setAppPhone] = useState('');
  const [appDesc, setAppDesc] = useState('');
  const [appSuccess, setAppSuccess] = useState(false);

  // Order Modal
  const [orderBuyerName, setOrderBuyerName] = useState('');
  const [orderBuyerPhone, setOrderBuyerPhone] = useState('');
  const [orderBuyerAddress, setOrderBuyerAddress] = useState('');
  const [orderQty, setOrderQty] = useState(1);
  const [orderSuccess, setOrderSuccess] = useState(false);

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    if (username) {
      const isAdmin = username.toLowerCase() === 'admin';
      setCurrentUser({
        username,
        is_admin: isAdmin,
        role: isAdmin ? 'admin' : 'user',
        is_merchant: true
      });
      setShowAuthModal(false);
      setUsername('');
      setPassword('');
    }
  };

  const handleAddProduct = (e: React.FormEvent) => {
    e.preventDefault();
    if (newProductName && newProductPrice) {
      const p: Product = {
        id: Date.now(),
        name: newProductName,
        slug: newProductName.toLowerCase().replace(/\s+/g, '-'),
        description: newProductDesc || 'منتج عالي الجودة متوفر لدى متجرنا',
        price: parseFloat(newProductPrice),
        category: newProductCategory,
        stock: parseInt(newProductStock) || 1,
        is_featured: true,
        store_name: 'تقنيات المستقبل',
        store_slug: 'future-tech',
        views: 1,
        sales: 0
      };
      setProducts([p, ...products]);
      setNewProductName('');
      setNewProductPrice('');
      setNewProductDesc('');
      alert('تم إضافة المنتج بنجاح!');
    }
  };

  const handleApplyStore = (e: React.FormEvent) => {
    e.preventDefault();
    if (appStoreName) {
      const newApp: Application = {
        id: Date.now(),
        store_name: appStoreName,
        business_type: appType,
        requested_slots: appSlots,
        contact_phone: appPhone || '01000000000',
        contact_email: currentUser ? `${currentUser.username}@example.com` : 'user@example.com',
        created_at: new Date().toISOString().split('T')[0],
        username: currentUser?.username || 'زائر'
      };
      setApplications([newApp, ...applications]);
      setAppSuccess(true);
      setTimeout(() => {
        setAppSuccess(false);
        setActiveTab('home');
      }, 2000);
    }
  };

  const handleApproveApp = (appId: number) => {
    const appItem = applications.find(a => a.id === appId);
    if (appItem) {
      setStores([...stores, {
        id: Date.now(),
        name: appItem.store_name,
        slug: appItem.store_name.toLowerCase().replace(/\s+/g, '-'),
        description: 'متجر معتمد جديد في منصة رفيق',
        type: appItem.business_type,
        slots: appItem.requested_slots,
        status: 'approved'
      }]);
      setApplications(applications.filter(a => a.id !== appId));
      alert(`تم اعتماد متجر ${appItem.store_name} بنجاح!`);
    }
  };

  const handleRejectApp = (appId: number) => {
    setApplications(applications.filter(a => a.id !== appId));
    alert('تم رفض الطلب');
  };

  const handlePlaceOrder = (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedProduct && orderBuyerName && orderBuyerPhone) {
      const newOrd = {
        id: Date.now(),
        buyer_name: orderBuyerName,
        buyer_phone: orderBuyerPhone,
        product_name: selectedProduct.name,
        quantity: orderQty,
        total: selectedProduct.price * orderQty,
        status: 'pending',
        date: new Date().toISOString().split('T')[0]
      };
      setOrders([newOrd, ...orders]);
      setOrderSuccess(true);
      setTimeout(() => {
        setOrderSuccess(false);
        setSelectedProduct(null);
        setOrderBuyerName('');
        setOrderBuyerPhone('');
        setOrderBuyerAddress('');
      }, 1800);
    }
  };

  const filteredProducts = products.filter(p => {
    const matchesSearch = p.name.includes(searchQuery) || p.description.includes(searchQuery);
    const matchesCategory = selectedCategory === 'all' || p.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  return (
    <div dir="rtl" className="min-h-screen bg-slate-900 text-slate-100 font-sans flex flex-col selection:bg-sky-500 selection:text-white">
      {/* Top Navbar */}
      <header className="sticky top-0 z-40 bg-slate-900/90 backdrop-blur-md border-b border-slate-800/80">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
          
          {/* Logo & Brand */}
          <div className="flex items-center gap-3 cursor-pointer" onClick={() => setActiveTab('home')}>
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-sky-500 to-indigo-600 flex items-center justify-center text-white shadow-lg shadow-sky-500/20 font-black text-xl">
              ر
            </div>
            <div>
              <span className="text-xl font-bold bg-gradient-to-r from-sky-400 via-indigo-400 to-emerald-400 bg-clip-text text-transparent">
                رفيق | Rafeeq
              </span>
              <span className="block text-[10px] text-slate-400 font-medium tracking-wide">
                منصة التجارة الذكية v3.1.0
              </span>
            </div>
          </div>

          {/* Nav Links */}
          <nav className="hidden md:flex items-center gap-1 bg-slate-800/50 p-1 rounded-xl border border-slate-700/50">
            <button
              onClick={() => setActiveTab('home')}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-all flex items-center gap-2 ${
                activeTab === 'home' ? 'bg-sky-500 text-white shadow-md' : 'text-slate-300 hover:text-white hover:bg-slate-700/50'
              }`}
            >
              <Store className="w-4 h-4" />
              المتجر الرئيسي
            </button>

            <button
              onClick={() => setActiveTab('apply')}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-all flex items-center gap-2 ${
                activeTab === 'apply' ? 'bg-sky-500 text-white shadow-md' : 'text-slate-300 hover:text-white hover:bg-slate-700/50'
              }`}
            >
              <Building className="w-4 h-4" />
              طلب توكيل تجاري
            </button>

            {currentUser && (
              <button
                onClick={() => setActiveTab('merchant')}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-all flex items-center gap-2 ${
                  activeTab === 'merchant' ? 'bg-sky-500 text-white shadow-md' : 'text-slate-300 hover:text-white hover:bg-slate-700/50'
                }`}
              >
                <LayoutDashboard className="w-4 h-4" />
                لوحة التاجر
              </button>
            )}

            {currentUser?.is_admin && (
              <button
                onClick={() => setActiveTab('admin')}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-all flex items-center gap-2 ${
                  activeTab === 'admin' ? 'bg-amber-500 text-white shadow-md' : 'text-amber-400 hover:text-amber-300 hover:bg-slate-700/50'
                }`}
              >
                <Shield className="w-4 h-4" />
                لوحة الإدارة
              </button>
            )}
          </nav>

          {/* User Controls & Notifications */}
          <div className="flex items-center gap-3">
            {/* Notification Dropdown Button */}
            <div className="relative">
              <button
                onClick={() => setShowNotifications(!showNotifications)}
                className="w-10 h-10 rounded-xl bg-slate-800 border border-slate-700/60 flex items-center justify-center text-slate-300 hover:text-white transition relative"
              >
                <Bell className="w-5 h-5" />
                {notifications.some(n => !n.is_read) && (
                  <span className="absolute top-2 right-2 w-2 h-2 rounded-full bg-sky-500 animate-ping" />
                )}
              </button>

              {showNotifications && (
                <div className="absolute left-0 mt-2 w-80 bg-slate-800 border border-slate-700 rounded-2xl shadow-2xl p-4 z-50">
                  <div className="flex items-center justify-between pb-2 border-b border-slate-700 mb-3">
                    <h4 className="font-bold text-sm text-slate-200">الإشعارات</h4>
                    <button 
                      onClick={() => setNotifications(notifications.map(n => ({ ...n, is_read: true })))}
                      className="text-xs text-sky-400 hover:underline"
                    >
                      تحديد الكل كقروء
                    </button>
                  </div>
                  <div className="space-y-2 max-h-60 overflow-y-auto">
                    {notifications.map(n => (
                      <div key={n.id} className="p-2.5 rounded-xl bg-slate-900/60 border border-slate-700/50 text-xs">
                        <div className="font-semibold text-slate-200">{n.title}</div>
                        <div className="text-slate-400 mt-0.5">{n.message}</div>
                        <div className="text-[10px] text-slate-500 mt-1">{n.time}</div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {currentUser ? (
              <div className="flex items-center gap-2">
                <div className="hidden sm:block text-left">
                  <div className="text-xs font-bold text-slate-200">{currentUser.username}</div>
                  <div className="text-[10px] text-sky-400 font-medium">
                    {currentUser.is_admin ? 'مدير النظام' : 'تاجر معتمد'}
                  </div>
                </div>
                <button
                  onClick={() => setCurrentUser(null)}
                  className="p-2 rounded-xl bg-red-500/10 text-red-400 hover:bg-red-500/20 border border-red-500/20 text-xs font-semibold flex items-center gap-1 transition"
                  title="تسجيل الخروج"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              </div>
            ) : (
              <button
                onClick={() => setShowAuthModal(true)}
                className="px-4 py-2 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 text-white font-bold text-xs shadow-lg shadow-sky-500/20 flex items-center gap-2 hover:opacity-95 transition"
              >
                <LogIn className="w-4 h-4" />
                تسجيل الدخول
              </button>
            )}
          </div>
        </div>
      </header>

      {/* Main Body View Controller */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        
        {/* VIEW 1: HOME STOREFRONT */}
        {activeTab === 'home' && (
          <div className="space-y-10">
            {/* Hero Section */}
            <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-800 via-slate-900 to-slate-950 border border-slate-800 p-8 sm:p-12 text-center">
              <div className="absolute top-0 right-1/4 w-96 h-96 bg-sky-500/10 rounded-full blur-3xl pointer-events-none" />
              <div className="absolute bottom-0 left-1/4 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />
              
              <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-sky-500/10 border border-sky-500/20 text-sky-400 text-xs font-semibold mb-4">
                <Star className="w-3.5 h-3.5 fill-sky-400" />
                منصة التجارة والتسويق المتكاملة
              </span>

              <h1 className="text-3xl sm:text-5xl font-black bg-gradient-to-r from-white via-slate-100 to-slate-300 bg-clip-text text-transparent leading-tight mb-4 max-w-3xl mx-auto">
                تسوق من أفضل المتاجر المعتمدة أو أنشئ توكيلك التجاري اليوم
              </h1>
              
              <p className="text-slate-400 text-sm sm:text-base max-w-2xl mx-auto mb-8 leading-relaxed">
                منظومة رفيق تمنحك أفضل تجربة شراء إلكترونية مع مساحات عرض تجارية مجهزة بأحدث أدوات الذكاء الاصطناعي والإحصائيات.
              </p>

              <div className="flex flex-wrap justify-center gap-4">
                <button
                  onClick={() => setActiveTab('apply')}
                  className="px-6 py-3.5 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 text-white font-bold text-sm shadow-xl shadow-sky-500/25 hover:shadow-sky-500/40 transition hover:-translate-y-0.5 flex items-center gap-2"
                >
                  <Building className="w-4 h-4" />
                  تقدم بطلب توكيل تجاري
                </button>
                <a
                  href="#products-section"
                  className="px-6 py-3.5 rounded-xl bg-slate-800 hover:bg-slate-700/80 text-slate-200 font-bold text-sm border border-slate-700 transition flex items-center gap-2"
                >
                  <ShoppingBag className="w-4 h-4" />
                  تصفح المنتجات المميزة
                </a>
              </div>
            </div>

            {/* Approved Stores Grid */}
            <section className="space-y-4">
              <div className="flex items-center justify-between">
                <h2 className="text-xl font-bold text-slate-100 flex items-center gap-2">
                  <Store className="w-5 h-5 text-sky-400" />
                  المتاجر المعتمدة في المنصة
                </h2>
                <span className="text-xs text-slate-400 font-medium">
                  {stores.length} متاجر نشطة
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                {stores.map(st => (
                  <div key={st.id} className="p-5 rounded-2xl bg-slate-800/60 border border-slate-700/60 hover:border-sky-500/50 transition cursor-pointer group">
                    <div className="w-12 h-12 rounded-xl bg-sky-500/10 text-sky-400 flex items-center justify-center font-bold text-xl mb-3 group-hover:bg-sky-500 group-hover:text-white transition">
                      🏪
                    </div>
                    <h3 className="font-bold text-slate-100 text-base mb-1">{st.name}</h3>
                    <p className="text-xs text-slate-400 line-clamp-2 mb-3">{st.description}</p>
                    <div className="flex items-center justify-between text-[11px] text-slate-500 border-t border-slate-700/50 pt-2.5">
                      <span className="text-sky-400 font-medium">{st.type}</span>
                      <span>{st.slots} مساحات عرض</span>
                    </div>
                  </div>
                ))}
              </div>
            </section>

            {/* Search & Filter Bar */}
            <section id="products-section" className="space-y-6 pt-4">
              <div className="flex flex-col sm:flex-row gap-4 items-center justify-between bg-slate-800/40 p-4 rounded-2xl border border-slate-800">
                {/* Search Box */}
                <div className="relative w-full sm:w-96">
                  <Search className="w-4 h-4 absolute right-3.5 top-3.5 text-slate-400" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={e => setSearchQuery(e.target.value)}
                    placeholder="ابحث عن منتج أو متجر..."
                    className="w-full pl-4 pr-10 py-2.5 rounded-xl bg-slate-900 border border-slate-700/70 text-slate-100 text-xs focus:outline-none focus:border-sky-500"
                  />
                </div>

                {/* Categories */}
                <div className="flex items-center gap-2 overflow-x-auto w-full sm:w-auto pb-1 sm:pb-0">
                  {[
                    { id: 'all', label: 'الكل' },
                    { id: 'tech', label: 'تقنية' },
                    { id: 'fashion', label: 'أزياء' },
                    { id: 'home', label: 'منزل' }
                  ].map(cat => (
                    <button
                      key={cat.id}
                      onClick={() => setSelectedCategory(cat.id)}
                      className={`px-3.5 py-2 rounded-xl text-xs font-semibold whitespace-nowrap transition ${
                        selectedCategory === cat.id 
                          ? 'bg-sky-500 text-white shadow-md' 
                          : 'bg-slate-800 text-slate-400 hover:text-white'
                      }`}
                    >
                      {cat.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Products Display Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                {filteredProducts.map(p => (
                  <div
                    key={p.id}
                    onClick={() => setSelectedProduct(p)}
                    className="bg-slate-800/70 border border-slate-700/60 rounded-2xl overflow-hidden hover:border-sky-500/50 hover:shadow-2xl hover:shadow-sky-500/10 transition cursor-pointer flex flex-col group"
                  >
                    <div className="h-44 bg-gradient-to-br from-slate-800 to-slate-900 flex items-center justify-center text-4xl relative group-hover:scale-105 transition duration-300">
                      📦
                      {p.old_price && (
                        <span className="absolute top-3 right-3 px-2 py-1 rounded-lg bg-red-500/20 text-red-400 border border-red-500/30 text-[10px] font-bold">
                          خصم مميز
                        </span>
                      )}
                    </div>
                    
                    <div className="p-4 flex-1 flex flex-col justify-between space-y-3">
                      <div>
                        <div className="text-[10px] font-semibold text-sky-400 mb-1">{p.store_name}</div>
                        <h3 className="font-bold text-slate-100 text-sm line-clamp-1 group-hover:text-sky-300 transition">{p.name}</h3>
                        <p className="text-xs text-slate-400 line-clamp-2 mt-1">{p.description}</p>
                      </div>

                      <div className="flex items-center justify-between border-t border-slate-700/50 pt-3">
                        <div>
                          <div className="text-base font-black text-slate-100">{p.price.toLocaleString()} ج.م</div>
                          {p.old_price && (
                            <div className="text-xs text-slate-500 line-through">{p.old_price.toLocaleString()} ج.م</div>
                          )}
                        </div>

                        <button className="px-3 py-1.5 rounded-lg bg-sky-500/10 text-sky-400 group-hover:bg-sky-500 group-hover:text-white text-xs font-bold transition flex items-center gap-1">
                          <ShoppingCart className="w-3.5 h-3.5" />
                          شراء
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          </div>
        )}

        {/* VIEW 2: APPLY FOR FRANCHISE / STORE SLOT */}
        {activeTab === 'apply' && (
          <div className="max-w-2xl mx-auto space-y-6">
            <div className="text-center space-y-2">
              <h2 className="text-3xl font-black bg-gradient-to-r from-sky-400 to-indigo-400 bg-clip-text text-transparent">
                طلب توكيل تجاري في منصة رفيق
              </h2>
              <p className="text-slate-400 text-xs sm:text-sm">
                احصل على مساحات عرض مجهزة وعقد موثق لتسويق منتجاتك إلى آلاف العملاء
              </p>
            </div>

            {appSuccess ? (
              <div className="p-8 rounded-3xl bg-emerald-500/10 border border-emerald-500/30 text-center space-y-3">
                <CheckCircle className="w-12 h-12 text-emerald-400 mx-auto" />
                <h3 className="text-xl font-bold text-emerald-300">تم إرسال طلب التوكيل بنجاح!</h3>
                <p className="text-xs text-slate-300">سيقوم فريق إدارة المنصة بمراجعة طلبك والموافقة عليه خلال 24 ساعة.</p>
              </div>
            ) : (
              <form onSubmit={handleApplyStore} className="bg-slate-800/80 border border-slate-700/80 p-6 sm:p-8 rounded-3xl space-y-5">
                <div>
                  <label className="block text-xs font-bold text-slate-300 mb-1.5">اسم المتجر / البراند *</label>
                  <input
                    type="text"
                    required
                    value={appStoreName}
                    onChange={e => setAppStoreName(e.target.value)}
                    placeholder="مثال: متجر الفخامة العصرية"
                    className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-slate-100 text-xs focus:outline-none focus:border-sky-500"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-slate-300 mb-1.5">نوع النشاط التجاري</label>
                    <select
                      value={appType}
                      onChange={e => setAppType(e.target.value)}
                      className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-slate-100 text-xs focus:outline-none focus:border-sky-500"
                    >
                      <option value="retail">تجارة تجزئة</option>
                      <option value="tech">تقنية وإلكترونيات</option>
                      <option value="fashion">أزياء وموضة</option>
                      <option value="home">منزل وديكور</option>
                      <option value="beauty">جمال وعناية</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-300 mb-1.5">عدد مساحات العرض المطلوبة</label>
                    <input
                      type="number"
                      min="1"
                      max="10"
                      value={appSlots}
                      onChange={e => setAppSlots(parseInt(e.target.value) || 1)}
                      className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-slate-100 text-xs focus:outline-none focus:border-sky-500"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-300 mb-1.5">رقم الهاتف للتواصل</label>
                  <input
                    type="tel"
                    value={appPhone}
                    onChange={e => setAppPhone(e.target.value)}
                    placeholder="01xxxxxxxx"
                    className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-slate-100 text-xs focus:outline-none focus:border-sky-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-300 mb-1.5">وصف المتجر والمنتجات</label>
                  <textarea
                    rows={3}
                    value={appDesc}
                    onChange={e => setAppDesc(e.target.value)}
                    placeholder="اكتب نبذة مختصرة عن المنتجات التي ترغب ببيعها..."
                    className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-slate-100 text-xs focus:outline-none focus:border-sky-500"
                  />
                </div>

                <button
                  type="submit"
                  className="w-full py-3.5 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 text-white font-bold text-sm shadow-xl shadow-sky-500/20 hover:opacity-95 transition"
                >
                  تأكيد وإرسال الطلب
                </button>
              </form>
            )}
          </div>
        )}

        {/* VIEW 3: MERCHANT DASHBOARD */}
        {activeTab === 'merchant' && (
          <div className="space-y-8">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-2xl font-black text-slate-100">لوحة تحكم التاجر</h2>
                <p className="text-xs text-slate-400">متابعة المبيعات والمنتجات وإدارة الطلبات</p>
              </div>

              <div className="px-3 py-1.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-bold flex items-center gap-1.5">
                <CheckCircle className="w-4 h-4" />
                حساب متجر معتمد نشط
              </div>
            </div>

            {/* Merchant Metrics */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="p-5 rounded-2xl bg-slate-800/80 border border-slate-700/80">
                <div className="text-slate-400 text-xs mb-1 font-medium">إجمالي المبيعات</div>
                <div className="text-2xl font-black text-sky-400">3,750 ج.م</div>
                <div className="text-[10px] text-emerald-400 mt-1">↑ 12% هذا الأسبوع</div>
              </div>

              <div className="p-5 rounded-2xl bg-slate-800/80 border border-slate-700/80">
                <div className="text-slate-400 text-xs mb-1 font-medium">عدد المنتجات</div>
                <div className="text-2xl font-black text-indigo-400">{products.length}</div>
                <div className="text-[10px] text-slate-500 mt-1">مساحة متوفرة: 8 slots</div>
              </div>

              <div className="p-5 rounded-2xl bg-slate-800/80 border border-slate-700/80">
                <div className="text-slate-400 text-xs mb-1 font-medium">الطلبات الواردة</div>
                <div className="text-2xl font-black text-emerald-400">{orders.length}</div>
                <div className="text-[10px] text-amber-400 mt-1">1 طلب جديد قيد الانتظار</div>
              </div>

              <div className="p-5 rounded-2xl bg-slate-800/80 border border-slate-700/80">
                <div className="text-slate-400 text-xs mb-1 font-medium">مشاهدات المتجر</div>
                <div className="text-2xl font-black text-amber-400">1,240</div>
                <div className="text-[10px] text-slate-500 mt-1">تفاعل ممتاز</div>
              </div>
            </div>

            {/* Product Add & Manage Grid */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              {/* Add Product Form */}
              <div className="bg-slate-800/80 border border-slate-700/80 p-6 rounded-3xl space-y-4">
                <h3 className="font-bold text-sm text-slate-100 flex items-center gap-2">
                  <Plus className="w-4 h-4 text-sky-400" />
                  إضافة منتج جديد للمتجر
                </h3>

                <form onSubmit={handleAddProduct} className="space-y-3">
                  <div>
                    <label className="block text-[11px] font-semibold text-slate-300 mb-1">اسم المنتج</label>
                    <input
                      type="text"
                      required
                      value={newProductName}
                      onChange={e => setNewProductName(e.target.value)}
                      placeholder="مثال: شاحن سريع 65W"
                      className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-700 text-xs text-slate-100 focus:outline-none focus:border-sky-500"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-[11px] font-semibold text-slate-300 mb-1">السعر (ج.م)</label>
                      <input
                        type="number"
                        required
                        value={newProductPrice}
                        onChange={e => setNewProductPrice(e.target.value)}
                        placeholder="350"
                        className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-700 text-xs text-slate-100 focus:outline-none focus:border-sky-500"
                      />
                    </div>

                    <div>
                      <label className="block text-[11px] font-semibold text-slate-300 mb-1">الكمية</label>
                      <input
                        type="number"
                        value={newProductStock}
                        onChange={e => setNewProductStock(e.target.value)}
                        className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-700 text-xs text-slate-100 focus:outline-none focus:border-sky-500"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-[11px] font-semibold text-slate-300 mb-1">الفئة</label>
                    <select
                      value={newProductCategory}
                      onChange={e => setNewProductCategory(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-700 text-xs text-slate-100 focus:outline-none focus:border-sky-500"
                    >
                      <option value="tech">تقنية</option>
                      <option value="fashion">أزياء</option>
                      <option value="home">منزل</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-[11px] font-semibold text-slate-300 mb-1">وصف المنتج</label>
                    <textarea
                      rows={2}
                      value={newProductDesc}
                      onChange={e => setNewProductDesc(e.target.value)}
                      placeholder="وصف المنتج..."
                      className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-700 text-xs text-slate-100 focus:outline-none focus:border-sky-500"
                    />
                  </div>

                  <button
                    type="submit"
                    className="w-full py-2.5 rounded-xl bg-sky-500 hover:bg-sky-400 text-white font-bold text-xs transition shadow-lg shadow-sky-500/20"
                  >
                    إضافة المنتج
                  </button>
                </form>
              </div>

              {/* Existing Products List */}
              <div className="lg:col-span-2 bg-slate-800/80 border border-slate-700/80 p-6 rounded-3xl space-y-4">
                <h3 className="font-bold text-sm text-slate-100 flex items-center gap-2">
                  <Package className="w-4 h-4 text-sky-400" />
                  قائمة منتجات المتجر
                </h3>

                <div className="space-y-3 max-h-[420px] overflow-y-auto pr-1">
                  {products.map(p => (
                    <div key={p.id} className="p-3.5 rounded-2xl bg-slate-900/70 border border-slate-700/50 flex items-center justify-between gap-4">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-slate-800 flex items-center justify-center text-lg">
                          📦
                        </div>
                        <div>
                          <div className="font-bold text-xs text-slate-100">{p.name}</div>
                          <div className="text-[10px] text-slate-400">
                            السعر: {p.price} ج.م | المخزون: {p.stock} قطعة
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => setProducts(products.filter(x => x.id !== p.id))}
                          className="px-2.5 py-1 rounded-lg bg-red-500/10 text-red-400 hover:bg-red-500/20 text-[11px] font-bold transition"
                        >
                          حذف
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* VIEW 4: ADMIN CONTROL PANEL */}
        {activeTab === 'admin' && (
          <div className="space-y-8">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-2xl font-black text-slate-100 flex items-center gap-2">
                  <Shield className="w-6 h-6 text-amber-400" />
                  لوحة إدارة منصة رفيق
                </h2>
                <p className="text-xs text-slate-400">اعتماد طلبات التوكيل التجاري ومتابعة أداء النظام</p>
              </div>
            </div>

            {/* Pending Applications List */}
            <div className="bg-slate-800/80 border border-slate-700/80 p-6 rounded-3xl space-y-4">
              <h3 className="font-bold text-sm text-slate-100 flex items-center justify-between">
                <span>طلبات التوكيل التجاري قيد المراجعة</span>
                <span className="text-xs px-2.5 py-1 rounded-full bg-amber-500/10 text-amber-400 font-bold">
                  {applications.length} طلبات
                </span>
              </h3>

              {applications.length === 0 ? (
                <div className="text-center py-8 text-xs text-slate-400">
                  لا توجد طلبات توكيل معلقة حالياً
                </div>
              ) : (
                <div className="space-y-3">
                  {applications.map(app => (
                    <div key={app.id} className="p-4 rounded-2xl bg-slate-900/80 border border-slate-700/60 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                      <div className="space-y-1">
                        <div className="font-bold text-sm text-slate-100">{app.store_name}</div>
                        <div className="text-xs text-slate-400">
                          المقدم: {app.username} | النوع: {app.business_type} | المساحات: {app.requested_slots}
                        </div>
                        <div className="text-[10px] text-slate-500">
                          هاتف: {app.contact_phone} | البريد: {app.contact_email}
                        </div>
                      </div>

                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => handleApproveApp(app.id)}
                          className="px-4 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-white font-bold text-xs transition flex items-center gap-1 shadow-md shadow-emerald-500/20"
                        >
                          <Check className="w-3.5 h-3.5" />
                          اعتماد التوكيل
                        </button>
                        <button
                          onClick={() => handleRejectApp(app.id)}
                          className="px-3 py-2 rounded-xl bg-red-500/10 text-red-400 hover:bg-red-500/20 text-xs font-bold transition flex items-center gap-1"
                        >
                          <X className="w-3.5 h-3.5" />
                          رفض
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </main>

      {/* Product Detail Modal */}
      {selectedProduct && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl max-w-lg w-full p-6 space-y-5 relative shadow-2xl">
            <button
              onClick={() => setSelectedProduct(null)}
              className="absolute top-4 left-4 p-2 rounded-full bg-slate-800 text-slate-400 hover:text-white"
            >
              <X className="w-4 h-4" />
            </button>

            <div className="text-center space-y-2">
              <div className="w-20 h-20 mx-auto rounded-2xl bg-slate-800 flex items-center justify-center text-4xl">
                📦
              </div>
              <h3 className="text-lg font-bold text-slate-100">{selectedProduct.name}</h3>
              <p className="text-xs text-slate-400">{selectedProduct.description}</p>
              <div className="text-xl font-black text-sky-400">{selectedProduct.price.toLocaleString()} ج.م</div>
            </div>

            {orderSuccess ? (
              <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-center text-xs text-emerald-400 font-bold">
                تم تسجيل طلبك بنجاح! يتواصل معك المتجر قريباً لتأكيد الشحن.
              </div>
            ) : (
              <form onSubmit={handlePlaceOrder} className="space-y-3 pt-2">
                <div>
                  <label className="block text-[11px] font-semibold text-slate-300 mb-1">الاسم بالكامل *</label>
                  <input
                    type="text"
                    required
                    value={orderBuyerName}
                    onChange={e => setOrderBuyerName(e.target.value)}
                    placeholder="أدخل اسمك الكريم"
                    className="w-full px-3 py-2 rounded-xl bg-slate-800 border border-slate-700 text-xs text-slate-100 focus:outline-none focus:border-sky-500"
                  />
                </div>

                <div>
                  <label className="block text-[11px] font-semibold text-slate-300 mb-1">رقم الهاتف *</label>
                  <input
                    type="tel"
                    required
                    value={orderBuyerPhone}
                    onChange={e => setOrderBuyerPhone(e.target.value)}
                    placeholder="01xxxxxxxx"
                    className="w-full px-3 py-2 rounded-xl bg-slate-800 border border-slate-700 text-xs text-slate-100 focus:outline-none focus:border-sky-500"
                  />
                </div>

                <button
                  type="submit"
                  className="w-full py-3 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 text-white font-bold text-xs shadow-lg shadow-sky-500/20 hover:opacity-95 transition"
                >
                  تأكيد الشراء الآن
                </button>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Login Modal */}
      {showAuthModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl max-w-sm w-full p-6 space-y-4 relative shadow-2xl">
            <button
              onClick={() => setShowAuthModal(false)}
              className="absolute top-4 left-4 p-2 rounded-full bg-slate-800 text-slate-400 hover:text-white"
            >
              <X className="w-4 h-4" />
            </button>

            <div className="text-center">
              <h3 className="text-lg font-bold text-slate-100">تسجيل الدخول</h3>
              <p className="text-xs text-slate-400 mt-0.5">ادخل لحساب التاجر أو الأدمن</p>
            </div>

            <form onSubmit={handleLogin} className="space-y-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-300 mb-1">اسم المستخدم</label>
                <input
                  type="text"
                  required
                  value={username}
                  onChange={e => setUsername(e.target.value)}
                  placeholder="ادخل admin أو اسم المستخدم"
                  className="w-full px-3 py-2 rounded-xl bg-slate-800 border border-slate-700 text-xs text-slate-100 focus:outline-none focus:border-sky-500"
                />
              </div>

              <div>
                <label className="block text-[11px] font-semibold text-slate-300 mb-1">كلمة المرور</label>
                <input
                  type="password"
                  required
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full px-3 py-2 rounded-xl bg-slate-800 border border-slate-700 text-xs text-slate-100 focus:outline-none focus:border-sky-500"
                />
              </div>

              <button
                type="submit"
                className="w-full py-2.5 rounded-xl bg-sky-500 text-white font-bold text-xs hover:bg-sky-400 transition"
              >
                دخول
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Footer */}
      <footer className="border-t border-slate-800 bg-slate-950 py-6 text-center text-slate-500 text-xs">
        <p>🏪 رفيق | منصة التجارة الذكية © 2026 — Wolf Digital Kernel</p>
      </footer>
    </div>
  );
}
