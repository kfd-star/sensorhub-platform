import React from 'react';

type WindDirectionIndicatorProps = {
  direction: number;
  speed: number;
};

const getDirectionText = (degree: number) => {
  const directions = [
    { min: 0, max: 22.5, text: '北' },
    { min: 22.5, max: 67.5, text: '东北' },
    { min: 67.5, max: 112.5, text: '东' },
    { min: 112.5, max: 157.5, text: '东南' },
    { min: 157.5, max: 202.5, text: '南' },
    { min: 202.5, max: 247.5, text: '西南' },
    { min: 247.5, max: 292.5, text: '西' },
    { min: 292.5, max: 337.5, text: '西北' },
    { min: 337.5, max: 360, text: '北' },
  ];
  return directions.find((item) => degree >= item.min && degree < item.max)?.text || '北';
};

const getSpeedColor = (speed: number) => {
  if (speed < 3) {
    return '#22c55e';
  }
  if (speed < 8) {
    return '#f59e0b';
  }
  if (speed < 15) {
    return '#f97316';
  }
  return '#ef4444';
};

const WindDirectionIndicator: React.FC<WindDirectionIndicatorProps> = ({ direction, speed }) => {
  const color = getSpeedColor(speed);

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 10,
        marginTop: 16,
      }}
    >
      <div
        style={{
          position: 'relative',
          width: 100,
          height: 100,
          borderRadius: '50%',
          background:
            'conic-gradient(from 0deg, rgba(22,119,255,0.08), rgba(124,58,237,0.18), rgba(6,182,212,0.08), rgba(22,119,255,0.08))',
          border: '1px solid rgba(0,0,0,0.06)',
        }}
      >
        {[
          { text: 'N', style: { top: 4, left: '50%', transform: 'translateX(-50%)' } },
          { text: 'E', style: { right: 4, top: '50%', transform: 'translateY(-50%)' } },
          { text: 'S', style: { bottom: 4, left: '50%', transform: 'translateX(-50%)' } },
          { text: 'W', style: { left: 4, top: '50%', transform: 'translateY(-50%)' } },
        ].map((item) => (
          <div
            key={item.text}
            style={{
              position: 'absolute',
              fontSize: 11,
              fontWeight: 700,
              color: '#1677ff',
              ...item.style,
            }}
          >
            {item.text}
          </div>
        ))}
        <div
          style={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            width: 3,
            height: 36,
            background: color,
            borderRadius: '999px 999px 0 0',
            transformOrigin: '50% 100%',
            transform: `translate(-50%, -100%) rotate(${direction}deg)`,
            boxShadow: `0 0 12px ${color}66`,
          }}
        >
          <div
            style={{
              position: 'absolute',
              top: -8,
              left: '50%',
              transform: 'translateX(-50%)',
              width: 0,
              height: 0,
              borderLeft: '6px solid transparent',
              borderRight: '6px solid transparent',
              borderBottom: `12px solid ${color}`,
            }}
          />
        </div>
        <div
          style={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            width: 10,
            height: 10,
            transform: 'translate(-50%, -50%)',
            borderRadius: '50%',
            background: color,
            border: '2px solid #fff',
          }}
        />
      </div>
      <div style={{ textAlign: 'center' }}>
        <div style={{ fontSize: 15, fontWeight: 700, color }}>{`${getDirectionText(direction)} ${direction}deg`}</div>
        <div style={{ fontSize: 12, color: 'rgba(0,0,0,0.55)' }}>{`风速 ${speed} m/s`}</div>
      </div>
    </div>
  );
};

export default WindDirectionIndicator;
