import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { Home, Search, Heart, User, LogOut, Menu, X, Award, MapPin, Shield, Moon, Sun } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import { NotificationCenter } from './NotificationCenter';
import { cn } from '../lib/utils';
import { motion, AnimatePresence } from 'motion/react';

export function Layout({ children }: { children: React.ReactNode }) {
  const { profile, logout, user } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const [isMenuOpen, setIsMenuOpen] = React.useState(false);
  const navigate = useNavigate();

  const navItems = [
    { to: '/', icon: Home, label: 'ড্যাশবোর্ড' },
    { to: '/requests', icon: Heart, label: 'অনুরোধ' },
    { to: '/search', icon: Search, label: 'ডোনার খুঁজুন' },
    { to: '/leaderboard', icon: Award, label: 'সেরা ডোনার' },
    { to: '/directory', icon: MapPin, label: 'ডিরেক্টরি' },
  ];

  const isAdmin = profile?.role === 'admin' || user?.email === 'aiyubuddin08@gmail.com';

  if (isAdmin) {
    navItems.push({ to: '/admin-panel', icon: Shield, label: 'অ্যাডমিন প্যানেল' });
  }

  navItems.push({ to: '/profile', icon: User, label: 'প্রোফাইল' });

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  return (
    <div className="min-h-screen flex flex-col transition-colors duration-300">
      {/* Header */}
      <header className="bg-white dark:bg-gray-900 border-b dark:border-gray-800 sticky top-0 z-50 shadow-sm transition-colors duration-300">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-20 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-red-600 rounded-2xl flex items-center justify-center blood-pulse shadow-lg shadow-red-200">
              <Heart className="w-6 h-6 text-white fill-current" />
            </div>
            <span className="text-2xl font-black text-gray-900 dark:text-white tracking-tight">রক্তসেতু</span>
          </div>

          {/* Desktop Nav */}
          <nav className="hidden lg:flex items-center gap-8">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  cn(
                    "flex items-center gap-2 text-base font-bold transition-all hover:text-red-600 dark:hover:text-red-500",
                    isActive ? "text-red-600 dark:text-red-500" : "text-gray-500 dark:text-gray-400"
                  )
                }
              >
                <item.icon className="w-5 h-5" />
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="flex items-center gap-2 sm:gap-4">
            {/* Theme Toggle */}
            <button
               onClick={toggleTheme}
               className="p-2.5 text-gray-400 hover:text-red-600 dark:hover:text-red-500 transition-colors"
               title={theme === 'light' ? 'ডার্ক মোড' : 'লাইট মোড'}
            >
               {theme === 'light' ? <Moon className="w-6 h-6" /> : <Sun className="w-6 h-6" />}
            </button>

            {/* Notification Center */}
            <NotificationCenter />

            {user && (
              <button
                onClick={handleLogout}
                className="hidden md:flex items-center gap-2 text-base font-bold text-gray-400 hover:text-red-600 dark:hover:text-red-500 transition-colors"
              >
                <LogOut className="w-5 h-5" />
                লগআউট
              </button>
            )}

            {/* Mobile Menu Button */}
            <button
              className="lg:hidden p-2 text-gray-500 dark:text-gray-400 hover:text-red-600 transition-colors"
              onClick={() => setIsMenuOpen(!isMenuOpen)}
            >
              {isMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </header>

      {/* Mobile Menu */}
      <AnimatePresence>
        {isMenuOpen && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="lg:hidden bg-white dark:bg-gray-900 border-b dark:border-gray-800 overflow-hidden"
          >
            <div className="px-4 py-4 space-y-2">
              {navItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  onClick={() => setIsMenuOpen(false)}
                  className={({ isActive }) =>
                    cn(
                      "flex items-center gap-3 px-4 py-3 rounded-2xl text-base font-bold transition-colors",
                      isActive ? "bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-500" : "text-gray-500 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-800"
                    )
                  }
                >
                  <item.icon className="w-5 h-5" />
                  {item.label}
                </NavLink>
              ))}
              {user && (
                <button
                  onClick={handleLogout}
                  className="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-base font-bold text-gray-500 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors text-left"
                >
                  <LogOut className="w-5 h-5" />
                  Logout
                </button>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Main Content */}
      <main className="flex-1 max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-8 animate-fade-in relative z-0">
        {children}
      </main>

      {/* Footer */}
      <footer className="bg-white dark:bg-gray-900 border-t dark:border-gray-800 py-12 transition-colors duration-300">
        <div className="max-w-7xl mx-auto px-4 text-center space-y-4">
          <div className="bg-orange-50 dark:bg-orange-900/10 border border-orange-100 dark:border-orange-900/20 p-6 rounded-[32px] max-w-2xl mx-auto mb-8">
            <h4 className="text-orange-900 dark:text-orange-400 font-black text-lg">⚠️ আর্থিক লেনদেন থেকে সাবধান!</h4>
            <p className="text-orange-700 dark:text-orange-500 text-sm font-bold mt-1">রক্তদান একটি মহৎ কাজ। দয়া করে এর বিনিময়ে কোনো অর্থ লেনদেন করবেন না। কেউ টাকা চাইলে সাথে সাথে কর্তৃপক্ষের সাহায্য নিন।</p>
          </div>
          <p className="text-gray-900 dark:text-white font-black text-lg uppercase tracking-widest flex items-center justify-center gap-2">
            ডেভলপার: <span className="text-red-600 dark:text-red-500">আইয়ুব উদ্দীন ফেরদৌস</span>
          </p>
          <p className="text-gray-500 dark:text-gray-400 font-bold flex items-center justify-center gap-2">
            যোগাযোগ: <a href="tel:01897971573" className="hover:text-red-600 dark:hover:text-red-500 transition-colors">০১৮৯৭৯৭১৫৭৩</a>
          </p>
          <div className="pt-8 border-t border-gray-100 dark:border-gray-800 text-gray-400 dark:text-gray-600 text-sm font-medium">
            © ২০২৬ রক্তসেতু। একটি অলাভজনক উদ্যোগ।
          </div>
        </div>
      </footer>
    </div>
  );
}
