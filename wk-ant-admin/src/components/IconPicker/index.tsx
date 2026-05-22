import * as AntIcons from '@ant-design/icons';
import { AppstoreOutlined } from '@ant-design/icons';
import type { InputProps } from 'antd';
import { Button, Empty, Input, Popover, Space } from 'antd';
import * as React from 'react';

const legacySvgIcons = [
  '404',
  'bell',
  'bug',
  'build',
  'button',
  'cascader',
  'chart',
  'checkbox',
  'clipboard',
  'code',
  'color',
  'component',
  'dark',
  'dashboard',
  'date',
  'date-range',
  'dict',
  'documentation',
  'download',
  'drag',
  'druid',
  'edit',
  'education',
  'email',
  'example',
  'excel',
  'eye',
  'eye-open',
  'fontsize',
  'form',
  'full-screen',
  'full-screen-cancel',
  'github',
  'guide',
  'icon',
  'input',
  'international',
  'job',
  'lang',
  'language',
  'light',
  'line-md',
  'link',
  'list',
  'lock',
  'log',
  'logininfor',
  'message',
  'money',
  'monitor',
  'nested',
  'number',
  'online',
  'password',
  'pdf',
  'people',
  'peoples',
  'phone',
  'post',
  'qq',
  'question',
  'radio',
  'rate',
  'redis',
  'redis-list',
  'reload',
  'row',
  'search',
  'select',
  'server',
  'settings',
  'shopping',
  'skill',
  'slider',
  'star',
  'swagger',
  'switch',
  'system',
  'tab',
  'table',
  'terminal',
  'textarea',
  'theme',
  'time',
  'time-range',
  'tool',
  'tree',
  'tree-table',
  'upload',
  'user',
  'validCode',
  'wechat',
  'zip',
] as const;

const iconComponentMap: Record<string, string> = {
  dashboard: 'DashboardOutlined',
  bell: 'BellOutlined',
  user: 'UserOutlined',
  team: 'TeamOutlined',
  safetyCertificate: 'SafetyCertificateOutlined',
  menu: 'MenuOutlined',
  apartment: 'ApartmentOutlined',
  idcard: 'IdcardOutlined',
  book: 'BookOutlined',
  environment: 'EnvironmentOutlined',
  safety: 'SafetyOutlined',
  fileText: 'FileTextOutlined',
  schedule: 'ScheduleOutlined',
  cluster: 'ClusterOutlined',
  appstore: 'AppstoreOutlined',
  message: 'MessageOutlined',
  monitor: 'MonitorOutlined',
  key: 'KeyOutlined',
  experiment: 'ExperimentOutlined',
  mail: 'MailOutlined',
  api: 'ApiOutlined',
  send: 'SendOutlined',
  history: 'HistoryOutlined',
  folderOpen: 'FolderOpenOutlined',
  global: 'GlobalOutlined',
  bars: 'BarsOutlined',
  read: 'ReadOutlined',
  link: 'LinkOutlined',
  deploymentUnit: 'DeploymentUnitOutlined',
  tablet: 'TabletOutlined',
  branches: 'BranchesOutlined',
  partition: 'PartitionOutlined',
  gateway: 'GatewayOutlined',
  cloudServer: 'CloudServerOutlined',
  home: 'HomeOutlined',
  lock: 'LockOutlined',
  unlock: 'UnlockOutlined',
  tool: 'ToolOutlined',
  setting: 'SettingOutlined',
  settings: 'SettingOutlined',
  search: 'SearchOutlined',
  reload: 'ReloadOutlined',
  download: 'DownloadOutlined',
  upload: 'UploadOutlined',
  edit: 'EditOutlined',
  delete: 'DeleteOutlined',
  plus: 'PlusOutlined',
  minus: 'MinusOutlined',
  eye: 'EyeOutlined',
  eyeOpen: 'EyeOutlined',
  file: 'FileOutlined',
  folder: 'FolderOutlined',
  cloudUpload: 'CloudUploadOutlined',
  cloudDownload: 'CloudDownloadOutlined',
  notification: 'NotificationOutlined',
  database: 'DatabaseOutlined',
  desktop: 'DesktopOutlined',
  laptop: 'LaptopOutlined',
  mobile: 'MobileOutlined',
  tabletAlt: 'TabletOutlined',
  like: 'LikeOutlined',
  star: 'StarOutlined',
  shop: 'ShopOutlined',
  shopping: 'ShoppingOutlined',
  tags: 'TagsOutlined',
  tag: 'TagOutlined',
  profile: 'ProfileOutlined',
  fileSearch: 'FileSearchOutlined',
  fileProtect: 'FileProtectOutlined',
  fileDone: 'FileDoneOutlined',
  control: 'ControlOutlined',
  code: 'CodeOutlined',
  codeSandbox: 'CodeSandboxOutlined',
  robot: 'RobotOutlined',
  radar: 'RadarChartOutlined',
  barChart: 'BarChartOutlined',
  pieChart: 'PieChartOutlined',
  lineChart: 'LineChartOutlined',
  areaChart: 'AreaChartOutlined',
  fundProjection: 'FundProjectionScreenOutlined',
  container: 'ContainerOutlined',
  alert: 'AlertOutlined',
  info: 'InfoCircleOutlined',
  warning: 'WarningOutlined',
  question: 'QuestionCircleOutlined',
  check: 'CheckCircleOutlined',
  close: 'CloseCircleOutlined',
  save: 'SaveOutlined',
  printer: 'PrinterOutlined',
  calendar: 'CalendarOutlined',
  clock: 'ClockCircleOutlined',
  phone: 'PhoneOutlined',
  mobilePhone: 'MobileOutlined',
  mailOpen: 'MailOutlined',
  camera: 'CameraOutlined',
  video: 'VideoCameraOutlined',
  audio: 'CustomerServiceOutlined',
  picture: 'PictureOutlined',
  fileImage: 'FileImageOutlined',
  filePdf: 'FilePdfOutlined',
  fileZip: 'FileZipOutlined',
  car: 'CarOutlined',
  wifi: 'WifiOutlined',
  compass: 'CompassOutlined',
  environmentFilled: 'EnvironmentOutlined',
  accountBook: 'AccountBookOutlined',
  medicineBox: 'MedicineBoxOutlined',
  bank: 'BankOutlined',
  solution: 'SolutionOutlined',
  audit: 'AuditOutlined',
  contacts: 'ContactsOutlined',
  teamOutlined: 'TeamOutlined',
  appstoreAdd: 'AppstoreAddOutlined',
};

const antIconNames = Object.keys(AntIcons).filter((name) =>
  /(?:Outlined|Filled|TwoTone)$/.test(name),
);

const legacySvgIconSet = new Set<string>(legacySvgIcons);

export const iconOptions = Array.from(
  new Set([...legacySvgIcons, ...Object.keys(iconComponentMap), ...antIconNames]),
).sort((a, b) => a.localeCompare(b));

const legacySvgIconUrl = (iconName: string) => `/icons/svg/${iconName}.svg`;

const renderLegacySvgIcon = (iconName: string) => (
  <img
    src={legacySvgIconUrl(iconName)}
    alt={iconName}
    style={{ width: 16, height: 16, objectFit: 'contain', verticalAlign: 'middle' }}
  />
);

export const renderNamedIcon = (iconName?: string) => {
  if (!iconName) {
    return null;
  }

  if (legacySvgIconSet.has(iconName)) {
    return renderLegacySvgIcon(iconName);
  }

  const componentName =
    iconComponentMap[iconName] ||
    (iconName in AntIcons ? iconName : undefined);
  const IconComponent = componentName
    ? (AntIcons[componentName as keyof typeof AntIcons] as React.ComponentType)
    : undefined;

  return IconComponent ? <IconComponent /> : null;
};

interface IconPickerProps extends Omit<InputProps, 'onChange' | 'value'> {
  value?: string;
  onChange?: (value: string) => void;
}

const IconPicker = ({ value, onChange, ...inputProps }: IconPickerProps) => {
  const [keyword, setKeyword] = React.useState('');

  const filteredOptions = React.useMemo(
    () => iconOptions.filter((item) => item.toLowerCase().includes(keyword.trim().toLowerCase())),
    [keyword],
  );

  const content = (
    <div style={{ width: 420 }}>
      <Input
        allowClear
        placeholder="搜索图标"
        value={keyword}
        onChange={(event) => setKeyword(event.target.value)}
        style={{ marginBottom: 12 }}
      />
      {filteredOptions.length ? (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
            gap: 8,
            maxHeight: 280,
            overflow: 'auto',
          }}
        >
          {filteredOptions.map((item) => (
            <Button
              key={item}
              type={item === value ? 'primary' : 'default'}
              onClick={() => onChange?.(item)}
              style={{ height: 'auto', paddingBlock: 10 }}
            >
              <Space orientation="vertical" size={4}>
                <span style={{ fontSize: 18 }}>{renderNamedIcon(item)}</span>
                <span style={{ fontSize: 12 }}>{item}</span>
              </Space>
            </Button>
          ))}
        </div>
      ) : (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="没有匹配的图标" />
      )}
    </div>
  );

  return (
    <Space.Compact style={{ width: '100%' }}>
      <Input
        {...inputProps}
        allowClear
        value={value}
        onChange={(event) => onChange?.(event.target.value)}
        prefix={renderNamedIcon(value)}
        placeholder="请输入或选择图标"
      />
      <Popover content={content} trigger="click" placement="bottomLeft">
        <Button icon={<AppstoreOutlined />}>选择</Button>
      </Popover>
    </Space.Compact>
  );
};

export default IconPicker;
