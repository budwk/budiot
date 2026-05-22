import { history } from '@umijs/max';
import * as React from 'react';

const UserAvatarPage = () => {
  React.useEffect(() => {
    history.replace('/platform/home/user?tab=avatar');
  }, []);

  return null;
};

export default UserAvatarPage;
