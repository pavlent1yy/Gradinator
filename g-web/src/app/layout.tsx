import './globals.css';
import { AuthProvider } from './providers/AuthProvider';
import { ScheduleProvider } from './providers/ScheduleProvider';
import Header from '../components/Header';
import CookieNotice from '../components/CookieNotice';
import Link from 'next/link';
import PageFlip from '../components/PageFlip';
import { Inter, Oswald, JetBrains_Mono, Caveat } from 'next/font/google';

const inter = Inter({
  subsets: ['latin', 'cyrillic'],
  variable: '--font-ui',
  display: 'swap',
});

const oswald = Oswald({
  subsets: ['latin', 'cyrillic'],
  variable: '--font-display',
  display: 'swap',
});

const jetbrainsMono = JetBrains_Mono({
  subsets: ['latin', 'cyrillic'],
  variable: '--font-mono',
  display: 'swap',
});

const caveat = Caveat({
  subsets: ['latin', 'cyrillic'],
  variable: '--font-hand',
  display: 'swap',
});

export const metadata = {
  title: 'GradInator — Расписание',
  description: 'Расписание ЯГК и учёт пропусков. Неофициальный студенческий проект.',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html
      lang="ru"
      className={`${inter.variable} ${oswald.variable} ${jetbrainsMono.variable} ${caveat.variable}`}
    >
      <body>
        <AuthProvider>
          <ScheduleProvider>
            <div className="desk">
              <div className="app" id="app-root">
                <span className="tape tape--left" aria-hidden="true" />
                <span className="tape tape--right" aria-hidden="true" />
                <span className="binder-holes" aria-hidden="true">
                  <span />
                  <span />
                  <span />
                </span>
                <Header />
                <main className="page-main">
                  <PageFlip>{children}</PageFlip>
                </main>
                <footer className="page-foot">
                  <div className="foot-left mono">
                    Неофициальный студенческий проект. Сверяйся с расписанием колледжа.
                  </div>
                  <nav className="foot-links" aria-label="Документы">
                    <Link href="/privacy">Конфиденциальность</Link>
                    <Link href="/consent">Согласие на ПДн</Link>
                    <Link href="/cookies">Cookie</Link>
                    <Link href="/terms">Соглашение</Link>
                  </nav>
                  <div className="foot-right foot-stamp">GRADINATOR · РЕЕСТР ЗАНЯТИЙ</div>
                </footer>
              </div>
            </div>
            <CookieNotice />
          </ScheduleProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
