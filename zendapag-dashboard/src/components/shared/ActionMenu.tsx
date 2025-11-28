// @ts-nocheck
import React from 'react';
import { Dropdown, Button, Space } from 'antd';
import { MoreOutlined, DownOutlined } from '@ant-design/icons';
import type { MenuProps } from 'antd';

export interface ActionMenuItem {
  key: string;
  label: React.ReactNode;
  icon?: React.ReactNode;
  danger?: boolean;
  disabled?: boolean;
  onClick?: () => void;
  divider?: boolean;
}

interface ActionMenuProps {
  items: ActionMenuItem[];
  trigger?: ('click' | 'hover' | 'contextMenu')[];
  placement?: 'topLeft' | 'topCenter' | 'topRight' | 'bottomLeft' | 'bottomCenter' | 'bottomRight';
  buttonType?: 'icon' | 'text' | 'dropdown';
  buttonText?: string;
  size?: 'small' | 'middle' | 'large';
  disabled?: boolean;
}

const ActionMenu: React.FC<ActionMenuProps> = ({
  items,
  trigger = ['click'],
  placement = 'bottomRight',
  buttonType = 'icon',
  buttonText = 'Ações',
  size = 'small',
  disabled = false,
}) => {
  const menuItems: MenuProps['items'] = items.map((item) => {
    if (item.divider) {
      return { type: 'divider', key: item.key };
    }

    return {
      key: item.key,
      label: item.label,
      icon: item.icon,
      danger: item.danger,
      disabled: item.disabled,
      onClick: item.onClick,
    };
  });

  const renderButton = () => {
    switch (buttonType) {
      case 'icon':
        return (
          <Button
            type="text"
            size={size}
            icon={<MoreOutlined />}
            disabled={disabled}
          />
        );
      case 'text':
        return (
          <Button type="link" size={size} disabled={disabled}>
            <Space>
              {buttonText}
              <DownOutlined />
            </Space>
          </Button>
        );
      case 'dropdown':
        return (
          <Button size={size} disabled={disabled}>
            <Space>
              {buttonText}
              <DownOutlined />
            </Space>
          </Button>
        );
      default:
        return (
          <Button
            type="text"
            size={size}
            icon={<MoreOutlined />}
            disabled={disabled}
          />
        );
    }
  };

  return (
    <Dropdown
      menu={{ items: menuItems }}
      trigger={trigger}
      placement={placement}
      disabled={disabled}
    >
      {renderButton()}
    </Dropdown>
  );
};

export default ActionMenu;
