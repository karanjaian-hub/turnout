import { useEffect, useRef, useState, useCallback } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface AdminAlert {
  type: 'RSVP' | 'REGISTRATION' | 'PAYMENT' | 'SYSTEM';
  message: string;
  timestamp: string;
  metadata?: Record<string, string>;
}

interface UseWebSocketResult {
  connected: boolean;
  latestAlert: AdminAlert | null;
  alerts: AdminAlert[];
}

const WS_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';
// Keep the last 50 alerts in memory — older ones scroll off the feed
const MAX_ALERTS = 50;

const useWebSocket = (): UseWebSocketResult => {
  const [connected, setConnected]     = useState(false);
  const [alerts, setAlerts]           = useState<AdminAlert[]>([]);
  const [latestAlert, setLatestAlert] = useState<AdminAlert | null>(null);
  // useRef so the STOMP client instance survives re-renders without triggering them
  const clientRef = useRef<Client | null>(null);

  const handleMessage = useCallback((message: IMessage) => {
    try {
      const alert: AdminAlert = JSON.parse(message.body);
      setLatestAlert(alert);
      setAlerts(prev => [alert, ...prev].slice(0, MAX_ALERTS));
    } catch {
      // Malformed message — log and move on, never crash the UI
      console.warn('Received malformed WebSocket message:', message.body);
    }
  }, []);

  useEffect(() => {
    const client = new Client({
      // SockJS falls back to HTTP long-polling if WebSocket is unavailable —
      // useful during development when the backend may not be running
      webSocketFactory: () => new SockJS(`${WS_URL}/ws`),
      reconnectDelay: 5000, // retry every 5s on disconnect

      onConnect: () => {
        setConnected(true);
        client.subscribe('/topic/admin-alerts', handleMessage);
      },

      onDisconnect: () => setConnected(false),

      // Suppress STOMP debug noise in production
      debug: process.env.NODE_ENV === 'development' ? console.log : () => {},
    });

    client.activate();
    clientRef.current = client;

    return () => {
      // Clean up on unmount — prevents memory leaks and duplicate subscriptions
      client.deactivate();
    };
  }, [handleMessage]);

  return { connected, latestAlert, alerts };
};

export default useWebSocket;
