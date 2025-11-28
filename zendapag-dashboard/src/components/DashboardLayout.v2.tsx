// @ts-nocheck
import React, { useState, useEffect } from 'react';
import { Layout, Menu, Avatar, Dropdown, Typography, Badge } from 'antd';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  BellOutlined,
  UserOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import { useAuthStore } from '@/store/authStore';
import { themeSelectors } from '@/store/themeStore';
import { navigationMenu } from '@/config/routes';

const { Header, Content, Sider } = Layout;
const { Text } = Typography;

/**
 * Convert lucide-react icons to Ant Design compatible format
 */
const LucideIconWrapper = ({ icon: Icon, ...props }) => {
  if (!Icon) return null;
  return <Icon size={16} {...props} />;
};

const DashboardLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);
  const { user, logout } = useAuthStore();
  const darkMode = themeSelectors.useDarkMode();

  // Apply dark theme to document
  useEffect(() => {
    if (darkMode) {
      document.documentElement.setAttribute('data-theme', 'dark');
    } else {
      document.documentElement.removeAttribute('data-theme');
    }
  }, [darkMode]);

  const handleMenuClick = (key: string) => {
    console.log('Menu clicked:', key);
    navigate(key);
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  // Build menu items from navigation config
  const menuItems = navigationMenu.map((section, index) => ({
    type: 'group',
    label: !collapsed ? section.label : '',
    key: `group-${index}`,
    children: section.items.map((item) => ({
      key: item.path,
      icon: <LucideIconWrapper icon={item.icon} />,
      label: item.name,
    })),
  }));

  // User dropdown menu
  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: 'Meu Perfil',
      onClick: () => navigate('/profile'),
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: 'Configurações',
      onClick: () => navigate('/settings'),
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Sair',
      onClick: handleLogout,
      danger: true,
    },
  ];

  return (
    <Layout style={{ minHeight: '100vh', background: darkMode ? '#000000' : '#f5f5f5' }}>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        style={{
          overflow: 'auto',
          height: '100vh',
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
          background: darkMode ? '#001529' : '#fff',
          boxShadow: darkMode ? 'none' : '2px 0 8px rgba(0, 0, 0, 0.05)',
        }}
      >
        {/* Logo */}
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: collapsed ? 16 : 20,
            fontWeight: 'bold',
            padding: '0 16px',
            borderBottom: darkMode ? '1px solid rgba(255, 255, 255, 0.1)' : '1px solid #f0f0f0',
            background: darkMode ? '#001529' : '#fff',
            color: darkMode ? '#fff' : '#0066FF',
          }}
        >
          {collapsed ? (
            <span style={{ fontSize: 24 }}>Z</span>
          ) : (
            <>
              <span style={{ color: '#0066FF' }}>Zenda</span>
              <span style={{ color: darkMode ? '#fff' : '#1F2937' }}>Pag</span>
            </>
          )}
        </div>

        {/* Navigation Menu */}
        <Menu
          theme={darkMode ? 'dark' : 'light'}
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => handleMenuClick(key)}
          style={{
            borderRight: 0,
            background: darkMode ? '#001529' : '#fff',
          }}
        />
      </Sider>

      <Layout style={{ marginLeft: collapsed ? 80 : 200, transition: 'all 0.2s' }}>
        {/* Header */}
        <Header
          style={{
            padding: '0 24px',
            background: darkMode ? '#001529' : '#fff',
            borderBottom: darkMode ? '1px solid rgba(255, 255, 255, 0.1)' : '1px solid #f0f0f0',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            position: 'sticky',
            top: 0,
            zIndex: 100,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            {/* Collapse trigger */}
            {React.createElement(collapsed ? MenuUnfoldOutlined : MenuFoldOutlined, {
              style: {
                fontSize: 18,
                color: darkMode ? '#fff' : '#1F2937',
                cursor: 'pointer',
              },
              onClick: () => setCollapsed(!collapsed),
            })}

            {/* Page title (optional) */}
            <Text
              strong
              style={{
                fontSize: 18,
                color: darkMode ? '#fff' : '#1F2937',
              }}
            >
              {/* You can make this dynamic based on current route */}
            </Text>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
            {/* Notifications */}
            <Badge count={3} size="small">
              <BellOutlined
                style={{
                  fontSize: 20,
                  color: darkMode ? '#fff' : '#6B7280',
                  cursor: 'pointer',
                }}
              />
            </Badge>

            {/* User menu */}
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight" arrow>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  cursor: 'pointer',
                  padding: '4px 8px',
                  borderRadius: 8,
                  transition: 'background 0.2s',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = darkMode
                    ? 'rgba(255, 255, 255, 0.1)'
                    : '#f5f5f5';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = 'transparent';
                }}
              >
                <Avatar
                  size={32}
                  style={{
                    background: '#0066FF',
                    color: '#fff',
                  }}
                >
                  {user?.name?.charAt(0)?.toUpperCase() || 'U'}
                </Avatar>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <Text
                    strong
                    style={{
                      fontSize: 14,
                      color: darkMode ? '#fff' : '#1F2937',
                      lineHeight: 1.2,
                    }}
                  >
                    {user?.name || 'Usuário'}
                  </Text>
                  <Text
                    type="secondary"
                    style={{
                      fontSize: 12,
                      color: darkMode ? '#9CA3AF' : '#6B7280',
                      lineHeight: 1.2,
                    }}
                  >
                    {user?.email || ''}
                  </Text>
                </div>
              </div>
            </Dropdown>
          </div>
        </Header>

        {/* Content */}
        <Content
          style={{
            margin: 0,
            padding: 0,
            minHeight: 280,
            background: darkMode ? '#000000' : '#f5f5f5',
          }}
        >
          <Outlet />
        </Content>

        {/* Footer (optional) */}
        <div
          style={{
            padding: '16px 24px',
            textAlign: 'center',
            background: darkMode ? '#001529' : '#fff',
            borderTop: darkMode ? '1px solid rgba(255, 255, 255, 0.1)' : '1px solid #f0f0f0',
          }}
        >
          <Text
            type="secondary"
            style={{
              fontSize: 12,
              color: darkMode ? '#9CA3AF' : '#6B7280',
            }}
          >
            © {new Date().getFullYear()} ZendaPag. Todos os direitos reservados.
          </Text>
        </div>
      </Layout>
    </Layout>
  );
};

export default DashboardLayout;
