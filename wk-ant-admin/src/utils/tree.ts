export const buildTree = <T extends { id: string; parentId?: string; children?: T[] }>(
  items: T[],
) => {
  const nodeMap = new Map<string, T>();
  const roots: T[] = [];

  items.forEach((item) => {
    nodeMap.set(item.id, { ...item, children: [] });
  });

  nodeMap.forEach((item) => {
    if (item.parentId && nodeMap.has(item.parentId)) {
      nodeMap.get(item.parentId)?.children?.push(item);
      return;
    }
    roots.push(item);
  });

  return roots;
};

export const collectTreeKeys = <
  T extends { id: string; children?: T[] },
>(
  items: T[],
  keys: React.Key[] = [],
) => {
  items.forEach((item) => {
    keys.push(item.id);
    if (item.children?.length) {
      collectTreeKeys(item.children, keys);
    }
  });
  return keys;
};
