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

function art(ctx) {
  const {W, H, slides} = ctx;
  const unit = Math.max(W, H);
  let defs = panoramaGradient(ctx, 'field', ['#0a2a36', '#0d3b45', '#0b3140', '#0f4448', '#0b2f3c', '#0d3a45']);
  defs += `<linearGradient id="dusk" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#03141c" stop-opacity=".35"/><stop offset=".45" stop-color="#03141c" stop-opacity="0"/><stop offset="1" stop-color="#03141c" stop-opacity=".3"/></linearGradient>`;
  let body = `<rect width="${W}" height="${H}" fill="url(#field)"/>`;
  // A soft light behind every card's product, wide enough to spill into the neighbours.
  slides.forEach((_, k) => {
    const cx = panoramaX(ctx, k + .5), cy = H * (W > H ? .55 : .62);
    defs += `<radialGradient id="glow${k}" gradientUnits="userSpaceOnUse" cx="${cx}" cy="${cy}" r="${unit * .62}"><stop offset="0" stop-color="#2bd3ac" stop-opacity=".26"/><stop offset=".55" stop-color="#2bd3ac" stop-opacity=".07"/><stop offset="1" stop-color="#2bd3ac" stop-opacity="0"/></radialGradient>`;
    body += `<rect width="${W}" height="${H}" fill="url(#glow${k})"/>`;
  });
  body += '<g mask="url(#quiet)">';
  for (const r of ripples) {
    const cx = panoramaX(ctx, r.x), cy = H * r.y;
    if (cx < -unit * 1.6 || cx > W + unit * 1.6) continue;
    for (let i = 0; i < r.rings; i++) {
      const radius = unit * (.12 + i * .13);
      const opacity = (.2 - i * .028).toFixed(3);
      body += `<g fill="none" stroke="#9af5e0" stroke-opacity="${opacity}" stroke-width="${(4 - i * .4).toFixed(1)}">${shapes.cookie(cx, cy, radius, i * .17)}</g>`;
    }
    body += `<g fill="#7ff3d8" fill-opacity=".06">${shapes.cookie(cx, cy, unit * .12, 0)}</g>`;
  }
  body += '</g>';
  // Ripples stay faint behind the headline and open up around the screens.
  defs += `<linearGradient id="hush" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff" stop-opacity=".35"/><stop offset=".3" stop-color="#fff" stop-opacity=".45"/><stop offset=".45" stop-color="#fff" stop-opacity="1"/></linearGradient><mask id="quiet" maskUnits="userSpaceOnUse" x="0" y="0" width="${W}" height="${H}"><rect width="${W}" height="${H}" fill="url(#hush)"/></mask>`;
  return `<defs>${defs}</defs>${body}<rect width="${W}" height="${H}" fill="url(#dusk)"/>`;
}

export default {
  art,

  compose({index, main, kind}) {
    if (index !== 0 || kind === 'readme') return;
    // The app opens the set; later cards carry only their headline.
    main.classList.add('first');
    const brand = document.createElement('div');
    brand.className = 'brand';
    brand.dataset.checkBounds = '';
    brand.innerHTML = '<img src="../brand/icon.png" alt="">QuietGuard';
    main.append(brand);
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

  // Screens may run off the bottom (or hang from the top on the stacked card), never off the sides,
  // and most of each screen stays in view.
  decorate({W, H, slide, devices}) {
    if (slide.layout === 'compare') {
      // Each name sits just under its screen.
      for (const label of devices[0].node.parentElement.querySelectorAll('.device-label')) label.style.setProperty('--h', `${devices[0].box.height}px`);
      // A scalloped "New" sticker on the top corner of each new screen.
      for (const {spec, box, node} of devices) {
        if (!/after/.test(spec.role || '')) continue;
        const sticker = document.createElement('div');
        sticker.className = 'sticker';
        sticker.dataset.checkBounds = '';
        sticker.innerHTML = `<svg viewBox="0 0 150 150" width="150" height="150"><g fill="#7ff3d8">${shapes.cookie(75, 75, 72)}</g>` +
          '<text x="75" y="88" text-anchor="middle" font-family="Quiet" font-weight="750" font-size="38" fill="#062a2a">New</text></svg>';
        Object.assign(sticker.style, {left: `${box.left + box.width - 96}px`, top: `${box.top - 82}px`});
        node.parentElement.append(sticker);
      }
      return;
    }
    const stacked = /stack/.test(slide.layout || '');
    for (const {box, spec} of devices) {
      if (box.left < 0 || box.left + box.width > W + 1) throw new Error(`Screen leaves the side of the card: ${spec.raw}`);
      if (stacked) continue;
      if (box.top < 0) throw new Error(`Screen leaves the top of the card: ${spec.raw}`);
      if ((H - box.top) / box.height < .62) throw new Error(`Too little of ${spec.raw} is visible`);
    }
  },
};
