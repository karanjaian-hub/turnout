import React from 'react';
import Sidebar from './Sidebar';
import Header from './Header';

// Every protected page wraps its content in AppLayout.
// The sidebar is 256px (w-64), header is 64px (h-16) — all other spacing derives from these.
interface AppLayoutProps {
  children: React.ReactNode;
}

const AppLayout: React.FC<AppLayoutProps> = ({ children }) => (
  <div className="min-h-screen bg-slate-50">
    <Sidebar />
    <Header />
    <main className="ml-64 mt-16 p-6">
      {children}
    </main>
  </div>
);

export default AppLayout;
