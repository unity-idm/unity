# &lt;vcf-tooltip&gt;

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vaadin/web-components?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge) [![npm version](https://badgen.net/npm/v/@vaadin-component-factory/vcf-tooltip)](https://www.npmjs.com/package/@vaadin-component-factory/vcf-tooltip) [![Published on Vaadin Directory](https://img.shields.io/badge/Vaadin%20Directory-published-00b4f0.svg)](https://vaadin.com/directory/component/vaadin-component-factoryvcf-tooltip)

[Live demo ↗](https://vcf-tooltip.netlify.com) | [API documentation ↗](https://vcf-tooltip.netlify.com/api/#/elements/Vaadin.VcfTooltip)

## Installation

Install `vcf-tooltip`:

```sh
npm i @vaadin-component-factory/vcf-tooltip --save
```

## Usage

Once installed, import it in your application:

```js
import '@vaadin-component-factory/vcf-tooltip';
```

Add `<vcf-tooltip>` element with attribute `for` which will contain id of target element, to the page. Now after hovering on target element, toltip will be displayed.

```html
<button id="element-id">Hover me</button>
<vcf-tooltip for="element-id" position="top">
  A short text describing the element.
</vcf-tooltip>
```

## Running demo

1. Fork the `vcf-tooltip` repository and clone it locally.

1. Make sure you have [npm](https://www.npmjs.com/) installed.

1. When in the `vcf-tooltip` directory, run `npm install` to install dependencies.

1. Run `npm start` to open the demo.

## Server-side API

This is the client-side (Polymer 3) web component. If you are looking for the server-side (Java) API for the Vaadin Platform, it can be found here: [Tooltip](https://vaadin.com/directory/component/tooltip)

## License

Apache License 2.0
