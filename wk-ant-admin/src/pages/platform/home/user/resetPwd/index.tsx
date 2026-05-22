import { history } from '@umijs/max';
import * as React from 'react';

const ResetPasswordPage = () => {
  React.useEffect(() => {
    history.replace('/platform/home/user?tab=password');
  }, []);

  return null;
};

export default ResetPasswordPage;
