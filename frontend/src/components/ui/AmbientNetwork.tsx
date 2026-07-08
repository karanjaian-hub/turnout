import React, { useEffect, useRef } from 'react';

const ACCENT     = '#2563EB';
const NODE_COUNT = 18;
const LINK_DIST  = 180;

interface Node  { x: number; y: number; vx: number; vy: number; }
interface Pulse { fi: number; ti: number; p: number; speed: number; }

const AmbientNetwork: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animId: number;
    let lastPulse  = 0;
    let nextDelay  = 4000 + Math.random() * 2000;
    let nodes: Node[] = [];
    const pulses: Pulse[] = [];

    const init = () => {
      // Use the actual rendered size — read AFTER layout
      canvas.width  = Math.round(window.innerWidth * 0.6);
      canvas.height = window.innerHeight;
      nodes = Array.from({ length: NODE_COUNT }, () => ({
        x:  Math.random() * canvas.width,
        y:  Math.random() * canvas.height,
        vx: (Math.random() - 0.5) * 0.2,
        vy: (Math.random() - 0.5) * 0.2,
      }));
    };

    // Wait one frame so the DOM has laid out and offsetWidth is real
    requestAnimationFrame(() => {
      init();
      window.addEventListener('resize', init);

      const tick = (ts: number) => {
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        // Move nodes
        nodes.forEach(n => {
          n.x += n.vx; n.y += n.vy;
          if (n.x < 0 || n.x > canvas.width)  n.vx *= -1;
          if (n.y < 0 || n.y > canvas.height) n.vy *= -1;
        });

        // Draw edges
        for (let i = 0; i < nodes.length; i++) {
          for (let j = i + 1; j < nodes.length; j++) {
            const dx   = nodes[i].x - nodes[j].x;
            const dy   = nodes[i].y - nodes[j].y;
            const dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < LINK_DIST) {
              ctx.beginPath();
              ctx.strokeStyle = `rgba(226,232,240,${0.12 * (1 - dist / LINK_DIST)})`;
              ctx.lineWidth   = 0.6;
              ctx.moveTo(nodes[i].x, nodes[i].y);
              ctx.lineTo(nodes[j].x, nodes[j].y);
              ctx.stroke();
            }
          }
        }

        // Draw nodes
        nodes.forEach(n => {
          ctx.beginPath();
          ctx.arc(n.x, n.y, 2, 0, Math.PI * 2);
          ctx.fillStyle = 'rgba(226,232,240,0.45)';
          ctx.fill();
        });

        // Spawn pulse
        if (ts - lastPulse > nextDelay) {
          const pairs: [number,number][] = [];
          for (let i = 0; i < nodes.length; i++)
            for (let j = i + 1; j < nodes.length; j++) {
              const dx = nodes[i].x - nodes[j].x;
              const dy = nodes[i].y - nodes[j].y;
              if (Math.sqrt(dx*dx + dy*dy) < LINK_DIST) pairs.push([i, j]);
            }
          if (pairs.length) {
            const [fi, ti] = pairs[Math.floor(Math.random() * pairs.length)];
            pulses.push({ fi, ti, p: 0, speed: 0.007 + Math.random() * 0.006 });
          }
          lastPulse = ts;
          nextDelay = 4000 + Math.random() * 2000;
        }

        // Animate pulses
        for (let i = pulses.length - 1; i >= 0; i--) {
          const pl   = pulses[i];
          pl.p      += pl.speed;
          const from = nodes[pl.fi];
          const to   = nodes[pl.ti];
          const px   = from.x + (to.x - from.x) * pl.p;
          const py   = from.y + (to.y - from.y) * pl.p;
          const g    = ctx.createRadialGradient(px, py, 0, px, py, 12);
          g.addColorStop(0,   ACCENT + 'BB');
          g.addColorStop(0.5, ACCENT + '33');
          g.addColorStop(1,   ACCENT + '00');
          ctx.beginPath();
          ctx.arc(px, py, 12, 0, Math.PI * 2);
          ctx.fillStyle = g;
          ctx.fill();
          if (pl.p >= 1) pulses.splice(i, 1);
        }

        animId = requestAnimationFrame(tick);
      };

      animId = requestAnimationFrame(tick);
    });

    return () => {
      cancelAnimationFrame(animId);
      window.removeEventListener('resize', init);
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', display: 'block' }}
    />
  );
};

export default AmbientNetwork;
