// Author: deepak.maheshwari

import { useState } from "react";
import api from "../api/client";
import { FileInfo } from "../api/types";
import { AsyncButton, formatBytes, formatDateTime } from "./ui";

// Shared by every screen that lists a document's attachments - member report/notice detail pages
// and the admin report/notice forms - so preview/download behave identically everywhere instead
// of drifting across copy-pasted implementations.
export default function AttachmentList({
  files,
  emptyLabel = "No attachments",
  showMeta = false,
}: {
  files: FileInfo[];
  emptyLabel?: string;
  // Version number and upload timestamp are only useful in admin contexts where an editor is
  // tracking re-uploads; member-facing views keep just the file name and size.
  showMeta?: boolean;
}) {
  const [preview, setPreview] = useState<string | null>(null);

  async function openPreview(fileId: string) {
    const { data } = await api.get(`/files/signed-url/${fileId}`);
    setPreview(data.previewUrl);
  }

  async function download(fileId: string, fileName: string) {
    const { data } = await api.get(`/files/signed-url/${fileId}`);
    // downloadUrl already includes the "/api" prefix the client's baseURL also adds - strip it
    // here or the request ends up double-prefixed as "/api/api/..." and 404s.
    const res = await api.get(data.downloadUrl.replace(/^\/api/, ""), { responseType: "blob" });
    const url = URL.createObjectURL(res.data);
    const a = document.createElement("a");
    a.href = url;
    a.download = fileName;
    a.click();
  }

  return (
    <>
      <ul className="divide-y divide-slate-100 rounded-md border border-slate-200">
        {files.map((f) => (
          <li key={f.id} className="flex items-center justify-between px-3 py-2 text-sm">
            <span className="truncate">
              {f.fileName}
              {showMeta && f.versionNo > 1 ? ` (v${f.versionNo})` : ""}
            </span>
            <span className="flex items-center gap-3 text-xs text-slate-400">
              {formatBytes(f.fileSize)}
              {showMeta && <span>{formatDateTime(f.uploadedAt)}</span>}
              <AsyncButton className="inline-flex items-center gap-1 text-brand-600 hover:underline" onClick={() => openPreview(f.id)}>
                Preview
              </AsyncButton>
              <AsyncButton className="inline-flex items-center gap-1 text-brand-600 hover:underline" onClick={() => download(f.id, f.fileName)}>
                Download
              </AsyncButton>
            </span>
          </li>
        ))}
        {files.length === 0 && <li className="p-4 text-center text-slate-400">{emptyLabel}</li>}
      </ul>

      {preview && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/50 p-4 print:hidden" onClick={() => setPreview(null)}>
          <div className="h-[85vh] w-full max-w-4xl rounded-lg bg-white p-2" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between p-1">
              {/* iOS Safari can't render a multi-page PDF's scrolling/pagination inside an
                  <iframe> - it just flattens to a static first page, no way to reach the rest.
                  Opening the same signed URL as a direct top-level navigation instead gets
                  Safari's full native PDF viewer, so this is the fallback for that case. */}
              <a href={preview} target="_blank" rel="noopener noreferrer" className="text-sm text-brand-600 hover:underline">
                Open in new tab ↗
              </a>
              <button className="text-slate-400 hover:text-slate-700" onClick={() => setPreview(null)}>
                ✕
              </button>
            </div>
            <iframe src={preview} className="h-full w-full rounded" title="preview" />
          </div>
        </div>
      )}
    </>
  );
}
