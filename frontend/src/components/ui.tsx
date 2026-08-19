// Author: deepak.maheshwari

import { ButtonHTMLAttributes, MouseEvent, ReactNode, useState } from "react";

export function Badge({ children, color = "slate" }: { children: ReactNode; color?: string }) {
  const map: Record<string, string> = {
    slate: "bg-slate-100 text-slate-700",
    green: "bg-emerald-100 text-emerald-700",
    red: "bg-red-100 text-red-700",
    amber: "bg-amber-100 text-amber-700",
    blue: "bg-blue-100 text-blue-700",
    purple: "bg-purple-100 text-purple-700",
  };
  return <span className={`badge ${map[color] ?? map.slate}`}>{children}</span>;
}

export function statusColor(status: string): string {
  switch (status) {
    case "ACTIVE":
    case "PUBLISHED":
    case "SENT":
      return "green";
    case "PENDING":
    case "DRAFT":
    case "INFO_REQUESTED":
      return "amber";
    case "REJECTED":
    case "SUSPENDED":
    case "FAILED":
      return "red";
    case "ARCHIVED":
      return "slate";
    default:
      return "blue";
  }
}

export function StatCard({ label, value, icon }: { label: string; value: string | number; icon?: ReactNode }) {
  return (
    <div className="card p-4 flex items-center gap-3">
      {icon && <div className="text-2xl">{icon}</div>}
      <div>
        <div className="text-2xl font-semibold text-slate-900">{value}</div>
        <div className="text-xs text-slate-500">{label}</div>
      </div>
    </div>
  );
}

export function EmptyState({ message }: { message: string }) {
  return <div className="py-12 text-center text-sm text-slate-400">{message}</div>;
}

export function Footer() {
  return (
    <footer className="py-4 text-center text-xs text-slate-400">
      © {new Date().getFullYear()} City Apartments Owner Association. All rights reserved.
    </footer>
  );
}

export function Spinner() {
  return (
    <div className="flex justify-center py-8">
      <div className="h-6 w-6 animate-spin rounded-full border-2 border-slate-300 border-t-brand-600" />
    </div>
  );
}

/** A small spinning circle sized for sitting inline next to a button's label. Uses
 *  border-current so it automatically matches whatever text color the button already has
 *  (white on btn-primary/btn-danger, slate on btn-secondary) with no per-variant styling.
 *  Exported directly for form-level submit buttons that already track their own busy state
 *  (see AsyncButton's doc comment for when to use which). */
export function ButtonSpinner() {
  return <span className="h-3.5 w-3.5 shrink-0 animate-spin rounded-full border-2 border-current border-t-transparent" />;
}

/**
 * Drop-in replacement for <button> for anything whose onClick fires an async action (an API
 * call). Tracks its own busy state - no per-page useState boilerplate needed - and shows
 * ButtonSpinner + disables itself for the duration, so a slow action always gives feedback
 * instead of leaving the user wondering whether their click registered.
 *
 * Only for onClick-driven actions. A <form onSubmit={...}> submit button's async work happens in
 * the form handler, not a click handler here - those keep tracking busy state at the form level
 * (already the existing pattern in this codebase) and just render ButtonSpinner directly instead.
 */
export function AsyncButton({
  onClick,
  children,
  disabled,
  type = "button",
  ...rest
}: {
  onClick: (e: MouseEvent<HTMLButtonElement>) => unknown;
} & Omit<ButtonHTMLAttributes<HTMLButtonElement>, "onClick">) {
  const [busy, setBusy] = useState(false);

  async function handleClick(e: MouseEvent<HTMLButtonElement>) {
    const result = onClick(e);
    if (result && typeof (result as PromiseLike<unknown>).then === "function") {
      setBusy(true);
      try {
        await result;
      } finally {
        setBusy(false);
      }
    }
  }

  return (
    <button type={type} disabled={disabled || busy} onClick={handleClick} {...rest}>
      {busy && <ButtonSpinner />}
      {children}
    </button>
  );
}

export function Modal({
  title,
  onClose,
  children,
  wide,
}: {
  title: string;
  onClose: () => void;
  children: ReactNode;
  wide?: boolean;
}) {
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
      <div className={`card w-full ${wide ? "max-w-2xl" : "max-w-md"} max-h-[90vh] overflow-y-auto p-5`}>
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold">{title}</h3>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-700">
            ✕
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <div className="flex items-center justify-center gap-2 py-4">
      <button className="btn-secondary btn-sm" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        Previous
      </button>
      <span className="text-sm text-slate-500">
        Page {page + 1} of {totalPages}
      </span>
      <button className="btn-secondary btn-sm" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Next
      </button>
    </div>
  );
}

export function ConfirmDialog({
  title,
  message,
  onConfirm,
  onCancel,
  danger,
}: {
  title: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
  danger?: boolean;
}) {
  return (
    <Modal title={title} onClose={onCancel}>
      <p className="mb-5 text-sm text-slate-600">{message}</p>
      <div className="flex justify-end gap-2">
        <button className="btn-secondary" onClick={onCancel}>
          Cancel
        </button>
        <button className={danger ? "btn-danger" : "btn-primary"} onClick={onConfirm}>
          Confirm
        </button>
      </div>
    </Modal>
  );
}

export function formatDate(value?: string): string {
  if (!value) return "-";
  const d = new Date(value);
  if (isNaN(d.getTime())) return "-";
  return d.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}

export function formatDateTime(value?: string): string {
  if (!value) return "-";
  const d = new Date(value);
  if (isNaN(d.getTime())) return "-";
  return d.toLocaleString("en-IN", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

export function formatBytes(bytes?: number): string {
  if (!bytes) return "-";
  const units = ["B", "KB", "MB", "GB"];
  let val = bytes;
  let i = 0;
  while (val >= 1024 && i < units.length - 1) {
    val /= 1024;
    i++;
  }
  return `${val.toFixed(val >= 10 || i === 0 ? 0 : 1)} ${units[i]}`;
}
