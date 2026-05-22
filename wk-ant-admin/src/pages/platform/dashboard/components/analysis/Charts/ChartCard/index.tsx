import omit from '@rc-component/util/es/omit';
import { Card } from 'antd';
import type { CardProps } from 'antd/es/card';
import classNames from 'classnames';
import React from 'react';
import useStyles from './index.style';

type TotalType = () => React.ReactNode;

export type ChartCardProps = {
  title: React.ReactNode;
  action?: React.ReactNode;
  total?: React.ReactNode | number | TotalType;
  footer?: React.ReactNode;
  contentHeight?: number;
  avatar?: React.ReactNode;
  style?: React.CSSProperties;
} & CardProps;

const ChartCard: React.FC<ChartCardProps> = (props) => {
  const { styles } = useStyles();

  const renderTotal = (total?: number | TotalType | React.ReactNode) => {
    if (!total && total !== 0) {
      return null;
    }
    if (typeof total === 'function') {
      return <div className={styles.total}>{total()}</div>;
    }
    return <div className={styles.total}>{total}</div>;
  };

  const renderContent = () => {
    const {
      contentHeight,
      title,
      avatar,
      action,
      total,
      footer,
      children,
      loading,
    } = props;
    if (loading) {
      return false;
    }
    return (
      <div className={styles.chartCard}>
        <div
          className={classNames(styles.chartTop, {
            [styles.chartTopMargin]: !children && !footer,
          })}
        >
          {avatar ? <div className={styles.avatar}>{avatar}</div> : null}
          <div className={styles.metaWrap}>
            <div className={styles.meta}>
              <span>{title}</span>
              {action ? <span className={styles.action}>{action}</span> : null}
            </div>
            {renderTotal(total)}
          </div>
        </div>
        {children ? (
          <div
            className={styles.content}
            style={{
              minHeight: contentHeight || 'auto',
            }}
          >
            <div className={contentHeight ? styles.contentFixed : undefined}>
              {children}
            </div>
          </div>
        ) : null}
        {footer ? (
          <div
            className={classNames(styles.footer, {
              [styles.footerMargin]: !children,
            })}
          >
            {footer}
          </div>
        ) : null}
      </div>
    );
  };

  const { loading = false, ...rest } = props;
  const cardProps = omit(rest, ['total', 'contentHeight', 'action']);

  return (
    <Card
      loading={loading}
      styles={{
        body: {
          padding: '20px 24px 8px 24px',
        },
      }}
      {...cardProps}
    >
      {renderContent()}
    </Card>
  );
};

export default ChartCard;
