import { InfoCircleOutlined } from '@ant-design/icons';
import { PageContainer, ProCard } from '@ant-design/pro-components';
import { useLocation, useModel } from '@umijs/max';
import { Alert, Empty, Space, Tag, Typography } from 'antd';
import * as React from 'react';
import { findRouteDefinition } from '@/constants/routeManifest';
import { getRuntimeRouteMeta } from '@/utils/menu';

const RuntimeShellPage = () => {
  const location = useLocation();
  const { initialState } = useModel('@@initialState');
  const routeMeta = getRuntimeRouteMeta(
    location.pathname,
    initialState?.routeMetaMap,
  );
  const routeDefinition = findRouteDefinition(location.pathname);
  const title = routeMeta?.title || routeDefinition?.title || '页面';
  const breadcrumb = routeMeta?.breadcrumb || [title];

  return (
    <PageContainer
      title={title}
      subTitle={location.pathname}
      content={
        <Space wrap size={[8, 8]}>
          {breadcrumb.map((item) => (
            <Tag key={item}>{item}</Tag>
          ))}
          {routeMeta?.permission ? (
            <Tag color="processing">{routeMeta.permission}</Tag>
          ) : null}
        </Space>
      }
    >
      <Alert
        type="info"
        showIcon
        icon={<InfoCircleOutlined />}
        title="当前页面"
      />
      <ProCard style={{ marginTop: 16 }}>
        <Typography.Paragraph>
          当前路径 <Typography.Text code>{location.pathname}</Typography.Text>{' '}
          已纳入系统路由，并由后端菜单控制展示与访问。
        </Typography.Paragraph>
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无内容" />
      </ProCard>
    </PageContainer>
  );
};

export default RuntimeShellPage;
