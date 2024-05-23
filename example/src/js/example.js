import { SunmiPlugin } from 'sunmi-capacitor-plugin';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    SunmiPlugin.echo({ value: inputValue })
}
