import { html, PolymerElement } from '@polymer/polymer';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin';
import { ElementMixin } from '@vaadin/component-base';
import '@vaadin/vaadin-lumo-styles/icons';
import './vcf-tooltip-styles.js';

/**
 * `<vcf-tooltip>` Web Component providing an easy way to display tooltips for any html element.
 *
 * ```html
 * <vcf-tooltip for="element-id" position="top">
 *   A short text describing the element.
 * </vcf-tooltip>
 * ```
 *
 * ### Styling
 *
 * The following parts are available for styling:
 *
 * Part name | Description
 * --|--
 * `container` | Container for content and close button
 * `content` | Tooltip content
 * `close-button` | Tooltip close button
 *
 * The following themes are available:
 *
 * Theme name | Description
 * --|--
 * `dark` (default) | Lumo dark theme
 * `light` | Lumo light theme
 *
 * @memberof Vaadin
 * @mixes ElementMixin
 * @mixes ThemableMixin
 * @demo demo/index.html
 */
export class Tooltip extends ElementMixin(ThemableMixin(PolymerElement)) {
  static get template() {
    return html`
      <style>
        :host {
          box-sizing: border-box;
          padding: 0;
          display: block;
          position: absolute;
          outline: none;
          z-index: 1000;
          -moz-user-select: none;
          -ms-user-select: none;
          -webkit-user-select: none;
          user-select: none;

          --tooltip-animation-duration: 250ms;
          --tooltip-delay: 500ms;
          animation: tooltipFadeIn var(--tooltip-animation-duration);
          animation-delay: var(--tooltip-delay);
          animation-fill-mode: both;
        }

        :host([manual]) {
          --tooltip-delay: 0;
        }

        :host([close-button]) [part='close-button'] {
          display: inline-block;
        }

        :host([close-button]) [part='container'] {
          padding-right: 0;
        }

        [part='close-button'] {
          display: none;
          margin: 0;
          padding: 0;
          margin-top: calc(-1 * var(--lumo-tooltip-size) / 6);
          cursor: pointer;
        }

        [part='content'] {
          box-sizing: border-box;
          width: 100%;
        }

        [part='container'] {
          display: flex;
          padding: calc(var(--lumo-tooltip-size) / 6) calc(var(--lumo-tooltip-size) / 4);
          color: var(--lumo-body-text-color);
          background-color: var(--lumo-base-color);
          border-radius: var(--lumo-border-radius);
          box-shadow: var(--lumo-box-shadow-xs);
        }

        :host([hidden]) [part='content'] {
          display: none !important;
        }

        :host ::slotted(*) {
          box-sizing: border-box;
        }

        @keyframes tooltipFadeIn {
          0% {
            opacity: 0;
          }
          100% {
            opacity: 1;
          }
        }
      </style>

      <div part="container" theme$="[[theme]]">
        <div part="content">
          <slot></slot>
        </div>
        <vaadin-button part="close-button" theme="icon tertiary small" on-click="hide" title="Close tooltip">
          <iron-icon icon="lumo:cross"></iron-icon>
        </vaadin-button>
      </div>
    `;
  }

  static get is() {
    return 'vcf-tooltip';
  }

  static get version() {
    return '23.0.4';
  }

  static get properties() {
    return {
      /**
       * The id of the target element. Must be a sibling.
       */
      for: {
        type: String
      },

      /**
       * Tooltip position. Possible values: top, right, left and bottom.
       */
      position: {
        type: String,
        value: 'top'
      },

      /*
       * Alignment to the target element. Possible values: top, bottom, left, right and center.
       */
      align: {
        type: String,
        value: 'center'
      },

      /**
       * Is the tooltip hidden.
       */
      hidden: {
        type: Boolean,
        value: true,
        notify: true,
        reflectToAttribute: true,
        observer: '_hiddenChanged'
      },

      /**
       * Enable manual mode.
       */
      manual: {
        type: Boolean,
        value: false,
        reflectToAttribute: true
      },

      /**
       * The tooltip is attached to this element.
       */
      targetElement: {
        type: Object,
        observer: '_attachToTarget'
      },

      /**
       * Show/hide tooltip close button.
       */
      closeButton: {
        type: Boolean,
        value: false,
        reflectToAttribute: true
      },

      /**
       * Set tooltip theme.
       */
      theme: {
        type: String,
        reflectToAttribute: true
      }
    };
  }

  static get observers() {
    return ['_setPosition(targetElement, hidden, position, align)', '_updateTarget(for)', '_manualObserver(manual)'];
  }

  constructor() {
    super();
    this._boundShow = this.show.bind(this);
    this._boundHide = this.hide.bind(this);
    this._boundOnKeyup = this._onKeyup.bind(this);
  }

  connectedCallback() {
    super.connectedCallback();
    this._attachToTarget();
    this._setDefaultId();
  }

  ready() {
    super.ready();
    // Debounced re-position on window resize
    window.addEventListener('resize', () => {
      clearTimeout(this._resizeTimeout);
      this._resizeTimeout = setTimeout(() => {
        const { targetElement, hidden, position } = this;
        if (!this.hidden) this._setPosition(targetElement, hidden, position);
      }, 100);
    });
    // Set default theme
    if (!this.getAttribute('theme')) this.setAttribute('theme', 'dark');
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this._detachFromTarget();
  }

  _manualObserver() {
    if (this.manual) this._removeEvents();
    else this._addEvents();
  }

  _attachToTarget(targetElement, oldTargetElement) {
    if (oldTargetElement) {
      this._removeTargetEvents(oldTargetElement);
      if (oldTargetElement.describedby) {
        oldTargetElement.removeAttribute('aria-describedby');
        delete oldTargetElement.describedby;
      }
    }
    if (targetElement) {
      this._addEvents();
      if (!targetElement.getAttribute('aria-describedby')) {
        targetElement.setAttribute('aria-describedby', this.id);
        targetElement.describedby = true;
      }
    }
  }

  _addEvents() {
    // Delay execution to ensure targetElement has rendered.
    requestAnimationFrame(() => {
      if (this.targetElement && !this.manual) {
        this.targetElement.addEventListener('mouseenter', this._boundShow);
        this.targetElement.addEventListener('focus', this._boundShow);
        this.targetElement.addEventListener('mouseleave', this._boundHide);
        this.targetElement.addEventListener('blur', this._boundHide);
        this.targetElement.addEventListener('tap', this._boundHide);
      }
      if (!this.manual) {
        this.addEventListener('mouseenter', this._boundShow);
        this.addEventListener('mouseleave', this._boundHide);
      }
    });
  }

  _detachFromTarget() {
    if (!this.manual) this._removeEvents();
    if (this.targetElement && this.targetElement.describedby) {
      this.targetElement.removeAttribute('aria-describedby');
      delete this.targetElement.describedby;
    }
  }

  _removeEvents() {
    if (this.targetElement) this._removeTargetEvents(this.targetElement);
    this.removeEventListener('mouseenter', this._boundShow);
    this.removeEventListener('mouseleave', this._boundHide);
  }

  _removeTargetEvents(target) {
    target.removeEventListener('mouseenter', this._boundShow);
    target.removeEventListener('focus', this._boundShow);
    target.removeEventListener('mouseleave', this._boundHide);
    target.removeEventListener('blur', this._boundHide);
    target.removeEventListener('tap', this._boundHide);
  }

  _updateTarget() {
    this.targetElement = this.parentNode.querySelector(`#${this.for}`);
  }

  _setPosition(targetElement, hidden, position) {
    if (targetElement && !hidden) {
      let parentRectHeight = window.innerHeight;
      let parentRectWidth = window.innerWidth;
      if (this.offsetParent) {
        parentRectHeight = this.offsetParent.scrollHeight;
        parentRectWidth = this.offsetParent.scrollWidth;
      }
      const targetRect = this.targetElement.getBoundingClientRect();
      const thisRect = this.getBoundingClientRect();
      const horizontalCenterOffset = (targetRect.width - thisRect.width) / 2;
      const verticalCenterOffset = (targetRect.height - thisRect.height) / 2;
      let targetLeft = targetRect.left;
      let targetTop = targetRect.top;
      let pageYOffset = window.pageYOffset;
      let tooltipLeft, tooltipTop;

      if (this._parentPostioned) {
        targetTop = this.targetElement.offsetTop;
        targetLeft = this.targetElement.offsetLeft;
        pageYOffset = 0;
      }

      switch (position) {
        case 'top':
          tooltipTop = targetTop - thisRect.height + pageYOffset;
          tooltipLeft = this._calculateLeft(targetLeft, targetRect, thisRect, horizontalCenterOffset);
          break;
        case 'bottom':
          tooltipTop = targetTop + targetRect.height + pageYOffset;
          tooltipLeft = this._calculateLeft(targetLeft, targetRect, thisRect, horizontalCenterOffset);
          break;
        case 'left':
          tooltipLeft = targetLeft - thisRect.width;
          tooltipTop = this._calculateTop(targetTop, targetRect, thisRect, verticalCenterOffset, pageYOffset);
          break;
        case 'right':
          tooltipLeft = targetLeft + targetRect.width;
          tooltipTop = this._calculateTop(targetTop, targetRect, thisRect, verticalCenterOffset, pageYOffset);
          break;
      }

      this._setPositionInVisibleBounds(parentRectHeight, parentRectWidth, tooltipLeft, tooltipTop, thisRect);
    }
  }

  // Detct if the offset parent is [positoned](https://developer.mozilla.org/en-US/docs/Web/CSS/position#types_of_positioning)
  get _parentPostioned() {
    return window.getComputedStyle(this.offsetParent).position !== 'static';
  }

  _setPositionInVisibleBounds(parentRectHeight, parentRectWidth, tooltipLeft, tooltipTop, thisRect) {
    // Check and fix horizontal positionparentRectHeight
    if (tooltipLeft + thisRect.width > parentRectWidth) {
      this.style.right = '0px';
      this.style.left = 'auto';
    } else {
      this.style.left = Math.max(0, tooltipLeft) + 'px';
      this.style.right = 'auto';
    }
    // Check and fix vertical position
    if (tooltipTop + thisRect.height > parentRectHeight) {
      this.style.bottom = parentRectHeight + 'px';
      this.style.top = 'auto';
    } else {
      this.style.top = Math.max(0, tooltipTop) + 'px';
      this.style.bottom = 'auto';
    }
  }

  _calculateLeft(targetLeft, targetRect, thisRect, horizontalCenterOffset) {
    switch (this.align) {
      case 'left':
        return targetLeft;
      case 'right':
        return targetRect.left + targetRect.width - thisRect.width;
      default:
        return targetLeft + horizontalCenterOffset;
    }
  }

  _calculateTop(targetTop, targetRect, thisRect, verticalCenterOffset, pageYOffset) {
    switch (this.align) {
      case 'top':
        return targetTop + pageYOffset;
      case 'bottom':
        return targetTop + targetRect.height - thisRect.height + pageYOffset;
      default:
        return targetTop + verticalCenterOffset + pageYOffset;
    }
  }

  show() {
    this.hidden = false;
  }

  hide() {
    this.hidden = true;
  }

  _hiddenChanged(hidden) {
    if (hidden) {
      this.setAttribute('aria-hidden', true);
      window.removeEventListener('keyup', this._boundOnKeyup);
    } else {
      this.setAttribute('aria-hidden', false);
      window.addEventListener('keyup', this._boundOnKeyup);
    }
  }

  _onKeyup(e) {
    // Hide on Escape key press
    if (e.keyCode === 27) this.hide();
  }

  _setDefaultId() {
    if (!this.id) {
      if (!Vaadin.tooltipIndex) Vaadin.tooltipIndex = 0;
      this.id = 'vcf-tooltip' + ++Vaadin.tooltipIndex;
    }
  }
}

customElements.define(Tooltip.is, Tooltip);

/**
 * @namespace Vaadin
 */
window.Vaadin.VcfTooltip = Tooltip;
