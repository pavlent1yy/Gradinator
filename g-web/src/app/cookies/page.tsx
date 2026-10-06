import Link from 'next/link';
import LegalPage from '../../components/LegalPage';

export const metadata = { title: 'Политика cookie — GradInator' };

export default function CookiesPage() {
  return (
    <LegalPage title="Политика cookie">
      <p>
        GradInator использует только то, без чего сайт не работает. Рекламных, аналитических и
        сторонних трекинговых cookie здесь нет, поэтому отдельно спрашивать разрешение не нужно —
        но рассказать, что именно хранится, мы обязаны.
      </p>

      <h2>Cookie</h2>
      <table>
        <thead>
          <tr><th>Имя</th><th>Зачем</th><th>Срок</th></tr>
        </thead>
        <tbody>
          <tr><td className="mono">gradinator_access</td><td>Держит тебя в аккаунте (короткоживущий токен входа). HttpOnly.</td><td>15 минут</td></tr>
          <tr><td className="mono">gradinator_refresh</td><td>Продлевает вход, чтобы не логиниться каждые 15 минут. HttpOnly.</td><td>30 дней</td></tr>
          <tr><td className="mono">JSESSIONID</td><td>Появляется только во время входа через Google/GitHub, защищает от подмены запроса.</td><td>до закрытия браузера</td></tr>
        </tbody>
      </table>

      <h2>Локальное хранилище браузера</h2>
      <table>
        <thead>
          <tr><th>Ключ</th><th>Зачем</th><th>Срок</th></tr>
        </thead>
        <tbody>
          <tr><td className="mono">gradinator.selectedGroup</td><td>Запоминает последнюю выбранную группу.</td><td>пока не очистишь</td></tr>
          <tr><td className="mono">gradinator.cookieNotice</td><td>Запоминает, что ты закрыл плашку про cookie.</td><td>пока не очистишь</td></tr>
        </tbody>
      </table>

      <h2>Как отключить</h2>
      <p>
        Cookie и локальное хранилище можно удалить или запретить в настройках браузера. Расписание
        будет работать и без них, а вот войти в аккаунт не получится.
      </p>
      <p>Что происходит с остальными данными — в <Link href="/privacy">политике конфиденциальности</Link>.</p>
    </LegalPage>
  );
}
