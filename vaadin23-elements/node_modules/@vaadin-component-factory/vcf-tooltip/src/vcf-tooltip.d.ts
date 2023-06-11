declare const Tooltip_base: typeof PolymerElement & import("@open-wc/dedupe-mixin").Constructor<import("@vaadin/vaadin-themable-mixin").ThemableMixinClass> & import("@open-wc/dedupe-mixin").Constructor<import("@vaadin/vaadin-themable-mixin/vaadin-theme-property-mixin").ThemePropertyMixinClass> & import("@open-wc/dedupe-mixin").Constructor<import("@vaadin/component-base/src/dir-mixin").DirMixinClass> & import("@open-wc/dedupe-mixin").Constructor<import("@vaadin/component-base/src/element-mixin").ElementMixinClass>;
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
export class Tooltip extends Tooltip_base {
    static get template(): HTMLTemplateElement;
    static get is(): string;
    static get version(): string;
    static get properties(): {
        /**
         * The id of the target element. Must be a sibling.
         */
        for: {
            type: StringConstructor;
        };
        /**
         * Tooltip position. Possible values: top, right, left and bottom.
         */
        position: {
            type: StringConstructor;
            value: string;
        };
        align: {
            type: StringConstructor;
            value: string;
        };
        /**
         * Is the tooltip hidden.
         */
        hidden: {
            type: BooleanConstructor;
            value: boolean;
            notify: boolean;
            reflectToAttribute: boolean;
            observer: string;
        };
        /**
         * Enable manual mode.
         */
        manual: {
            type: BooleanConstructor;
            value: boolean;
            reflectToAttribute: boolean;
        };
        /**
         * The tooltip is attached to this element.
         */
        targetElement: {
            type: ObjectConstructor;
            observer: string;
        };
        /**
         * Show/hide tooltip close button.
         */
        closeButton: {
            type: BooleanConstructor;
            value: boolean;
            reflectToAttribute: boolean;
        };
        /**
         * Set tooltip theme.
         */
        theme: {
            type: StringConstructor;
            reflectToAttribute: boolean;
        };
    };
    static get observers(): string[];
    _boundShow: any;
    _boundHide: any;
    _boundOnKeyup: any;
    connectedCallback(): void;
    ready(): void;
    _resizeTimeout: NodeJS.Timeout;
    _manualObserver(): void;
    _attachToTarget(targetElement: any, oldTargetElement: any): void;
    _addEvents(): void;
    _detachFromTarget(): void;
    _removeEvents(): void;
    _removeTargetEvents(target: any): void;
    _updateTarget(): void;
    targetElement: Element;
    _setPosition(targetElement: any, hidden: any, position: any): void;
    get _parentPostioned(): boolean;
    _setPositionInVisibleBounds(parentRectHeight: any, parentRectWidth: any, tooltipLeft: any, tooltipTop: any, thisRect: any): void;
    _calculateLeft(targetLeft: any, targetRect: any, thisRect: any, horizontalCenterOffset: any): any;
    _calculateTop(targetTop: any, targetRect: any, thisRect: any, verticalCenterOffset: any, pageYOffset: any): any;
    show(): void;
    hide(): void;
    _hiddenChanged(hidden: any): void;
    _onKeyup(e: any): void;
    _setDefaultId(): void;
}
import { PolymerElement } from "@polymer/polymer";
export {};
//# sourceMappingURL=vcf-tooltip.d.ts.map