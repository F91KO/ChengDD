export interface PageResponseRaw<T> {
  list: T[];
  page: number;
  page_size: number;
  total: number;
}
