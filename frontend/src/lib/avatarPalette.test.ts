import { describe, expect, it } from 'vitest';
import { HUMAN_PALETTE, paletteClass } from './avatarPalette';

describe('paletteClass', () => {
  it('is deterministic for the same name', () => {
    expect(paletteClass('Mischa Dasberg')).toBe(paletteClass('Mischa Dasberg'));
  });

  it('always picks one of the stylesheet palettes', () => {
    for (const name of ['Mischa Dasberg', 'Amara Okoye', 'Jonas Vik', 'Priya Rao', 'Sam Beck']) {
      expect(HUMAN_PALETTE).toContain(paletteClass(name));
    }
  });

  it('spreads distinct names across more than one palette', () => {
    const names = ['Mischa Dasberg', 'Amara Okoye', 'Jonas Vik', 'Priya Rao', 'Sam Beck'];
    const classes = new Set(names.map(paletteClass));
    expect(classes.size).toBeGreaterThan(1);
  });
});
