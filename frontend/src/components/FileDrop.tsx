import { useRef, useState, DragEvent } from "react";
import { formatBytes } from "./ui";

export default function FileDrop({
  files,
  onChange,
  multiple = true,
  accept,
}: {
  files: File[];
  onChange: (files: File[]) => void;
  multiple?: boolean;
  accept?: string;
}) {
  const [dragOver, setDragOver] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  function addFiles(list: FileList | null) {
    if (!list) return;
    const arr = Array.from(list);
    onChange(multiple ? [...files, ...arr] : arr.slice(0, 1));
  }

  function handleDrop(e: DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setDragOver(false);
    addFiles(e.dataTransfer.files);
  }

  function removeAt(i: number) {
    onChange(files.filter((_, idx) => idx !== i));
  }

  return (
    <div>
      <div
        onDragOver={(e) => {
          e.preventDefault();
          setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={handleDrop}
        onClick={() => inputRef.current?.click()}
        className={`cursor-pointer rounded-md border-2 border-dashed p-6 text-center text-sm transition ${
          dragOver ? "border-brand-500 bg-brand-50" : "border-slate-300 hover:border-brand-400"
        }`}
      >
        <p className="text-slate-600">Drag &amp; drop files here, or click to browse</p>
        <p className="mt-1 text-xs text-slate-400">PDF, DOC, XLS, PPT, JPG, PNG, ZIP - up to 25 MB each</p>
        <input
          ref={inputRef}
          type="file"
          multiple={multiple}
          accept={accept}
          className="hidden"
          onChange={(e) => addFiles(e.target.files)}
        />
      </div>
      {files.length > 0 && (
        <ul className="mt-3 divide-y divide-slate-100 rounded-md border border-slate-200">
          {files.map((f, i) => (
            <li key={i} className="flex items-center justify-between px-3 py-2 text-sm">
              <span className="truncate">{f.name}</span>
              <span className="flex items-center gap-3 text-xs text-slate-400">
                {formatBytes(f.size)}
                <button type="button" className="text-red-500 hover:text-red-700" onClick={() => removeAt(i)}>
                  Remove
                </button>
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
