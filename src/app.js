/* ============================================================
   KOMPENDIUM HOMEOPATIE — aplikační logika
   Bez závislostí. Vše běží lokálně, offline.
   Moduly: theme, router, progress, glossary, search, remedies, quiz
   ============================================================ */
(function(){
"use strict";

var LS = {
  theme:'hk.theme', done:'hk.done', quiz:'hk.quiz', pos:'hk.pos'
};
function lsGet(k,def){ try{ var v=localStorage.getItem(k); return v===null?def:JSON.parse(v); }catch(e){ return def; } }
function lsSet(k,v){ try{ localStorage.setItem(k,JSON.stringify(v)); }catch(e){} }
function $(s,r){ return (r||document).querySelector(s); }
function $$(s,r){ return Array.prototype.slice.call((r||document).querySelectorAll(s)); }
function el(tag,attrs,html){
  var n=document.createElement(tag);
  if(attrs) for(var k in attrs){ if(k==='class') n.className=attrs[k]; else n.setAttribute(k,attrs[k]); }
  if(html!=null) n.innerHTML=html;
  return n;
}
function esc(s){ return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }
function deacc(s){
  return String(s).toLowerCase()
    .replace(/[áàâä]/g,'a').replace(/[čç]/g,'c').replace(/ď/g,'d').replace(/[éěëè]/g,'e')
    .replace(/[íîï]/g,'i').replace(/ň/g,'n').replace(/[óôö]/g,'o').replace(/ř/g,'r')
    .replace(/š/g,'s').replace(/ť/g,'t').replace(/[úůüû]/g,'u').replace(/ý/g,'y').replace(/ž/g,'z');
}
function shuffle(a){ a=a.slice(); for(var i=a.length-1;i>0;i--){ var j=Math.floor(Math.random()*(i+1)); var t=a[i]; a[i]=a[j]; a[j]=t; } return a; }

/* ------------------------------------------------------------
   1. TÉMA
   ------------------------------------------------------------ */
var root=document.documentElement;
function applyTheme(t){ root.setAttribute('data-theme',t); }
applyTheme(lsGet(LS.theme, (window.matchMedia && matchMedia('(prefers-color-scheme: dark)').matches)?'dark':'light'));
$('#themebtn').addEventListener('click', function(){
  var t = root.getAttribute('data-theme')==='dark' ? 'light':'dark';
  applyTheme(t); lsSet(LS.theme,t);
});

/* ------------------------------------------------------------
   2. ROUTER — přepínání zobrazení
   ------------------------------------------------------------ */
var META = window.HK_META;
var CH = META.chapters;
var VIEWS = {};
$$('.view').forEach(function(v){ VIEWS[v.id.replace(/^view-/,'')] = v; });

var NAMES = {hub:'Rozcestník', slovnik:'Slovník pojmů', leky:'100 léků ECH', test:'Interaktivní test'};
CH.forEach(function(c){ NAMES[c.id] = 'Okruh '+c.n+' — '+c.title; });

function viewKeyOf(node){
  var v = node.closest ? node.closest('.view') : null;
  return v ? v.id.replace(/^view-/,'') : null;
}
var current = 'hub';

function showView(key, anchorId, noScroll){
  if(!VIEWS[key]) key='hub';
  if(current!==key){
    Object.keys(VIEWS).forEach(function(k){
      VIEWS[k].classList.toggle('active', k===key);
    });
    current=key;
  }
  updateNav(key); updateCrumbs(key); buildChapTOC(key);
  if(anchorId){
    var t=document.getElementById(anchorId);
    if(t){ setTimeout(function(){ t.scrollIntoView({block:'start', behavior:'auto'}); flash(t); },0); return; }
  }
  if(!noScroll) window.scrollTo(0,0);
  readbar();
}
function flash(t){
  t.style.transition='background-color .5s';
  var old=t.style.backgroundColor;
  t.style.backgroundColor='color-mix(in srgb, var(--c-accent) 22%, transparent)';
  setTimeout(function(){ t.style.backgroundColor=old||''; },900);
}
function route(){
  var h=(location.hash||'#hub').slice(1);
  if(!h) h='hub';
  if(VIEWS[h]){ showView(h); return; }
  var node=document.getElementById(h);
  if(node){
    var k=viewKeyOf(node);
    if(k){ showView(k,h); return; }
  }
  showView('hub');
}
window.addEventListener('hashchange', route);

/* postranní navigace */
function updateNav(key){
  $$('#sidenav a[data-view]').forEach(function(a){
    if(a.getAttribute('data-view')===key) a.setAttribute('aria-current','true');
    else a.removeAttribute('aria-current');
  });
  document.body.classList.remove('nav-open');
  $('#navtoggle').setAttribute('aria-expanded','false');
}
/* drobečky */
function updateCrumbs(key){
  var c=$('#crumbs'); c.innerHTML='';
  if(key==='hub'){ c.innerHTML='<span>Rozcestník kompendia</span>'; return; }
  c.appendChild(el('a',{href:'#hub'},'Rozcestník'));
  c.appendChild(el('span',{class:'sep'},'/'));
  var ch = CH.filter(function(x){ return x.id===key; })[0];
  if(ch){
    c.appendChild(el('span',{},'Okruh '+ch.n));
    c.appendChild(el('span',{class:'sep'},'/'));
    c.appendChild(el('span',{},esc(ch.title)));
  } else {
    c.appendChild(el('span',{},esc(NAMES[key]||key)));
  }
}
/* obsah aktuální kapitoly v postranním menu */
function buildChapTOC(key){
  $$('#sidenav .sidenav__toc').forEach(function(n){ n.remove(); });
  var view=VIEWS[key]; if(!view) return;
  var link=$('#sidenav a[data-view="'+key+'"]'); if(!link) return;
  var hs=$$('h2[id]', view);
  if(!hs.length) return;
  var ol=el('ol',{class:'sidenav__toc'});
  hs.forEach(function(h){
    var li=el('li'); var a=el('a',{href:'#'+h.id}, esc(h.textContent.replace(/^\d+\.\d+\s*/,'')));
    li.appendChild(a); ol.appendChild(li);
  });
  link.parentNode.appendChild(ol);
}

$('#navtoggle').addEventListener('click', function(){
  var open=document.body.classList.toggle('nav-open');
  this.setAttribute('aria-expanded', open?'true':'false');
});

/* ukazatel postupu čtení */
function readbar(){
  var h=document.documentElement;
  var max=h.scrollHeight-h.clientHeight;
  var p = max>20 ? (h.scrollTop/max*100) : 0;
  $('#readbar').style.width = p.toFixed(1)+'%';
}
window.addEventListener('scroll', readbar, {passive:true});
window.addEventListener('resize', readbar);

/* ------------------------------------------------------------
   3. POSTUP STUDIA
   ------------------------------------------------------------ */
function getDone(){ var d=lsGet(LS.done,[]); return Array.isArray(d)?d:[]; }
function setDone(arr){ lsSet(LS.done,arr); paintProgress(); }
function toggleDone(id){
  var d=getDone(), i=d.indexOf(id);
  if(i<0) d.push(id); else d.splice(i,1);
  setDone(d); return d.indexOf(id)>=0;
}
function paintProgress(){
  var d=getDone();
  $$('#hub-tiles .tile').forEach(function(t){
    t.setAttribute('data-done', d.indexOf(t.getAttribute('data-ch'))>=0 ? '1':'0');
    var s=$('.state',t); if(s) s.textContent = d.indexOf(t.getAttribute('data-ch'))>=0 ? 'prostudováno' : 'neprostudováno';
  });
  $$('#sidenav a[data-view]').forEach(function(a){
    var mark=$('.done',a);
    var isDone = d.indexOf(a.getAttribute('data-view'))>=0;
    if(isDone && !mark){ a.appendChild(el('span',{class:'done','aria-label':'prostudováno'},'✓')); }
    if(!isDone && mark) mark.remove();
  });
  var n=d.filter(function(x){ return /^ch\d+$/.test(x); }).length;
  var pt=$('#prog-txt'); if(pt) pt.textContent='Postup studia: '+n+' z '+CH.length+' okruhů';
  var pb=$('#prog-bar'); if(pb) pb.style.width=(n/CH.length*100)+'%';
  $$('.donebtn').forEach(function(b){
    var on=d.indexOf(b.getAttribute('data-ch'))>=0;
    b.setAttribute('aria-pressed', on?'true':'false');
    b.textContent = on ? '✓ Označeno jako prostudované' : 'Označit okruh jako prostudovaný';
  });
}
document.addEventListener('click', function(e){
  var b=e.target.closest('.donebtn'); if(!b) return;
  toggleDone(b.getAttribute('data-ch'));
});
var pr=$('#prog-reset');
if(pr) pr.addEventListener('click', function(){
  if(confirm('Vynulovat označení prostudovaných okruhů i uložené výsledky testů?')){
    setDone([]); lsSet(LS.quiz,{}); renderModes();
  }
});

/* ------------------------------------------------------------
   4. SLOVNÍK — render, tooltip, obousměrné provázání
   ------------------------------------------------------------ */
var GL = window.HK_GLOSSARY;
var GLMAP = {};
GL.forEach(function(g){ GLMAP[g.k]=g; });

/* 4a. očíslovat výskyty pojmů v textu a posbírat zpětné odkazy */
var backrefs = {};   /* slug -> [{id, view, title}] */
(function indexTerms(){
  var i=0;
  $$('a.term[data-term]').forEach(function(a){
    var slug=a.getAttribute('data-term');
    var g=GLMAP[slug];
    if(!a.id) a.id='tref-'+slug+'-'+(++i);
    a.setAttribute('href','#gl-'+slug);
    if(g){ a.setAttribute('title', g.t+' — klikněte pro heslo ve slovníku'); }
    else { a.removeAttribute('title'); a.classList.add('term-missing'); }
    var vk=viewKeyOf(a);
    if(vk && vk!=='slovnik'){
      (backrefs[slug]=backrefs[slug]||[]).push({id:a.id, view:vk, title:NAMES[vk]||vk});
    }
  });
})();

/* 4b. vykreslit slovník */
function renderGlossary(){
  var body=$('#gl-body'), abc=$('#gl-abc');
  if(!body) return;
  var sorted=GL.slice().sort(function(a,b){ return a.t.localeCompare(b.t,'cs'); });
  var groups={}, order=[];
  sorted.forEach(function(g){
    var L=deacc(g.t.charAt(0)).toUpperCase();
    if(!groups[L]){ groups[L]=[]; order.push(L); }
    groups[L].push(g);
  });
  body.innerHTML='';
  order.forEach(function(L){
    var sec=el('section',{class:'gl-group', id:'gl-abc-'+L});
    sec.appendChild(el('h2',{},L));
    groups[L].forEach(function(g){
      var d=el('div',{class:'gl-e', id:'gl-'+g.k});
      d.appendChild(el('h3',{}, esc(g.t)+(g.alt?' <span class="alt">'+esc(g.alt)+'</span>':'')));
      d.appendChild(el('p',{}, g.d));
      var refs=backrefs[g.k]||[];
      var seen={}, chips=[];
      refs.forEach(function(r){ if(!seen[r.view]){ seen[r.view]=1; chips.push(r); } });
      var nav=el('div',{class:'gl-back'});
      if(chips.length){
        nav.appendChild(el('span',{},'Výskyt v textu:'));
        chips.forEach(function(r){
          nav.appendChild(el('a',{href:'#'+r.id}, esc(shortName(r.view))));
        });
      } else if(g.see && g.see.length){
        nav.appendChild(el('span',{},'Souvisí s okruhy:'));
        g.see.forEach(function(v){ nav.appendChild(el('a',{href:'#'+v}, esc(shortName(v)))); });
      } else {
        nav.appendChild(el('span',{},'Heslo bez přímého odkazu v textu.'));
      }
      d.appendChild(nav);
      sec.appendChild(d);
    });
    body.appendChild(sec);
  });
  /* abecední rejstřík A–Ž */
  var alphabet='ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');
  abc.innerHTML='';
  alphabet.forEach(function(L){
    var has=!!groups[L];
    abc.appendChild(el('a',{href: has?('#gl-abc-'+L):'#', class: has?'':'off', 'aria-disabled': has?'false':'true'}, L));
  });
  var mt=$('#m-terms'); if(mt) mt.textContent=GL.length;
  var tt=$('#tile-terms'); if(tt) tt.textContent=GL.length+' hesel · abecedně';
}
function shortName(v){
  var c=CH.filter(function(x){ return x.id===v; })[0];
  return c ? (c.n+'. '+c.short) : (NAMES[v]||v);
}

/* 4c. filtr slovníku */
var glf=$('#gl-filter');
if(glf) glf.addEventListener('input', function(){
  var q=deacc(this.value.trim());
  $$('#gl-body .gl-e').forEach(function(e){
    var show = !q || deacc(e.textContent).indexOf(q)>=0;
    e.hidden=!show;
  });
  $$('#gl-body .gl-group').forEach(function(g){
    g.hidden = !$$('.gl-e:not([hidden])',g).length;
  });
});

/* 4d. tooltip */
var tip=el('div',{id:'tip',role:'tooltip'});
document.body.appendChild(tip);
var tipTimer=null;
function showTip(a){
  var g=GLMAP[a.getAttribute('data-term')]; if(!g) return;
  tip.innerHTML='<strong>'+esc(g.t)+'</strong>'+g.d+'<span class="tip-more">Kliknutím otevřete celé heslo ve slovníku.</span>';
  tip.style.display='block';
  var r=a.getBoundingClientRect();
  var w=tip.offsetWidth, h=tip.offsetHeight;
  var x=Math.min(Math.max(8, r.left+window.scrollX), window.scrollX+document.documentElement.clientWidth-w-10);
  var y=r.top+window.scrollY-h-8;
  if(y < window.scrollY+4) y=r.bottom+window.scrollY+8;
  tip.style.left=x+'px'; tip.style.top=y+'px';
}
function hideTip(){ tip.style.display='none'; }
document.addEventListener('mouseover', function(e){
  var a=e.target.closest('a.term[data-term]');
  if(a){ clearTimeout(tipTimer); tipTimer=setTimeout(function(){ showTip(a); },120); }
});
document.addEventListener('mouseout', function(e){
  if(e.target.closest('a.term[data-term]')){ clearTimeout(tipTimer); hideTip(); }
});
document.addEventListener('focusin', function(e){
  var a=e.target.closest && e.target.closest('a.term[data-term]');
  if(a) showTip(a); else hideTip();
});
window.addEventListener('scroll', hideTip, {passive:true});

/* 4e. klik na pojem → slovník + nabídka zpětného skoku */
document.addEventListener('click', function(e){
  var a=e.target.closest('a.term[data-term]');
  if(!a) return;
  hideTip();
  lsSet(LS.pos, {id:a.id, view:viewKeyOf(a)});
  setTimeout(markReturn, 30);
});
function markReturn(){
  var p=lsGet(LS.pos,null);
  $$('.gl-return').forEach(function(n){ n.remove(); });
  if(!p || !document.getElementById(p.id)) return;
  var host=$('#gl-body');
  if(!host || current!=='slovnik') return;
  var b=el('p',{class:'gl-return small'},'');
  var link=el('a',{href:'#'+p.id,class:'xref'},'Zpět do textu — '+esc(shortName(p.view)));
  b.appendChild(link);
  host.parentNode.insertBefore(b, host);
}

/* ------------------------------------------------------------
   5. FULLTEXTOVÉ HLEDÁNÍ
   ------------------------------------------------------------ */
var INDEX=[];
(function buildIndex(){
  var sel='p, li, td, th, dt, dd, h1, h2, h3, h4, summary, figcaption, .a, blockquote';
  var uid=0;
  Object.keys(VIEWS).forEach(function(k){
    if(k==='test') return;
    var heading='';
    var nodes=$$(sel, VIEWS[k]);
    nodes.forEach(function(n){
      if(n.querySelector && n.querySelector(sel)) { /* kontejner — indexujeme jen listy */ }
      var txt=(n.textContent||'').replace(/\s+/g,' ').trim();
      if(txt.length<12) return;
      if(/^H[1-4]$/.test(n.tagName)){ heading=txt; }
      if(!n.id) n.id='ix'+(++uid);
      INDEX.push({v:k, id:n.id, h:heading, t:txt, n:deacc(txt)});
    });
  });
  /* banka testových otázek do indexu */
  (window.HK_QUIZ||[]).forEach(function(q,i){
    var txt='Testová otázka: '+q.q+' '+(q.expl||'');
    INDEX.push({v:'test', id:'', h:'Test — okruh '+q.ch, t:txt, n:deacc(txt)});
  });
})();

var qinp=$('#q'), rbox=$('#results'), rsel=-1;
function closeResults(){ rbox.classList.remove('open'); qinp.setAttribute('aria-expanded','false'); rsel=-1; }
function snippet(t, q){
  var i=deacc(t).indexOf(q);
  if(i<0) return esc(t.slice(0,150))+(t.length>150?'…':'');
  var s=Math.max(0,i-60), e=Math.min(t.length, i+q.length+110);
  return (s>0?'…':'')+esc(t.slice(s,i))+'<mark>'+esc(t.substr(i,q.length))+'</mark>'+esc(t.slice(i+q.length,e))+(e<t.length?'…':'');
}
function doSearch(){
  var raw=qinp.value.trim();
  if(raw.length<2){ closeResults(); rbox.innerHTML=''; return; }
  var q=deacc(raw);
  var hits=[];
  for(var i=0;i<INDEX.length && hits.length<400;i++){
    if(INDEX[i].n.indexOf(q)>=0) hits.push(INDEX[i]);
  }
  hits.sort(function(a,b){
    var aw = /^H[1-4]/.test('')?0:0;
    return (a.n.indexOf(q)) - (b.n.indexOf(q));
  });
  rbox.innerHTML='';
  if(!hits.length){ rbox.innerHTML='<p class="rnone">Nic nenalezeno. Zkuste jiné slovo nebo kratší tvar.</p>'; rbox.classList.add('open'); return; }
  var cnt=el('p',{class:'rnone'},'Nalezeno '+hits.length+' míst'+(hits.length>60?' (zobrazeno prvních 60)':'')+'.');
  rbox.appendChild(cnt);
  hits.slice(0,60).forEach(function(h){
    var a=el('a',{class:'rhit', href: h.id? ('#'+h.id) : ('#'+h.v), role:'option'});
    a.appendChild(el('span',{class:'rwhere'}, esc(shortName(h.v))+(h.h?' · '+esc(h.h):'')));
    a.appendChild(el('span',{class:'rsnip'}, snippet(h.t,q)));
    rbox.appendChild(a);
  });
  rbox.classList.add('open'); qinp.setAttribute('aria-expanded','true');
}
if(qinp){
  var st=null;
  qinp.addEventListener('input', function(){ clearTimeout(st); st=setTimeout(doSearch,140); });
  qinp.addEventListener('keydown', function(e){
    var items=$$('.rhit',rbox);
    if(e.key==='Escape'){ closeResults(); this.blur(); return; }
    if(e.key==='ArrowDown'||e.key==='ArrowUp'){
      e.preventDefault();
      if(!items.length) return;
      if(rsel>=0 && items[rsel]) items[rsel].classList.remove('sel');
      rsel = e.key==='ArrowDown' ? Math.min(items.length-1, rsel+1) : Math.max(0, rsel-1);
      items[rsel].classList.add('sel'); items[rsel].scrollIntoView({block:'nearest'});
    }
    if(e.key==='Enter'){
      if(rsel>=0 && items[rsel]){ e.preventDefault(); items[rsel].click(); }
    }
  });
  rbox.addEventListener('click', function(e){ if(e.target.closest('.rhit')) closeResults(); });
  document.addEventListener('click', function(e){
    if(!e.target.closest('.searchbox')) closeResults();
  });
}

/* ------------------------------------------------------------
   6. 100 LÉKŮ — filtr a karty
   ------------------------------------------------------------ */
var RX = window.HK_REMEDIES||[];
(function remedies(){
  var grid=$('#mm-grid'); if(!grid) return;
  var kinds=[{k:'all',l:'Vše'},{k:'plantae',l:'Rostliny'},{k:'mineralia',l:'Minerály a soli'},{k:'animalia',l:'Živočichové'},{k:'nosody',l:'Nozody a sarkody'},{k:'imponderabilia',l:'Imponderabilia'}];
  var chips=$('#mm-chips');
  kinds.forEach(function(kk,i){
    var b=el('button',{class:'chip',type:'button','data-k':kk.k,'aria-pressed': i===0?'true':'false'}, kk.l);
    chips.appendChild(b);
  });
  var KLBL={plantae:'Rostlina',mineralia:'Minerál / sůl',animalia:'Živočich',nosody:'Nozod / sarkod',imponderabilia:'Imponderabilium'};
  grid.innerHTML = RX.map(function(r){
    return '<li class="mmcard" data-k="'+r.k+'" data-s="'+esc(deacc(r.n+' '+r.a+' '+r.kn+' '+r.sf+' '+(r.src||'')))+'">'
      +'<h4>'+esc(r.n)+' <span class="abbr">'+esc(r.a)+'</span></h4>'
      +'<p class="kingdom">'+KLBL[r.k]+(r.src?' · '+esc(r.src):'')+'</p>'
      +'<p><b>Vodítka:</b> <span class="kn">'+esc(r.kn)+'</span></p>'
      +'<p><b>Sféra:</b> <span class="kn">'+esc(r.sf)+'</span></p>'
      +(r.md?'<p><b>Modality:</b> <span class="kn">'+esc(r.md)+'</span></p>':'')
      +'</li>';
  }).join('');
  var qi=$('#mm-q'), cur='all';
  function apply(){
    var q=deacc((qi.value||'').trim()), shown=0;
    $$('.mmcard',grid).forEach(function(c){
      var ok=(cur==='all'||c.getAttribute('data-k')===cur) && (!q||c.getAttribute('data-s').indexOf(q)>=0);
      c.hidden=!ok; if(ok) shown++;
    });
    $('#mm-count').textContent='Zobrazeno '+shown+' z '+RX.length+' léků';
  }
  chips.addEventListener('click', function(e){
    var b=e.target.closest('.chip'); if(!b) return;
    $$('.chip',chips).forEach(function(x){ x.setAttribute('aria-pressed', x===b?'true':'false'); });
    cur=b.getAttribute('data-k'); apply();
  });
  qi.addEventListener('input', apply);
  apply();
})();

/* ------------------------------------------------------------
   7. TEST
   ------------------------------------------------------------ */
var QB = window.HK_QUIZ||[];
var quiz=null;

function chLabel(n){ var c=CH.filter(function(x){ return x.n===n; })[0]; return c? (n+'. '+c.short) : ('Okruh '+n); }
function chId(n){ var c=CH.filter(function(x){ return x.n===n; })[0]; return c?c.id:'hub'; }

function renderModes(){
  var mm=$('#test-modes-main'); if(!mm) return;
  var saved=lsGet(LS.quiz,{})||{};
  mm.innerHTML='';
  [
    {id:'exam',t:'Zkouškový test',d:'40 otázek náhodně napříč všemi 11 okruhy. Simulace písemné části.',n:40},
    {id:'quick',t:'Krátký test',d:'15 otázek náhodně napříč okruhy. Na zahřátí nebo do tramvaje.',n:15},
    {id:'all',t:'Celá banka',d:QB.length+' otázek v pořadí po okruzích. Maratonní režim pro finální opakování.',n:QB.length}
  ].forEach(function(m){
    var s=saved['m:'+m.id];
    var b=el('button',{class:'mode',type:'button','data-mode':m.id},
      '<b>'+m.t+'</b><span>'+m.d+(s?' <br><b style="color:var(--c-accent)">Naposledy: '+s.pct+' %</b>':'')+'</span>');
    mm.appendChild(b);
  });
  var mc=$('#test-modes-ch'); mc.innerHTML='';
  CH.forEach(function(c){
    var n=QB.filter(function(q){ return q.ch===c.n; }).length;
    var s=saved['ch:'+c.n];
    var b=el('button',{class:'mode',type:'button','data-mode':'ch'+c.n},
      '<b>'+c.n+'. '+esc(c.short)+' <span style="font-weight:400;color:var(--c-ink-muted)">('+n+')</span></b>'
      +'<span>'+esc(c.tags.join(' · '))+(s?'<br><b style="color:var(--c-accent)">Naposledy: '+s.pct+' %</b>':'')+'</span>');
    mc.appendChild(b);
  });
  var mq=$('#m-quiz'); if(mq) mq.textContent=QB.length;
  var tt=$('#tile-test'); if(tt) tt.textContent=QB.length+' otázek · '+CH.length+' okruhů';
  var mctrl=$('#m-ctrl'); if(mctrl) mctrl.textContent=$$('.q').length;
}
renderModes();

document.addEventListener('click', function(e){
  var b=e.target.closest('[data-mode]'); if(!b) return;
  startQuiz(b.getAttribute('data-mode'));
});

function startQuiz(mode){
  var qs;
  if(mode==='exam') qs=shuffle(QB).slice(0,Math.min(40,QB.length));
  else if(mode==='quick') qs=shuffle(QB).slice(0,Math.min(15,QB.length));
  else if(mode==='all') qs=QB.slice();
  else {
    var n=parseInt(mode.replace('ch',''),10);
    qs=shuffle(QB.filter(function(q){ return q.ch===n; }));
  }
  quiz={mode:mode, qs:qs, i:0, pts:0, res:[], answered:false};
  $('#test-intro').hidden=true; $('#test-report').hidden=true; $('#test-run').hidden=false;
  paintQ();
}
function endQuiz(){
  quiz=null;
  $('#test-run').hidden=true; $('#test-intro').hidden=false; $('#test-report').hidden=true;
  renderModes(); window.scrollTo(0,0);
}
var qq=$('#qquit'); if(qq) qq.addEventListener('click', function(){
  if(quiz && quiz.i>0 && !confirm('Ukončit test? Průběh se zahodí.')) return;
  endQuiz();
});

function paintQ(){
  var q=quiz.qs[quiz.i];
  $('#qnum').textContent=(quiz.i+1)+' / '+quiz.qs.length;
  $('#qbar').style.width=((quiz.i)/quiz.qs.length*100)+'%';
  $('#qscore').textContent=quiz.pts+' b.';
  var host=$('#qhost'); host.innerHTML='';
  var card=el('div',{class:'qcard'});
  var TYPE={single:'Jedna správná',multi:'Více správných',match:'Přiřazování',cas:'Případový scénář'};
  card.appendChild(el('div',{class:'qmeta'},
    '<span class="badge">'+esc(chLabel(q.ch))+'</span><span class="badge">'+(TYPE[q.type]||q.type)+'</span>'));
  if(q.scen) card.appendChild(el('p',{class:'qscen'}, q.scen));
  card.appendChild(el('p',{class:'qtext'}, q.q));

  if(q.type==='match'){
    var opts=shuffle(q.pairs.map(function(p){ return p.r; }));
    var wrap=el('div',{class:'match'});
    card.appendChild(el('p',{class:'qhint'},'Ke každé položce vyberte odpovídající protějšek.'));
    q.pairs.forEach(function(p,i){
      var row=el('div',{class:'mrow','data-i':i});
      row.appendChild(el('span',{}, p.l));
      var sel=el('select',{'aria-label':'Přiřazení k: '+p.l.replace(/<[^>]+>/g,'')});
      sel.appendChild(el('option',{value:''},'— vyberte —'));
      opts.forEach(function(o){ sel.appendChild(el('option',{value:o}, o)); });
      row.appendChild(sel); wrap.appendChild(row);
    });
    card.appendChild(wrap);
  } else {
    card.appendChild(el('p',{class:'qhint'}, q.type==='multi'?'Správných odpovědí je více než jedna. Bod se uděluje jen za úplně přesnou sadu.':'Vyberte jednu odpověď.'));
    var ul=el('ul',{class:'opts'});
    var idx=q.opts.map(function(o,i){ return i; });
    if(q.noshuffle!==true) idx=shuffle(idx);
    quiz.order=idx;
    idx.forEach(function(oi){
      var li=el('li',{class:'opt',role:'presentation','data-oi':oi});
      var inp=el('input',{type:(q.type==='multi'?'checkbox':'radio'), name:'opt', value:oi, id:'o'+oi});
      var lab=el('label',{for:'o'+oi}, q.opts[oi]);
      li.appendChild(inp); li.appendChild(lab); ul.appendChild(li);
      li.addEventListener('click', function(ev){
        if(li.classList.contains('locked')) return;
        if(ev.target!==inp){ if(q.type==='multi') inp.checked=!inp.checked; else inp.checked=true; }
      });
    });
    card.appendChild(ul);
  }
  var foot=el('div',{class:'quizfoot'});
  foot.appendChild(el('button',{class:'btn btn--primary',type:'button',id:'qcheck'},'Vyhodnotit'));
  foot.appendChild(el('span',{class:'sp'}));
  foot.appendChild(el('span',{class:'small'}, 'Otázka '+(quiz.i+1)+' z '+quiz.qs.length));
  card.appendChild(foot);
  host.appendChild(card);
  quiz.answered=false;
  $('#qcheck').addEventListener('click', checkQ);
}

function checkQ(){
  if(quiz.answered) return;
  var q=quiz.qs[quiz.i], card=$('#qhost .qcard'), ok=false;
  if(q.type==='match'){
    ok=true;
    $$('.mrow',card).forEach(function(row){
      var i=+row.getAttribute('data-i'), sel=$('select',row);
      var right = sel.value===q.pairs[i].r;
      row.classList.add(right?'is-right':'is-wrong');
      if(!right){ ok=false; row.appendChild(el('span',{class:'fix'},'Správně: '+q.pairs[i].r)); }
      sel.disabled=true;
    });
  } else {
    var chosen=[];
    $$('.opt',card).forEach(function(li){
      var inp=$('input',li); if(inp.checked) chosen.push(+inp.value);
      inp.disabled=true; li.classList.add('locked');
    });
    var corr = Array.isArray(q.correct)? q.correct.slice().sort() : [q.correct];
    chosen.sort();
    ok = chosen.length===corr.length && chosen.every(function(v,i){ return v===corr[i]; });
    $$('.opt',card).forEach(function(li){
      var oi=+li.getAttribute('data-oi');
      var isCorr=corr.indexOf(oi)>=0, isChosen=chosen.indexOf(oi)>=0;
      if(isCorr && isChosen) li.classList.add('is-right');
      else if(!isCorr && isChosen) li.classList.add('is-wrong');
      else if(isCorr && !isChosen) li.classList.add('is-miss');
    });
  }
  if(ok) quiz.pts++;
  quiz.res.push({ch:q.ch, ok:ok, id:q.id});
  $('#qscore').textContent=quiz.pts+' b.';
  var fb=el('div',{class:'fb '+(ok?'fb--ok':'fb--no')});
  fb.appendChild(el('b',{}, ok?'Správně.':'Nesprávně.'));
  fb.appendChild(el('div',{}, q.expl));
  var links=el('div',{class:'links'});
  links.appendChild(el('a',{class:'xref',href:'#'+(q.ref||chId(q.ch))}, 'Kapitola: '+esc(chLabel(q.ch))));
  if(q.term && GLMAP[q.term]) links.appendChild(el('a',{class:'xref',href:'#gl-'+q.term},'Slovník: '+esc(GLMAP[q.term].t)));
  fb.appendChild(links);
  var foot=$('.quizfoot',card);
  card.insertBefore(fb, foot);
  foot.innerHTML='';
  var last = quiz.i>=quiz.qs.length-1;
  var nb=el('button',{class:'btn btn--primary',type:'button'}, last?'Zobrazit výsledek':'Další otázka →');
  nb.addEventListener('click', function(){
    if(last) report(); else { quiz.i++; paintQ(); window.scrollTo(0, Math.max(0,$('#test-run').offsetTop-70)); }
  });
  foot.appendChild(nb);
  foot.appendChild(el('span',{class:'sp'}));
  foot.appendChild(el('span',{class:'small'},'Skóre: '+quiz.pts+' / '+(quiz.i+1)));
  quiz.answered=true;
  nb.focus();
}

function report(){
  var total=quiz.qs.length, pts=quiz.pts, pct=Math.round(pts/total*100);
  var per={};
  quiz.res.forEach(function(r){
    per[r.ch]=per[r.ch]||{n:0,ok:0};
    per[r.ch].n++; if(r.ok) per[r.ch].ok++;
  });
  var saved=lsGet(LS.quiz,{})||{};
  var key = /^ch\d+$/.test(quiz.mode) ? ('ch:'+quiz.mode.replace('ch','')) : ('m:'+quiz.mode);
  saved[key]={pct:pct, pts:pts, total:total, when:Date.now()};
  lsSet(LS.quiz,saved);

  var verdict = pct>=90?'Výborně — úroveň, se kterou se ke zkoušce chodí.'
    : pct>=80?'Velmi dobře. Doladit slabé okruhy a je to.'
    : pct>=70?'Prošel/prošla jste hranicí 70 %. Zóna, kde se ještě dá spadnout — opakovat.'
    : pct>=50?'Základ je, ale ke zkoušce to nestačí. Vraťte se k okruhům pod 60 %.'
    : 'Látka není zvládnutá. Doporučeno projít kapitoly znovu podle studijního plánu.';
  var host=$('#test-report'); host.innerHTML='';
  var r=el('div',{class:'report'});
  r.appendChild(el('h2',{style:'margin-top:0'},'Závěrečná zpráva'));
  var sb=el('div',{class:'scorebig'});
  sb.appendChild(el('div',{},'<div class="big" style="color:'+(pct>=70?'var(--c-ok)':'var(--c-crit)')+'">'+pct+' %</div><div class="small">'+pts+' z '+total+' bodů</div>'));
  sb.appendChild(el('div',{},'<div class="verdict">'+verdict+'</div><div class="small">Hranice úspěšnosti: 70 %</div>'));
  r.appendChild(sb);
  r.appendChild(el('h3',{},'Úspěšnost po okruzích'));
  var bars=el('div',{class:'bars'});
  Object.keys(per).map(Number).sort(function(a,b){return a-b;}).forEach(function(n){
    var p=per[n], v=Math.round(p.ok/p.n*100);
    var lvl = v>=80?'hi':(v>=60?'mid':'lo');
    var row=el('div',{class:'brow','data-lvl':lvl});
    row.appendChild(el('span',{class:'bt'},'<a href="#'+chId(n)+'" style="text-decoration:none;color:inherit">'+esc(chLabel(n))+'</a>'));
    row.appendChild(el('span',{class:'bb'},'<i style="width:'+v+'%"></i>'));
    row.appendChild(el('span',{class:'bv'}, v+' %'));
    bars.appendChild(row);
  });
  r.appendChild(bars);
  var weak=Object.keys(per).map(Number).filter(function(n){ return per[n].ok/per[n].n < 0.8; })
    .sort(function(a,b){ return (per[a].ok/per[a].n)-(per[b].ok/per[b].n); });
  var rec=el('div',{class:'rec'});
  rec.appendChild(el('h3',{},'Co doučit'));
  if(!weak.length){
    rec.appendChild(el('p',{},'Všechny testované okruhy nad 80 %. Doporučení: zkuste režim <b>Zkoušková test</b> na celé bance a pak se soustřeďte na praktickou část — repertorizaci modelových případů (okruh 5).'));
  } else {
    var ul=el('ul');
    weak.forEach(function(n){
      var c=CH.filter(function(x){ return x.n===n; })[0]||{};
      ul.appendChild(el('li',{}, '<b><a href="#'+chId(n)+'">'+esc(chLabel(n))+'</a></b> — '+Math.round(per[n].ok/per[n].n*100)+' %. '
        +'Projděte znovu výklad a kontrolní otázky; poté procvičte okruh v režimu „Procvičování po okruzích“.'
        +(c.tags?' Klíčová témata: <i>'+esc(c.tags.join(', '))+'</i>.':'')));
    });
    rec.appendChild(ul);
  }
  r.appendChild(rec);
  var f=el('div',{class:'quizfoot'});
  var again=el('button',{class:'btn btn--primary',type:'button'},'Zkusit znovu');
  again.addEventListener('click', function(){ startQuiz(quiz.mode); });
  f.appendChild(again);
  var back=el('button',{class:'btn',type:'button'},'Zpět na volbu režimu');
  back.addEventListener('click', endQuiz);
  f.appendChild(back);
  f.appendChild(el('span',{class:'sp'}));
  f.appendChild(el('a',{class:'btn',href:'#hub'},'Rozcestník'));
  r.appendChild(f);
  host.appendChild(r);
  $('#test-run').hidden=true; host.hidden=false;
  window.scrollTo(0, Math.max(0, host.offsetTop-70));
  renderModes();
}

/* ------------------------------------------------------------
   8. KLÁVESOVÉ ZKRATKY
   ------------------------------------------------------------ */
document.addEventListener('keydown', function(e){
  var t=e.target, tag=(t.tagName||'').toLowerCase();
  if(tag==='input'||tag==='select'||tag==='textarea'||t.isContentEditable) return;
  if(e.metaKey||e.ctrlKey||e.altKey) return;
  if(e.key==='/'){ e.preventDefault(); qinp.focus(); qinp.select(); return; }
  if(e.key==='Escape'){ closeResults(); hideTip(); document.body.classList.remove('nav-open'); return; }
  if(e.key==='h'){ location.hash='#hub'; return; }
  if(e.key==='ArrowLeft'||e.key==='ArrowRight'){
    var i=CH.map(function(c){ return c.id; }).indexOf(current);
    if(i<0) return;
    var j = e.key==='ArrowRight' ? i+1 : i-1;
    if(j>=0 && j<CH.length) location.hash='#'+CH[j].id;
  }
});

/* ------------------------------------------------------------
   9. START
   ------------------------------------------------------------ */
renderGlossary();
paintProgress();
route();
markReturn();
window.addEventListener('hashchange', function(){ if(current==='slovnik') markReturn(); });

})();
