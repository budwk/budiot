import { MoreOutlined } from '@ant-design/icons';
import { Button, Dropdown } from 'antd';
import type { ItemType } from 'antd/es/menu/interface';
import * as React from 'react';

export type TableRowActionItem = {
  key: React.Key;
  label: React.ReactNode;
  icon?: React.ReactNode;
  danger?: boolean;
  disabled?: boolean;
  hidden?: boolean;
  onClick?: () => void | Promise<void>;
};

type TableRowActionsProps = {
  actions: TableRowActionItem[];
  maxVisible?: number;
};

const TableRowActions = ({
  actions,
  maxVisible = 2,
}: TableRowActionsProps) => {
  const visibleActions = actions.filter((item) => !item.hidden);
  const primaryActions = visibleActions.slice(0, maxVisible);
  const overflowActions = visibleActions.slice(maxVisible);

  const menuItems: ItemType[] = overflowActions.map((item) => ({
    key: String(item.key),
    label: item.label,
    icon: item.icon,
    danger: item.danger,
    disabled: item.disabled,
    onClick: item.onClick,
  }));

  return (
    <div
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 0,
        flexWrap: 'nowrap',
        whiteSpace: 'nowrap',
      }}
    >
      {primaryActions.map((item) => (
        <Button
          key={item.key}
          type="link"
          size="small"
          icon={item.icon}
          danger={item.danger}
          disabled={item.disabled}
          onClick={item.onClick}
        >
          {item.label}
        </Button>
      ))}
      {overflowActions.length ? (
        <Dropdown menu={{ items: menuItems }} trigger={['click']}>
          <Button type="link" size="small" icon={<MoreOutlined />}>
            更多
          </Button>
        </Dropdown>
      ) : null}
    </div>
  );
};

export default TableRowActions;
