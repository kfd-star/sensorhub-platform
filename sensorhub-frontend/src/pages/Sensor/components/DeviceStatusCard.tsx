import React from 'react';

type DeviceStatusCardProps = {
  deviceName: string;
  icon?: string;
  status: 'online' | 'offline';
  color?: string;
  lastUpdate?: string;
  children: React.ReactNode;
};

const DeviceStatusCard: React.FC<DeviceStatusCardProps> = ({
  deviceName,
  icon = '[DEV]',
  status,
  color = '#1677ff',
  lastUpdate,
  children,
}) => {
  const isActive = status === 'online';

  return (
    <div
      style={{
        background: `linear-gradient(180deg, ${color}12 0%, rgba(255,255,255,0.98) 48%)`,
        border: `1px solid ${color}22`,
        borderRadius: 20,
        padding: 20,
        position: 'relative',
        overflow: 'hidden',
        minHeight: 280,
        boxShadow: isActive ? `0 14px 40px ${color}18` : '0 8px 24px rgba(15, 23, 42, 0.06)',
        transition: 'all 0.25s ease',
      }}
    >
      <div
        style={{
          position: 'absolute',
          inset: '0 0 auto 0',
          height: 4,
          background: isActive
            ? `linear-gradient(90deg, ${color} 0%, ${color}cc 100%)`
            : 'linear-gradient(90deg, rgba(107,114,128,0.55) 0%, rgba(107,114,128,0.3) 100%)',
        }}
      />
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span style={{ fontSize: 20, fontWeight: 700, color }}>{icon}</span>
          <div>
            <div style={{ fontSize: 17, fontWeight: 600, color: '#1f2937' }}>{deviceName}</div>
            <div
              style={{
                fontSize: 12,
                color: isActive ? '#16a34a' : '#ef4444',
                fontWeight: 600,
                marginTop: 4,
              }}
            >
              {isActive ? '在线' : '离线'}
            </div>
          </div>
        </div>
        <div
          style={{
            width: 10,
            height: 10,
            borderRadius: '50%',
            background: isActive ? '#16a34a' : '#ef4444',
            boxShadow: isActive ? '0 0 18px rgba(22,163,74,0.55)' : 'none',
          }}
        />
      </div>
      {lastUpdate ? (
        <div
          style={{
            fontSize: 12,
            color: 'rgba(0,0,0,0.45)',
            marginBottom: 12,
            padding: '8px 12px',
            background: 'rgba(255,255,255,0.7)',
            borderRadius: 10,
          }}
        >
          最后更新: {new Date(lastUpdate).toLocaleString('zh-CN')}
        </div>
      ) : null}
      <div>{children}</div>
    </div>
  );
};

export default DeviceStatusCard;
