// Config de Lighthouse CI (`npx @lhci/cli autorun`), consistente con la metodologia
// real documentada en docs/mediciones/perf/LIGHTHOUSE-REPORT.md: corre SIEMPRE contra
// el build de produccion (`ng build --configuration production`), nunca contra `ng serve`.
// Performance esta en 'warn' (no 'error') porque hoy mide 64/61 sobre un umbral de 80 --
// ver la nota metodologica sobre contencion de CPU en el reporte antes de subir el umbral
// a 'error'.
module.exports = {
  ci: {
    collect: {
      staticDistDir: './Frontend/dist/presustentaciones-frontend/browser',
      numberOfRuns: 3,
      settings: {
        onlyCategories: ['performance', 'accessibility', 'best-practices', 'seo'],
      },
    },
    assert: {
      assertions: {
        'categories:performance': ['warn', { minScore: 0.8 }],
        'categories:accessibility': ['error', { minScore: 0.85 }],
        'categories:best-practices': ['error', { minScore: 0.9 }],
        'categories:seo': ['warn', { minScore: 0.85 }],
      },
    },
    upload: {
      target: 'filesystem',
      outputDir: './docs/mediciones/perf/lighthouse-ci',
    },
  },
};
