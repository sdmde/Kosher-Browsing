const fs=require('fs'),vm=require('vm'),assert=require('assert'),path=require('path');
const base=path.join(__dirname,'../app/src/main/assets/protection');
const listeners={},writes=[],ports=[];
const event=name=>({addListener:fn=>listeners[name]=fn});
const context={URL,Blob,Uint8Array,ArrayBuffer,console,setTimeout:()=>0,clearTimeout:()=>{},browser:{runtime:{getURL:name=>'moz-extension://test/'+name,connectNative:name=>{const p={name,postMessage:value=>writes.push({...value}),onDisconnect:{addListener(){}}};ports.push(p);return p;}},storage:{local:{get:async()=>({stats:{ads:4}}),set:()=>{}}},webRequest:{onBeforeRequest:event('before'),onBeforeSendHeaders:event('send'),onHeadersReceived:event('headers'),onCompleted:event('complete'),onErrorOccurred:event('error')}}};
vm.createContext(context);for(const name of ['adult-hosts.js','policy.js','rules.js','background.js'])vm.runInContext(fs.readFileSync(path.join(base,name),'utf8'),context);
for(const [url,reason] of [['https://redditstatic.com/foo','social'],['https://www.youtube.com/shorts/abc','shorts'],['https://doubleclick.net/ad','ad'],['https://www.pornhub.com/','adult']]){const r=listeners.before({url,type:'main_frame',method:'GET'});assert.equal(new URL(r.redirectUrl).searchParams.get('reason'),reason);assert.equal(new URL(r.redirectUrl).protocol,'moz-extension:');}
assert(listeners.before({url:'https://reddit.com/a',type:'image',method:'GET'}).cancel);
assert.equal(new URL(listeners.before({url:'https://duckduckgo.com/?q=cats&kp=-2',type:'main_frame',method:'GET'}).redirectUrl).searchParams.get('kp'),'1');
const headers=listeners.send({requestId:'resume1',url:'https://example.org/file',requestHeaders:[{name:'X-Kosher-Resume',value:'1'},{name:'Range',value:'bytes=123-'},{name:'If-Range',value:'"file1"'}]}).requestHeaders;
assert(!headers.some(h=>h.name.toLowerCase()==='x-kosher-resume'));assert(headers.some(h=>h.name==='Range'&&h.value==='bytes=123-'));
const resumed=listeners.headers({requestId:'resume1',type:'main_frame',responseHeaders:[{name:'Content-Type',value:'application/pdf'}]}).responseHeaders;
assert(resumed.some(h=>h.name==='Content-Disposition'&&h.value==='attachment'));assert(resumed.some(h=>h.value==='application/pdf'));
listeners.complete({requestId:'resume1'});assert(!listeners.headers({requestId:'resume1',type:'main_frame',responseHeaders:[]}).responseHeaders);
console.log('Advanced request checks passed: diagnostic redirects, subresource blocks, Safe Search, private resume marker removal, download interception and request cleanup');
