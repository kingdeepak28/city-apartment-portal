// Author: deepak.maheshwari

import { useEffect, useRef } from "react";

/** A small dependency-free rich-text editor backed by contentEditable + execCommand,
 *  sufficient for notice bodies (headings, bold/italic, lists, links) per FR-AD-41. */
export default function RichTextEditor({ value, onChange }: { value: string; onChange: (html: string) => void }) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (ref.current && ref.current.innerHTML !== value) {
      ref.current.innerHTML = value || "";
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function exec(cmd: string, arg?: string) {
    document.execCommand(cmd, false, arg);
    ref.current?.focus();
    onChange(ref.current?.innerHTML ?? "");
  }

  const buttons: { label: string; cmd: string; arg?: string }[] = [
    { label: "B", cmd: "bold" },
    { label: "I", cmd: "italic" },
    { label: "U", cmd: "underline" },
    { label: "H2", cmd: "formatBlock", arg: "H2" },
    { label: "P", cmd: "formatBlock", arg: "P" },
    { label: "• List", cmd: "insertUnorderedList" },
    { label: "1. List", cmd: "insertOrderedList" },
  ];

  return (
    <div className="rounded-md border border-slate-300">
      <div className="flex flex-wrap gap-1 border-b border-slate-200 bg-slate-50 p-1.5">
        {buttons.map((b) => (
          <button
            key={b.label}
            type="button"
            className="rounded px-2 py-1 text-xs font-medium text-slate-600 hover:bg-slate-200"
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => exec(b.cmd, b.arg)}
          >
            {b.label}
          </button>
        ))}
        <button
          type="button"
          className="rounded px-2 py-1 text-xs font-medium text-slate-600 hover:bg-slate-200"
          onMouseDown={(e) => e.preventDefault()}
          onClick={() => {
            const url = window.prompt("Link URL");
            if (url) exec("createLink", url);
          }}
        >
          Link
        </button>
      </div>
      <div
        ref={ref}
        contentEditable
        className="min-h-[160px] px-3 py-2 text-sm focus:outline-none prose prose-sm max-w-none"
        onInput={() => onChange(ref.current?.innerHTML ?? "")}
      />
    </div>
  );
}
