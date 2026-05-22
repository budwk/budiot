import type { TreeDataNode } from 'antd';
import dayjs from 'dayjs';
import type {
  IotCategoryRecord,
  IotOption,
  IotOptionValue,
} from '@/services/budiot/iot/typing';

export const resolveIotOptionValue = (value?: IotOptionValue) =>
  typeof value === 'string' ? value : value?.value || '';

export const resolveIotOptionText = (options: IotOption[], value?: IotOptionValue) => {
  const currentValue = resolveIotOptionValue(value);
  return options.find((item) => item.value === currentValue)?.text || currentValue || '-';
};

export const toSelectOptions = (options: IotOption[]) =>
  options.map((item) => ({
    label: item.text,
    value: item.value,
  }));

export const formatDateTime = (value?: number | string | null) =>
  value ? dayjs(Number(value)).format('YYYY-MM-DD HH:mm:ss') : '-';

export const safeJsonParse = <T,>(value: string | undefined, fallback: T): T => {
  if (!value) {
    return fallback;
  }
  try {
    return JSON.parse(value) as T;
  } catch {
    return fallback;
  }
};

export const safeJsonStringify = (value: unknown) => {
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return '';
  }
};

export const toIotTreeSelectData = (items: IotCategoryRecord[]): TreeDataNode[] =>
  items.map((item) => ({
    key: item.id,
    value: item.id,
    title: item.name,
    parentId: item.parentId,
    children: item.children ? toIotTreeSelectData(item.children) : undefined,
  }));

export const moveIotTreeNode = (
  tree: TreeDataNode[],
  dragKey: React.Key,
  dropKey: React.Key,
  position: 'before' | 'after',
) => {
  const clone = JSON.parse(JSON.stringify(tree)) as TreeDataNode[];
  let draggedNode: TreeDataNode | undefined;

  const traverse = (
    items: TreeDataNode[],
    key: React.Key,
    callback: (item: TreeDataNode, index: number, arr: TreeDataNode[]) => void,
  ) => {
    items.forEach((item, index, arr) => {
      if (item.key === key) {
        callback(item, index, arr);
        return;
      }
      if (item.children) {
        traverse(item.children, key, callback);
      }
    });
  };

  traverse(clone, dragKey, (item, index, arr) => {
    draggedNode = item;
    arr.splice(index, 1);
  });

  if (!draggedNode) {
    return clone;
  }

  traverse(clone, dropKey, (_item, index, arr) => {
    if (!draggedNode) {
      return;
    }
    arr.splice(position === 'before' ? index : index + 1, 0, draggedNode);
  });

  return clone;
};

export const flattenIotTreeIds = (items: TreeDataNode[], acc: string[] = []) => {
  items.forEach((item) => {
    acc.push(String(item.key));
    if (item.children?.length) {
      flattenIotTreeIds(item.children, acc);
    }
  });
  return acc;
};

export const runtimeStatusColorMap: Record<string, string> = {
  ONLINE: 'success',
  RUNNING: 'success',
  OFFLINE: 'default',
  STOPPED: 'default',
  SUSPENDED: 'warning',
  FAIL: 'error',
  ERROR: 'error',
};
