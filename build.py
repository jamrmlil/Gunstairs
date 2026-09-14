#!/usr/bin/env python3
"""
Build kompendia: sestaví jeden samostatný HTML soubor z modulů v src/.
Použití:  python3 build.py             → build/kompendium-homeopatie.html
          python3 build.py --check     → build + kontrola integrity
          python3 build.py --artifact  → navíc build/kompendium-artefakt.html
                                         (bez <html>/<head>, pro publikaci jako Artifact)
"""
import json, os, re, sys, html

ROOT = os.path.dirname(os.path.abspath(__file__))
SRC  = os.path.join(ROOT, 'src')
OUT  = os.path.join(ROOT, 'build', 'kompendium-homeopatie.html')
OUT_ARTIFACT = os.path.join(ROOT, 'build', 'kompendium-artefakt.html')

def read(*p):
    fp = os.path.join(SRC, *p)
    if not os.path.exists(fp):
        return ''
    with open(fp, encoding='utf-8') as f:
        return f.read()

meta = json.loads(read('data', 'meta.json'))
CH = meta['chapters']

# ---------- postranní navigace ----------
def sidenav():
    o = ['<h2>Kompendium</h2><ol>']
    o.append('<li><a href="#hub" data-view="hub"><span class="num">•</span><span>Rozcestník</span></a></li>')
    o.append('</ol><h2>Zkouškové okruhy</h2><ol>')
    for c in CH:
        o.append('<li><a href="#%s" data-view="%s"><span class="num">%d</span><span>%s</span></a></li>'
                 % (c['id'], c['id'], c['n'], html.escape(c['short'])))
    o.append('</ol><h2>Reference a ověření</h2><ol>')
    o.append('<li><a href="#slovnik" data-view="slovnik"><span class="num">A–Ž</span><span>Slovník pojmů</span></a></li>')
    o.append('<li><a href="#leky" data-view="leky"><span class="num">100</span><span>Léky ECH</span></a></li>')
    o.append('<li><a href="#test" data-view="test"><span class="num">✎</span><span>Interaktivní test</span></a></li>')
    o.append('</ol>')
    o.append('<h2>Nápověda</h2><p class="small" style="padding:0 .5rem;color:var(--c-ink-muted)">'
             '<kbd>/</kbd> hledat · <kbd>h</kbd> rozcestník · <kbd>←</kbd><kbd>→</kbd> kapitoly. '
             'Pojmy tečkovaně podtržené vedou do slovníku.</p>')
    return '\n'.join(o)

# ---------- dlaždice rozcestníku ----------
def tiles():
    o = []
    for c in CH:
        o.append(
          '<li><a class="tile" href="#%s" data-ch="%s" data-done="0">'
          '<span class="tile__n">OKRUH %d</span>'
          '<span class="tile__h">%s</span>'
          '<span class="tile__d">%s</span>'
          '<span class="tile__f"><span class="dot"></span><span class="state">neprostudováno</span>'
          '<span style="margin-left:auto">%s</span></span>'
          '</a></li>' % (c['id'], c['id'], c['n'], html.escape(c['title']),
                         html.escape(c['desc']), html.escape(c['pages'])))
    return '\n'.join(o)

# ---------- navigace mezi kapitolami (patička kapitoly) ----------
def chapnav(i):
    prev = CH[i-1] if i > 0 else None
    nxt  = CH[i+1] if i < len(CH)-1 else None
    o = ['<nav class="chapnav" aria-label="Navigace mezi kapitolami">']
    if prev:
        o.append('<a class="prev" href="#%s"><span class="lbl">← Předchozí okruh</span>'
                 '<span class="tt">%d. %s</span></a>' % (prev['id'], prev['n'], html.escape(prev['short'])))
    else:
        o.append('<a class="prev" href="#hub"><span class="lbl">←</span><span class="tt">Rozcestník</span></a>')
    if nxt:
        o.append('<a class="next" href="#%s"><span class="lbl">Další okruh →</span>'
                 '<span class="tt">%d. %s</span></a>' % (nxt['id'], nxt['n'], html.escape(nxt['short'])))
    else:
        o.append('<a class="next" href="#test"><span class="lbl">Pokračovat →</span>'
                 '<span class="tt">Interaktivní test</span></a>')
    o.append('</nav>')
    return '\n'.join(o)

def chapter_footer(c, i):
    return ('\n<div class="box box--exam" style="margin-top:2.5rem">'
            '<p class="box__t">Hotovo s okruhem %d?</p>'
            '<p>Označte si okruh jako prostudovaný — postup se ukládá v prohlížeči a ukáže se na rozcestníku. '
            'Pak si okruh procvičte v testu.</p>'
            '<p style="display:flex;gap:.6rem;flex-wrap:wrap;margin:0">'
            '<button class="btn btn--primary donebtn" type="button" data-ch="%s" aria-pressed="false">'
            'Označit okruh jako prostudovaný</button> '
            '<a class="btn" href="#test">Procvičit okruh v testu</a> '
            '<a class="btn" href="#hub">Zpět na rozcestník</a></p></div>\n'
            % (c['n'], c['id'])) + chapnav(i) + '\n</div>\n</section>\n'

# ---------- sestavení ----------
def build(artifact=False):
    """artifact=True: varianta pro Artifact — bez doctype, <html>, <head> a <body>,
    protože hostitel dodává vlastní skeleton. Obsah i chování jsou totožné."""
    parts = []
    if artifact:
        parts.append('<title>%s</title>\n<style>\n' % html.escape(meta['title']))
    else:
        parts.append('<!DOCTYPE html>\n<html lang="cs" data-theme="light">\n<head>\n'
                     '<meta charset="utf-8">\n'
                     '<meta name="viewport" content="width=device-width, initial-scale=1">\n'
                     '<meta name="description" content="%s">\n'
                     '<meta name="color-scheme" content="light dark">\n'
                     '<title>%s</title>\n<style>\n' % (html.escape(meta['subtitle']), html.escape(meta['title'])))
    parts.append(read('base.css'))
    parts.append('\n</style>\n' if artifact else '\n</style>\n</head>\n<body>\n')

    top = read('shell-top.html').replace('<!--SIDENAV-->', sidenav())
    parts.append(top)
    parts.append(read('hub.html').replace('<!--TILES-->', tiles()))

    missing = []
    for i, c in enumerate(CH):
        body = read('chapters', '%s.html' % c['id'])
        if not body.strip():
            missing.append(c['id'])
            body = ('<section class="view chapter" id="view-%s"><div class="wrap">'
                    '<h1>Okruh %d — %s</h1><p class="lead">(kapitola se připravuje)</p>'
                    % (c['id'], c['n'], html.escape(c['title'])))
        parts.append(body)
        parts.append(chapter_footer(c, i))

    parts.append(read('views-tail.html'))

    parts.append('\n<script>\nwindow.HK_META = %s;\n</script>\n'
                 % json.dumps(meta, ensure_ascii=False))
    for f in ('glossary.js', 'remedies.js', 'quiz.js'):
        parts.append('<script>\n' + read('data', f) + '\n</script>\n')
    parts.append('<script>\n' + read('app.js') + '\n</script>\n')
    if not artifact:
        parts.append('</body>\n</html>\n')

    doc = ''.join(parts)
    out = OUT_ARTIFACT if artifact else OUT
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, 'w', encoding='utf-8') as f:
        f.write(doc)
    return doc, missing

# ---------- kontrola integrity ----------
def check(doc):
    errs, warns = [], []
    ids = set(re.findall(r'\bid="([^"]+)"', doc))
    # kotvy — jen z HTML, nikoli z vloženého JavaScriptu (kde jsou href sestavovány za běhu)
    doc_html = re.sub(r'<script\b.*?</script>', '', doc, flags=re.S)
    anchors = set(a for a in re.findall(r'href="#([^"]+)"', doc_html) if a)
    js_ids = set()
    # id generovaná JS: gl-<slug>, gl-abc-<L>, tref-*, ix*
    gl = read('data', 'glossary.js')
    slugs = set(re.findall(r'\{\s*k\s*:\s*[\'"]([^\'"]+)[\'"]', gl))
    for s in slugs:
        js_ids.add('gl-' + s)
    for L in 'ABCDEFGHIJKLMNOPQRSTUVWXYZ':
        js_ids.add('gl-abc-' + L)
    known = ids | js_ids | {'hub'}
    view_keys = {m.replace('view-', '') for m in ids if m.startswith('view-')}
    known |= view_keys
    for a in sorted(anchors):
        if a in known:
            continue
        errs.append('mrtvá kotva: #' + a)
    # pojmy v textu vs. slovník
    used = set(re.findall(r'data-term="([^"]+)"', doc))
    for u in sorted(used - slugs):
        errs.append('pojem bez hesla ve slovníku: data-term="%s"' % u)
    for s in sorted(slugs - used):
        warns.append('heslo bez výskytu v textu: %s' % s)
    # test: reference na kapitoly
    quiz = read('data', 'quiz.js')
    for r in sorted(set(re.findall(r"ref\s*:\s*'([^']+)'", quiz))):
        if r not in known:
            errs.append('testová otázka odkazuje na neznámou kotvu: #' + r)
    for t in sorted(set(re.findall(r"term\s*:\s*'([^']+)'", quiz))):
        if t not in slugs:
            errs.append('testová otázka odkazuje na neznámé heslo: %s' % t)
    return errs, warns

def stats(doc):
    txt = re.sub(r'<script.*?</script>|<style.*?</style>', '', doc, flags=re.S)
    txt = re.sub(r'<[^>]+>', ' ', txt)
    words = len(txt.split())
    # slovník a testová banka jsou v <script>, ale jsou to studijní texty — počítají se
    for f in ('glossary.js', 'quiz.js', 'remedies.js'):
        data = re.sub(r'<[^>]+>', ' ', read('data', f))
        words += len(re.findall(r"[^\s'\"]+", data)) // 1
    quiz = read('data', 'quiz.js')
    return {
        'bytes': len(doc.encode('utf-8')),
        'words': words,
        'a4_lo': int(words / 500.0), 'a4_hi': int(words / 350.0),
        'glossary': len(re.findall(r'\{\s*k\s*:', read('data', 'glossary.js'))),
        'remedies': len(re.findall(r'\{\s*n\s*:', read('data', 'remedies.js'))),
        'quiz': len(re.findall(r'\{\s*id\s*:', quiz)),
        'control_q': doc.count('class="q"'),
        'figures': doc.count('<svg'),
        'tables': doc.count('<table'),
    }

if __name__ == '__main__':
    doc, missing = build()
    if '--artifact' in sys.argv:
        adoc, _ = build(artifact=True)
        print('→ %s  (varianta pro Artifact, %.2f MB)' % (OUT_ARTIFACT, len(adoc.encode('utf-8'))/1048576.0))
    s = stats(doc)
    print('→ %s' % OUT)
    print('   %.2f MB · %d slov · ~%d–%d stran A4 (podle sazby 350–500 slov/strana)'
          % (s['bytes']/1048576.0, s['words'], s['a4_lo'], s['a4_hi']))
    print('   %d hesel slovníku · %d léků · %d testových otázek · %d kontrolních otázek'
          % (s['glossary'], s['remedies'], s['quiz'], s['control_q']))
    print('   %d SVG grafik · %d tabulek' % (s['figures'], s['tables']))
    if missing:
        print('   CHYBÍ kapitoly: %s' % ', '.join(missing))
    if '--check' in sys.argv:
        errs, warns = check(doc)
        print('\n--- kontrola integrity ---')
        for e in errs:
            print('  CHYBA: ' + e)
        for w in warns[:40]:
            print('  pozn.: ' + w)
        print('  celkem: %d chyb, %d poznámek' % (len(errs), len(warns)))
        sys.exit(1 if errs else 0)
