import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
});

// ─── Global error interceptor ─────────────────────────────────────────────────
api.interceptors.response.use(
  response => response,
  error => {
    const message = error.response?.data?.message || 'Something went wrong';
    return Promise.reject(new Error(message));
  }
);

export interface RsvpDetails {
  valid: boolean;
  guestId: string;
  guestName: string;
  guestEmail?: string;
  eventId: string;
  eventTitle: string;
  eventDate: string;
  eventLocation: string;
  eventDescription?: string;
  organizerName?: string;
  maxCapacity?: number;
  currentRsvpCount?: number;
  alreadyResponded: boolean;
  currentRsvpStatus: string | null;
}

export const validateToken = async (token: string): Promise<RsvpDetails> => {
  const { data } = await api.get(`/api/rsvp/validate?token=${token}`);
  if (data.status === 0) {
    throw new Error(data.message);
  }
  return data.data as RsvpDetails;
};

export const submitResponse = async (
  token: string,
  response: 'CONFIRMED' | 'DECLINED'
): Promise<void> => {
  const { data } = await api.post('/api/rsvp/submit', { token, rsvpStatus: response });
  if (data.status === 0) {
    throw new Error(data.message);
  }
};
