import './globals.css';

export const metadata = {
  title: 'GradInator — Расписание',
  description: 'Интерфейс расписания',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ru">
      <body>{children}</body>
    </html>
  );
}