import React, { createContext, useContext, useState } from 'react';
import axios from 'axios';

type Account = { login: string; authorities: string[] };
const Context = createContext<Account | null>(null);
export const useAuth = () => {
  const account = useContext(Context);
  if (!account) throw new Error('AuthGate is required');
  return account;
};

export function AuthGate({ children }: React.PropsWithChildren) {
  const [account, setAccount] = useState<Account | null>(null);
  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const signIn = async (event: React.FormEvent) => {
    event.preventDefault(); setBusy(true); setError('');
    try {
      const loginResponse = await axios.post<{ id_token: string }>('/api/authenticate', { username: login, password });
      // The token stays in memory and is sent explicitly; no cookie or browser storage authentication.
      axios.defaults.headers.common.Authorization = `Bearer ${loginResponse.data.id_token}`;
      const response = await axios.get<Account>('/api/account');
      setAccount(response.data); setPassword('');
    } catch {
      delete axios.defaults.headers.common.Authorization; setError('로그인 정보를 확인하세요.');
    } finally { setBusy(false); }
  };
  if (!account) return <form className="login" onSubmit={signIn}>
    <h1>로그인</h1>
    <label>아이디<input autoComplete="username" value={login} onChange={event => setLogin(event.target.value)} required/></label>
    <label>비밀번호<input type="password" autoComplete="current-password" value={password} onChange={event => setPassword(event.target.value)} required/></label>
    {error && <p role="alert">{error}</p>}
    <button disabled={busy}>로그인</button>
  </form>;
  return <Context.Provider value={account}>
    <header className="session"><span>{account.login}</span><button onClick={() => {
      delete axios.defaults.headers.common.Authorization; setAccount(null);
    }}>로그아웃</button></header>{children}
  </Context.Provider>;
}
