// Author: deepak.maheshwari

import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api from "../../api/client";
import { DocumentDetail } from "../../api/types";
import AttachmentList from "../../components/AttachmentList";
import { Badge, formatDate, Spinner } from "../../components/ui";

export default function NoticeDetail() {
  const { id } = useParams();
  const [doc, setDoc] = useState<DocumentDetail | null>(null);

  useEffect(() => {
    api.get<DocumentDetail>(`/member/notices/${id}`).then((r) => setDoc(r.data));
  }, [id]);

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
            <AttachmentList files={doc.files} />
          </div>
        )}
      </div>
    </div>
  );
}
