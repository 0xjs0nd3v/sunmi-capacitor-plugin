export interface SunmiPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
