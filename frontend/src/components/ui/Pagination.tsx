import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface PaginationProps {
  page: number;         // 0-indexed (Spring default)
  totalPages: number;
  onPageChange: (page: number) => void;
}

const Pagination: React.FC<PaginationProps> = ({ page, totalPages, onPageChange }) => {
  if (totalPages <= 1) return null;

  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '12px 16px', borderTop: '1px solid var(--border)',
    }}>
      <p style={{ fontSize: 13, color: 'var(--text-secondary)', fontFamily: 'Inter' }}>
        Page <strong>{page + 1}</strong> of <strong>{totalPages}</strong>
      </p>
      <div style={{ display: 'flex', gap: 6 }}>
        <button
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
          style={{
            display: 'flex', alignItems: 'center', gap: 4,
            padding: '6px 12px', borderRadius: 8, border: '1px solid var(--border)',
            background: 'var(--bg-card)', color: page === 0 ? 'var(--text-muted)' : 'var(--text-primary)',
            fontSize: 13, fontFamily: 'Inter', cursor: page === 0 ? 'not-allowed' : 'pointer',
            transition: 'all 150ms ease',
          }}
          onMouseEnter={e => { if (page > 0) e.currentTarget.style.background = 'var(--bg-app)'; }}
          onMouseLeave={e => { e.currentTarget.style.background = 'var(--bg-card)'; }}
        >
          <ChevronLeft size={14}/> Prev
        </button>
        <button
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
          style={{
            display: 'flex', alignItems: 'center', gap: 4,
            padding: '6px 12px', borderRadius: 8, border: '1px solid var(--border)',
            background: 'var(--bg-card)', color: page >= totalPages - 1 ? 'var(--text-muted)' : 'var(--text-primary)',
            fontSize: 13, fontFamily: 'Inter', cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer',
            transition: 'all 150ms ease',
          }}
          onMouseEnter={e => { if (page < totalPages - 1) e.currentTarget.style.background = 'var(--bg-app)'; }}
          onMouseLeave={e => { e.currentTarget.style.background = 'var(--bg-card)'; }}
        >
          Next <ChevronRight size={14}/>
        </button>
      </div>
    </div>
  );
};

export default Pagination;
