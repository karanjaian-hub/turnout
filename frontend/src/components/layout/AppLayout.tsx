import React, { useState } from 'react';
import Sidebar from './Sidebar';
import Header from './Header';


interface AppLayoutProps {
  children: React.ReactNode;
}

const AppLayout: React.FC<AppLayoutProps> = ({ children }) => {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-app)' }}>
      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          onClick={() => setSidebarOpen(false)}
          style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 35, display: 'none' }}
          className="lg:hidden-overlay"
        />
      )}

      {/* Sidebar — always visible on desktop, toggled on mobile */}
      <div style={{
        position: 'fixed', left: 0, top: 0, height: '100%', zIndex: 40,
        transform: sidebarOpen ? 'translateX(0)' : 'translateX(-100%)',
        transition: 'transform 250ms ease',
      }} className="mobile-sidebar">
        <Sidebar onClose={() => setSidebarOpen(false)} />
      </div>

      {/* Desktop sidebar — always visible */}
      <div className="desktop-sidebar">
        <Sidebar />
      </div>

      {/* Header */}
      <Header onMenuClick={() => setSidebarOpen(v => !v)} />

      {/* Main content */}
      <main className="main-content" style={{ padding: 24 }}>
        {children}
      </main>

      <style>{`
        .mobile-sidebar  { display: block; }
        .desktop-sidebar { display: none; }
        .main-content    { margin-top: 64px; margin-left: 0; }

        @media (min-width: 1024px) {
          .mobile-sidebar  { display: none !important; }
          .desktop-sidebar { display: block; }
          .main-content    { margin-left: 256px; }
        }
        @media (max-width: 1023px) {
          .mobile-sidebar {
            transform: ${sidebarOpen ? 'translateX(0)' : 'translateX(-100%)'} !important;
          }
        }
      `}</style>
    </div>
  );
};

export default AppLayout;
