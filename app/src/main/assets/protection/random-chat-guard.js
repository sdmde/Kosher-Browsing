// Isolated extension world: classify interactive stranger-chat pages and SPA changes.
(()=>{
 if(!/^https?:$/.test(location.protocol))return;
 let blocked=false,pending=false;
 function evidence(){return {title:document.title.slice(0,512),description:(document.querySelector('meta[name="description"]')?.content||'').slice(0,1024),headings:[...document.querySelectorAll('h1,h2,[role="heading"]')].slice(0,16).map(e=>e.textContent.slice(0,256)),actions:[...document.querySelectorAll('button,a,[role="button"],input[type="button"],input[type="submit"]')].slice(0,200).map(e=>(e.textContent||e.value||e.getAttribute('aria-label')||'').slice(0,128))};}
 function stop(info){if(blocked)return;blocked=true;window.stop();document.documentElement?.setAttribute('data-shield-chat-blocked','yes');browser.runtime.sendMessage({kind:'restrict-random-chat',evidence:info}).catch(()=>{});location.replace(browser.runtime.getURL('blocked.html')+'?reason=randomchat&url='+encodeURIComponent(location.href));}
 function check(){pending=false;if(blocked)return;const info=evidence();if(ShieldPolicy.isRandomChatHost(location.hostname)||ShieldPolicy.randomChatDocument(info))stop(info);}
 function schedule(){if(!pending&&!blocked){pending=true;queueMicrotask(check);}}
 new MutationObserver(schedule).observe(document,{subtree:true,childList:true,characterData:true,attributes:true,attributeFilter:['content','aria-label']});
 document.addEventListener('click',event=>{if(blocked){event.preventDefault();event.stopImmediatePropagation();return;}const action=event.target.closest?.('button,a,[role="button"],input[type="submit"]');if(!action)return;const info=evidence();info.actions.unshift((action.textContent||action.value||'').slice(0,128));if(ShieldPolicy.randomChatDocument(info)){event.preventDefault();event.stopImmediatePropagation();stop(info);}},true);
 window.addEventListener('popstate',schedule);window.addEventListener('hashchange',schedule);document.addEventListener('DOMContentLoaded',check,{once:true});schedule();
})();
