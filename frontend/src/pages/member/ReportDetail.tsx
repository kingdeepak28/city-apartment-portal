// Author: deepak.maheshwari

import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api from "../../api/client";
import { DocumentDetail } from "../../api/types";
import AttachmentList from "../../components/AttachmentList";
import { AsyncButton, formatDate, Spinner } from "../../components/ui";

export default function ReportDetail() {
  const { id } = useParams();
  const [doc, setDoc] = useState<DocumentDetail | null>(null);

  useEffect(() => {
    api.get<DocumentDetail>(`/member/reports/${id}`).then((r) => setDoc(r.data));
  }, [id]);

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
          <AttachmentList files={doc.files} />
        </div>
      </div>
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
