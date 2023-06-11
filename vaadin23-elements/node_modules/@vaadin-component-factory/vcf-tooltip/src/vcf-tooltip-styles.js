import '@vaadin/vaadin-lumo-styles/color';
import '@vaadin/vaadin-lumo-styles/style';
import '@vaadin/vaadin-lumo-styles/sizing';
import '@vaadin/vaadin-lumo-styles/spacing';
import '@vaadin/vaadin-lumo-styles/typography';
import { registerStyles, css } from '@vaadin/vaadin-themable-mixin/register-styles.js';

export const styles = css`
  :host {
    /* Sizing */
    --lumo-tooltip-size: var(--lumo-size-m);
    /* Style */
    font-family: var(--lumo-font-family);
    font-size: var(--lumo-font-size-s, 0.875rem);
    line-height: var(--lumo-line-height-s, 1.375);
    cursor: default;
    -webkit-tap-highlight-color: transparent;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
  }
`;

registerStyles('vcf-tooltip', styles, {
  include: ['lumo-color', 'lumo-typography']
});
