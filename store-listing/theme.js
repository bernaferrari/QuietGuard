// QuietGuard: one deep-teal field under the whole strip, with quiet ripples (the scalloped shapes
// of the Home shield) spreading across the card seams. Everything is drawn in panorama coordinates,
// so the strip joins exactly in the store carousel.
import {panoramaGradient, panoramaX, shapes} from './studio/shapes.js';

// Ripple centres in card widths across the whole strip, and heights as a fraction of the canvas.
// They sit on or near the seams, so each set of rings is shared by two neighbouring cards.
const ripples = [
  {x: .5, y: .2, rings: 5},
  {x: 1.95, y: .86, rings: 6},
  {x: 3.6, y: .14, rings: 5},
  {x: 5.0, y: .9, rings: 6},
  {x: 6.55, y: .18, rings: 5},
  {x: 8.0, y: .82, rings: 6},
];

const MOODS = {
  night: {field: ['#0a2a36', '#0d3b45', '#0b3140', '#0f4448', '#0b2f3c', '#0d3a45'], shade: '#03141c', shadeTop: .35, shadeBottom: .3, glow: '#2bd3ac', ring: '#9af5e0'},
  mint: {field: ['#c9f4e8', '#dcf9ef', '#bff0e2', '#d6f7ec'], shade: '#ffffff', shadeTop: .4, shadeBottom: 0, glow: '#ffffff', ring: '#2a9c84'},
  coral: {field: ['#f7826b', '#f88f73', '#f67c68', '#f98d70'], shade: '#ffffff', shadeTop: .15, shadeBottom: 0, glow: '#ffd2c2', ring: '#ffffff'},
  sunrise: {field: ['#ffd9b8', '#ffc4a8', '#ffb29e', '#ffc9ae'], shade: '#ffffff', shadeTop: .35, shadeBottom: 0, glow: '#ffffff', ring: '#e8846b'},
};

function art(ctx) {
  const {W, H, slides} = ctx;
  const unit = Math.max(W, H);
  // Coral by default: the same ripples on a bright warm field, so the app is the boldest thing in view.
  const mood = MOODS[ctx.slide?.mood || 'coral'] || MOODS.coral;
  let defs = panoramaGradient(ctx, 'field', mood.field);
  defs += `<linearGradient id="dusk" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${mood.shade}" stop-opacity="${mood.shadeTop}"/><stop offset=".45" stop-color="${mood.shade}" stop-opacity="0"/><stop offset="1" stop-color="${mood.shade}" stop-opacity="${mood.shadeBottom}"/></linearGradient>`;
  let body = `<rect width="${W}" height="${H}" fill="url(#field)"/>`;
  // A soft light behind every card's product, wide enough to spill into the neighbours.
  slides.forEach((_, k) => {
    const cx = panoramaX(ctx, k + .5), cy = H * (W > H ? .55 : .62);
    defs += `<radialGradient id="glow${k}" gradientUnits="userSpaceOnUse" cx="${cx}" cy="${cy}" r="${unit * .62}"><stop offset="0" stop-color="${mood.glow}" stop-opacity=".26"/><stop offset=".55" stop-color="${mood.glow}" stop-opacity=".07"/><stop offset="1" stop-color="${mood.glow}" stop-opacity="0"/></radialGradient>`;
    body += `<rect width="${W}" height="${H}" fill="url(#glow${k})"/>`;
  });
  body += '<g mask="url(#quiet)">';
  for (const r of ripples) {
    const cx = panoramaX(ctx, r.x), cy = H * r.y;
    if (cx < -unit * 1.6 || cx > W + unit * 1.6) continue;
    for (let i = 0; i < r.rings; i++) {
      const radius = unit * (.12 + i * .13);
      const opacity = (.2 - i * .028).toFixed(3);
      body += `<g fill="none" stroke="${mood.ring}" stroke-opacity="${opacity}" stroke-width="${(4 - i * .4).toFixed(1)}">${shapes.cookie(cx, cy, radius, i * .17)}</g>`;
    }
    body += `<g fill="${mood.ring}" fill-opacity=".06">${shapes.cookie(cx, cy, unit * .12, 0)}</g>`;
  }
  body += '</g>';
  // Ripples stay faint behind the headline and open up around the screens.
  defs += `<linearGradient id="hush" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff" stop-opacity=".35"/><stop offset=".3" stop-color="#fff" stop-opacity=".45"/><stop offset=".45" stop-color="#fff" stop-opacity="1"/></linearGradient><mask id="quiet" maskUnits="userSpaceOnUse" x="0" y="0" width="${W}" height="${H}"><rect width="${W}" height="${H}" fill="url(#hush)"/></mask>`;
  return `<defs>${defs}</defs>${body}<rect width="${W}" height="${H}" fill="url(#dusk)"/>`;
}

export default {
  art,

  // Every card sits on coral unless it asks for another mood; light moods swap the night frames for white ones.
  compose({main, slide}) {
    const mood = slide.mood || 'coral';
    if (mood !== 'night') main.classList.add('light-field', `mood-${mood}`);
  },

  /** Bezel and corners follow the frame's width, so small and large screens look like one device family. */
  layout({main}) {
    for (const node of main.querySelectorAll('.device:not(.device-frame-none)')) {
      if (main.classList.contains('compare')) break;
      const image = node.querySelector('img'), width = node.clientWidth;
      const landscape = image.naturalWidth > image.naturalHeight;
      const tablet = node.classList.contains('device-frame-tablet');
      node.style.setProperty('--bezel', `${Math.round(width * (landscape ? .012 : tablet ? .018 : .02))}px`);
      node.style.setProperty('--radius', `${Math.round(width * (landscape ? .035 : tablet ? .05 : .09))}px`);
    }
  },

  // Screens may run off the bottom (or hang from the top on the stacked card), never off the sides (except the
  // README spread),
  // and most of each screen stays in view.
  decorate({W, H, slide, devices}) {
    if (slide.layout === 'compare') {
      // Each name sits just under its screen.
      for (const label of devices[0].node.parentElement.querySelectorAll('.device-label')) label.style.setProperty('--h', `${devices[0].box.height}px`);
      return;
    }
    const stacked = /stack/.test(slide.layout || '');
    // The README spread and the phone tablet card run their tablet off the right edge on purpose.
    const bleeds = slide.layout === 'spread' || slide.layout === 'bleed';
    for (const {box, spec} of devices) {
      if (!bleeds && (box.left < 0 || box.left + box.width > W + 1)) throw new Error(`Screen leaves the side of the card: ${spec.raw}`);
      if (stacked) continue;
      if (box.top < 0) throw new Error(`Screen leaves the top of the card: ${spec.raw}`);
      if ((H - box.top) / box.height < .62) throw new Error(`Too little of ${spec.raw} is visible`);
    }
  },
};
