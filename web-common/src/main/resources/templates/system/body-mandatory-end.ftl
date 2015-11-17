  <script type="text/javascript" src="./VAADIN/vaadinBootstrap.js?v=${vaadinVersion}"></script>
  
  <script type="text/javascript">//<![CDATA[
if (!window.vaadin) alert("Failed to load the bootstrap javascript: ./VAADIN/vaadinBootstrap.js?v=${vaadinVersion}");

if (typeof window.__gwtStatsEvent != 'function') {
vaadin.gwtStatsEvents = [];
window.__gwtStatsEvent = function(event) {vaadin.gwtStatsEvents.push(event); return true;};
}

vaadin.initApplication("${appId}",{
    "browserDetailsUrl": "vaadin-ui/",
    "serviceUrl": "vaadin-ui/",
    "theme": "${theme}",
    "versionInfo": {
        "vaadinVersion": "${vaadinVersion}"
    },
    "widgetset": "pl.edu.icm.unity.webui.customWidgetset",
    "comErrMsg": {
        "caption": "${comErrMsgCaption}",
        "message": "${comErrMsg}",
        "url": null
    },
    "authErrMsg": {
        "caption": "${authErrMsgCaption}",
        "message": "${authErrMsg}",
        "url": null
    },
    "sessExpMsg": {
        "caption": "${sessExpMsgCaption}",
        "message": "${sessExpMsg}",
        "url": null
    },
    "vaadinDir": "./VAADIN/",
    "debug": ${debug},
    "standalone": false,
    "heartbeatInterval": ${heartbeat}
});
//]]></script>