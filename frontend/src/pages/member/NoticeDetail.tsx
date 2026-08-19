// Author: deepak.maheshwari

import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api from "../../api/client";
import { DocumentDetail } from "../../api/types";
import { AsyncButton, Badge, formatBytes, formatDate, Spinner } from "../../components/ui";

export default function NoticeDetail() {
  const { id } = useParams();
  const [doc, setDoc] = useState<DocumentDetail | null>(null);
  const [preview, setPreview] = useState<string | null>(null);

  useEffect(() => {
    api.get<DocumentDetail>(`/member/notices/${id}`).then((r) => setDoc(r.data));
  }, [id]);

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

  if (!doc) return <Spinner />;

  return (
    <div className="max-w-3xl space-y-4 print:max-w-full">
      <div className="flex items-center justify-between print:hidden">
        <Link to="/member/notices" className="text-sm text-brand-600 hover:underline">
          ← Back to Notices
        </Link>
        <button className="btn-secondary btn-sm" onClick={() => window.print()}>
          Print
        </button>
      </div>
      <div className="card p-6">
        <div className="flex items-center gap-2">
          <Badge color="blue">{doc.categoryName}</Badge>
          <Badge color={doc.priority === "URGENT" ? "red" : doc.priority === "IMPORTANT" ? "amber" : "slate"}>{doc.priority}</Badge>
          {doc.status === "ARCHIVED" && <Badge color="slate">Archived</Badge>}
        </div>
        <h1 className="mt-2 text-xl font-semibold">{doc.title}</h1>
        <div className="mt-1 text-xs text-slate-400">
          {doc.noticeNumber} - Published {formatDate(doc.publishedOn)}
          {doc.expiryAt && ` - Expires ${formatDate(doc.expiryAt)}`}
        </div>

        <div className="prose prose-sm mt-4 max-w-none" dangerouslySetInnerHTML={{ __html: doc.bodyHtml || "" }} />

        {doc.files.length > 0 && (
          <div className="mt-6 border-t border-slate-100 pt-4 print:hidden">
            <h2 className="mb-2 font-semibold">Attachments</h2>
            <ul className="divide-y divide-slate-100 rounded-md border border-slate-200">
              {doc.files.map((f) => (
                <li key={f.id} className="flex items-center justify-between px-3 py-2 text-sm">
                  <span>{f.fileName}</span>
                  <span className="flex items-center gap-3 text-xs text-slate-400">
                    {formatBytes(f.fileSize)}
                    <AsyncButton className="inline-flex items-center gap-1 text-brand-600 hover:underline" onClick={() => openPreview(f.id)}>
                      Preview
                    </AsyncButton>
                    <AsyncButton className="inline-flex items-center gap-1 text-brand-600 hover:underline" onClick={() => download(f.id, f.fileName)}>
                      Download
                    </AsyncButton>
                  </span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

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
    </div>
  );
}
