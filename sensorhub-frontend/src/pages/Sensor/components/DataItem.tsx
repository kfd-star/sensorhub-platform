import React from 'react';

type DataItemProps = {
  label: string;
  value: React.ReactNode;
  unit?: string;
  color?: string;
};

const DataItem: React.FC<DataItemProps> = ({ label, value, unit, color = '#1677ff' }) => (
  <div
    style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      gap: 12,
      padding: '10px 0',
      borderBottom: '1px solid rgba(5, 5, 5, 0.06)',
    }}
  >
    <span
      style={{
        fontSize: 13,
        color: 'rgba(0,0,0,0.55)',
        fontWeight: 500,
      }}
    >
      {label}
    </span>
    <span
      style={{
        fontSize: 14,
        fontWeight: 600,
        color: value === '--' ? 'rgba(0,0,0,0.35)' : color,
        fontFamily: 'Consolas, Monaco, monospace',
      }}
    >
      {value}
      {unit && value !== '--' ? ` ${unit}` : ''}
    </span>
  </div>
);

export default DataItem;
