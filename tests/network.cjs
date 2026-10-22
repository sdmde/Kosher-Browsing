const fs=require('fs'),vm=require('vm'),assert=require('assert');
const base=require('path').join(__dirname,'../app/src/main/assets/protection/');
const listeners={},filters=new Map();
global.browser={webRequest:{onBeforeRequest:{addListener:f=>listeners.before=f},onHeadersReceived:{addListener:f=>listeners.headers=f},onBeforeSendHeaders:{addListener:f=>listeners.send=f},filterResponseData(id){const f={writes:[],closed:false,write(v){this.writes.push(v)},close(){this.closed=true;}};filters.set(id,f);return f;}}};
for(const f of ['adult-hosts.js','policy.js','rules.js','captcha.js','filter.js','background.js'])vm.runInThisContext(fs.readFileSync(base+f,'utf8'));
async function run(){
 let i=0;const req=(url,type='main_frame',extra={})=>listeners.before({url,type,method:'GET',requestId:''+(++i),...extra});
 for(const u of ['https://doubleclick.net/ad','https://www.youtube.com/shorts/abc','https://www.reddit.com/','https://www.instagram.com/','https://x.com/'])assert(req(u).cancel,u);
 for(const url of ['https://duckduckgo.com/','https://www.google.com/search','https://www.bing.com/search','https://search.yahoo.com/search','https://yandex.com/search/']){
  for(const type of ['main_frame','sub_frame'])assert(req(url,type,{method:'POST',requestBody:{formData:{q:['cats']}}}).cancel,'Supported search POST navigation must stay blocked');
 }
 // These values are synthetic test strings, never real credentials or network requests.
 const credentialPost=req('https://accounts.google.com/signin','main_frame',{method:'POST',requestBody:{formData:{identifier:['test@example.invalid'],password:['synthetic-porn-only'],q:['synthetic-sex-only']}}});
 assert(!credentialPost.cancel&&!credentialPost.redirectUrl,'Login POST fields must not be classified as search queries');
 assert(!req('https://example.org/account','main_frame',{method:'POST',requestBody:{formData:{password:['synthetic-porn-only']}}}).cancel,'Ordinary form POST credentials must not be classified as search queries');
 assert(req('https://www.google.com/search','xmlhttprequest',{method:'POST',requestBody:{formData:{q:['porn']}}}).cancel,'Supported search POST queries remain filtered outside navigation');
 assert(!req('https://www.google.com/search?safe=active','xmlhttprequest',{method:'POST',requestBody:{formData:{q:['cats'],password:['synthetic-porn-only']}}}).cancel,'Search body filtering only checks designated query fields');
 assert(req('https://www.reddit.com/submit','main_frame',{method:'POST',requestBody:{formData:{title:['cats']}}}).cancel,'Social host restrictions still apply to POSTs');
 assert(req('https://example.org/search?q=porn','main_frame',{method:'POST',requestBody:{formData:{password:['test-only']}}}).cancel,'URL search restrictions still apply to POSTs');
 assert.equal(new URL(req('https://duckduckgo.com/?q=cats').redirectUrl).searchParams.get('kp'),'1');
 const challenge='https://www.google.com/recaptcha/api2/bframe?k=test';
 const payload='https://www.google.com/recaptcha/api2/payload?p=test';
 let before=filters.size;assert(!req(payload,'image',{documentUrl:challenge}).cancel);assert.equal(filters.size,before,'CAPTCHA bytes must not be filtered');
 assert(!listeners.headers({url:payload,type:'image',documentUrl:challenge,responseHeaders:[{name:'Content-Type',value:'image/jpeg'}]}).responseHeaders,'CAPTCHA headers remain original');
 req(payload,'image',{documentUrl:'https://example.org/'});assert.equal(filters.size,before+1,'Outside a trusted challenge images remain filtered');
 assert(!ShieldCaptcha.challenge('https://www.google.com.evil.test/recaptcha/api2/anchor'));
 assert(!ShieldCaptcha.challenge('https://example.org/?captcha=yes'));
 for(const h of ShieldPolicy.randomChat){assert(req('https://'+h+'/').cancel);assert(req('https://sub.'+h+'/','xmlhttprequest').cancel);}
 assert(!req('https://chatroulette.com.example.org/').cancel,'Do not block unrelated hostname suffixes');
 let result=req('https://example.org/picture.jpg','image');let f=filters.get(''+i);assert(f);f.ondata({data:new Uint8Array([1,2]).buffer});assert.equal(f.writes.length,0,'Original bytes must not escape before processing');
 ShieldFilter.image=async()=>{throw Error('Decode failed')};await f.onstop();assert(f.closed);assert.equal(f.writes.length,0);
 req('https://example.org/ok.png','image');f=filters.get(''+i);ShieldFilter.image=async()=>new Blob([new Uint8Array([9,8,7])]);await f.onstop();assert(f.closed);assert.deepEqual([...new Uint8Array(f.writes[0])],[9,8,7]);
 req('https://example.org/oversize.png','image');f=filters.get(''+i);f.ondata({data:new ArrayBuffer(21*1024*1024)});assert(f.closed);assert.equal(f.writes.length,0);
 let h=listeners.send({url:'https://www.youtube.com/watch?v=test',requestHeaders:[{name:'YouTube-Restrict',value:'Off'}]});assert.equal(h.requestHeaders.filter(h=>h.name.toLowerCase()==='youtube-restrict').length,1);assert.equal(h.requestHeaders[0].value,'Strict');
 console.log('Network tests passed: sites/ads/Shorts/search POST blocks, login POST preserved, safe redirect, delayed filtered-only output, empty output on decode/size failure, YouTube Strict header');
}run().catch(e=>{console.error(e);process.exitCode=1});
