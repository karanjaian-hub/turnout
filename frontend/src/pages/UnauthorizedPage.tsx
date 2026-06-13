import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldOff } from 'lucide-react';
import Button from '../components/ui/Button';

const UnauthorizedPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50">
      <div className="text-center max-w-sm">
        <div className="w-16 h-16 rounded-full bg-red-100 flex items-center justify-center mx-auto mb-4">
          <ShieldOff size={32} className="text-danger" />
        </div>
        <h1 className="text-xl font-bold text-navy mb-2">Access Denied</h1>
        <p className="text-slate-500 text-sm mb-6">
          This panel is restricted to SUPER_ADMIN and ADMIN accounts only.
        </p>
        <Button onClick={() => navigate('/login')}>Back to Login</Button>
      </div>
    </div>
  );
};

export default UnauthorizedPage;
