// QuietGuard's store screens on the capture emulator. The capture build seeds fictional apps and traffic and never
// starts the VPN or touches the network. `npx screenshot-studio recapture` installs it, runs this tour for every
// language and device size, and exports. Alone: npx screenshot-studio tour [--profile phone] [--locale de-DE].

/** A navigation tab, by its text: the Home hero's toggle is also described as "Firewall". */
const tab = (d, label) => d.tap(label, {textOnly: true});
/** The 10-inch tablet shows the list and the detail side by side. */
const wide = d => d.profile === 'tablet10';

/** @type {import('screenshot-studio').Tour} */
export default {
  app: 'com.bernaferrari.quietguard.storecapture',
  activity: 'com.bernaferrari.quietguard.ActivityMain',
  scenes: {
    light: {
      '01-home': null,
      async '02-firewall'(d) { await tab(d, 'Firewall'); if (wide(d)) await d.tap('Camera'); },
      '04-detail': d => wide(d) ? false : d.tap('Camera'),
      async '03-log'(d) { if (!wide(d)) await d.back(); await tab(d, 'Log'); },
      async '08-blocked'(d) { await d.tap('Blocked'); await d.tap('By app'); },
      '05-settings': d => tab(d, 'Settings'),
      '06-rules': d => d.profile === 'phone' ? d.tap('Firewall rules') : false,
      async '07-appearance'(d) {
        if (d.profile !== 'phone') return false;
        await d.back();
        for (let i = 0; i < 4; i++) await d.scroll(.55, {start: .75, duration: 600});
      },
    },
    dark: {
      '09-home-dark': null,
      async '10-firewall-dark'(d) { await tab(d, 'Firewall'); if (wide(d)) await d.tap('Camera'); },
    },
  },
};
