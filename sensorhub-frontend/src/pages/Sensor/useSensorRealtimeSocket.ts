import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

export type SensorRealtimeMessage = {
  type?: string;
  device?: string;
  device_name?: string;
  timestamp?: string;
  data?: Record<string, any>;
};

type SocketStats = {
  totalMessages: number;
  activeDevices: number;
  messageRate: string;
  uptime: string;
};

const DEFAULT_STATS: SocketStats = {
  totalMessages: 0,
  activeDevices: 0,
  messageRate: '0.0',
  uptime: '0:00',
};

export const useSensorRealtimeSocket = (url?: string) => {
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState('');
  const [messages, setMessages] = useState<SensorRealtimeMessage[]>([]);
  const [stats, setStats] = useState<SocketStats>(DEFAULT_STATS);

  const mountedRef = useRef(true);
  const socketRef = useRef<WebSocket | null>(null);
  const reconnectTimerRef = useRef<number>();
  const manualDisconnectRef = useRef(false);
  const startedAtRef = useRef(Date.now());
  const totalMessagesRef = useRef(0);
  const reconnectAttemptsRef = useRef(0);

  const clearReconnectTimer = useCallback(() => {
    if (reconnectTimerRef.current !== undefined) {
      window.clearTimeout(reconnectTimerRef.current);
      reconnectTimerRef.current = undefined;
    }
  }, []);

  const closeSocket = useCallback((code?: number, reason?: string) => {
    if (!socketRef.current) {
      return;
    }
    socketRef.current.onopen = null;
    socketRef.current.onmessage = null;
    socketRef.current.onerror = null;
    socketRef.current.onclose = null;
    socketRef.current.close(code, reason);
    socketRef.current = null;
  }, []);

  const clearMessages = useCallback(() => {
    if (!mountedRef.current) {
      return;
    }
    totalMessagesRef.current = 0;
    startedAtRef.current = Date.now();
    setMessages([]);
    setStats(DEFAULT_STATS);
  }, []);

  const clearError = useCallback(() => {
    if (mountedRef.current) {
      setError('');
    }
  }, []);

  const disconnect = useCallback(() => {
    manualDisconnectRef.current = true;
    clearReconnectTimer();
    closeSocket(1000, 'manual disconnect');
    if (mountedRef.current) {
      setConnected(false);
    }
  }, [clearReconnectTimer, closeSocket]);

  const connect = useCallback(() => {
    if (!mountedRef.current) {
      return;
    }
    if (!url) {
      setError('WebSocket URL is not configured.');
      return;
    }

    clearReconnectTimer();
    manualDisconnectRef.current = false;

    if (socketRef.current) {
      const readyState = socketRef.current.readyState;
      if (readyState === WebSocket.OPEN || readyState === WebSocket.CONNECTING) {
        return;
      }
      closeSocket();
    }

    const socket = new WebSocket(url);
    socketRef.current = socket;

    socket.onopen = () => {
      if (!mountedRef.current) {
        return;
      }
      reconnectAttemptsRef.current = 0;
      startedAtRef.current = Date.now();
      setConnected(true);
      setError('');
    };

    socket.onmessage = (event) => {
      if (!mountedRef.current) {
        return;
      }
      try {
        const parsed = JSON.parse(event.data) as SensorRealtimeMessage;
        if (parsed.type !== 'telemetry') {
          return;
        }
        totalMessagesRef.current += 1;
        setMessages((previous) => [parsed, ...previous].slice(0, 80));
      } catch (parseError) {
        setError('Failed to parse realtime message.');
      }
    };

    socket.onerror = () => {
      if (mountedRef.current) {
        setError('WebSocket connection error.');
      }
    };

    socket.onclose = (event) => {
      if (!mountedRef.current) {
        return;
      }

      setConnected(false);
      socketRef.current = null;

      if (manualDisconnectRef.current) {
        return;
      }

      if (reconnectAttemptsRef.current >= 5) {
        setError('Realtime reconnect failed too many times.');
        return;
      }

      const delay = Math.min(1000 * 2 ** reconnectAttemptsRef.current, 15000);
      reconnectAttemptsRef.current += 1;
      setError(`Realtime connection closed. Retrying in ${Math.round(delay / 1000)}s.`);

      reconnectTimerRef.current = window.setTimeout(() => {
        if (mountedRef.current && event.code !== 1000) {
          connect();
        }
      }, delay);
    };
  }, [clearReconnectTimer, closeSocket, url]);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      manualDisconnectRef.current = true;
      clearReconnectTimer();
      closeSocket();
    };
  }, [clearReconnectTimer, closeSocket]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      if (!mountedRef.current || !connected) {
        return;
      }
      const uptimeSeconds = Math.max(Math.floor((Date.now() - startedAtRef.current) / 1000), 1);
      const activeDevices = new Set(messages.map((item) => item.device).filter(Boolean)).size;
      setStats({
        totalMessages: totalMessagesRef.current,
        activeDevices,
        messageRate: (totalMessagesRef.current / uptimeSeconds).toFixed(1),
        uptime: `${Math.floor(uptimeSeconds / 60)}:${String(uptimeSeconds % 60).padStart(2, '0')}`,
      });
    }, 1000);

    return () => {
      window.clearInterval(timer);
    };
  }, [connected, messages]);

  const latestMessageMap = useMemo(() => {
    const result = new Map<string, SensorRealtimeMessage>();
    messages.forEach((message) => {
      if (message.device && !result.has(message.device)) {
        result.set(message.device, message);
      }
    });
    return result;
  }, [messages]);

  return {
    connected,
    error,
    messages,
    stats,
    latestMessageMap,
    connect,
    disconnect,
    clearMessages,
    clearError,
  };
};
