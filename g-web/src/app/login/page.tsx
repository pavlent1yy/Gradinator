'use client';

import { Suspense, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { useAuthContext } from '../providers/AuthProvider';
import * as api from '../../lib/api';

const YANDEX_OAUTH_ENABLED = process.env.NEXT_PUBLIC_YANDEX_OAUTH_ENABLED === 'true';
const VK_OAUTH_ENABLED = process.env.NEXT_PUBLIC_VK_OAUTH_ENABLED === 'true';
const OAUTH_ENABLED = process.env.NEXT_PUBLIC_OAUTH_ENABLED !== 'false';

const OAUTH_ERROR_MESSAGE = 'Не удалось войти через выбранный сервис. Попробуй ещё раз.';

export default function LoginPage() {
  return (
    <Suspense>
      <LoginForm />
    </Suspense>
  );
}

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const oauthFailed = searchParams.get('error') === 'oauth';
  const { login, authLoading, authError, authErrorCode, clearAuthError } = useAuthContext();
  const [resendState, setResendState] = useState<'idle' | 'sending' | 'sent'>('idle');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [clientMessage, setClientMessage] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setClientMessage(null);
    clearAuthError();
    setResendState('idle');

    if (!email.trim() || !password) {
      setClientMessage('Укажи email и пароль');
      return;
    }

    const ok = await login(email, password);
    if (ok) {
      setClientMessage('Вход выполнен. Перенаправление…');
      setTimeout(() => router.push('/'), 700);
    }
  }

  const message = clientMessage ?? authError ?? (oauthFailed ? OAUTH_ERROR_MESSAGE : null);

  return (
    <section className="auth-panel" aria-labelledby="login-title">
      <h1 id="login-title" className="auth-title">Вход</h1>
      <p className="auth-lead">Войди, чтобы продолжить работу с расписанием.</p>

      <form className="auth-form" onSubmit={onSubmit}>
        <label className="field">
          <span className="field-label">Email</span>
          <input
            className="field-input"
            type="email"
            value={email}
            onChange={e => setEmail(e.target.value)}
            autoComplete="email"
            required
          />
        </label>

        <label className="field">
          <span className="field-label">Пароль</span>
          <input
            className="field-input"
            type="password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
        <Link href={email ? `/forgot-password?email=${encodeURIComponent(email)}` : '/forgot-password'} className="forgot-link">
          Забыл пароль?
        </Link>

        {message && <div className="warning-note" role="status">{message}</div>}

        {authErrorCode === 'EMAIL_NOT_VERIFIED' && (
          <button
            type="button"
            className="btn btn-ghost"
            disabled={resendState !== 'idle'}
            onClick={async () => {
              setResendState('sending');
              try {
                await api.resendVerification(email.trim());
                setResendState('sent');
              } catch {
                setResendState('idle');
              }
            }}
          >
            {resendState === 'sent' ? 'Письмо отправлено — проверь почту' : resendState === 'sending' ? 'Отправка…' : 'Отправить письмо ещё раз'}
          </button>
        )}

        <div className="auth-actions">
          <button type="submit" className="btn btn-primary" disabled={authLoading}>
            {authLoading ? 'Вход…' : 'Войти'}
          </button>
          <button type="button" className="btn btn-ghost" onClick={() => router.push('/')}>
            Отмена
          </button>
        </div>

        {OAUTH_ENABLED && <div className="auth-actions auth-actions--oauth">
          {/*
            Полный редирект (не Link): запускает OAuth2-рукопожатие в g-core.
            После успеха g-core сам перенаправит на /profile.
            Прокси /oauth2/* настроен в next.config.ts.
          */}
          <a href="/oauth2/authorization/google" className="btn btn-oauth">
            <svg width="18" height="18" viewBox="0 0 48 48" aria-hidden="true">
              <path fill="#FFC107" d="M43.611 20.083H42V20H24v8h11.303c-1.649 4.657-6.08 8-11.303 8-6.627 0-12-5.373-12-12s5.373-12 12-12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 12.955 4 4 12.955 4 24s8.955 20 20 20 20-8.955 20-20c0-1.341-.138-2.65-.389-3.917z"/>
              <path fill="#FF3D00" d="M6.306 14.691l6.571 4.819C14.655 15.108 18.961 12 24 12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 16.318 4 9.656 8.337 6.306 14.691z"/>
              <path fill="#4CAF50" d="M24 44c5.166 0 9.86-1.977 13.409-5.192l-6.19-5.238C29.211 35.091 26.715 36 24 36c-5.202 0-9.619-3.317-11.283-7.946l-6.522 5.025C9.505 39.556 16.227 44 24 44z"/>
              <path fill="#1976D2" d="M43.611 20.083H42V20H24v8h11.303c-.792 2.237-2.231 4.166-4.087 5.571l6.19 5.238C36.971 39.205 44 34 44 24c0-1.341-.138-2.65-.389-3.917z"/>
            </svg>
            Google
          </a>
          <a href="/oauth2/authorization/github" className="btn btn-oauth">
            <svg width="18" height="18" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
              <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z"/>
            </svg>
            GitHub
          </a>
          {YANDEX_OAUTH_ENABLED && (
              <a href="/oauth2/authorization/yandex" className="btn btn-oauth">
                <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
                  <circle cx="12" cy="12" r="12" fill="#FC3F1D"/>
                  <path fill="#fff" d="M13.32 7.66h-.93c-1.7 0-2.6.86-2.6 2.13 0 1.44.62 2.11 1.89 2.97l1.05.7-3.02 4.51H7.46l2.72-4.03c-1.56-1.12-2.44-2.2-2.44-4.05 0-2.31 1.61-3.89 4.65-3.89h3.02v11.97h-2.09V7.66z"/>
                </svg>
                Яндекс
              </a>
          )}
          {VK_OAUTH_ENABLED && (
              <a href="/oauth2/authorization/vk" className="btn btn-oauth">
                <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
                  <rect width="24" height="24" rx="6" fill="#0077FF"/>
                  <path fill="#fff" d="M12.77 16.5c-4.79 0-7.52-3.28-7.63-8.75h2.4c.08 4.01 1.85 5.71 3.25 6.06V7.75h2.26v3.46c1.38-.15 2.84-1.73 3.33-3.46h2.26c-.38 2.14-1.95 3.72-3.07 4.37 1.12.53 2.92 1.91 3.6 4.38h-2.49c-.53-1.65-1.86-2.93-3.63-3.11v3.11h-.28z"/>
                </svg>
                VK
              </a>
          )}
        </div>}

        {OAUTH_ENABLED && <p className="auth-legal">
          Входя через внешнюю учётную запись впервые, ты принимаешь <Link href="/terms">соглашение</Link> и даёшь{' '}
          <Link href="/consent">согласие на обработку данных</Link>.
        </p>}

        <p className="auth-switch">
          Нет аккаунта? <Link href="/register">Зарегистрироваться</Link>
        </p>
      </form>
    </section>
  );
}
