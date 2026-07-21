import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import RsvpPage      from './pages/RsvpPage';
import ConfirmedPage from './pages/ConfirmedPage';
import DeclinedPage  from './pages/DeclinedPage';
import InvalidPage   from './pages/InvalidPage';
import ErrorPage     from './pages/ErrorPage';

const App: React.FC = () => (
  <BrowserRouter>
    <Toaster position="top-center" />
    <Routes>
      <Route path="/rsvp"           element={<RsvpPage />} />
      <Route path="/rsvp/confirmed" element={<ConfirmedPage />} />
      <Route path="/rsvp/declined"  element={<DeclinedPage />} />
      <Route path="/rsvp/invalid"   element={<InvalidPage />} />
      <Route path="/rsvp/error"     element={<ErrorPage />} />
      <Route path="*"               element={<Navigate to="/rsvp/invalid" replace />} />
    </Routes>
  </BrowserRouter>
);

export default App;
