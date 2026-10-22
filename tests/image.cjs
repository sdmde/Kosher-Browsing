const {chromium}=require('playwright');
const fs=require('fs');
const path=require('path');
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.CHROMIUM_PATH?{executablePath:process.env.CHROMIUM_PATH}:{})});
 try {
  const page=await browser.newPage();
  await page.goto('about:blank');
  await page.addScriptTag({content:fs.readFileSync(path.join(__dirname,'../app/src/main/assets/protection/filter.js'),'utf8')});
  console.log(await page.evaluate(async()=>{
   const cases=[
    {name:'40x40 centered',w:160,h:160,x:60,y:60,sw:40,sh:40,expected:70*70},
    {name:'top-left edge',w:100,h:100,x:0,y:0,sw:40,sh:40,expected:55*55},
    {name:'bottom-right edge',w:100,h:100,x:60,y:60,sw:40,sh:40,expected:55*55},
    {name:'image smaller than border',w:9,h:7,x:3,y:2,sw:3,sh:2,expected:9*7},
    {name:'single skin pixel',w:80,h:80,x:40,y:40,sw:1,sh:1,expected:31*31},
    {name:'no skin',w:50,h:50,x:0,y:0,sw:0,sh:0,expected:0}
   ];
   for(const test of cases){
    const canvas=document.createElement('canvas');canvas.width=test.w;canvas.height=test.h;
    const ctx=canvas.getContext('2d',{willReadFrequently:true});
    ctx.fillStyle='rgb(20,80,200)';ctx.fillRect(0,0,test.w,test.h);
    ctx.fillStyle='rgb(190,130,90)';ctx.fillRect(test.x,test.y,test.sw,test.sh);
    const original=ctx.getImageData(0,0,test.w,test.h).data;
    const input=await new Promise(resolve=>canvas.toBlob(resolve,'image/png'));
    const output=await ShieldFilter.image(input);
    if(output.type!=='image/png')throw Error(test.name+': output is not PNG');
    const bitmap=await createImageBitmap(output);
    if(bitmap.width!==test.w||bitmap.height!==test.h)throw Error(test.name+': dimensions changed');
    ctx.clearRect(0,0,test.w,test.h);ctx.drawImage(bitmap,0,0);bitmap.close();
    const actual=ctx.getImageData(0,0,test.w,test.h).data;
    let filtered=0;
    for(let y=0;y<test.h;y++)for(let x=0;x<test.w;x++){
     const p=(y*test.w+x)*4;
     const inside=test.sw>0&&test.sh>0&&x>=test.x-15&&x<test.x+test.sw+15&&y>=test.y-15&&y<test.y+test.sh+15;
     if(inside){
      filtered++;
      if(actual[p]!==actual[p+1]||actual[p+1]!==actual[p+2]||actual[p+3]!==255)throw Error(test.name+': unfiltered mask pixel at '+x+','+y);
     }else for(let channel=0;channel<4;channel++)if(actual[p+channel]!==original[p+channel])throw Error(test.name+': outside pixel changed at '+x+','+y);
    }
    if(filtered!==test.expected)throw Error(test.name+': incorrect mask area '+filtered);
   }
   let rejected=false;
   try{await ShieldFilter.image(new Blob(['invalid']));}catch{rejected=true;}
   if(!rejected)throw Error('Invalid image accepted');
   return 'Image tests passed: exact 40x40 -> 70x70 region, square corners, clipped edges, tiny image, single pixel, unchanged outside, PNG output, invalid decode rejected';
  }));
 } finally {await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
