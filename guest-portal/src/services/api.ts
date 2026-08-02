import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
});

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
  const { data } = await api.get<RsvpDetails>(`/api/rsvp/validate?token=${token}`);
  return data;
};

export const submitResponse = async (
  token: string,
  response: 'CONFIRMED' | 'DECLINED'
): Promise<void> => {
  await api.post('/api/rsvp/submit', { token, rsvpStatus: response });
};
