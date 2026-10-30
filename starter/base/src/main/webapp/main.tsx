import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Shell, features } from './selected';
import './style.css';

function App() {
  const [path, setPath] = useState(window.location.pathname);
  useEffect(() => {
    const update = () => setPath(window.location.pathname);
    window.addEventListener('popstate', update);
    return () => window.removeEventListener('popstate', update);
  }, []);
  const feature = path === '/' ? features[0] : features.find(item => item.path === path);
  const Page = feature?.component;
  return <main className="container" onClick={event => {
    const link = (event.target as HTMLElement).closest('a');
    if (!link || link.target || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
    const url = new URL(link.href);
    if (url.origin === location.origin && features.some(item => item.path === url.pathname)) {
      event.preventDefault(); history.pushState(null, '', url.pathname); setPath(url.pathname);
    }
  }}>
    <nav aria-label="업무 메뉴">{features.map(item => <a key={item.path} href={item.path}>{item.label}</a>)}</nav>
    {Page ? <Page/> : <section><h1>업무 서비스</h1><p>서비스가 준비되었습니다.</p></section>}
  </main>;
}

createRoot(document.getElementById('root')!).render(<React.StrictMode><Shell><App/></Shell></React.StrictMode>);
