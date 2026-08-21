// Author: deepak.maheshwari

import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../api/client";
import { CategoryResponse, DocumentListItem, Page } from "../../api/types";
import { formatDate, Pagination, Spinner } from "../../components/ui";

export default function Reports() {
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [expanded, setExpanded] = useState<Set<string>>(new Set());
  const [categoryId, setCategoryId] = useState("");
  // Which category's results panel is open in the mobile accordion (see render below). Kept
  // separate from categoryId (which defaults to "" = All, so desktop shows every report on
  // load) - on mobile nothing should be expanded until the member actually taps a category, "All
  // Reports" included, so this starts at null rather than mirroring categoryId's default.
  const [mobileOpen, setMobileOpen] = useState<string | null>(null);
  const [keyword, setKeyword] = useState("");
  const [sortBy, setSortBy] = useState("publishedOn");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<DocumentListItem> | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<CategoryResponse[]>("/member/reports/categories").then((r) => {
      setCategories(r.data);
      // Start with every branch open - with a handful of top-level categories there's no benefit
      // to hiding sub-categories behind an extra click, only a chance of a member never noticing
      // they exist. The collapse arrow is there for whoever'd rather fold a branch away.
      setExpanded(new Set(r.data.filter((c) => c.parentId).map((c) => c.parentId!)));
    });
  }, []);

  const topLevelCategories = categories.filter((c) => !c.parentId);
  const childrenByParent = new Map<string, CategoryResponse[]>();
  categories.forEach((c) => {
    if (!c.parentId) return;
    if (!childrenByParent.has(c.parentId)) childrenByParent.set(c.parentId, []);
    childrenByParent.get(c.parentId)!.push(c);
  });

  function selectCategory(id: string) {
    setCategoryId(id);
    setPage(0);
  }

  // Shared onClick for every category row (desktop pills and mobile accordion headers alike):
  // selects the category as before, and on mobile also opens its panel - tapping an already-open
  // one closes it again.
  function handleCategoryClick(id: string) {
    selectCategory(id);
    setMobileOpen((prev) => (prev === id ? null : id));
  }

  function toggleExpanded(id: string) {
    setExpanded((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  useEffect(() => {
    setLoading(true);
    api
      .get<Page<DocumentListItem>>("/member/reports", {
        params: { categoryId: categoryId || undefined, keyword: keyword || undefined, sortBy, page, size: 12 },
      })
      .then((r) => setData(r.data))
      .finally(() => setLoading(false));
  }, [categoryId, keyword, sortBy, page]);

  const filters = (
    <div className="card flex flex-wrap gap-3 p-4">
      <input className="input max-w-xs" placeholder="Search reports..." value={keyword} onChange={(e) => { setKeyword(e.target.value); setPage(0); }} />
      <select className="input max-w-[160px]" value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
        <option value="publishedOn">Newest first</option>
        <option value="title">Title (A-Z)</option>
      </select>
    </div>
  );

  // Shared by the desktop content column and, on mobile, by whichever category row is currently
  // selected - both just reflect the same `data`/`loading` for the active categoryId.
  const results = loading ? (
    <Spinner />
  ) : (
    <>
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
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
      {data && <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />}
    </>
  );

  // Below md, results render inline under whichever category row is selected instead of in the
  // side-by-side content column - otherwise the full tree (with every branch open by default)
  // pushes the actual reports below the fold, past every category, before a member sees any.
  const mobileResults = (
    <div className="mt-2 space-y-4 border-t border-slate-100 pt-3 md:hidden">{results}</div>
  );

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Reports</h1>

      <div className="md:hidden">{filters}</div>

      <div className="flex flex-col gap-4 md:flex-row">
        <nav className="card shrink-0 space-y-0.5 p-3 md:w-64">
          <div>
            <CategoryRow
              label={`All Reports (${categories.reduce((a, c) => a + c.documentCount, 0)})`}
              selected={!categoryId}
              onClick={() => handleCategoryClick("")}
            />
            {mobileOpen === "" && mobileResults}
          </div>
          {topLevelCategories.map((parent) => {
            const children = childrenByParent.get(parent.id) || [];
            const isExpanded = expanded.has(parent.id);
            return (
              <div key={parent.id}>
                <div className="flex items-center">
                  {children.length > 0 ? (
                    <button
                      className="w-6 shrink-0 text-slate-400 hover:text-slate-600"
                      onClick={() => toggleExpanded(parent.id)}
                      aria-label={isExpanded ? "Collapse" : "Expand"}
                    >
                      {isExpanded ? "▾" : "▸"}
                    </button>
                  ) : (
                    <span className="w-6 shrink-0" />
                  )}
                  <CategoryRow
                    label={`${parent.name} (${parent.documentCount})`}
                    selected={categoryId === parent.id}
                    onClick={() => handleCategoryClick(parent.id)}
                    className="flex-1"
                  />
                </div>
                {mobileOpen === parent.id && <div className="ml-6">{mobileResults}</div>}
                {children.length > 0 && isExpanded && (
                  <div className="ml-6 space-y-0.5 border-l border-slate-200 pl-2">
                    {children.map((child) => (
                      <div key={child.id}>
                        <CategoryRow
                          label={`${child.name} (${child.documentCount})`}
                          selected={categoryId === child.id}
                          onClick={() => handleCategoryClick(child.id)}
                        />
                        {mobileOpen === child.id && mobileResults}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </nav>

        <div className="hidden min-w-0 flex-1 space-y-4 md:block">
          {filters}
          {results}
        </div>
      </div>
    </div>
  );
}

function CategoryRow({
  label,
  selected,
  onClick,
  className = "",
}: {
  label: string;
  selected: boolean;
  onClick: () => void;
  className?: string;
}) {
  return (
    <button
      className={`w-full rounded-md px-2 py-1.5 text-left text-sm ${
        selected ? "bg-brand-50 font-medium text-brand-700" : "text-slate-600 hover:bg-slate-100"
      } ${className}`}
      onClick={onClick}
    >
      {label}
    </button>
  );
}
