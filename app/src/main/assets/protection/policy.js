globalThis.ShieldPolicy={
 social:['reddit.com','redd.it','redditmedia.com','redditstatic.com','twitter.com','x.com','t.co','twimg.com','instagram.com','cdninstagram.com','threads.net','threads.com'],
 randomChat:["omgle.com","omegle.me","omegle.chat","omegle.online","omegle.fun","omegle.life","omegle.live","omegle.org","omegle.fm","omegleapp.com","omegleweb.io","omegletv.chat","omeglehang.com","uhmegle.net","uhmegle.tv","uhmegle.social","holla.world","holla.video","azarlive.com","tinychat.com","faceflow.com","camloo.com","chatki.com","chatrad.com","bazoocam.org","hay.fun","minichat.com","livu.me","livuapp.com","coomeet.com","omegle-talk.com",'omegle.com','ome.tv','ometv.chat','ommetv.com','ometv.pl','ometvlive.com','omeet.tv','chatroulette.com','chatspin.com','chatspin.tv','emeraldchat.com','chatrandom.com','shagle.com','chathub.cam','chatruletka.com','camsurf.com','camgo.com','strangermeetup.com','strangercam.com','monkey.app','monkey.cool','omegleapp.me','omegleweb.com','omegle.onl','omegle.tv','uhmegle.com','omegleme.chat','joingy.com','thundr.com','chitchat.gg'],
 isRandomChatHost(host){
  host=host.toLowerCase().replace(/\.$/,'');if(this.randomChat.some(h=>host===h||host.endsWith('.'+h)))return true;
  const labels=host.split('.'),suffix=labels.slice(-2).join('.');
  const grouped=['co.uk','org.uk','com.au','co.nz','co.in','co.il','co.jp','co.kr','com.br','com.mx','com.tr','com.cn','com.ar','github.io','pages.dev','vercel.app','netlify.app','blogspot.com','appspot.com'].includes(suffix);
  const brand=labels[labels.length-(grouped?3:2)]||'';
  return /^(?:free-)?(?:omegle|omgle|uhmegle|ometv|omegle(?:app|web|tv|me|chat|live|online|video|hang)|chatroulette|chatrandom|chatspin|chatruletka|emeraldchat|joingy|strangercam)(?:-(?:chat|live|online|video|app|web|tv|talk))?$/.test(brand);
 },
 adultTerms:/\b(porn(?:ography|ographic|hub)?|xxx|nsfw|hentai|rule\s*34|onlyfans|xvideos|xnxx|nudes?|nudity|naked|erotic\w*|fetish\w*|blowjob\w*|handjob\w*|masturbat\w*|orgasm\w*|anal\s+sex|sex(?:ual)?|sexy|boobs?|tits?|pussy|cock|penis|vagina|cumshot\w*|escort\w*|camgirls?|adult\s+(?:video|content|site|dating)\w*)\b/i,
 hasAdultHost(host){let lo=0,hi=ADULT_HOSTS.length-1;while(lo<=hi){let mid=(lo+hi)>>>1,v=ADULT_HOSTS[mid];if(v===host)return true;if(v<host)lo=mid+1;else hi=mid-1;}return false;},
 inspect(value,type='main_frame'){
  let u;try{u=new URL(value);}catch{return {cancel:true};}
  let path;try{path=decodeURIComponent(u.pathname);}catch{return {cancel:true};}if(/\.(apk|apks|apkm|xapk)(?:$|\/)/i.test(path)||[...u.searchParams.values()].some(v=>/\.(apk|apks|apkm|xapk)$/i.test(v)))return {cancel:true};
  let host=u.hostname.toLowerCase().replace(/\.$/,'');
  if(/porn|xxx|hentai|xnxx|xvideos|onlyfans|redtube|youporn|chaturbate|bongacams|stripchat/.test(host))return {cancel:true};
  if(this.isRandomChatHost(host))return {cancel:true};
  if(this.social.some(h=>host===h||host.endsWith('.'+h)))return {cancel:true};
  for(let h=host;h.includes('.');h=h.slice(h.indexOf('.')+1))if(this.hasAdultHost(h))return {cancel:true};
  const query=['q','query','p','search_query','search','text','wd','keyword'].map(k=>u.searchParams.get(k)||'').join(' ').normalize('NFKC').replace(/[\u200b-\u200f\u2060]/g,'');
  if(this.adultTerms.test(query))return {cancel:true};
  if(type==='main_frame'||type==='sub_frame'){
   try{if(this.adultTerms.test(decodeURIComponent(u.pathname).replace(/[_+/-]/g,' ')))return {cancel:true};}catch{return {cancel:true};}
  }
  let key,valueSafe;
  if(host==='duckduckgo.com'||host.endsWith('.duckduckgo.com')){key='kp';valueSafe='1';}
  else if(/(^|\.)google\.[a-z.]+$/.test(host)&&u.pathname==='/search'){key='safe';valueSafe='active';}
  else if(host==='bing.com'||host.endsWith('.bing.com')){key='adlt';valueSafe='strict';}
  else if(host==='search.yahoo.com'){key='vm';valueSafe='r';}
  else if(host==='yandex.com'||host==='yandex.ru'){key='family';valueSafe='yes';}
  if(key&&u.searchParams.get(key)!==valueSafe){u.searchParams.set(key,valueSafe);return {redirectUrl:u.href};}
  return {};
 }
};

