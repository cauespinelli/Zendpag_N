// @ts-nocheck
import { useState, useEffect, useCallback } from 'react';
import { storage } from '@/utils/helpers';

type SetValue<T> = T | ((val: T) => T);

interface UseLocalStorageOptions<T> {
  serializer?: {
    read: (value: string) => T;
    write: (value: T) => string;
  };
  syncData?: boolean;
  onError?: (error: Error) => void;
}

export function useLocalStorage<T>(
  key: string,
  initialValue: T,
  options: UseLocalStorageOptions<T> = {}
): [T, (value: SetValue<T>) => void, () => void] {
  const {
    serializer = {
      read: (v: string) => {
        try {
          return JSON.parse(v);
        } catch {
          return v as unknown as T;
        }
      },
      write: (v: T) => JSON.stringify(v),
    },
    syncData = true,
    onError = (e) => console.error('useLocalStorage error:', e),
  } = options;

  // Read from localStorage on initialization
  const readValue = useCallback((): T => {
    try {
      const item = localStorage.getItem(key);
      if (item === null) {
        return initialValue;
      }
      return serializer.read(item);
    } catch (error) {
      onError(error as Error);
      return initialValue;
    }
  }, [key, initialValue, serializer, onError]);

  const [storedValue, setStoredValue] = useState<T>(readValue);

  // Update localStorage when value changes
  const setValue = useCallback(
    (value: SetValue<T>) => {
      try {
        const valueToStore = value instanceof Function ? value(storedValue) : value;
        setStoredValue(valueToStore);
        localStorage.setItem(key, serializer.write(valueToStore));

        // Dispatch custom event for cross-tab synchronization
        if (syncData) {
          window.dispatchEvent(
            new CustomEvent('local-storage-change', {
              detail: { key, value: valueToStore },
            })
          );
        }
      } catch (error) {
        onError(error as Error);
      }
    },
    [key, serializer, storedValue, syncData, onError]
  );

  // Remove item from localStorage
  const removeValue = useCallback(() => {
    try {
      localStorage.removeItem(key);
      setStoredValue(initialValue);

      if (syncData) {
        window.dispatchEvent(
          new CustomEvent('local-storage-change', {
            detail: { key, value: null },
          })
        );
      }
    } catch (error) {
      onError(error as Error);
    }
  }, [key, initialValue, syncData, onError]);

  // Listen for localStorage changes from other tabs
  useEffect(() => {
    if (!syncData) return;

    const handleStorageChange = (e: StorageEvent) => {
      if (e.key !== key || e.storageArea !== localStorage) return;

      try {
        const newValue = e.newValue ? serializer.read(e.newValue) : initialValue;
        setStoredValue(newValue);
      } catch (error) {
        onError(error as Error);
      }
    };

    const handleCustomStorageChange = (e: CustomEvent) => {
      if (e.detail.key === key) {
        setStoredValue(e.detail.value ?? initialValue);
      }
    };

    window.addEventListener('storage', handleStorageChange);
    window.addEventListener('local-storage-change', handleCustomStorageChange as EventListener);

    return () => {
      window.removeEventListener('storage', handleStorageChange);
      window.removeEventListener('local-storage-change', handleCustomStorageChange as EventListener);
    };
  }, [key, initialValue, serializer, syncData, onError]);

  // Update state when key changes
  useEffect(() => {
    setStoredValue(readValue());
  }, [readValue]);

  return [storedValue, setValue, removeValue];
}

// Specialized hooks for common use cases
export const useLocalStorageState = <T>(key: string, initialValue: T) => {
  return useLocalStorage(key, initialValue, { syncData: false });
};

export const usePersistedState = <T>(key: string, initialValue: T) => {
  const [value, setValue] = useLocalStorage(key, initialValue);
  return { value, setValue };
};

// Hook for managing user preferences
export const useUserPreferences = () => {
  const [preferences, setPreferences] = useLocalStorage('user-preferences', {
    theme: 'light',
    language: 'pt-BR',
    currency: 'BRL',
    dateFormat: 'DD/MM/YYYY',
    notifications: {
      email: true,
      push: true,
      desktop: false,
    },
    dashboard: {
      showWelcome: true,
      defaultPeriod: '30d',
      autoRefresh: true,
      refreshInterval: 30,
    },
    table: {
      pageSize: 20,
      showColumns: ['id', 'amount', 'status', 'createdAt'],
    },
  });

  const updatePreference = useCallback(
    (path: string, value: any) => {
      setPreferences(prev => {
        const keys = path.split('.');
        const updated = { ...prev };
        let current: any = updated;

        for (let i = 0; i < keys.length - 1; i++) {
          if (!current[keys[i]]) {
            current[keys[i]] = {};
          }
          current = current[keys[i]];
        }

        current[keys[keys.length - 1]] = value;
        return updated;
      });
    },
    [setPreferences]
  );

  const resetPreferences = useCallback(() => {
    setPreferences({
      theme: 'light',
      language: 'pt-BR',
      currency: 'BRL',
      dateFormat: 'DD/MM/YYYY',
      notifications: {
        email: true,
        push: true,
        desktop: false,
      },
      dashboard: {
        showWelcome: true,
        defaultPeriod: '30d',
        autoRefresh: true,
        refreshInterval: 30,
      },
      table: {
        pageSize: 20,
        showColumns: ['id', 'amount', 'status', 'createdAt'],
      },
    });
  }, [setPreferences]);

  return {
    preferences,
    updatePreference,
    resetPreferences,
  };
};

// Hook for managing table state (filters, sorting, pagination)
export const useTableState = (tableKey: string) => {
  const [tableState, setTableState] = useLocalStorage(`table-state-${tableKey}`, {
    filters: {},
    sorter: {},
    pagination: { current: 1, pageSize: 20 },
    columns: [],
  });

  const updateFilters = useCallback(
    (filters: any) => {
      setTableState(prev => ({ ...prev, filters }));
    },
    [setTableState]
  );

  const updateSorter = useCallback(
    (sorter: any) => {
      setTableState(prev => ({ ...prev, sorter }));
    },
    [setTableState]
  );

  const updatePagination = useCallback(
    (pagination: any) => {
      setTableState(prev => ({ ...prev, pagination }));
    },
    [setTableState]
  );

  const updateColumns = useCallback(
    (columns: any[]) => {
      setTableState(prev => ({ ...prev, columns }));
    },
    [setTableState]
  );

  const resetTableState = useCallback(() => {
    setTableState({
      filters: {},
      sorter: {},
      pagination: { current: 1, pageSize: 20 },
      columns: [],
    });
  }, [setTableState]);

  return {
    tableState,
    updateFilters,
    updateSorter,
    updatePagination,
    updateColumns,
    resetTableState,
  };
};

// Hook for managing form drafts
export const useFormDraft = <T extends object>(formKey: string) => {
  const [draft, setDraft] = useLocalStorage<T | null>(`form-draft-${formKey}`, null);

  const saveDraft = useCallback(
    (values: T) => {
      setDraft(values);
    },
    [setDraft]
  );

  const clearDraft = useCallback(() => {
    setDraft(null);
  }, [setDraft]);

  const hasDraft = draft !== null;

  return {
    draft,
    saveDraft,
    clearDraft,
    hasDraft,
  };
};

// Hook for managing recently viewed items
export const useRecentlyViewed = <T extends { id: string }>(key: string, maxItems = 10) => {
  const [recentItems, setRecentItems] = useLocalStorage<T[]>(`recently-viewed-${key}`, []);

  const addRecentItem = useCallback(
    (item: T) => {
      setRecentItems(prev => {
        const filtered = prev.filter(i => i.id !== item.id);
        return [item, ...filtered].slice(0, maxItems);
      });
    },
    [setRecentItems, maxItems]
  );

  const removeRecentItem = useCallback(
    (id: string) => {
      setRecentItems(prev => prev.filter(i => i.id !== id));
    },
    [setRecentItems]
  );

  const clearRecentItems = useCallback(() => {
    setRecentItems([]);
  }, [setRecentItems]);

  return {
    recentItems,
    addRecentItem,
    removeRecentItem,
    clearRecentItems,
  };
};

export default useLocalStorage;