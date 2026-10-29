// QuietGuard: one deep-teal field under the whole strip, with quiet ripples (the scalloped shapes
// of the Home shield) spreading across the card seams. Everything is drawn in panorama coordinates,
// so the strip joins exactly in the store carousel.
import {panoramaGradient, panoramaLights, panoramaX, shapes} from './studio/shapes.js';

// Ripple centres in card widths across the strip, and heights as a fraction of the canvas. Sets of different sizes
// rise from the bottom edge and from the seams at mid-height, never behind a headline, and each set's outer ring
// stays clear of its neighbours' (checked for the phone strip), so no two sets ever cross.
const ripples = [
  {x: .25, y: .96, rings: 3, step: .07},
  {x: 1.05, y: .54, rings: 4, step: .06},
  {x: 2.2, y: .98, rings: 5, step: .075},
  {x: 3.25, y: .58, rings: 3, step: .06},
  {x: 4.1, y: .99, rings: 4, step: .07},
  {x: 5.55, y: .93, rings: 4, step: .08},
  {x: 6.5, y: .54, rings: 3, step: .06},
  {x: 7.45, y: .97, rings: 4, step: .08},
];

const MOODS = {
  night: {field: ['#0a2a36', '#0d3b45', '#0b3140', '#0f4448', '#0b2f3c', '#0d3a45'], shade: '#03141c', shadeTop: .35, shadeBottom: .3, glow: '#2bd3ac', ring: '#9af5e0'},
  mint: {field: ['#c9f4e8', '#dcf9ef', '#bff0e2', '#d6f7ec'], shade: '#ffffff', shadeTop: .4, shadeBottom: 0, glow: '#ffffff', ring: '#2a9c84'},
  coral: {field: ['#f7826b', '#f88f73', '#f67c68', '#f98d70'], shade: '#ffffff', shadeTop: .15, shadeBottom: 0, glow: '#ffd2c2', ring: '#ffffff', ringBoost: 2.1, ringWeight: 1.5},
  sunrise: {field: ['#ffd9b8', '#ffc4a8', '#ffb29e', '#ffc9ae'], shade: '#ffffff', shadeTop: .35, shadeBottom: 0, glow: '#ffffff', ring: '#e8846b'},
};

function art(ctx) {
  const {W, H} = ctx;
  const unit = Math.max(W, H);
  // Coral by default: the same ripples on a bright warm field, so the app is the boldest thing in view.
  const mood = MOODS[ctx.slide?.mood || 'coral'] || MOODS.coral;
  let defs = panoramaGradient(ctx, 'field', mood.field);
  defs += `<linearGradient id="dusk" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${mood.shade}" stop-opacity="${mood.shadeTop}"/><stop offset=".45" stop-color="${mood.shade}" stop-opacity="0"/><stop offset="1" stop-color="${mood.shade}" stop-opacity="${mood.shadeBottom}"/></linearGradient>`;
  let body = `<rect width="${W}" height="${H}" fill="url(#field)"/>`;
  // A soft light behind every card's product, wide enough to spill into the neighbours.
  body += panoramaLights(ctx, [{x: .5, y: W > H ? .55 : .62, r: .62, color: mood.glow, stops: [[0, .26], [.55, .07], [1, 0]]}], {id: 'glow'});
  body += '<g mask="url(#quiet)">';
  // An image can opt out (the comparison) or place its own sets (the tablet spread, centred behind the tablet).
  // An image's own sets are placed within that image; the shared sets run across the whole strip.
  const own = Array.isArray(ctx.slide?.ripples);
  for (const r of ctx.slide?.ripples === false ? [] : own ? ctx.slide.ripples : ripples) {
    const cx = own ? W * r.x : panoramaX(ctx, r.x), cy = H * r.y;
    if (cx < -unit * 1.6 || cx > W + unit * 1.6) continue;
    for (let i = 0; i < r.rings; i++) {
      const radius = unit * (.1 + i * r.step);
      const opacity = Math.min(.6, (.2 - i * .028) * (mood.ringBoost || 1)).toFixed(3);
      body += `<g fill="none" stroke="${mood.ring}" stroke-opacity="${opacity}" stroke-width="${((4 - i * .4) * (mood.ringWeight || 1)).toFixed(1)}">${shapes.cookie(cx, cy, radius, i * .17)}</g>`;
    }
    body += `<g fill="${mood.ring}" fill-opacity=".06">${shapes.cookie(cx, cy, unit * .12, 0)}</g>`;
  }
  body += '</g>';
  // The night field fades its ripples under the headline; light fields let them run freely, text included.
  const [axis, hush] = mood === MOODS.night ? ['y', [[0, .35], [.3, .45], [.45, 1]]] : ['y', [[0, 1], [1, 1]]];
    defs += `<linearGradient id="hush" x1="0" y1="0" x2="${axis === 'x' ? 1 : 0}" y2="${axis === 'y' ? 1 : 0}">${hush.map(([o, a]) => `<stop offset="${o}" stop-color="#fff" stop-opacity="${a}"/>`).join('')}</linearGradient><mask id="quiet" maskUnits="userSpaceOnUse" x="0" y="0" width="${W}" height="${H}"><rect width="${W}" height="${H}" fill="url(#hush)"/></mask>`;
  return `<defs>${defs}</defs>${body}<rect width="${W}" height="${H}" fill="url(#dusk)"/>`;
}

export default {
  art,
  continuous: true,

  // Every card sits on coral unless it asks for another mood; light moods swap the night frames for white ones.
  compose({main, slide}) {
    const mood = slide.mood || 'coral';
    if (mood !== 'night') main.classList.add('light-field', `mood-${mood}`);
  },
};
