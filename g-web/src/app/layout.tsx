import './globals.css';
import { AuthProvider } from './providers/AuthProvider';
import { ScheduleProvider } from './providers/ScheduleProvider';
import Header from '../components/Header';
import { Inter, Oswald, Roboto_Mono, Patrick_Hand, Special_Elite } from 'next/font/google';

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

const robotoMono = Roboto_Mono({
  subsets: ['latin', 'cyrillic'],
  variable: '--font-mono',
  display: 'swap',
});

const patrickHand = Patrick_Hand({
  weight: '400',
  subsets: ['latin'],
  variable: '--font-hand',
  display: 'swap',
});

const specialElite = Special_Elite({
  weight: '400',
  subsets: ['latin'],
  variable: '--font-stamp',
  display: 'swap',
});

export const metadata = {
  title: 'GradInator — Расписание',
  description: 'Интерфейс расписания',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html
      lang="ru"
      className={`${inter.variable} ${oswald.variable} ${robotoMono.variable} ${patrickHand.variable} ${specialElite.variable}`}
    >
      <body>
        <AuthProvider>
          <ScheduleProvider>
            <div className="desk">
              <div className="app" id="app-root">
                <span className="clip" aria-hidden="true">
                  <span className="clip-rivet clip-rivet--left" />
                  <span className="clip-rivet clip-rivet--right" />
                </span>
                <span className="tape tape--left" aria-hidden="true" />
                <span className="tape tape--right" aria-hidden="true" />
                <span className="binder-holes" aria-hidden="true">
                  <span />
                  <span />
                  <span />
                </span>
                <Header />
                <main className="page-main">{children}</main>
                <footer className="page-foot">
                  <div className="foot-left mono">Источник: учебный план — 2026</div>
                  <div className="foot-right foot-stamp">GRADINATOR · РЕЕСТР ЗАНЯТИЙ</div>
                </footer>
              </div>
            </div>
          </ScheduleProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
