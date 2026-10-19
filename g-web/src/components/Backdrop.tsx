const STARS = Array.from({ length: 46 }, (_, i) => ({
  cx: (91 + i * 337.7) % 1600,
  cy: (18 + i * 151.3) % 270,
  r: i % 5 === 0 ? 1.8 : 1.1,
}));

const WINDOWS = [0, 1, 2].flatMap((row) =>
  [0, 1, 2, 3, 4, 5, 6, 7].map((col) => ({ x: 668 + col * 34 + (col > 3 ? 32 : 0), y: 262 + row * 34, lit: (row * 8 + col) % 3 === 1 })),
);

export default function Backdrop() {
  return (
    <div className="scene" aria-hidden="true">
      <svg className="scene-svg" viewBox="0 0 1600 560" preserveAspectRatio="xMidYMax slice">
        <defs>
          <linearGradient id="scene-sky" x1="0" y1="0" x2="0" y2="560" gradientUnits="userSpaceOnUse">
            <stop offset="0" style={{ stopColor: 'var(--sky-horizon)', stopOpacity: 0 }} />
            <stop offset="0.65" style={{ stopColor: 'var(--sky-horizon)', stopOpacity: 1 }} />
          </linearGradient>
          <mask id="scene-crescent">
            <circle cx="1220" cy="150" r="42" fill="#fff" />
            <circle cx="1240" cy="137" r="37" fill="#000" />
          </mask>
        </defs>

        <rect width="1600" height="560" fill="url(#scene-sky)" />

        <g className="scene-stars">
          {STARS.map((s, i) => (
            <circle key={i} cx={s.cx} cy={s.cy} r={s.r} />
          ))}
        </g>

        <g className="scene-sun">
          <circle cx="1220" cy="150" r="120" fill="var(--sun-halo)" />
          <circle cx="1220" cy="150" r="84" fill="var(--sun-halo)" />
          <circle cx="1220" cy="150" r="52" fill="var(--sun)" />
        </g>
        <g className="scene-moon">
          <circle cx="1220" cy="150" r="72" fill="var(--sun-halo)" />
          <circle cx="1220" cy="150" r="42" fill="var(--sun)" mask="url(#scene-crescent)" />
        </g>

        <g className="scene-clouds">
          <circle cx="260" cy="140" r="20" />
          <circle cx="290" cy="128" r="28" />
          <circle cx="322" cy="142" r="18" />
          <rect x="242" y="138" width="96" height="22" rx="11" />
          <circle cx="1440" cy="96" r="16" />
          <circle cx="1464" cy="86" r="22" />
          <circle cx="1488" cy="98" r="14" />
          <rect x="1426" y="94" width="78" height="18" rx="9" />
          <circle cx="540" cy="84" r="11" />
          <circle cx="556" cy="77" r="15" />
          <circle cx="572" cy="86" r="10" />
          <rect x="530" y="82" width="52" height="13" rx="6.5" />
        </g>

        <g className="scene-birds">
          <path d="M980 120q7-7 14 0q7-7 14 0" />
          <path d="M1018 100q5-5 10 0q5-5 10 0" />
          <path d="M1046 128q4-4 8 0q4-4 8 0" />
        </g>

        <g fill="var(--far)">
          <path d="M0 360C200 346 400 350 600 354S1000 342 1200 348S1500 344 1600 350V560H0Z" />
          <path d="M70 356V300H118V356ZM130 356V276H170V356ZM182 356V318H230V356Z" />
          <path d="M1330 352V292H1372V352ZM1384 352V262H1416L1430 276V352ZM1442 352V310H1494V352Z" />
        </g>

        <path fill="var(--mid)" d="M0 400C160 372 300 376 430 388S650 384 780 380S1000 392 1120 390S1330 364 1450 368S1570 380 1600 382V560H0Z" />

        <g className="scene-college">
          <path className="scene-building" d="M640 392V246H960V392ZM624 250 800 182 976 250Z" />
          <path className="scene-building" d="M772 246V214H828V246Z" />
          <circle className="scene-clock" cx="800" cy="226" r="11" />
          <path className="scene-line" d="M800 226V219M800 226 805 229" />
          <path className="scene-line" d="M800 182V146" />
          <path className="scene-flag" d="M800 146H826L820 154 826 162H800Z" />
          <path className="scene-door" d="M784 392V350A16 16 0 0 1 816 350V392Z" />
          {WINDOWS.map((w, i) => (
            <rect key={i} className={w.lit ? 'scene-window is-lit' : 'scene-window'} x={w.x} y={w.y} width="18" height="22" rx="2" />
          ))}
        </g>

        <path fill="var(--near)" d="M0 460C140 440 260 430 380 442S600 470 760 466S980 436 1100 434S1320 458 1460 460S1570 454 1600 452V560H0Z" />

        <path className="scene-path" d="M800 392C790 420 730 440 700 470S690 540 700 560" />

        <g className="scene-lamp">
          <path className="scene-line scene-line--thick" d="M380 446V368M380 368H394" />
          <circle className="scene-led" cx="398" cy="372" r="6" />
        </g>
        <g className="scene-lamp">
          <path className="scene-line scene-line--thick" d="M1040 438V362M1040 362H1026" />
          <circle className="scene-led" cx="1022" cy="366" r="6" />
        </g>

        <g className="scene-tree">
          <path className="scene-trunk" d="M1260 452V360M1260 392 1232 364M1260 380 1290 352" />
          <g className="scene-canopy">
            <circle cx="1232" cy="340" r="34" />
            <circle cx="1270" cy="318" r="42" />
            <circle cx="1304" cy="344" r="30" />
            <circle cx="1258" cy="356" r="28" />
          </g>
        </g>

        <g className="scene-student">
          <circle cx="1214" cy="406" r="10" />
          <path className="scene-limb" d="M1216 418 1222 442M1222 442H1250L1256 456" />
          <path className="scene-arm" d="M1218 426 1238 436" />
          <path className="scene-laptop" d="M1232 440H1262" />
          <path className="scene-screen" d="M1262 440 1254 422" />
          <path className="scene-books" d="M1190 452H1208V446H1190ZM1192 446H1206V440H1192Z" />
        </g>
      </svg>
    </div>
  );
}
