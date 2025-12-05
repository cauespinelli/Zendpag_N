// @ts-nocheck
import React from 'react';
import { Dropdown, Button, Menu } from 'antd';
import { MoreOutlined, EllipsisOutlined } from '@ant-design/icons';

export interface ActionMenuItem {
  key: string;
  label: React.ReactNode;
  icon?: React.ReactNode;
  danger?: boolean;
  disabled?: boolean;
  onClick?: () => void;
}

interface ActionMenuProps {
  items: ActionMenuItem[];
  trigger?: ('click' | 'hover' | 'contextMenu')[];
  placement?: 'topLeft' | 'topCenter' | 'topRight' | 'bottomLeft' | 'bottomCenter' | 'bottomRight';
  icon?: React.ReactNode;
}

const ActionMenu: React.FC<ActionMenuProps> = ({
  items,
  trigger = ['click'],
  placement = 'bottomRight',
  icon,
}) => {
  const menuItems = items.map((item) => ({
    key: item.key,
    label: item.label,
    icon: item.icon,
    danger: item.danger,
    disabled: item.disabled,
    onClick: item.onClick,
  }));

  return (
    <Dropdown
      menu={{ items: menuItems }}
      trigger={trigger}
      placement={placement}
    >
      <Button type="text" icon={icon || <EllipsisOutlined />} />
    </Dropdown>
  );
};

export default ActionMenu;
