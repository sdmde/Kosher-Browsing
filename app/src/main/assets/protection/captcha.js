// Exceptions require a recognized challenge document and image endpoint.
globalThis.ShieldCaptcha={
 challenge(value){try{const u=new URL(value);if(u.protocol!=='https:')return false;
 return ((u.hostname==='www.google.com'||u.hostname==='www.recaptcha.net'||u.hostname==='recaptcha.google.com')&&u.pathname.startsWith('/recaptcha/'))||((u.hostname==='hcaptcha.com'||u.hostname.endsWith('.hcaptcha.com'))&&u.pathname.startsWith('/captcha/'))||(u.hostname==='challenges.cloudflare.com'&&u.pathname.startsWith('/cdn-cgi/challenge-platform/'));
 }catch{return false;}},
 image(url,documentUrl){if(!this.challenge(documentUrl))return false;try{const u=new URL(url);if(u.protocol!=='https:')return false;const d=new URL(documentUrl);
 if(d.hostname==='challenges.cloudflare.com')return u.hostname===d.hostname&&u.pathname.startsWith('/cdn-cgi/challenge-platform/');
 if(d.hostname==='hcaptcha.com'||d.hostname.endsWith('.hcaptcha.com'))return u.hostname==='imgs.hcaptcha.com'||u.hostname==='newassets.hcaptcha.com';
 return ((u.hostname==='www.google.com'||u.hostname==='www.recaptcha.net'||u.hostname==='recaptcha.google.com')&&/^\/recaptcha\/(api2|enterprise)\/payload$/.test(u.pathname))||(u.hostname==='www.gstatic.com'&&u.pathname.startsWith('/recaptcha/'));
 }catch{return false;}}
};
