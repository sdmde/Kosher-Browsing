const stats={ads:0,sites:0,shorts:0,filteredImages:0,failedImages:0};let statsPort,statsTimer;
function publish(){try{statsPort?.postMessage(stats);}catch{}browser.storage?.local.set({stats});}
browser.storage?.local.get('stats').then(saved=>{for(const k of Object.keys(stats))stats[k]+=Math.max(0,Number(saved.stats?.[k])||0);publish();});
function count(key){stats[key]++;clearTimeout(statsTimer);statsTimer=setTimeout(publish,700);}
function connectStats(){if(!browser.runtime?.connectNative)return;try{statsPort=browser.runtime.connectNative('kosherStats');statsPort.onDisconnect.addListener(()=>{statsPort=null;setTimeout(connectStats,1500);});publish();}catch{setTimeout(connectStats,1500);}}
connectStats();
function reason(url){try{const u=new URL(url);if((u.hostname==='youtube.com'||u.hostname.endsWith('.youtube.com'))&&/^\/shorts(?:\/|$)/.test(u.pathname))return 'shorts';let host=u.hostname;while(host.includes('.')){if(AD_HOSTS.has(host))return 'ad';host=host.slice(host.indexOf('.')+1);}return AD_PATHS.some(p=>u.pathname.includes(p))?'ad':null;}catch{return 'adult';}}
function deny(details,why){count(why==='ad'?'ads':why==='shorts'?'shorts':'sites');return details.type==='main_frame'&&browser.runtime?.getURL?{redirectUrl:browser.runtime.getURL('blocked.html')+'?reason='+why+'&url='+encodeURIComponent(details.url)}:{cancel:true};}
function policyReason(url){const host=new URL(url).hostname.toLowerCase().replace(/\.$/,'');if(ShieldPolicy.isRandomChatHost(host))return 'randomchat';return ShieldPolicy.social.some(h=>host===h||host.endsWith('.'+h))?'social':'adult';}
browser.webRequest.onBeforeRequest.addListener(details=>{
 if(details.method==='POST'){
  const u=new URL(details.url), searchHost=/(^|\.)(duckduckgo\.com|bing\.com|search\.yahoo\.com|yandex\.(com|ru))$/.test(u.hostname)||(/(^|\.)google\.[a-z.]+$/.test(u.hostname)&&u.pathname==='/search');
  if((details.type==='main_frame'||details.type==='sub_frame')&&searchHost)return deny(details,'search');
  const fields=details.requestBody?.formData;
  if(searchHost&&fields){
   const searchText=['q','query','p','search_query','search','text','wd','keyword'].flatMap(key=>fields[key]||[]).join(' ').normalize('NFKC');
   if(ShieldPolicy.adultTerms.test(searchText))return deny(details,'adult');
  }
 }
 const policy=ShieldPolicy.inspect(details.url,details.type);if(policy.cancel)return deny(details,policyReason(details.url));if(policy.redirectUrl)return policy;
 const why=reason(details.url);if(why)return deny(details,why);
 if(details.type!=='image')return {};
 if(ShieldCaptcha.image(details.url,details.documentUrl))return {};
 let filter;try{filter=browser.webRequest.filterResponseData(details.requestId);}catch{count('failedImages');return {cancel:true};}
 let chunks=[],size=0,ended=false,success=false;
 const close=()=>{if(ended)return;ended=true;clearTimeout(timer);chunks=[];count(success?'filteredImages':'failedImages');try{filter.close();}catch{}};
 const timer=setTimeout(close,20000);
 filter.ondata=e=>{size+=e.data.byteLength;if(size>20*1024*1024)close();else if(!ended)chunks.push(e.data);};
 filter.onerror=close;
 filter.onstop=async()=>{if(ended)return;try{const output=await ShieldFilter.image(new Blob(chunks));const bytes=await output.arrayBuffer();if(!ended){filter.write(bytes);success=true;}}catch{}finally{close();}};
 return {};
},{urls:['http://*/*','https://*/*']},['blocking','requestBody']);
const resumes=new Set();
browser.webRequest.onBeforeSendHeaders.addListener(details=>{
 const host=new URL(details.url).hostname;
 let changed=false,headers=details.requestHeaders.filter(h=>{if(h.name.toLowerCase()==='x-kosher-resume'){resumes.add(details.requestId);changed=true;return false;}return true;});
 if(host==='youtube.com'||host.endsWith('.youtube.com')||host==='youtubei.googleapis.com'){
  headers=headers.filter(h=>h.name.toLowerCase()!=='youtube-restrict');headers.push({name:'YouTube-Restrict',value:'Strict'});changed=true;
 }
 return changed?{requestHeaders:headers}:{};
},{urls:['http://*/*','https://*/*']},['blocking','requestHeaders']);
browser.webRequest.onHeadersReceived.addListener(details=>{let headers=details.responseHeaders,changed=false;const mime=(headers.find(h=>h.name.toLowerCase()==='content-type')?.value||'').toLowerCase();if(mime.includes('application/vnd.android.package-archive'))return {cancel:true};if(['main_frame','sub_frame','object'].includes(details.type)&&mime.startsWith('image/')&&!ShieldCaptcha.challenge(details.url))return {cancel:true};if(resumes.has(details.requestId)){headers=headers.filter(h=>h.name.toLowerCase()!=='content-disposition');headers.push({name:'Content-Disposition',value:'attachment'});changed=true;}if(details.type==='image'&&!ShieldCaptcha.image(details.url,details.documentUrl)){headers=headers.filter(h=>!['content-length','content-type','content-encoding','content-security-policy','x-content-type-options','cache-control'].includes(h.name.toLowerCase())).concat([{name:'Content-Type',value:'image/png'},{name:'Cache-Control',value:'no-store'}]);changed=true;}return changed?{responseHeaders:headers}:{};},{urls:['http://*/*','https://*/*']},['blocking','responseHeaders']);
browser.webRequest.onCompleted?.addListener(d=>resumes.delete(d.requestId),{urls:['http://*/*','https://*/*']});
browser.webRequest.onErrorOccurred?.addListener(d=>resumes.delete(d.requestId),{urls:['http://*/*','https://*/*']});

// Content scripts only reveal an HTTP image after this second, explicit verification.
let imageJobs=0;const imageQueue=[];
async function filteredImage(url){
 const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),20000);
 try{const u=new URL(url);if(!/^https?:$/.test(u.protocol)||ShieldPolicy.inspect(url,'image').cancel||reason(url))throw Error('Unsupported or blocked URL');
 const response=await fetch(url,{credentials:'include',cache:'no-store',signal:controller.signal});if(!response.ok||ShieldPolicy.inspect(response.url||url,'image').cancel||reason(response.url||url))throw Error('Image request failed');
 const reader=response.body.getReader(),chunks=[];let size=0;
 while(true){const part=await reader.read();if(part.done)break;size+=part.value.byteLength;if(size>20*1024*1024){await reader.cancel();throw Error('Image too large');}chunks.push(part.value);}
 const output=await ShieldFilter.image(new Blob(chunks));const data=await new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(reader.result);reader.onerror=reject;reader.readAsDataURL(output);});return {ok:true,data};
 }catch{return {ok:false};}finally{clearTimeout(timer);}
}
function drainImages(){while(imageJobs<4&&imageQueue.length){const job=imageQueue.shift();imageJobs++;filteredImage(job.url).then(job.resolve,()=>job.resolve({ok:false})).finally(()=>{imageJobs--;drainImages();});}}
browser.runtime?.onMessage?.addListener((message,sender)=>{if(message?.kind!=='filter-image')return;if(!sender?.tab||!/^https?:/i.test(sender.url||'')||imageQueue.length>=100)return Promise.resolve({ok:false});return new Promise(resolve=>{imageQueue.push({url:message.url,resolve});drainImages();});});
