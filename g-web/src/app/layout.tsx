import '../styles/base.css';
import '../styles/classic.css';
import '../styles/modern.css';
import { AuthProvider } from './providers/AuthProvider';
import { ScheduleProvider } from './providers/ScheduleProvider';
import Header from '../components/Header';
import CookieNotice from '../components/CookieNotice';
import Link from 'next/link';
import PageFlip from '../components/PageFlip';
import Backdrop from '../components/Backdrop';
import { Inter, Oswald, JetBrains_Mono, Caveat, Onest } from 'next/font/google';
import { DEFAULT_THEME, themeInitScript } from '../lib/theme';

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

const onest = Onest({
  subsets: ['latin', 'cyrillic'],
  variable: '--font-sans',
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
      data-theme={DEFAULT_THEME}
      suppressHydrationWarning
      className={`${inter.variable} ${oswald.variable} ${jetbrainsMono.variable} ${caveat.variable} ${onest.variable}`}
    >
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeInitScript }} />
      </head>
      <body>
        <AuthProvider>
          <ScheduleProvider>
            <Backdrop />
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
