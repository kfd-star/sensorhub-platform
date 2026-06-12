import React from 'react';
import { Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';

export const formatCellValue = (value: any): React.ReactNode => {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  if (typeof value === 'boolean') {
    return <Tag color={value ? 'green' : 'default'}>{value ? 'true' : 'false'}</Tag>;
  }
  if (typeof value === 'object') {
    const text = JSON.stringify(value);
    const shortText = text.length > 120 ? `${text.slice(0, 120)}...` : text;
    return (
      <Tooltip title={<pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{text}</pre>}>
        <Typography.Text>{shortText}</Typography.Text>
      </Tooltip>
    );
  }
  const text = String(value);
  if (text.length > 60) {
    return (
      <Tooltip title={text}>
        <Typography.Text>{`${text.slice(0, 60)}...`}</Typography.Text>
      </Tooltip>
    );
  }
  return text;
};

export const buildDynamicColumns = (records: Record<string, any>[]): ColumnsType<Record<string, any>> => {
  const keys = Array.from(
    new Set(
      records.flatMap((record) => Object.keys(record || {})),
    ),
  );
  return keys.map((key) => ({
    title: key,
    dataIndex: key,
    key,
    ellipsis: true,
    render: (_, record) => formatCellValue(record[key]),
  }));
};

export const buildRowKey = (record: Record<string, any>, index: number): string => {
  const keyParts = [
    record.id,
    record.timestamp,
    record.timestamp_millisecond,
    record.device_token,
    index,
  ].filter(Boolean);
  return keyParts.join('-');
};
