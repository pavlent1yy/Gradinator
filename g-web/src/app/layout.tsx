import './globals.css';
import { ScheduleProvider } from './providers/ScheduleProvider';
import Header from '../components/Header';
import { Inter, Oswald, Roboto_Mono, Patrick_Hand } from 'next/font/google';

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

export const metadata = {
  title: 'GradInator — Расписание',
  description: 'Интерфейс расписания',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ru" className={`${inter.variable} ${oswald.variable} ${robotoMono.variable} ${patrickHand.variable}`}>
      <body>
        <ScheduleProvider>
          <div className="app" id="app-root">
            <Header />
            <main className="page-main">{children}</main>
            <footer className="page-foot">
              <div className="foot-left mono">Источник: учебный план — 2026</div>
              <div className="foot-right">GradInator · analog editorial</div>
            </footer>
          </div>
        </ScheduleProvider>
      </body>
    </html>
  );
}
