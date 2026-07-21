import React from 'react';

const Spinner: React.FC = () => (
  <div style={{
    width: 40, height: 40,
    border: '3px solid rgba(37,99,235,0.2)',
    borderTop: '3px solid #2563EB',
    borderRadius: '50%',
    animation: 'spin 800ms linear infinite',
    margin: '0 auto',
  }}>
    <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
  </div>
);

export default Spinner;
