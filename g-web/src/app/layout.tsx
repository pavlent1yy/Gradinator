import './globals.css';
import { ScheduleProvider } from './providers/ScheduleProvider';
import Header from '../components/Header'

export const metadata = {
  title: 'GradInator — Расписание',
  description: 'Интерфейс расписания',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ru">
      <body>
        <ScheduleProvider>
          <div className="app" id="app-root">
            <Header />
            <main style={{ paddingTop: 8 }}>
              {children}
            </main>

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