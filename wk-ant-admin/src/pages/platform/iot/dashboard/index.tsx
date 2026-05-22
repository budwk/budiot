import {
  AlertOutlined,
  ApartmentOutlined,
  ApiOutlined,
  CloudServerOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { Area, Column, Pie } from '@ant-design/plots';
import { GridContent } from '@ant-design/pro-components';
import { history } from '@umijs/max';
import { createStyles } from 'antd-style';
import { Button, Col, DatePicker, Empty, Row, Segmented, Space } from 'antd';
import type { Dayjs } from 'dayjs';
import * as React from 'react';
import { ChartCard, Field } from '@/pages/platform/dashboard/components/analysis/Charts';
import Trend from '@/pages/platform/dashboard/components/analysis/Trend';
import useAnalysisStyles from '@/pages/platform/dashboard/components/analysis.style';
import { getIotDashboardData } from '@/services/budiot/iot/dashboard';
import type {
  IotDashboardCard,
  IotDashboardData,
  IotDashboardRatioStat,
} from '@/services/budiot/iot/typing';

const { RangePicker } = DatePicker;

const defaultData: IotDashboardData = {
  cards: [],
  vendorStats: [],
  typeStats: [],
  trend: {
    labels: [],
    values: [],
    total: 0,
  },
};

const useStyles = createStyles(({ token }) => ({
  trendToolbar: {
    display: 'flex',
    justifyContent: 'flex-end',
    marginBottom: 24,
    [`@media screen and (max-width: ${token.screenLG}px)`]: {
      justifyContent: 'flex-start',
    },
  },
  rankItem: {
    marginTop: 16,
  },
  rankTop: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    marginBottom: 12,
  },
  rankValue: {
    color: token.colorTextHeading,
    fontWeight: 700,
  },
  progressOuter: {
    overflow: 'hidden',
    height: 8,
    marginTop: 8,
    borderRadius: token.borderRadiusSM,
    background: token.colorFillSecondary,
  },
  progressInner: {
    height: '100%',
    borderRadius: 999,
    background: 'linear-gradient(90deg, #1677ff 0%, #36cfc9 100%)',
  },
  progressMeta: {
    display: 'flex',
    justifyContent: 'space-between',
    marginTop: 8,
    color: token.colorTextDescription,
    fontSize: 12,
  },
}));

const rangeOptions = [
  { label: '今日', value: 'today' },
  { label: '近7天', value: 'week' },
  { label: '近30天', value: 'month' },
  { label: '近一年', value: 'year' },
] as const;

const metricIconMap: Record<string, React.ReactNode> = {
  设备数量: <ApartmentOutlined />,
  当前在线: <CloudServerOutlined />,
  今日设备通信量: <ApiOutlined />,
  今日告警数量: <AlertOutlined />,
};

const getCardByTitle = (cards: IotDashboardCard[], title: string) =>
  cards.find((item) => item.title === title);

const formatCount = (value?: number) =>
  Intl.NumberFormat('zh-CN', { maximumFractionDigits: 0 }).format(value || 0);

const ratioPercent = (value?: number) => Math.min(Math.max(value || 0, 0), 100);

const trendFlag = (trend?: string) =>
  trend === 'DOWN' ? 'down' : trend === 'UP' ? 'up' : undefined;

const DashboardPage: React.FC = () => {
  const { styles } = useStyles();
  const { styles: analysisStyles } = useAnalysisStyles();
  const [rangeType, setRangeType] = React.useState('today');
  const [chartType, setChartType] = React.useState<'pie' | 'bar'>('pie');
  const [dateRange, setDateRange] = React.useState<[Dayjs, Dayjs] | null>(null);
  const [initialLoading, setInitialLoading] = React.useState(true);
  const [trendLoading, setTrendLoading] = React.useState(false);
  const [data, setData] = React.useState<IotDashboardData>(defaultData);

  const loadData = React.useCallback(
    async (
      nextRangeType: string,
      nextDateRange?: [Dayjs, Dayjs] | null,
      mode: 'initial' | 'trend' = 'trend',
    ) => {
      if (mode === 'initial') {
        setInitialLoading(true);
      } else {
        setTrendLoading(true);
      }
      try {
        const response = await getIotDashboardData({
          rangeType: nextRangeType,
          startAt: nextDateRange?.[0]?.valueOf(),
          endAt: nextDateRange?.[1]?.valueOf(),
        });
        setData({
          cards: response.data.cards || [],
          vendorStats: response.data.vendorStats || [],
          typeStats: response.data.typeStats || [],
          trend: response.data.trend || defaultData.trend,
        });
      } finally {
        if (mode === 'initial') {
          setInitialLoading(false);
        } else {
          setTrendLoading(false);
        }
      }
    },
    [],
  );

  React.useEffect(() => {
    loadData('today', null, 'initial').catch(() => {
      setInitialLoading(false);
    });
  }, [loadData]);

  const overviewCards = React.useMemo(
    () =>
      data.cards.map((card) => ({
        ...card,
        icon: metricIconMap[card.title] || <ApiOutlined />,
      })),
    [data.cards],
  );

  const onlineCard = getCardByTitle(data.cards, '当前在线');
  const alertCard = getCardByTitle(data.cards, '今日告警数量');
  const deviceCard = getCardByTitle(data.cards, '设备数量');

  const vendorChartData = React.useMemo(
    () =>
      data.vendorStats.map((item: IotDashboardRatioStat) => ({
        name: item.name,
        value: item.deviceCount || item.value || 0,
        ratio: item.ratio || 0,
      })),
    [data.vendorStats],
  );

  const typeChartData = React.useMemo(
    () =>
      data.typeStats.map((item: IotDashboardRatioStat) => ({
        name: item.name,
        value: item.value || item.deviceCount || 0,
      })),
    [data.typeStats],
  );

  const trendChartData = React.useMemo(
    () =>
      (data.trend.labels || []).map((label, index) => ({
        time: label,
        value: data.trend.values?.[index] || 0,
      })),
    [data.trend.labels, data.trend.values],
  );

  const totalTypeDevices = React.useMemo(
    () => typeChartData.reduce((sum, item) => sum + item.value, 0),
    [typeChartData],
  );

  const handleRangeChange = (value: string) => {
    setDateRange(null);
    setRangeType(value);
    loadData(value, null, 'trend').catch(() => undefined);
  };

  const handleDateRangeChange = (value: null | [Dayjs | null, Dayjs | null]) => {
    if (value?.[0] && value?.[1]) {
      const nextRange = [value[0], value[1]] as [Dayjs, Dayjs];
      setDateRange(nextRange);
      loadData(rangeType, nextRange, 'trend').catch(() => undefined);
      return;
    }
    setDateRange(null);
    loadData(rangeType, null, 'trend').catch(() => undefined);
  };

  const handleRefresh = () => {
    loadData(rangeType, dateRange, 'trend').catch(() => undefined);
  };

  const navigateTo = (path?: string) => {
    if (path) {
      history.push(path);
    }
  };

  return (
    <GridContent>
      <Row gutter={24}>
        {overviewCards.map((card) => (
          <Col key={card.title} xs={24} sm={12} xl={6} style={{ marginBottom: 24 }}>
            <ChartCard
              variant="borderless"
              loading={initialLoading}
              title={card.title}
              avatar={card.icon}
              total={formatCount(card.value)}
              footer={
                card.title === '今日告警数量' ? (
                  <Field
                    label="告警构成"
                    value={`原生 ${card.rawAlerts || 0} / 规则 ${card.ruleAlerts || 0}`}
                  />
                ) : (
                  <Field label="说明" value={card.subLabel || '-'} />
                )
              }
              contentHeight={46}
              onClick={() => navigateTo(card.target)}
              style={{ cursor: card.target ? 'pointer' : 'default' }}
            >
              {typeof card.changeRate === 'number' && trendFlag(card.trend) ? (
                <Trend
                  flag={trendFlag(card.trend) as 'up' | 'down'}
                  reverseColor={card.rateType === 'danger'}
                >
                  较上一周期 {Math.abs(card.changeRate).toFixed(2)}%
                </Trend>
              ) : (
                <span style={{ color: 'rgba(0,0,0,0.45)' }}>点击查看详情</span>
              )}
            </ChartCard>
          </Col>
        ))}
      </Row>

      <Row gutter={24}>
        <Col xs={24} xl={12} style={{ marginBottom: 24 }}>
          <ChartCard
            title="厂家设备统计"
            loading={initialLoading}
            variant="borderless"
            total={formatCount(deviceCard?.value)}
            footer={<Field label="在线设备" value={formatCount(onlineCard?.value)} />}
            style={{ height: '100%' }}
          >
            <ul className={analysisStyles.rankingList}>
              {vendorChartData.length ? (
                vendorChartData.map((item, index) => (
                  <li key={item.name} className={styles.rankItem}>
                    <div className={styles.rankTop}>
                      <span
                        className={
                          index < 3
                            ? analysisStyles.rankingItemNumberActive
                            : analysisStyles.rankingItemNumber
                        }
                      >
                        {index + 1}
                      </span>
                      <span className={analysisStyles.rankingItemTitle}>{item.name}</span>
                      <span className={styles.rankValue}>{formatCount(item.value)} 台</span>
                    </div>
                    <div className={styles.progressOuter}>
                      <div
                        className={styles.progressInner}
                        style={{ width: `${ratioPercent(item.ratio)}%` }}
                      />
                    </div>
                    <div className={styles.progressMeta}>
                      <span>占比</span>
                      <span>{(item.ratio || 0).toFixed(2)}%</span>
                    </div>
                  </li>
                ))
              ) : (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无厂家数据" />
              )}
            </ul>
          </ChartCard>
        </Col>

        <Col xs={24} xl={12} style={{ marginBottom: 24 }}>
          <ChartCard
            title="设备类型分布"
            loading={initialLoading}
            variant="borderless"
            action={
              <Segmented
                value={chartType}
                options={[
                  { label: '饼图', value: 'pie' },
                  { label: '柱状图', value: 'bar' },
                ]}
                onChange={(value) => setChartType(value as 'pie' | 'bar')}
              />
            }
            className={analysisStyles.salesCard}
            total={formatCount(totalTypeDevices)}
            footer={<Field label="类型数量" value={typeChartData.length} />}
            style={{ height: '100%' }}
          >
            {typeChartData.length ? (
              chartType === 'pie' ? (
                <Pie
                  height={280}
                  data={typeChartData}
                  angleField="value"
                  colorField="name"
                  label={{ text: 'name', position: 'outside' }}
                  legend={{ color: { position: 'right' } }}
                />
              ) : (
                <Column
                  height={280}
                  data={typeChartData}
                  xField="name"
                  yField="value"
                  label={{ position: 'top' }}
                  color="#1677ff"
                />
              )
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无设备类型数据" />
            )}
          </ChartCard>
        </Col>
      </Row>

      <Row gutter={24}>
        <Col xs={24} style={{ marginBottom: 24 }}>
          <ChartCard
            title="设备通信趋势"
            loading={initialLoading || trendLoading}
            variant="borderless"
            className={analysisStyles.salesCard}
            footer={
              <Space wrap size={[24, 8]}>
                <Field
                  label="统计区间"
                  value={`${data.trend.startLabel || '-'} ~ ${data.trend.endLabel || '-'}`}
                />
                <Field label="累计通信量" value={formatCount(data.trend.total)} />
                <Field label="今日告警" value={formatCount(alertCard?.value)} />
              </Space>
            }
          >
            <div className={styles.trendToolbar}>
              <Space wrap>
                <Segmented
                  value={rangeType}
                  options={rangeOptions.map((item) => ({
                    label: item.label,
                    value: item.value,
                  }))}
                  onChange={(value) => handleRangeChange(String(value))}
                />
                <RangePicker showTime value={dateRange} onChange={handleDateRangeChange} />
                <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
                  刷新
                </Button>
              </Space>
            </div>
            <Area
              height={320}
              data={trendChartData}
              xField="time"
              yField="value"
              shapeField="smooth"
              axis={{ y: { title: false } }}
              colorField="value"
              style={{
                fill: 'linear-gradient(-90deg, rgba(22,119,255,0.08) 0%, rgba(54,207,201,0.32) 100%)',
                lineWidth: 2,
              }}
              point={{
                size: 3,
                shape: 'circle',
              }}
            />
          </ChartCard>
        </Col>
      </Row>
    </GridContent>
  );
};

export default DashboardPage;
