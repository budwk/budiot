import {
  BellOutlined,
  LockOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { PageContainer, ProCard, StatisticCard } from '@ant-design/pro-components';
import { history, useModel } from '@umijs/max';
import { Button, Empty, Space, Tag, Typography } from 'antd';
import * as React from 'react';

const collectQuickEntries = (menus: any[] = [], acc: any[] = []) => {
  menus.forEach((item) => {
    if (item?.path?.startsWith('/platform/')) {
      acc.push(item);
    }
    if (item?.routes?.length) {
      collectQuickEntries(item.routes, acc);
    }
  });
  return acc;
};

const DashboardPage = () => {
  const { initialState } = useModel('@@initialState');
  const quickEntries = collectQuickEntries(initialState?.menuData)
    .filter((item) => item.path !== '/platform/dashboard')
    .slice(0, 8);

  return (
    <PageContainer
      title="控制台"
      content={
        <>
          欢迎回来，{initialState?.currentUser?.name || '-'}。
        </>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <StatisticCard.Group>
          <StatisticCard
            statistic={{
              title: '当前用户',
              value: initialState?.currentUser?.name || '-',
              icon: <UserOutlined />,
            }}
          />
          <StatisticCard
            statistic={{
              title: '角色数量',
              value: initialState?.roles?.length || 0,
              icon: <LockOutlined />,
            }}
          />
          <StatisticCard
            statistic={{
              title: '权限数量',
              value: initialState?.permissions?.length || 0,
              icon: <BellOutlined />,
            }}
          />
        </StatisticCard.Group>

        <ProCard split="vertical">
          <ProCard title="平台概览" colSpan="55%">
            <Typography.Paragraph>
              平台名称：
              <Typography.Text strong>
                {initialState?.platformInfo?.AppName || '-'}
              </Typography.Text>
            </Typography.Paragraph>
            <Typography.Paragraph>
              当前版本：
              <Typography.Text code>
                {initialState?.platformInfo?.AppVersion || '-'}
              </Typography.Text>
            </Typography.Paragraph>
          </ProCard>
        </ProCard>
      </div>
    </PageContainer>
  );
};

export default DashboardPage;
