const query=new URLSearchParams(location.search);
const reasons={randomchat:'Random stranger-chat and video-chat sites are restricted.',adult:'This address matches the adult-site or adult-search protection.',social:'Reddit, X/Twitter, Instagram and related domains are restricted.',shorts:'YouTube Shorts are restricted.',ad:'This address matches a bundled advertising rule.',search:'This search submission could not enforce strict Safe Search. Use the address-bar search instead.'};
document.getElementById('reason').textContent=reasons[query.get('reason')]||'A built-in browsing protection blocked this address.';
try{document.getElementById('site').textContent=new URL(query.get('url')).hostname;}catch{}
