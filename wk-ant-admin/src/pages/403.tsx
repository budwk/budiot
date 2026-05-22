import { Result } from 'antd';

const ForbiddenPage = () => {
  return <Result status="403" title="403" subTitle="暂无访问权限" />;
};

export default ForbiddenPage;
