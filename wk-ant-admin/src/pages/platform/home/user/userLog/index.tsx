import { history } from '@umijs/max';
import * as React from 'react';

const UserLogPage = () => {
  React.useEffect(() => {
    history.replace('/platform/home/user?tab=log');
  }, []);

  return null;
};

export default UserLogPage;
