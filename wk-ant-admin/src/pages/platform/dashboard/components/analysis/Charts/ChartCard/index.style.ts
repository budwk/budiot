import { createStyles } from 'antd-style';

const useStyles = createStyles(({ token }) => ({
  chartCard: {
    position: 'relative',
  },
  chartTop: {
    position: 'relative',
    width: '100%',
    overflow: 'hidden',
  },
  chartTopMargin: {
    marginBottom: '12px',
  },
  metaWrap: {
    minWidth: 0,
    overflow: 'hidden',
  },
  avatar: {
    position: 'relative',
    top: '4px',
    float: 'left',
    marginRight: '20px',
  },
  meta: {
    position: 'relative',
    minHeight: '22px',
    paddingRight: '120px',
    color: token.colorTextSecondary,
    fontSize: token.fontSize,
    lineHeight: '22px',
    [`@media screen and (max-width: ${token.screenSM}px)`]: {
      paddingRight: 0,
    },
  },
  action: {
    position: 'absolute',
    top: 0,
    right: 0,
    lineHeight: 1,
  },
  total: {
    marginTop: '4px',
    marginBottom: 0,
    overflow: 'hidden',
    color: token.colorTextHeading,
    fontSize: '30px',
    lineHeight: '38px',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
  },
  content: {
    position: 'relative',
    width: '100%',
    marginBottom: '12px',
  },
  contentFixed: {
    width: '100%',
  },
  footer: {
    marginTop: '8px',
    paddingTop: '9px',
    borderTop: `1px solid ${token.colorSplit}`,
  },
  footerMargin: {
    marginTop: '20px',
  },
}));

export default useStyles;
