import { registerPlugin } from '@capacitor/core';

import type { SunmiPluginPlugin } from './definitions';

const SunmiPlugin = registerPlugin<SunmiPluginPlugin>(
    'SunmiPlugin',
    {
      web: () => import('./web').then(m => new m.SunmiCardReaderWeb()),
    },
  );

export * from './definitions';
export { SunmiPlugin };
