import { CaretDownOutlined, CaretUpOutlined } from '@ant-design/icons';
import { createStyles } from 'antd-style';
import React from 'react';

const useStyles = createStyles(({ token }) => ({
  trendItem: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: 4,
    color: token.colorTextDescription,
    fontSize: token.fontSizeSM,
    lineHeight: 1,
  },
  up: {
    color: token.colorError,
  },
  down: {
    color: token.colorSuccess,
  },
  reverseUp: {
    color: token.colorSuccess,
  },
  reverseDown: {
    color: token.colorError,
  },
}));

export type TrendProps = {
  flag?: 'up' | 'down';
  reverseColor?: boolean;
  children?: React.ReactNode;
};

const Trend: React.FC<TrendProps> = ({ flag, reverseColor = false, children }) => {
  const { styles, cx } = useStyles();

  const flagClassName =
    flag === 'up'
      ? reverseColor
        ? styles.reverseUp
        : styles.up
      : flag === 'down'
        ? reverseColor
          ? styles.reverseDown
          : styles.down
        : undefined;

  return (
    <span className={cx(styles.trendItem, flagClassName)}>
      {flag === 'up' ? <CaretUpOutlined /> : null}
      {flag === 'down' ? <CaretDownOutlined /> : null}
      <span>{children}</span>
    </span>
  );
};

export default Trend;
