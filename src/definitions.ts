import { PluginCallback } from '@capacitor/core';

export interface SunmiPluginPlugin {
  /**
   * Get the system params details.
   */
  getSysParam(options: { key: string }): Promise<{ result: any }>;
  /**
   * Start listing to read the card,
   * you have to initialize the SDK first before calling this functionality.
   */
  readCard: (options: { cardType: string }, callback: PluginCallback) => void;
  /**
   * Get the device model details.
   */
  getDeviceModel: () => Promise<SunmiCardDeviceModel>;
  /**
   * Initializes the SDK to start reading cards.
   */
  initSunmiSDK: () => Promise<void>;
  /**
   * Closes the card reader.
   */
  closeCardReader: () => Promise<void>;
}

export type SunmiCardDeviceModel = {
  model: string;
  isP2: boolean;
  isP1N: boolean;
  isP2Lite: boolean;
  isP2Pro: boolean;
  isP14G: boolean;
};
