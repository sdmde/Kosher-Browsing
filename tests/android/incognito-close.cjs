const fs=require('fs'),assert=require('assert'),Adb=require('./direct-adb.cjs');
const pause=ms=>new Promise(r=>setTimeout(r,ms));fs.mkdirSync('work',{recursive:true});
(async()=>{const a=new Adb();try{
async function xml(){await a.exec('uiautomator dump /data/local/tmp/close.xml');return(await a.exec('cat /data/local/tmp/close.xml')).toString();}
async function tap(label,prefix=false){const q=label.toLowerCase(),n=((await xml()).match(/<node\s[^>]+>/g)||[]).find(n=>n.toLowerCase().includes('content-desc="'+q+(prefix?'':'"'))||n.toLowerCase().includes('text="'+q+(prefix?'':'"')));assert(n,'Missing '+label);const b=n.match(/bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/);await a.exec('input tap '+Math.floor((+b[1]+ +b[3])/2)+' '+Math.floor((+b[2]+ +b[4])/2));await pause(300);}
async function ready(){for(let i=0;i<15;i++){const x=await xml();if(x.includes('Browser menu')&&/Tabs: [1-9]/.test(x))return x;await pause(500);}throw Error('Browser not ready');}
async function regular(){const x=await xml();assert(x.includes('Browser menu'),'Tray should close when private mode ends');assert(!x.includes('Incognito search or address'));assert(!x.includes('History and tabs are not saved.'));return x;}
async function privateTab(){await tap('Browser menu');await tap('New incognito tab');const x=await xml();assert(x.includes('Incognito search or address'));}
await a.exec('am start -n org.shield.browser/.MainActivity');let x=await ready();
const before='work/incognito-upgrade-before.json';
if(process.argv.includes('--before-upgrade')){const count=+x.match(/content-desc="Tabs: (\d+)"/)[1];await tap('Tabs: ',true);x=await xml();const grouped=x.includes('Tab group Work');await a.exec('input keyevent 4');fs.writeFileSync(before,JSON.stringify({count,grouped}));console.log('Existing normal tabs recorded for upgrade check');return;}
if(fs.existsSync(before)){const old=JSON.parse(fs.readFileSync(before));assert.equal(+x.match(/content-desc="Tabs: (\d+)"/)[1],old.count,'Update lost normal tabs');await tap('Tabs: ',true);x=await xml();if(old.grouped)assert(x.includes('Tab group Work'),'Update lost saved group');await a.exec('input keyevent 4');}
const access=(await a.exec('run-as org.shield.browser id')).toString();assert(/not debuggable/i.test(access),'Installed app must reject debug run-as');
await privateTab();await tap('Tabs: ',true);await tap('Close Home');await regular();
await tap('Tabs: ',true);await tap('New tab');await regular();
await privateTab();await privateTab();await tap('Tabs: ',true);await tap('Close Home');x=await xml();assert(x.includes('Tab options')&&x.includes('Incognito'),'Another private tab should keep private mode');await tap('Close Home');await regular();
await privateTab();await privateTab();await tap('Tabs: ',true);await tap('Tab options');await tap('Close all tabs');await tap('Close all');await regular();
await privateTab();await tap('Tabs: ',true);await tap('Tab options');await tap('New tab group');await a.exec('input text ExitPrivate');await tap('Create');await tap('Back to tabs');await tap('Close Home');await tap('ExitPrivate');await tap('Open Home');await tap('Close grouped tab Home');await regular();
await tap('Tabs: ',true);await tap('Tab options');await tap('Close all tabs');await tap('Close all');x=await xml();assert(x.includes('You’ll find your tabs here'));await tap('Tab options');await tap('New incognito tab');await tap('Tabs: ',true);await tap('Close Home');x=await regular();assert(x.includes('Tabs: 1'),'Closing the only private tab must create one normal tab');
await a.exec('am force-stop org.shield.browser');await a.exec('am start -n org.shield.browser/.MainActivity');await ready();await regular();
fs.writeFileSync('work/incognito-close-results.json',JSON.stringify({api:30,releaseVariant:true,debugRunAsDenied:true,existingNormalTabsPreservedOnUpdate:fs.existsSync(before),lastPrivateReturnsToNormal:true,newTabAfterPrivateIsNormal:true,remainingPrivateTabRetainsMode:true,closeAllPrivateReturnsToNormal:true,groupStripCloseReturnsToNormal:true,onlyPrivateCreatesSingleNormalTab:true,relaunchNormal:true},null,2)+'\n');console.log('Release update, debug-disabled, private-tab closure and normal-mode regression checks passed');
}finally{a.close();}})().catch(e=>{console.error(e);process.exit(1)});
