// Author: deepak.maheshwari

import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api, { apiErrorMessage } from "../../api/client";
import { CategoryResponse, DocumentDetail } from "../../api/types";
import AttachmentList from "../../components/AttachmentList";
import FileDrop from "../../components/FileDrop";
import RichTextEditor from "../../components/RichTextEditor";
import { ButtonSpinner, Spinner } from "../../components/ui";
import { useToast } from "../../context/ToastContext";

// <input type="datetime-local"> both displays and submits a timezone-less "local wall clock"
// string (e.g. "2026-12-31T18:00"), but the backend's publishAt/expiryAt fields are
// java.time.OffsetDateTime, which requires an explicit offset - Jackson rejects a bare
// timezone-less string outright. These two helpers convert in each direction using the browser's
// own local timezone, which is what an admin typing a date/time here actually means.
function toDatetimeLocalValue(iso?: string | null): string {
  if (!iso) return "";
  const d = new Date(iso);
  const localMs = d.getTime() - d.getTimezoneOffset() * 60000;
  return new Date(localMs).toISOString().slice(0, 16);
}

function fromDatetimeLocalValue(value: string): string | null {
  if (!value) return null;
  return new Date(value).toISOString();
}

export default function NoticeForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { notify } = useToast();

  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [existing, setExisting] = useState<DocumentDetail | null>(null);
  const [loading, setLoading] = useState(isEdit);
  const [busy, setBusy] = useState(false);
  const [files, setFiles] = useState<File[]>([]);

  const [form, setForm] = useState({
    title: "",
    categoryId: "",
    priority: "NORMAL",
    bodyHtml: "",
    tags: "",
    visibilityType: "ALL",
    visibilityBlocks: "",
    status: "DRAFT",
    publishAt: "",
    expiryAt: "",
    pinned: false,
  });

  useEffect(() => {
    api.get<CategoryResponse[]>("/admin/categories", { params: { type: "NOTICE" } }).then((r) => setCategories(r.data));
  }, []);

  useEffect(() => {
    if (!isEdit) return;
    api.get<DocumentDetail>(`/admin/notices/${id}`).then((r) => {
      const d = r.data;
      setExisting(d);
      setForm({
        title: d.title,
        categoryId: d.categoryId || "",
        priority: d.priority || "NORMAL",
        bodyHtml: d.bodyHtml || "",
        tags: d.tags || "",
        visibilityType: d.visibilityType,
        visibilityBlocks: d.visibilityBlocks?.join(", ") || "",
        status: d.status,
        publishAt: toDatetimeLocalValue(d.publishAt),
        expiryAt: toDatetimeLocalValue(d.expiryAt),
        pinned: d.pinned,
      });
      setLoading(false);
    });
  }, [id, isEdit]);

  async function save(publish: boolean) {
    setBusy(true);
    try {
      const payload = {
        title: form.title,
        categoryId: form.categoryId || null,
        priority: form.priority,
        bodyHtml: form.bodyHtml,
        tags: form.tags,
        visibilityType: form.visibilityType,
        visibilityBlocks: form.visibilityType === "BLOCKS" ? form.visibilityBlocks.split(",").map((s) => s.trim()).filter(Boolean) : [],
        status: publish ? "PUBLISHED" : "DRAFT",
        publishAt: fromDatetimeLocalValue(form.publishAt),
        expiryAt: fromDatetimeLocalValue(form.expiryAt),
        pinned: form.pinned,
      };
      let docId = id;
      if (isEdit) {
        await api.put(`/admin/notices/${id}`, payload);
      } else {
        const { data } = await api.post("/admin/notices", payload);
        docId = data.id;
      }
      if (files.length > 0 && docId) {
        const fd = new FormData();
        files.forEach((f) => fd.append("files", f));
        await api.post(`/admin/notices/${docId}/files`, fd);
      }
      notify(publish ? "Notice published" : "Notice saved as draft", "success");
      navigate("/admin/notices");
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <Spinner />;

  return (
    <div className="max-w-3xl space-y-4">
      <h1 className="text-xl font-semibold">{isEdit ? "Edit Notice" : "Post Notice"}</h1>

      <div className="card space-y-4 p-5">
        <div>
          <label className="label">Title *</label>
          <input className="input" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="label">Category</label>
            <select className="input" value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}>
              <option value="">Select category</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="label">Priority</label>
            <select className="input" value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}>
              <option value="NORMAL">Normal</option>
              <option value="IMPORTANT">Important</option>
              <option value="URGENT">Urgent (bypasses notification preferences)</option>
            </select>
          </div>
        </div>

        <div>
          <label className="label">Body *</label>
          <RichTextEditor value={form.bodyHtml} onChange={(html) => setForm((f) => ({ ...f, bodyHtml: html }))} />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="label">Scheduled Publish (optional)</label>
            <input className="input" type="datetime-local" value={form.publishAt} onChange={(e) => setForm({ ...form, publishAt: e.target.value })} />
          </div>
          <div>
            <label className="label">Expiry Date</label>
            <input className="input" type="datetime-local" value={form.expiryAt} onChange={(e) => setForm({ ...form, expiryAt: e.target.value })} />
          </div>
        </div>

        <div>
          <label className="label">Tags (comma separated)</label>
          <input className="input" value={form.tags} onChange={(e) => setForm({ ...form, tags: e.target.value })} />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="label">Visibility</label>
            <select className="input" value={form.visibilityType} onChange={(e) => setForm({ ...form, visibilityType: e.target.value })}>
              <option value="ALL">All Users</option>
              <option value="OWNERS">Owners only</option>
              <option value="TENANTS">Tenants only</option>
              <option value="BLOCKS">Selected block(s)</option>
            </select>
          </div>
          {form.visibilityType === "BLOCKS" && (
            <div>
              <label className="label">Blocks (comma separated)</label>
              <input className="input" placeholder="A, B, C" value={form.visibilityBlocks} onChange={(e) => setForm({ ...form, visibilityBlocks: e.target.value })} />
            </div>
          )}
        </div>

        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={form.pinned} onChange={(e) => setForm({ ...form, pinned: e.target.checked })} />
          Pin to top of member notice list
        </label>

        <div>
          <label className="label">Attachments</label>
          <FileDrop files={files} onChange={setFiles} />
        </div>

        {existing && existing.files.length > 0 && (
          <div>
            <label className="label">Existing Files</label>
            <AttachmentList files={existing.files} showMeta />
          </div>
        )}

        <div className="flex justify-end gap-2 border-t border-slate-100 pt-4">
          <button className="btn-secondary" onClick={() => navigate("/admin/notices")}>
            Cancel
          </button>
          <button className="btn-secondary" disabled={busy} onClick={() => save(false)}>
            {busy && <ButtonSpinner />}
            Save as Draft
          </button>
          <button className="btn-primary" disabled={busy} onClick={() => save(true)}>
            {busy && <ButtonSpinner />}
            Publish
          </button>
        </div>
      </div>
    </div>
  );
}
