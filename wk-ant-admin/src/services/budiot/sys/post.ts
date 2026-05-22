import type { PagedList, SysPostListRecord } from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_SYS_POST_LIST = '/platform/sys/post/list';
const API_SYS_POST_GET = '/platform/sys/post/get/';
const API_SYS_POST_CREATE = '/platform/sys/post/create';
const API_SYS_POST_DELETE = '/platform/sys/post/delete/';
const API_SYS_POST_UPDATE = '/platform/sys/post/update';
const API_SYS_POST_LOCATION = '/platform/sys/post/location';

export const getPostPage = async (data: Record<string, unknown>) =>
  request<PagedList<SysPostListRecord>>(API_SYS_POST_LIST, {
    method: 'POST',
    data,
  });

export const getPostDetail = async (id: string) =>
  request<SysPostListRecord>(`${API_SYS_POST_GET}${id}`, {
    method: 'GET',
  });

export const createPost = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_POST_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updatePost = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_POST_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deletePost = async (id: string) =>
  request<unknown>(`${API_SYS_POST_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const updatePostLocation = async (data: {
  id: string;
  location: number;
}) =>
  request<unknown>(API_SYS_POST_LOCATION, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

