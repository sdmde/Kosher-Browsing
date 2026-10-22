const fs=require('fs'),vm=require('vm'),assert=require('assert');
const base=require('path').join(__dirname,'../app/src/main/assets/protection/');
for(const f of ['adult-hosts.js','policy.js','rules.js','captcha.js','filter.js'])vm.runInThisContext(fs.readFileSync(base+f,'utf8'),{filename:f});
let total=0;function test(url,check,type){assert(check(ShieldPolicy.inspect(url,type)),url);total++;}
for(const h of ['reddit.com','old.reddit.com','redd.it','x.com','mobile.twitter.com','www.instagram.com','cdninstagram.com','pornhub.com','www.xvideos.com'])test('https://'+h+'/',r=>r.cancel);
for(const q of ['porn','porn videos','nudes','hentai','onlyfans','adult videos','SEX'])test('https://duckduckgo.com/?q='+encodeURIComponent(q),r=>r.cancel);
for(const [url,key,value] of [['https://duckduckgo.com/?q=weather&kp=-2','kp','1'],['https://www.google.com/search?q=flowers&safe=off','safe','active'],['https://www.bing.com/search?q=cats&adlt=off','adlt','strict'],['https://search.yahoo.com/search?p=cats','vm','r']])test(url,r=>new URL(r.redirectUrl).searchParams.get(key)===value);
test('https://duckduckgo.com/?q=weather&kp=1',r=>!r.cancel&&!r.redirectUrl);
test('https://example.org/',r=>!r.cancel&&!r.redirectUrl);
assert(ShieldFilter.skin(190,130,90));assert(!ShieldFilter.skin(20,80,200));assert(!ShieldFilter.skin(200,200,200));
for(const f of ['background.js','content.js'])new vm.Script(fs.readFileSync(base+f,'utf8'),{filename:f});
console.log(total+' policy checks plus RGB and script syntax checks passed');
