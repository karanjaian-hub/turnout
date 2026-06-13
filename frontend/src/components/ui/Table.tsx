import React, { useState } from 'react';
import { clsx } from 'clsx';
import { ChevronUp, ChevronDown } from 'lucide-react';

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

  const handleSort = (key: string) => {
    if (sortKey === key) {
      setSortDir(d => d === 'asc' ? 'desc' : 'asc');
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
  };

  return (
    // Horizontal scroll on narrow screens prevents layout breakage
    <div className="overflow-x-auto rounded-card">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-slate-50 border-b border-slate-200">
            {columns.map(col => (
              <th
                key={col.key}
                className={clsx(
                  'px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide',
                  col.sortable && 'cursor-pointer select-none hover:text-navy'
                )}
                onClick={() => col.sortable && handleSort(col.key)}
              >
                <span className="inline-flex items-center gap-1">
                  {col.header}
                  {col.sortable && (
                    <span className="text-slate-300">
                      {sortKey === col.key
                        ? sortDir === 'asc' ? <ChevronUp size={14} /> : <ChevronDown size={14} />
                        : <ChevronUp size={14} />
                      }
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
              <td colSpan={columns.length} className="px-4 py-12 text-center text-slate-400">
                Loading...
              </td>
            </tr>
          ) : data.length === 0 ? (
            <tr>
              <td colSpan={columns.length}>
                {emptyState ?? (
                  <p className="px-4 py-12 text-center text-slate-400">No results found.</p>
                )}
              </td>
            </tr>
          ) : (
            data.map((row, i) => (
              <tr
                key={keyExtractor(row)}
                className={clsx(
                  'border-b border-slate-100 transition-colors',
                  // Stripe every other row — improves scannability on dense tables
                  i % 2 === 0 ? 'bg-white' : 'bg-slate-50/50',
                  'hover:bg-blue-50/40'
                )}
              >
                {columns.map(col => (
                  <td key={col.key} className="px-4 py-3 text-slate-700">
                    {col.render(row)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

export default Table;
