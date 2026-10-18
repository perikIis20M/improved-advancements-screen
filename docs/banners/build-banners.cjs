// Compact, deterministic heading graphics using the mod icon and Minecraft's bitmap glyphs.
const fs = require('node:fs');
const path = require('node:path');
const sharp = require('sharp');

const root = __dirname;
const source = path.join(root, 'source');
const glyphs = JSON.parse(fs.readFileSync(path.join(source, 'glyphs.json'), 'utf8').replace(/^\uFEFF/, ''));
const logo = fs.readFileSync(path.join(source, 'mod-icon.png')).toString('base64');
const paper = JSON.parse(fs.readFileSync(path.join(source, 'paper-pixels.json'), 'utf8').replace(/^\uFEFF/, ''));
const banners = [
  { name: 'modern-screen', text: 'MODERN SCREEN', icon: 'diamond' },
  { name: 'requirements', text: "WHAT'S LEFT", icon: 'paper' },
  { name: 'old-screen', text: 'OLD SCREEN', icon: 'book' },
  { name: 'settings', text: 'MAKE IT YOURS', icon: 'pickaxe' },
];

function framedIcon(kind) {
  const box = kind === 'diamond' ? '367 161 520 520'
    : kind === 'pickaxe' ? '779 725 392 392' : '83 725 392 392';
  let icon = `<svg x="8" y="8" width="80" height="80" viewBox="${box}" overflow="hidden"><image href="data:image/png;base64,${logo}" width="1254" height="1254" style="image-rendering:pixelated"/></svg>`;
  if (kind === 'paper') icon += `<rect x="23" y="23" width="50" height="50" fill="#82828a"/><g shape-rendering="crispEdges">${paper.map(([x, y, color]) => `<rect x="${24 + x * 3}" y="${24 + y * 3}" width="3" height="3" fill="${color}"/>`).join('')}</g>`;
  return icon;
}

function lettering(text) {
  let x = 112;
  let d = '';
  for (const letter of text) {
    const glyph = glyphs[letter];
    if (!glyph) throw new Error(`Missing native glyph: ${letter}`);
    for (const [px, py] of glyph.pixels) d += `M${x + px * 8},${20 + py * 8}h8v8h-8z`;
    x += glyph.advance * 8;
  }
  if (x > 792) throw new Error(`Heading clips: ${text}`);
  return `<g shape-rendering="crispEdges"><path d="${d}" fill="#343438" transform="translate(2 2)"/><path d="${d}" fill="#f0f0f2"/></g>`;
}

(async () => {
  for (const banner of banners) {
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="800" height="96" viewBox="0 0 800 96" role="img" aria-label="${banner.text}"><title>${banner.text}</title>${framedIcon(banner.icon)}${lettering(banner.text)}</svg>`;
    fs.writeFileSync(path.join(root, `${banner.name}.svg`), svg + '\n');
    await sharp(Buffer.from(svg)).png({ palette: true }).toFile(path.join(root, `${banner.name}.png`));
    console.log(`${banner.name}.png: 800 x 96, transparent, native bitmap lettering`);
  }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
