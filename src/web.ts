import { WebPlugin } from '@capacitor/core';

import type { SunmiPluginPlugin } from './definitions';

export class SunmiPluginWeb extends WebPlugin implements SunmiPluginPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
