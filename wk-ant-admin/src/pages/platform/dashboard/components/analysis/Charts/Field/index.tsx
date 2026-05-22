import React from 'react';
import useStyles from './index.style';

export type FieldProps = {
  label?: React.ReactNode;
  value?: React.ReactNode;
};

const Field: React.FC<FieldProps> = ({ label, value }) => {
  const { styles } = useStyles();

  return (
    <span className={styles.field}>
      {label ? <span className={styles.label}>{label}</span> : null}
      <span className={styles.value}>{value}</span>
    </span>
  );
};

export default Field;
