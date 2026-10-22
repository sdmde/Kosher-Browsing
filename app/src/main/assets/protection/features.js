// Trusted extension commands only. No window.postMessage bridge or page scripts.
if(window===window.top){
 let port,original=null,font=19;
 function connect(){try{port=browser.runtime.connectNative('kosherPage');port.onDisconnect.addListener(()=>{port=null;setTimeout(connect,1200)});port.onMessage.addListener(command);}catch{setTimeout(connect,1200)}}
 function command(message){
  if(message.op==='reader'){
   if(original){document.body.replaceWith(original);original=null;return;}
   const source=document.querySelector('article')||document.querySelector('main')||document.body;
   const copy=source.cloneNode(true);copy.querySelectorAll('script,style,nav,header,footer,aside,form,button,input,select,textarea,iframe,video,audio,canvas,svg,object,embed').forEach(el=>el.remove());
   const paragraphs=[...copy.querySelectorAll('h1,h2,h3,h4,p,li,blockquote,pre')].filter(el=>!el.parentElement?.closest('p,li,blockquote,pre'));
   const body=document.createElement('body');body.style.cssText='margin:0;background:#faf8f2;color:#202124;font:19px/1.75 Georgia,serif;padding:24px;';
   const article=document.createElement('article');article.style.cssText='max-width:760px;margin:auto;overflow-wrap:anywhere';
   const title=document.createElement('h1');title.textContent=document.title;article.append(title);
   if(paragraphs.length)for(const el of paragraphs){const p=document.createElement(el.tagName.toLowerCase());p.textContent=el.textContent;article.append(p);}
   else{const p=document.createElement('p');p.style.whiteSpace='pre-wrap';p.textContent=copy.innerText||copy.textContent;article.append(p);}
   body.append(article);original=document.body;document.body.replaceWith(body);document.body.style.fontSize=font+'px';
  }else if(message.op==='font'){font=Math.max(14,Math.min(36,Number(message.size)||19));if(original)document.body.style.fontSize=font+'px';}
 }
 connect();
}
