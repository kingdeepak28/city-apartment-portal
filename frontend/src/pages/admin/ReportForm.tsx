import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api, { apiErrorMessage } from "../../api/client";
import { CategoryResponse, DocumentDetail } from "../../api/types";
import FileDrop from "../../components/FileDrop";
import { formatBytes, formatDateTime, Spinner } from "../../components/ui";
import { useToast } from "../../context/ToastContext";

const BLOCKS_PLACEHOLDER = "A, B, C";

export default function ReportForm() {
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
    subCategoryId: "",
    financialYear: "",
    reportPeriod: "",
    description: "",
    preparedBy: "",
    reportDate: "",
    tags: "",
    visibilityType: "ALL",
    visibilityBlocks: "",
    status: "DRAFT",
  });

  useEffect(() => {
    api.get<CategoryResponse[]>("/admin/categories", { params: { type: "REPORT" } }).then((r) => setCategories(r.data));
  }, []);

  useEffect(() => {
    if (!isEdit) return;
    api.get<DocumentDetail>(`/admin/reports/${id}`).then((r) => {
      const d = r.data;
      setExisting(d);
      setForm({
        title: d.title,
        categoryId: d.categoryId || "",
        subCategoryId: d.subCategoryId || "",
        financialYear: d.financialYear || "",
        reportPeriod: d.reportPeriod || "",
        description: d.description || "",
        preparedBy: d.preparedBy || "",
        reportDate: d.reportDate || "",
        tags: d.tags || "",
        visibilityType: d.visibilityType,
        visibilityBlocks: d.visibilityBlocks?.join(", ") || "",
        status: d.status,
      });
      setLoading(false);
    });
  }, [id, isEdit]);

  const topCategories = categories.filter((c) => !c.parentId);
  const subCategories = categories.filter((c) => c.parentId === form.categoryId);

  async function save(publish: boolean) {
    setBusy(true);
    try {
      const payload = {
        title: form.title,
        categoryId: form.categoryId || null,
        subCategoryId: form.subCategoryId || null,
        financialYear: form.financialYear,
        reportPeriod: form.reportPeriod,
        description: form.description,
        preparedBy: form.preparedBy,
        reportDate: form.reportDate || null,
        tags: form.tags,
        visibilityType: form.visibilityType,
        visibilityBlocks: form.visibilityType === "BLOCKS" ? form.visibilityBlocks.split(",").map((s) => s.trim()).filter(Boolean) : [],
        status: publish ? "PUBLISHED" : "DRAFT",
      };
      let docId = id;
      if (isEdit) {
        await api.put(`/admin/reports/${id}`, payload);
      } else {
        const { data } = await api.post("/admin/reports", payload);
        docId = data.id;
      }
      if (files.length > 0 && docId) {
        const fd = new FormData();
        files.forEach((f) => fd.append("files", f));
        await api.post(`/admin/reports/${docId}/files`, fd);
      }
      notify(publish ? "Report published" : "Report saved as draft", "success");
      navigate("/admin/reports");
    } catch (err) {
      notify(apiErrorMessage(err), "error");
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <Spinner />;

  return (
    <div className="max-w-3xl space-y-4">
      <h1 className="text-xl font-semibold">{isEdit ? "Edit Report" : "Upload Report"}</h1>

      <div className="card space-y-4 p-5">
        <div>
          <label className="label">Title *</label>
          <input className="input" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="label">Category *</label>
            <select className="input" value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value, subCategoryId: "" })}>
              <option value="">Select category</option>
              {topCategories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="label">Sub-category</label>
            <select className="input" value={form.subCategoryId} onChange={(e) => setForm({ ...form, subCategoryId: e.target.value })} disabled={!subCategories.length}>
              <option value="">None</option>
              {subCategories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="grid grid-cols-3 gap-3">
          <div>
            <label className="label">Financial Year</label>
            <input className="input" placeholder="2025-26" value={form.financialYear} onChange={(e) => setForm({ ...form, financialYear: e.target.value })} />
          </div>
          <div>
            <label className="label">Report Period</label>
            <input className="input" placeholder="Q1 / Annual" value={form.reportPeriod} onChange={(e) => setForm({ ...form, reportPeriod: e.target.value })} />
          </div>
          <div>
            <label className="label">Report Date</label>
            <input className="input" type="date" value={form.reportDate} onChange={(e) => setForm({ ...form, reportDate: e.target.value })} />
          </div>
        </div>

        <div>
          <label className="label">Prepared By</label>
          <input className="input" value={form.preparedBy} onChange={(e) => setForm({ ...form, preparedBy: e.target.value })} />
        </div>

        <div>
          <label className="label">Description</label>
          <textarea className="input" rows={3} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
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
              <input className="input" placeholder={BLOCKS_PLACEHOLDER} value={form.visibilityBlocks} onChange={(e) => setForm({ ...form, visibilityBlocks: e.target.value })} />
            </div>
          )}
        </div>

        <div>
          <label className="label">Attachments</label>
          <FileDrop files={files} onChange={setFiles} />
        </div>

        {existing && existing.files.length > 0 && (
          <div>
            <label className="label">Existing Files</label>
            <ul className="divide-y divide-slate-100 rounded-md border border-slate-200 text-sm">
              {existing.files.map((f) => (
                <li key={f.id} className="flex justify-between px-3 py-2">
                  <span>{f.fileName} (v{f.versionNo})</span>
                  <span className="text-xs text-slate-400">{formatBytes(f.fileSize)} - {formatDateTime(f.uploadedAt)}</span>
                </li>
              ))}
            </ul>
          </div>
        )}

        <div className="flex justify-end gap-2 border-t border-slate-100 pt-4">
          <button className="btn-secondary" onClick={() => navigate("/admin/reports")}>
            Cancel
          </button>
          <button className="btn-secondary" disabled={busy} onClick={() => save(false)}>
            Save as Draft
          </button>
          <button className="btn-primary" disabled={busy} onClick={() => save(true)}>
            Publish
          </button>
        </div>
      </div>
    </div>
  );
}
