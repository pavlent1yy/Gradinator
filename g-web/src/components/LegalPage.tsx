import Link from 'next/link';
import { LEGAL } from '../lib/legal';

const LINKS = [
  { href: '/privacy', label: 'Конфиденциальность' },
  { href: '/consent', label: 'Согласие на обработку ПДн' },
  { href: '/cookies', label: 'Cookie' },
  { href: '/terms', label: 'Соглашение' },
];

export default function LegalPage({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <article className="legal">
      <nav className="legal-nav" aria-label="Документы">
        {LINKS.map((l) => (
          <Link key={l.href} href={l.href}>{l.label}</Link>
        ))}
      </nav>
      <h1 className="auth-title">{title}</h1>
      <p className="legal-updated mono">Редакция от {LEGAL.updatedAt}</p>
      {children}
    </article>
  );
}
