import { createStyles } from 'antd-style';

const useStyles = createStyles(({ token }) => {
  return {
    rankingList: {
      margin: '0',
      padding: '0',
      listStyle: 'none',
      li: {
        display: 'block',
      },
    },
    rankingItemNumber: {
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      width: '20px',
      height: '20px',
      marginRight: '12px',
      fontWeight: 600,
      fontSize: '12px',
      lineHeight: '20px',
      textAlign: 'center',
      borderRadius: '50%',
      backgroundColor: token.colorBgContainerDisabled,
    },
    rankingItemNumberActive: {
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      width: '20px',
      height: '20px',
      marginRight: '12px',
      fontWeight: 600,
      fontSize: '12px',
      lineHeight: '20px',
      textAlign: 'center',
      color: '#fff',
      borderRadius: '50%',
      backgroundColor: token.colorPrimary,
    },
    rankingItemTitle: {
      flex: 1,
      marginRight: '8px',
      overflow: 'hidden',
      whiteSpace: 'nowrap',
      textOverflow: 'ellipsis',
    },
    salesCard: {
      '.ant-card-head': {
        position: 'relative',
      },
      '.ant-card-head-title': {
        alignItems: 'normal',
      },
      '.ant-card-body': {
        paddingTop: '20px',
      },
      [`@media screen and (max-width: ${token.screenSM}px)`]: {
        '.ant-card-body': {
          padding: '16px',
        },
      },
    },
  };
});

export default useStyles;
