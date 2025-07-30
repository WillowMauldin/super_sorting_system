import React, { useContext, createContext, useMemo } from 'react';
import { useQuery } from 'react-query';
import { getItems } from '../api/data';
import { ItemTypeById } from '../api/data_types';
import { SplashScreen } from '../screens/SplashScreen';

export type McData = {
  items: ItemTypeById;
};

export const McDataContext = createContext<McData>(null as any);

export const McDataProvider: React.FC<{
  children: React.ReactChild;
}> = ({ children }) => {
  const {
    isLoading: mcItemsLoading,
    isError: mcItemsError,
    data: mcItemsArr,
  } = useQuery('mc_data_items', getItems, {
    staleTime: Infinity,
    cacheTime: Infinity,
  });

  const items = useMemo(() => {
    if (!mcItemsArr) return undefined;

    const map: ItemTypeById = new Map();

    for (let item of mcItemsArr.data) {
      map.set(item.rawId, item);
    }

    return map;
  }, [mcItemsArr]);

  if (mcItemsLoading) return <SplashScreen message="Loading Minecraft Data" />;

  if (mcItemsError)
    return <SplashScreen message="Failed to load Minecraft Data!" />;

  return (
    <McDataContext.Provider value={{ items: items! }}>
      {children}
    </McDataContext.Provider>
  );
};

export const useMcData = (): McData => useContext(McDataContext);
