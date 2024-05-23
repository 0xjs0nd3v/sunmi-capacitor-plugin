import { registerPlugin } from '@capacitor/core';

import type { SunmiPluginPlugin } from './definitions';

const SunmiPlugin = registerPlugin<SunmiPluginPlugin>('SunmiPlugin')

export * from './definitions';
export { SunmiPlugin };
