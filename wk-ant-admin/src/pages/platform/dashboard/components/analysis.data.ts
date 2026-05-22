export type DataItem = {
  x: string;
  y: number;
};

export type RadarData = {
  name: string;
  label: string;
  value: number;
};

export type OfflineDataItem = {
  name: string;
  cvr: number;
};

export type SearchDataItem = {
  index: number;
  keyword: string;
  count: number;
  range: number;
  status: number;
};

export type AnalysisData = {
  visitData: DataItem[];
  visitData2: DataItem[];
  salesData: DataItem[];
  searchData: SearchDataItem[];
  offlineData: OfflineDataItem[];
  offlineChartData: Array<{
    date: string;
    type: string;
    value: number;
  }>;
  salesTypeData: DataItem[];
  salesTypeDataOnline: DataItem[];
  salesTypeDataOffline: DataItem[];
  radarData: RadarData[];
};
