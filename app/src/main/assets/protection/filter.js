/* Skin predicate matches the user-supplied Nudity Filter 3.10 RGB range.
 * New implementation: square dilation by 15 pixels, grayscale + 24px blur.
 * Always re-encodes as PNG, freezing animated images to their first frame. */
globalThis.ShieldFilter = {
 skin(r,g,b) { return r>95 && g>40 && b>20 && Math.max(r,g,b)-Math.min(r,g,b)>15 && Math.abs(r-g)>15 && r>g && r>b; },
 async image(blob) {
  const bitmap=await createImageBitmap(blob);
  try {
   const w=bitmap.width,h=bitmap.height;
   if(!w||!h||w*h>12000000)throw Error('Image dimensions exceed limit');
   const canvas=document.createElement('canvas');canvas.width=w;canvas.height=h;
   const ctx=canvas.getContext('2d',{willReadFrequently:true});ctx.drawImage(bitmap,0,0);
   const pixels=ctx.getImageData(0,0,w,h),mask=new Uint8Array(w*h),wide=new Uint8Array(w*h),expanded=new Uint8Array(w*h);
   for(let i=0;i<mask.length;i++){let p=i*4;mask[i]=this.skin(pixels.data[p],pixels.data[p+1],pixels.data[p+2])?1:0;}
   // A 15px border turns a 40x40 skin region into a 70x70 filtered region.
   // Two sliding windows give square dilation in linear time and clip at image edges.
   const border=15;
   for(let y=0;y<h;y++){let sum=0;for(let x=-border;x<w;x++){if(x+border<w)sum+=mask[y*w+x+border];if(x-border-1>=0)sum-=mask[y*w+x-border-1];if(x>=0)wide[y*w+x]=sum?1:0;}}
   for(let x=0;x<w;x++){let sum=0;for(let y=-border;y<h;y++){if(y+border<h)sum+=wide[(y+border)*w+x];if(y-border-1>=0)sum-=wide[(y-border-1)*w+x];if(y>=0)expanded[y*w+x]=sum?1:0;}}
   const blurred=document.createElement('canvas');blurred.width=w;blurred.height=h;const b=blurred.getContext('2d');b.filter='grayscale(1) blur(24px)';b.drawImage(canvas,0,0);const bp=b.getImageData(0,0,w,h).data;
   for(let i=0;i<expanded.length;i++)if(expanded[i]){const p=i*4;pixels.data[p]=bp[p];pixels.data[p+1]=bp[p+1];pixels.data[p+2]=bp[p+2];pixels.data[p+3]=255;}
   ctx.putImageData(pixels,0,0);
   return await new Promise((resolve,reject)=>canvas.toBlob(v=>v?resolve(v):reject(Error('PNG encoding failed')),'image/png'));
  } finally {bitmap.close();}
 }
};
