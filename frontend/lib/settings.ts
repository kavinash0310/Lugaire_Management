export type AppSetting = {
  key: string;
  value: string;
  settingType: "STRING" | "NUMBER" | "BOOLEAN";
  description: string;
  updatedAt: string;
};

export function settingsToMap(settings: AppSetting[]) {
  return Object.fromEntries(settings.map((setting) => [setting.key, setting.value]));
}
