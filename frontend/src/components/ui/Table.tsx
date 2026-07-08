import React, { useState } from 'react';
import { ChevronUp } from "lucide-react";

export interface Column<T> {
  key: string;
  header: string;
  sortable?: boolean;
  render: (row: T) => React.ReactNode;
}

interface TableProps<T> {
  columns: Column<T>[];
  data: T[];
  keyExtractor: (row: T) => string;
  loading?: boolean;
  emptyState?: React.ReactNode;
}

function Table<T>({ columns, data, keyExtractor, loading, emptyState }: TableProps<T>) {
  const [sortKey, setSortKey] = useState<string | null>(null);
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc');
  const [hoveredCol, setHoveredCol] = useState<string | null>(null);

  const handleSort = (key: string) => {
    if (sortKey === key) setSortDir(d => d === 'asc' ? 'desc' : 'asc');
    else { setSortKey(key); setSortDir('asc'); }
  };

  return (
    <div style={{ overflowX: 'auto', borderRadius: 12 }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
        <thead>
          <tr style={{ background: 'var(--bg-app)', borderBottom: '1px solid var(--border)' }}>
            {columns.map(col => (
              <th
                key={col.key}
                onClick={() => col.sortable && handleSort(col.key)}
                onMouseEnter={() => col.sortable && setHoveredCol(col.key)}
                onMouseLeave={() => setHoveredCol(null)}
                style={{
                  padding: '12px 16px', textAlign: 'left',
                  fontSize: 11, fontWeight: 600, letterSpacing: '0.05em',
                  textTransform: 'uppercase', fontFamily: 'Inter',
                  color: sortKey === col.key ? 'var(--text-primary)' : 'var(--text-secondary)',
                  cursor: col.sortable ? 'pointer' : 'default',
                  userSelect: 'none',
                  whiteSpace: 'nowrap',
                  transition: 'color 150ms',
                }}
              >
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                  {col.header}
                  {col.sortable && (
                    <span style={{
                      opacity: sortKey === col.key ? 1 : hoveredCol === col.key ? 0.5 : 0,
                      transition: 'opacity 150ms, transform 150ms',
                      transform: sortKey === col.key && sortDir === 'desc' ? 'rotate(180deg)' : 'none',
                      display: 'inline-flex',
                    }}>
                      <ChevronUp size={12}/>
                    </span>
                  )}
                </span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr>
              <td colSpan={columns.length} style={{ padding: '48px 16px', textAlign: 'center', color: 'var(--text-muted)', fontFamily: 'Inter' }}>
                Loading...
              </td>
            </tr>
          ) : (!Array.isArray(data) || data.length === 0) ? (
            <tr>
              <td colSpan={columns.length}>
                {emptyState ?? (
                  <p style={{ padding: '48px 16px', textAlign: 'center', color: 'var(--text-muted)', fontFamily: 'Inter', fontSize: 13 }}>
                    No results found.
                  </p>
                )}
              </td>
            </tr>
          ) : (
            ( Array.isArray(data) ? data : []).map((row, i) => (
              <TableRow key={keyExtractor(row)} row={row} columns={columns} index={i} />
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

function TableRow<T>({ row, columns, index }: { row: T; columns: Column<T>[]; index: number }) {
  const [hovered, setHovered] = useState(false);
  const delay = Math.min(index, 9) * 30;

  return (
    <tr
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        borderBottom: '1px solid var(--border)',
        background: hovered ? 'var(--bg-app)' : index % 2 === 0 ? 'var(--bg-card)' : 'var(--bg-app)',
        transition: 'background 100ms ease',
        animation: `rowFadeIn 250ms ease ${delay}ms both`,
      }}
    >
      {columns.map(col => (
        <td key={col.key} style={{ padding: '12px 16px', color: 'var(--text-primary)', fontFamily: 'Inter', fontSize: 14 }}>
          {col.render(row)}
        </td>
      ))}
    </tr>
  );
}

export default Table;
