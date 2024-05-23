# sunmi-capacitor-plugin

Capacitor plugin for Sunmi devices.

## Install

```bash
npm install sunmi-capacitor-plugin
npx cap sync
```

## API

<docgen-index>

* [`echo(...)`](#echo)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### echo(...)

```typescript
echo(options: { value: string; }) => Promise<{ value: string; }>
```

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ value: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### Interfaces


#### PluginResultData


#### PluginResultError

| Prop          | Type                |
| ------------- | ------------------- |
| **`message`** | <code>string</code> |


### Type Aliases


#### PluginCallback

<code>(data: <a href="#pluginresultdata">PluginResultData</a>, error?: <a href="#pluginresulterror">PluginResultError</a>): void</code>


#### SunmiCardDeviceModel

<code>{ model: string; isP2: boolean; isP1N: boolean; isP2Lite: boolean; isP2Pro: boolean; isP14G: boolean; }</code>

</docgen-api>
