'use client';

import { useCallback, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthContext } from '../app/providers/AuthProvider';
import ConfirmDialog from './ConfirmDialog';

type Props = {
  className: string;
  title?: string;
  ariaLabel?: string;
  children: React.ReactNode;
};

export default function LogoutButton({ className, title, ariaLabel, children }: Props) {
  const router = useRouter();
  const { logout } = useAuthContext();
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const close = useCallback(() => setOpen(false), []);

  async function confirm() {
    setBusy(true);
    try {
      await logout();
      setOpen(false);
      router.push('/');
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <button type="button" className={className} onClick={() => setOpen(true)} title={title} aria-label={ariaLabel}>
        {children}
      </button>
      <ConfirmDialog
        open={open}
        title="Выйти из аккаунта?"
        confirmLabel={busy ? 'Выход…' : 'Выйти'}
        busy={busy}
        onConfirm={confirm}
        onCancel={close}
      >
        <p>Чтобы снова увидеть свою группу и пропуски, нужно будет войти заново.</p>
      </ConfirmDialog>
    </>
  );
}
