export type AccountType = "USER" | "ADMIN";
export type AdminRole = "SUPER_ADMIN" | "ADMIN" | "UPLOADER";

export interface LoginResponse {
  token: string;
  id: string;
  name: string;
  email: string;
  accountType: AccountType;
  role: string; // SUPER_ADMIN / ADMIN / UPLOADER / MEMBER
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface UserSummary {
  id: string;
  name: string;
  flatNo: string;
  block: string;
  residentType: "OWNER" | "TENANT";
  mobile: string;
  email: string;
  status: "PENDING" | "ACTIVE" | "REJECTED" | "SUSPENDED" | "INFO_REQUESTED";
  registeredOn: string;
  proofFileUrl?: string;
  overdue: boolean;
  approvedByName?: string;
  approvedOn?: string;
  lastLogin?: string;
}

export interface CategoryResponse {
  id: string;
  type: string;
  name: string;
  parentId?: string;
  parentName?: string;
  displayOrder: number;
  active: boolean;
  documentCount: number;
}

export interface FileInfo {
  id: string;
  fileName: string;
  fileSize?: number;
  mimeType?: string;
  versionNo: number;
  current: boolean;
  uploadedAt: string;
  downloadUrl: string;
  previewUrl: string;
}

export interface DocumentListItem {
  id: string;
  contentType: string;
  title: string;
  categoryName?: string;
  subCategoryName?: string;
  status: string;
  visibilityType: string;
  pinned: boolean;
  tags?: string;
  publishedOn?: string;
  publishAt?: string;
  expiryAt?: string;
  createdAt?: string;
  fileCount: number;
  viewCount: number;
  downloadCount: number;
  unread: boolean;
  financialYear?: string;
  reportPeriod?: string;
  reportDate?: string;
  preparedBy?: string;
  noticeNumber?: string;
  priority?: string;
}

export interface DocumentDetail {
  id: string;
  contentType: string;
  title: string;
  categoryId?: string;
  categoryName?: string;
  subCategoryId?: string;
  subCategoryName?: string;
  description?: string;
  bodyHtml?: string;
  tags?: string;
  status: string;
  visibilityType: string;
  visibilityBlocks: string[];
  visibilityUserIds: string[];
  pinned: boolean;
  publishAt?: string;
  publishedOn?: string;
  expiryAt?: string;
  createdByName?: string;
  createdAt: string;
  updatedAt: string;
  viewCount: number;
  downloadCount: number;
  files: FileInfo[];
  financialYear?: string;
  reportPeriod?: string;
  reportDate?: string;
  preparedBy?: string;
  noticeNumber?: string;
  priority?: string;
  readCount?: number;
  unreadCount?: number;
}

export interface NotificationItem {
  id: string;
  type: string;
  title: string;
  body?: string;
  link?: string;
  read: boolean;
  createdAt: string;
}

export interface AdminDashboardResponse {
  pendingApprovals: number;
  totalActiveUsers: number;
  totalDocumentsPublished: number;
  uploadsThisMonth: number;
  noticesLive: number;
  recentPendingApprovals: UserSummary[];
  recentlyUploaded: {
    id: string;
    contentType: string;
    title: string;
    uploaderName: string;
    timestamp: string;
    status: string;
  }[];
  uploadsPerMonthByType: Record<string, Record<string, number>>;
  mostViewed: DocumentListItem[];
  alerts: { type: string; message: string; link: string }[];
}

export interface MemberDashboardResponse {
  memberName: string;
  flatNo: string;
  unreadNotificationCount: number;
  latestNotices: DocumentListItem[];
  recentReports: DocumentListItem[];
  pinnedNotice?: DocumentListItem;
}

export interface AdminSummary {
  id: string;
  name: string;
  email: string;
  mobile?: string;
  role: AdminRole;
  status: string;
  lastLogin?: string;
  createdAt: string;
}
