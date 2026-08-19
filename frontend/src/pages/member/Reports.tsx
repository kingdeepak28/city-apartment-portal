// Author: deepak.maheshwari

import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../api/client";
import { CategoryResponse, DocumentListItem, Page } from "../../api/types";
import { formatDate, Pagination, Spinner } from "../../components/ui";

export default function Reports() {
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [categoryId, setCategoryId] = useState("");
  const [keyword, setKeyword] = useState("");
  const [sortBy, setSortBy] = useState("publishedOn");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<DocumentListItem> | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<CategoryResponse[]>("/member/reports/categories").then((r) => setCategories(r.data));
  }, []);

  useEffect(() => {
    setLoading(true);
    api
      .get<Page<DocumentListItem>>("/member/reports", {
        params: { categoryId: categoryId || undefined, keyword: keyword || undefined, sortBy, page, size: 12 },
      })
      .then((r) => setData(r.data))
      .finally(() => setLoading(false));
  }, [categoryId, keyword, sortBy, page]);

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Reports</h1>

      <div className="flex flex-wrap gap-2">
        <button
          className={`rounded-full px-3 py-1.5 text-sm ${!categoryId ? "bg-brand-600 text-white" : "bg-white border border-slate-300"}`}
          onClick={() => {
            setCategoryId("");
            setPage(0);
          }}
        >
          All ({categories.reduce((a, c) => a + c.documentCount, 0)})
        </button>
        {categories.map((c) => (
          <button
            key={c.id}
            className={`rounded-full px-3 py-1.5 text-sm ${categoryId === c.id ? "bg-brand-600 text-white" : "bg-white border border-slate-300"}`}
            onClick={() => {
              setCategoryId(c.id);
              setPage(0);
            }}
          >
            {c.name} ({c.documentCount})
          </button>
        ))}
      </div>

      <div className="card flex flex-wrap gap-3 p-4">
        <input className="input max-w-xs" placeholder="Search reports..." value={keyword} onChange={(e) => { setKeyword(e.target.value); setPage(0); }} />
        <select className="input max-w-[160px]" value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
          <option value="publishedOn">Newest first</option>
          <option value="title">Title (A-Z)</option>
        </select>
      </div>

      {loading ? (
        <Spinner />
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {data?.content.map((r) => (
            <Link key={r.id} to={`/member/reports/${r.id}`} className="card p-4 hover:shadow-md transition">
              <div className="text-xs font-medium text-brand-600">{r.categoryName}</div>
              <div className="mt-1 font-semibold">{r.title}</div>
              <div className="mt-2 flex items-center justify-between text-xs text-slate-400">
                <span>{r.financialYear || formatDate(r.publishedOn)}</span>
                <span>{r.fileCount} file(s)</span>
              </div>
            </Link>
          ))}
          {data?.content.length === 0 && <p className="col-span-full py-12 text-center text-slate-400">No reports found</p>}
        </div>
      )}
      {data && <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />}
    </div>
  );
}
