const captchaDocument=ShieldCaptcha.challenge(location.href);
if(captchaDocument){const style=document.createElement('style');style.textContent='img,svg,svg image,canvas,input[type="image"],object,embed {visibility:visible !important;} *,*::before,*::after {background-image:revert !important;}';const attach=()=>{if(document.documentElement)(document.head||document.documentElement).appendChild(style);};attach();if(!style.isConnected)document.addEventListener('DOMContentLoaded',attach,{once:true});}
const verified=new WeakMap(),pending=new WeakMap();
function shortRoute(){if(/(^|\.)youtube\.com$/.test(location.hostname)&&/^\/shorts(?:\/|$)/.test(location.pathname))location.replace('https://www.youtube.com/');}
async function image(img){
 if(captchaDocument){img.style.setProperty('visibility','visible','important');img.setAttribute('data-shield-ready','yes');return;}
 const source=img.src||img.currentSrc;if(verified.get(img)===source&&img.src===source){img.style.setProperty('visibility','visible','important');return;}
 img.style.setProperty('visibility','hidden','important');img.removeAttribute('data-shield-ready');if(!source||pending.get(img)===source)return;
 pending.set(img,source);
 // Freeze picture/srcset selection before starting asynchronous verification.
 if(img.parentElement?.tagName==='PICTURE')img.parentElement.querySelectorAll('source').forEach(s=>s.remove());
 img.removeAttribute('srcset');if(img.src!==source)img.src=source;
 try{
  let value;
  if(/^https?:/i.test(source)){
   const result=await Promise.race([browser.runtime.sendMessage({kind:'filter-image',url:source}),new Promise((_,reject)=>setTimeout(()=>reject(Error('Image timeout')),22000))]);
   if(!result?.ok||!/^data:image\/png;base64,/.test(result.data))throw Error('Image filtering failed');value=result.data;
  }else if(/^(data:|blob:)/i.test(source)){
   const response=await fetch(source);const blob=await response.blob();if(blob.size>20*1024*1024)throw Error('Too large');
   const output=await ShieldFilter.image(blob);value=await new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(reader.result);reader.onerror=reject;reader.readAsDataURL(output);});
  }else throw Error('Unsupported image');
  if(pending.get(img)!==source||img.src!==source)return;
  verified.set(img,value);img.src=value;img.style.setProperty('visibility','visible','important');img.setAttribute('data-shield-ready','yes');
 }catch{if(pending.get(img)===source)img.removeAttribute('data-shield-ready');}finally{if(pending.get(img)===source)pending.delete(img);}
}
function scan(){shortRoute();document.querySelectorAll('img').forEach(image);if(!captchaDocument)document.querySelectorAll('video[poster]').forEach(v=>v.removeAttribute('poster'));document.querySelectorAll('a[href*="/shorts/"]').forEach(a=>{try{if(/(^|\.)youtube\.com$/.test(new URL(a.href).hostname))a.style.setProperty('display','none','important');}catch{}});}
new MutationObserver(scan).observe(document,{childList:true,subtree:true,attributes:true,attributeFilter:['src','srcset','href','poster']});
document.addEventListener('load',e=>{if(e.target instanceof HTMLImageElement)image(e.target);},true);addEventListener('popstate',scan);setInterval(shortRoute,500);scan();


