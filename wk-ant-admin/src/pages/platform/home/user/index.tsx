import {
  MailOutlined,
  PhoneOutlined,
  SaveOutlined,
  TeamOutlined,
  UploadOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { PageContainer, ProCard } from '@ant-design/pro-components';
import { history, useLocation, useModel } from '@umijs/max';
import {
  App,
  Avatar,
  Button,
  Col,
  Form,
  Input,
  Pagination,
  Radio,
  Row,
  Space,
  Tabs,
  Tag,
  Timeline,
  Typography,
  Upload,
} from 'antd';
import type { TabsProps, UploadFile } from 'antd';
import { createStyles } from 'antd-style';
import dayjs from 'dayjs';
import * as React from 'react';
import {
  changeHomeUserAvatar,
  changeHomeUserInfo,
  changeHomeUserPassword,
  getHomeUserInfo,
  getHomeUserLogPage,
} from '@/services/budiot/home/user';
import type {
  BudiotAuthInfo,
  HomeUserInfo,
  SysLogRecord,
} from '@/services/budiot/typing';
import { uploadFile } from '@/utils/file';
import { setStoredAuthInfo } from '@/utils/session';

const useStyles = createStyles(({ token }) => ({
  profileCard: {
    '.ant-pro-card-body': {
      paddingInline: 0,
    },
  },
  profileHeader: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 12,
    paddingBottom: 20,
    marginBottom: 20,
    borderBottom: `${token.lineWidth}px solid ${token.colorSplit}`,
  },
  profileList: {
    display: 'flex',
    flexDirection: 'column',
    gap: 12,
    paddingInline: 24,
  },
  profileItem: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
    color: token.colorText,
  },
  profileItemLabel: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: 8,
    color: token.colorTextSecondary,
  },
  formCard: {
    maxWidth: 640,
  },
  logHint: {
    marginBottom: 16,
  },
}));

type UserCenterTab = 'userinfo' | 'avatar' | 'password' | 'log';

type ResetPasswordValues = {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
};

type UserInfoValues = {
  id?: string;
  username: string;
  email?: string;
  mobile?: string;
  sex?: number;
  avatar?: string;
};

const getTabFromSearch = (search: string): UserCenterTab => {
  const value = new URLSearchParams(search).get('tab');
  if (
    value === 'avatar' ||
    value === 'password' ||
    value === 'log' ||
    value === 'userinfo'
  ) {
    return value;
  }
  return 'userinfo';
};

const UserHomePage = () => {
  const { styles } = useStyles();
  const { message } = App.useApp();
  const location = useLocation();
  const { initialState, setInitialState } = useModel('@@initialState');
  const [userForm] = Form.useForm<UserInfoValues>();
  const [passwordForm] = Form.useForm<ResetPasswordValues>();
  const [submittingInfo, setSubmittingInfo] = React.useState(false);
  const [submittingPassword, setSubmittingPassword] = React.useState(false);
  const [uploading, setUploading] = React.useState(false);
  const [userLoading, setUserLoading] = React.useState(false);
  const [logLoading, setLogLoading] = React.useState(false);
  const [fileList, setFileList] = React.useState<UploadFile[]>([]);
  const [userInfo, setUserInfo] = React.useState<HomeUserInfo>();
  const [pageNo, setPageNo] = React.useState(1);
  const [pageSize, setPageSize] = React.useState(10);
  const [total, setTotal] = React.useState(0);
  const [logData, setLogData] = React.useState<SysLogRecord[]>([]);
  const currentTab = getTabFromSearch(location.search);
  const fileDomain = initialState?.platformInfo?.AppFileDomain || '';
  const avatarUrl = userInfo?.avatar ? `${fileDomain}${userInfo.avatar}` : undefined;

  const switchTab = React.useCallback(
    (tab: UserCenterTab) => {
      const searchParams = new URLSearchParams(location.search);
      if (tab === 'userinfo') {
        searchParams.delete('tab');
      } else {
        searchParams.set('tab', tab);
      }
      history.push(
        `/platform/home/user${
          searchParams.toString() ? `?${searchParams.toString()}` : ''
        }`,
      );
    },
    [location.search],
  );

  const syncCurrentUser = React.useCallback(
    (nextUser: Partial<HomeUserInfo>) => {
      const authInfo = initialState?.authInfo;
      if (!authInfo) {
        return;
      }
      const nextAuthInfo: BudiotAuthInfo = {
        ...authInfo,
        user: {
          ...authInfo.user,
          avatar: nextUser.avatar ?? authInfo.user.avatar,
          username: nextUser.username ?? authInfo.user.username,
          email: nextUser.email ?? authInfo.user.email,
          mobile: nextUser.mobile ?? authInfo.user.mobile,
        },
      };
      setStoredAuthInfo(nextAuthInfo);
      setInitialState((prev) =>
        prev
          ? {
              ...prev,
              authInfo: nextAuthInfo,
              currentUser: prev.currentUser
                ? {
                    ...prev.currentUser,
                    name:
                      nextUser.username ?? prev.currentUser.name,
                    username:
                      nextUser.username ?? prev.currentUser.username,
                    avatar: nextUser.avatar ?? prev.currentUser.avatar,
                    email: nextUser.email ?? prev.currentUser.email,
                    mobile: nextUser.mobile ?? prev.currentUser.mobile,
                  }
                : prev.currentUser,
            }
          : prev,
      );
    },
    [initialState?.authInfo, setInitialState],
  );

  const loadUserInfo = React.useCallback(async () => {
    setUserLoading(true);
    try {
      const response = await getHomeUserInfo();
      const nextUser = response.data;
      setUserInfo(nextUser);
      userForm.setFieldsValue({
        id: nextUser.id,
        username: nextUser.username,
        email: nextUser.email,
        mobile: nextUser.mobile,
        sex: nextUser.sex ?? 0,
        avatar: nextUser.avatar,
      });
    } finally {
      setUserLoading(false);
    }
  }, [userForm]);

  const loadLogs = React.useCallback(
    async (nextPageNo = 1, nextPageSize = pageSize) => {
      setLogLoading(true);
      try {
        const response = await getHomeUserLogPage({
          pageNo: nextPageNo,
          pageSize: nextPageSize,
          pageOrderName: 'createdAt',
          pageOrderBy: 'descending',
        });
        setLogData(response.data.list || []);
        setTotal(response.data.totalCount || 0);
      } finally {
        setLogLoading(false);
      }
    },
    [pageSize],
  );

  React.useEffect(() => {
    void loadUserInfo();
  }, [loadUserInfo]);

  React.useEffect(() => {
    if (currentTab === 'log') {
      void loadLogs(1, pageSize);
      setPageNo(1);
    }
  }, [currentTab, loadLogs, pageSize]);

  const submitUserInfo = async () => {
    const values = await userForm.validateFields();
    setSubmittingInfo(true);
    try {
      await changeHomeUserInfo(values);
      setUserInfo((prev) =>
        prev
          ? {
              ...prev,
              ...values,
            }
          : prev,
      );
      syncCurrentUser(values);
    } finally {
      setSubmittingInfo(false);
    }
  };

  const submitPassword = async () => {
    const values = await passwordForm.validateFields();
    setSubmittingPassword(true);
    try {
      await changeHomeUserPassword(values.oldPassword, values.newPassword);
      passwordForm.resetFields();
    } finally {
      setSubmittingPassword(false);
    }
  };

  const persistAvatar = async (nextAvatar: string) => {
    await changeHomeUserAvatar(nextAvatar);
    setUserInfo((prev) =>
      prev
        ? {
            ...prev,
            avatar: nextAvatar,
          }
        : prev,
    );
    syncCurrentUser({ avatar: nextAvatar });
    setFileList([
      {
        uid: `${Date.now()}`,
        name: 'avatar',
        status: 'done',
        url: `${fileDomain}${nextAvatar}`,
      },
    ]);
  };

  const tabItems: TabsProps['items'] = [
    {
      key: 'log',
      label: '操作日志',
      children: (
        <div>
          <Tag color="blue" className={styles.logHint}>
            仅显示最近两个月
          </Tag>
          <Timeline
            items={logData.map((item) => ({
              color: item.exception ? 'red' : 'blue',
              children: (
                <div>
                  <Typography.Text delete={Boolean(item.exception)}>
                    {item.msg || '-'}
                  </Typography.Text>
                  {item.tag ? (
                    <Tag style={{ marginInlineStart: 8 }}>{item.tag}</Tag>
                  ) : null}
                  <div style={{ marginTop: 4, color: 'rgba(0,0,0,0.45)' }}>
                    {item.createdAt
                      ? dayjs(item.createdAt).format('YYYY-MM-DD HH:mm:ss')
                      : '-'}
                  </div>
                </div>
              ),
            }))}
          />
          <Pagination
            current={pageNo}
            pageSize={pageSize}
            total={total}
            showSizeChanger
            onChange={async (nextPage, nextPageSize) => {
              setPageNo(nextPage);
              setPageSize(nextPageSize);
              await loadLogs(nextPage, nextPageSize);
            }}
          />
          {logLoading ? (
            <Typography.Text type="secondary">加载中...</Typography.Text>
          ) : null}
        </div>
      ),
    },
    {
      key: 'userinfo',
      label: '修改资料',
      forceRender: true,
      children: (
        <div className={styles.formCard}>
          <Form form={userForm} layout="vertical">
            <Form.Item name="id" hidden>
              <Input />
            </Form.Item>
            <Form.Item
              label="用户姓名"
              name="username"
              rules={[{ required: true, message: '请输入用户姓名' }]}
            >
              <Input placeholder="请输入用户姓名" />
            </Form.Item>
            <Form.Item label="Email" name="email">
              <Input placeholder="请输入 Email" />
            </Form.Item>
            <Form.Item label="手机号码" name="mobile">
              <Input placeholder="请输入手机号码" />
            </Form.Item>
            <Form.Item label="性别" name="sex">
              <Radio.Group>
                <Radio value={1}>男</Radio>
                <Radio value={2}>女</Radio>
                <Radio value={0}>未知</Radio>
              </Radio.Group>
            </Form.Item>
            <Space>
              <Button
                type="primary"
                icon={<SaveOutlined />}
                loading={submittingInfo}
                onClick={submitUserInfo}
              >
                保存
              </Button>
            </Space>
          </Form>
        </div>
      ),
    },
    {
      key: 'password',
      label: '修改密码',
      forceRender: true,
      children: (
        <div className={styles.formCard}>
          <Form form={passwordForm} layout="vertical">
            <Form.Item
              label="旧密码"
              name="oldPassword"
              rules={[{ required: true, message: '请输入旧密码' }]}
            >
              <Input.Password placeholder="请输入旧密码" />
            </Form.Item>
            <Form.Item
              label="新密码"
              name="newPassword"
              rules={[{ required: true, message: '请输入新密码' }]}
            >
              <Input.Password placeholder="请输入新密码" />
            </Form.Item>
            <Form.Item
              label="确认密码"
              name="confirmPassword"
              dependencies={['newPassword']}
              rules={[
                { required: true, message: '请确认新密码' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!value || getFieldValue('newPassword') === value) {
                      return Promise.resolve();
                    }
                    return Promise.reject(
                      new Error('两次输入的密码不一致'),
                    );
                  },
                }),
              ]}
            >
              <Input.Password placeholder="请确认新密码" />
            </Form.Item>
            <Button
              type="primary"
              icon={<SaveOutlined />}
              loading={submittingPassword}
              onClick={submitPassword}
            >
              保存
            </Button>
          </Form>
        </div>
      ),
    },
    {
      key: 'avatar',
      label: '修改头像',
      children: (
        <div>
          <Upload
            accept=".png,.jpg,.jpeg,.gif,.bmp,.svg"
            showUploadList={
              fileList.length > 0 ? { showRemoveIcon: false } : false
            }
            fileList={fileList}
            beforeUpload={async (file) => {
              setUploading(true);
              try {
                const formData = new FormData();
                formData.append('Filedata', file);
                const response = await uploadFile(formData, { type: 'image' });
                const nextUrl =
                  (response.data as { url?: string })?.url || '';
                if (!nextUrl) {
                  throw new Error('头像上传返回为空');
                }
                await persistAvatar(nextUrl);
                message.success('头像已更新');
              } finally {
                setUploading(false);
              }
              return false;
            }}
          >
            <Button icon={<UploadOutlined />} loading={uploading}>
              选择并上传头像
            </Button>
          </Upload>
        </div>
      ),
    },
  ];

  const roleNames = (userInfo?.roles || []).map((item) => item.name).join(', ');
  const postName =
    typeof userInfo?.post === 'string'
      ? userInfo.post
      : userInfo?.post?.name;

  return (
    <PageContainer title={false}>
      <Row gutter={[24, 24]}>
        <Col xs={24} lg={6}>
          <ProCard
            title="个人资料"
            loading={userLoading}
            className={styles.profileCard}
          >
            <div className={styles.profileHeader}>
              <Avatar size={96} src={avatarUrl}>
                {userInfo?.username?.[0]}
              </Avatar>
              <Typography.Title level={4} style={{ margin: 0 }}>
                {userInfo?.username || '-'}
              </Typography.Title>
              <Tag color="blue">{userInfo?.loginname || '-'}</Tag>
            </div>
            <div className={styles.profileList}>
              <div className={styles.profileItem}>
                <span className={styles.profileItemLabel}>
                  <UserOutlined />
                  用户名称
                </span>
                <span>{userInfo?.username || '-'}</span>
              </div>
              <div className={styles.profileItem}>
                <span className={styles.profileItemLabel}>
                  <PhoneOutlined />
                  手机号码
                </span>
                <span>{userInfo?.mobile || '-'}</span>
              </div>
              <div className={styles.profileItem}>
                <span className={styles.profileItemLabel}>
                  <MailOutlined />
                  用户邮箱
                </span>
                <span>{userInfo?.email || '-'}</span>
              </div>
              <div className={styles.profileItem}>
                <span className={styles.profileItemLabel}>
                  <TeamOutlined />
                  所属部门
                </span>
                <span>
                  {postName ? `${postName} / ` : ''}
                  {userInfo?.unit?.name || '-'}
                </span>
              </div>
              <div className={styles.profileItem}>
                <span className={styles.profileItemLabel}>
                  <TeamOutlined />
                  所属角色
                </span>
                <span>{roleNames || '-'}</span>
              </div>
              <div className={styles.profileItem}>
                <span className={styles.profileItemLabel}>
                  <UserOutlined />
                  最后登录IP
                </span>
                <span>{initialState?.currentUser?.loginIp || '-'}</span>
              </div>
              <div className={styles.profileItem}>
                <span className={styles.profileItemLabel}>
                  <UserOutlined />
                  创建时间
                </span>
                <span>
                  {userInfo?.createdAt
                    ? dayjs(userInfo.createdAt).format('YYYY-MM-DD HH:mm:ss')
                    : '-'}
                </span>
              </div>
            </div>
          </ProCard>
        </Col>
        <Col xs={24} lg={18}>
          <ProCard title="用户信息">
            <Tabs
              activeKey={currentTab}
              items={tabItems}
              onChange={(key) => switchTab(key as UserCenterTab)}
            />
          </ProCard>
        </Col>
      </Row>
    </PageContainer>
  );
};

export default UserHomePage;
