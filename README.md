# Kompendium klasické homeopatie

Interaktivní studijní materiál pro přípravu ke zkoušce z homeopatie na úrovni
profesního/lékařského diplomu (rámec ECH / LMHI). Výstupem je **jeden samostatný
soubor HTML** bez jakýchkoli externích závislostí — funguje offline, stačí ho
otevřít v prohlížeči.

## Výstup

```
build/kompendium-homeopatie.html
```

Obsahuje 11 zkouškových okruhů, slovník pojmů, referenční přílohu 100 léků
a interaktivní test. Veškeré CSS a JS je vloženo, grafika je nativní SVG,
dokument je responzivní od 320 px a optimalizovaný pro tisk se stránkováním
po kapitolách.

## Sestavení

```bash
python3 build.py            # sestaví výsledný HTML soubor
python3 build.py --check    # sestaví a zvaliduje integritu
```

Kontrola integrity ověřuje, že všechny kotvy v dokumentu existují, že každý
pojem označený v textu má heslo ve slovníku a že testové otázky odkazují
na existující kapitoly a hesla. Návratový kód je nenulový, pokud je nalezena chyba.

## Struktura zdrojů

```
src/
  base.css              jeden design systém (světlý i tmavý režim, tisk)
  app.js                router, slovník, fulltext, filtr léků, testový engine
  shell-top.html        horní lišta, postranní navigace, drobečky
  hub.html              rozcestník s dlaždicemi a postupem studia
  views-tail.html       slovník, příloha léků, test, zápatí
  chapters/ch1–ch11.html  obsahové kapitoly
  data/
    meta.json           názvy a popisy okruhů (řídí navigaci i dlaždice)
    glossary.js         hesla slovníku
    remedies.js         karty 100 léků
    quiz.js             banka testových otázek
build.py                sestavení a kontrola integrity
```

## Jak přidat obsah

- **Kapitolu** upravíte v `src/chapters/`. Sekce označujte `<h2 id="chN-M">` —
  postranní obsah kapitoly se generuje automaticky z nadpisů druhé úrovně.
- **Pojem do slovníku** přidáte do `src/data/glossary.js`; v textu ho označíte
  jako `<a class="term" data-term="slug">…</a>`. Zpětné odkazy z hesla do kapitol
  se vytvářejí za běhu podle skutečného výskytu, není třeba je udržovat ručně.
- **Testovou otázku** přidáte do `src/data/quiz.js` (typy `single`, `multi`,
  `match`, `cas`). Pole `ref` a `term` musí ukazovat na existující kotvu a heslo —
  `--check` to ověří.

## Poznámka o povaze textu

Materiál důsledně odděluje dvě vrstvy: **nauku**, jak ji učí homeopatická tradice
a jak ji očekává zkouška, a **vědecký konsensus včetně kritiky**, který je
u převažujícího stanoviska k účinnosti odmítavý. Obě vrstvy mají vlastní grafické
značení a nevydávají se jedna za druhou. Materiál není lékařský návod.
