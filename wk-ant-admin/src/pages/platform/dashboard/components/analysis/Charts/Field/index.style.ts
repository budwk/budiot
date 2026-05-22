import { createStyles } from 'antd-style';

const useStyles = createStyles(({ token }) => ({
  field: {
    display: 'inline-flex',
    alignItems: 'baseline',
    gap: 8,
    color: token.colorTextSecondary,
    fontSize: token.fontSizeSM,
    lineHeight: 1.5,
  },
  label: {
    color: token.colorTextDescription,
  },
  value: {
    color: token.colorTextHeading,
    fontWeight: 500,
  },
}));

export default useStyles;
