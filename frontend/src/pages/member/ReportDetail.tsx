// Author: deepak.maheshwari

import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api from "../../api/client";
import { DocumentDetail } from "../../api/types";
import { AsyncButton, formatBytes, formatDate, Spinner } from "../../components/ui";

export default function ReportDetail() {
  const { id } = useParams();
  const [doc, setDoc] = useState<DocumentDetail | null>(null);
  const [preview, setPreview] = useState<string | null>(null);

  useEffect(() => {
    api.get<DocumentDetail>(`/member/reports/${id}`).then((r) => setDoc(r.data));
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

  async function downloadAll() {
    const res = await api.get(`/member/reports/${id}/download-all`, { responseType: "blob" });
    const url = URL.createObjectURL(res.data);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${doc?.title || "report"}.zip`;
    a.click();
  }

  if (!doc) return <Spinner />;

  return (
    <div className="max-w-3xl space-y-4">
      <Link to="/member/reports" className="text-sm text-brand-600 hover:underline">
        ← Back to Reports
      </Link>
      <div className="card p-5">
        <div className="text-xs font-medium text-brand-600">{doc.categoryName}</div>
        <h1 className="mt-1 text-xl font-semibold">{doc.title}</h1>
        <dl className="mt-4 grid grid-cols-2 gap-3 text-sm sm:grid-cols-3">
          <Info label="Financial Year" value={doc.financialYear} />
          <Info label="Period" value={doc.reportPeriod} />
          <Info label="Report Date" value={formatDate(doc.reportDate)} />
          <Info label="Prepared By" value={doc.preparedBy} />
          <Info label="Published" value={formatDate(doc.publishedOn)} />
        </dl>
        {doc.description && <p className="mt-4 whitespace-pre-wrap text-sm text-slate-600">{doc.description}</p>}

        <div className="mt-6 border-t border-slate-100 pt-4">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="font-semibold">Attachments</h2>
            {doc.files.length > 1 && (
              <AsyncButton className="btn-secondary btn-sm" onClick={downloadAll}>
                Download All (ZIP)
              </AsyncButton>
            )}
          </div>
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
            {doc.files.length === 0 && <li className="p-4 text-center text-slate-400">No attachments</li>}
          </ul>
        </div>
      </div>

      {preview && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/50 p-4" onClick={() => setPreview(null)}>
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

function Info({ label, value }: { label: string; value?: string }) {
  if (!value) return null;
  return (
    <div>
      <div className="text-xs text-slate-400">{label}</div>
      <div className="font-medium">{value}</div>
    </div>
  );
}
