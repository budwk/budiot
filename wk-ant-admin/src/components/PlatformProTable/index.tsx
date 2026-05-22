import type { ProTableProps } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';

type PlatformProTableProps<T extends Record<string, any>, U extends Record<string, any>> = ProTableProps<T, U> & {
  persistenceKey: string;
};

const PlatformProTable = <T extends Record<string, any>, U extends Record<string, any>>({
  persistenceKey,
  search,
  options,
  pagination,
  ...rest
}: PlatformProTableProps<T, U>) => (
  <ProTable<T, U>
    cardBordered
    dateFormatter="string"
    search={
      search === false
        ? false
        : {
            labelWidth: 'auto',
            defaultCollapsed: true,
            ...search,
          }
    }
    options={{
      reload: true,
      density: true,
      setting: {
        listsHeight: 400,
      },
      fullScreen: true,
      ...options,
    }}
    pagination={{
      showSizeChanger: true,
      ...pagination,
    }}
    columnsState={{
      persistenceType: 'localStorage',
      persistenceKey,
    }}
    {...rest}
  />
);

export default PlatformProTable;
