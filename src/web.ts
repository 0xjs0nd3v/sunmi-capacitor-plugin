import { WebPlugin } from '@capacitor/core';

import type { SunmiCardDeviceModel, SunmiPluginPlugin } from './definitions';

export class SunmiCardReaderWeb
  extends WebPlugin
  implements SunmiPluginPlugin {
    
  getSysParam(): Promise<{ result: any }> {
      throw new Error('No web implementation please run the application on an android device [Sunmi Device]');
  }

  async initSunmiSDK (): Promise<void> {
    throw new Error('No web implementation please run the application on an android device [Sunmi Device]');
  }

  readCard (): void {
    throw new Error('No web implementation please run the application on an android device [Sunmi Device]');
  }

  async closeCardReader (): Promise<void> {
    throw new Error('No web implementation please run the application on an android device [Sunmi Device]');
  }

  async getDeviceModel (): Promise<SunmiCardDeviceModel> {
    try {
      throw new Error('No web implementation please run the application on an android device [Sunmi Device]');
    } catch (e) {
      throw e;
    }
  }
}