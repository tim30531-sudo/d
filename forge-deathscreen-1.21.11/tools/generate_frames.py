from PIL import Image,ImageDraw,ImageFilter,ImageChops,ImageEnhance
import math,random,sys
from pathlib import Path
OUT=Path(sys.argv[1]);OUT.mkdir(parents=True,exist_ok=True)
W,H,S,N=640,360,2,120; SW,SH=W*S,H*S
random.seed(30531)
ambient=[(random.random(),random.random(),random.uniform(.4,1),random.uniform(.5,2)) for _ in range(80)]
shards=[(random.uniform(-1,1),random.uniform(-1,1),random.uniform(.4,1.4)) for _ in range(60)]
sparks=[(random.uniform(0,math.tau),random.uniform(.5,1.3)) for _ in range(90)]
def cl(v,a=0,b=1):return max(a,min(b,v))
def sm(v):v=cl(v);return v*v*(3-2*v)
def sm5(v):v=cl(v);return v*v*v*(v*(v*6-15)+10)
def out(v):v=cl(v);return 1-(1-v)**3
def X(v):return int(round(v*S))
def P(x,y):return(X(x),X(y))
def layer():return Image.new('RGBA',(SW,SH),(0,0,0,0))
def poly(d,ps,c):d.polygon([P(*p) for p in ps],fill=c)
def glow(im,cx,cy,r,c,a,b=18):
 l=layer();ImageDraw.Draw(l).ellipse((X(cx-r),X(cy-r),X(cx+r),X(cy+r)),fill=(*c,int(a)));l=l.filter(ImageFilter.GaussianBlur(X(b)));return Image.alpha_composite(im,l)
def lineglow(im,a,b,w,c,alpha=255,blur=8):
 l=layer();d=ImageDraw.Draw(l);d.line([P(*a),P(*b)],fill=(*c,int(alpha*.55)),width=X(w*3));l=l.filter(ImageFilter.GaussianBlur(X(blur)));im=Image.alpha_composite(im,l);ImageDraw.Draw(im).line([P(*a),P(*b)],fill=(*c,alpha),width=max(1,X(w)));return im
def hpts(cx,cy,z):
 q=[]
 for i in range(180):
  t=2*math.pi*i/180;x=16*math.sin(t)**3*z/32;y=-(13*math.cos(t)-5*math.cos(2*t)-2*math.cos(3*t)-math.cos(4*t))*z/32;q.append((cx+x,cy+y))
 return q
def hmask(cx,cy,z):
 m=Image.new('L',(SW,SH),0);ImageDraw.Draw(m).polygon([P(*p) for p in hpts(cx,cy,z)],fill=255);return m
def heart(im,cx,cy,z,alpha,crack,split,explode):
 m=hmask(cx,cy,z);g=layer();gd=ImageDraw.Draw(g)
 y0,y1=X(cy-z*.55),X(cy+z*.58)
 for y in range(max(0,y0),min(SH,y1+1)):
  q=cl((y-y0)/max(1,y1-y0));gd.line((0,y,SW,y),fill=(int(80-45*q),int(235-75*q),int(255-55*q),alpha))
 full=Image.composite(g,layer(),m);f=layer();d=ImageDraw.Draw(f)
 for ps,c in [([(-.36,-.18),(0,-.42),(-.04,.03)],(190,255,255,120)),([(0,-.42),(.34,-.18),(-.04,.03)],(95,200,255,80)),([(-.36,-.18),(-.04,.03),(-.20,.34)],(20,145,240,80)),([(.34,-.18),(-.04,.03),(.18,.34)],(10,115,225,100)),([(-.20,.34),(-.04,.03),(.18,.34),(0,.49)],(0,75,175,95))]:
  poly(d,[(cx+x*z,cy+y*z) for x,y in ps],c)
 f.putalpha(ImageChops.multiply(f.getchannel('A'),m));full=Image.alpha_composite(full,f)
 o=layer();od=ImageDraw.Draw(o);od.line([P(*p) for p in hpts(cx,cy,z)],fill=(225,255,255,alpha),width=X(2),joint='curve');full=Image.alpha_composite(full,o)
 sh=layer();sd=ImageDraw.Draw(sh);sd.arc((X(cx-z*.38),X(cy-z*.34),X(cx+z*.06),X(cy+z*.12)),195,330,fill=(245,255,255,int(alpha*.8)),width=X(3));sh.putalpha(ImageChops.multiply(sh.getchannel('A'),m));full=Image.alpha_composite(full,sh)
 bl=full.filter(ImageFilter.GaussianBlur(X(14)));bl.putalpha(bl.getchannel('A').point(lambda v:int(v*.68)));im=Image.alpha_composite(im,bl)
 if split<=0:im=Image.alpha_composite(im,full)
 else:
  for side in(-1,1):
   hm=Image.new('L',(SW,SH),0);hd=ImageDraw.Draw(hm);hd.rectangle((0,0,X(cx),SH) if side<0 else (X(cx),0,SW,SH),fill=255)
   part=Image.composite(full,layer(),hm).rotate(side*(split*22+explode*18),Image.Resampling.BICUBIC,center=P(cx,cy));shift=layer();shift.alpha_composite(part,(X(side*(split*z*.22+explode*z*.55)),X(explode*z*.13)));im=Image.alpha_composite(im,shift)
 if crack>0:
  d=ImageDraw.Draw(im);a=int(255*crack)
  for ls in [[(cx,cy-z*.22),(cx-z*.05,cy-z*.04),(cx+z*.03,cy+z*.08),(cx-z*.08,cy+z*.25)],[(cx-z*.05,cy-z*.04),(cx-z*.22,cy+z*.04),(cx-z*.31,cy+z*.17)],[(cx+z*.03,cy+z*.08),(cx+z*.18,cy+z*.18),(cx+z*.24,cy+z*.34)]]:
   d.line([P(*p) for p in ls],fill=(3,15,35,a),width=X(2));d.line([P(p[0]+1,p[1]-1) for p in ls],fill=(235,255,255,int(a*.6)),width=X(1))
 return im
def rot(p,c,a):
 x,y=p;cx,cy=c;co,si=math.cos(a),math.sin(a);dx,dy=x-cx,y-cy;return(cx+dx*co-dy*si,cy+dx*si+dy*co)
def knight(im,cx,cy,z,pose,alpha):
 l=layer();d=ImageDraw.Draw(l);Q=lambda x,y:(cx+x*z,cy+y*z)
 poly(d,[Q(-20,-40),Q(38,-32),Q(54,90),Q(-38,98)],(4,9,22,int(alpha*.9)));poly(d,[Q(16,-25),Q(42,-18),Q(50,88),Q(23,80)],(14,23,48,int(alpha*.7)))
 poly(d,[Q(-35,-2),Q(-22,-26),Q(25,-26),Q(40,-1),Q(30,70),Q(-28,70)],(17,28,53,alpha));poly(d,[Q(-20,-18),Q(2,-8),Q(22,-18),Q(28,55),Q(2,66),Q(-23,54)],(29,46,79,alpha))
 for yy in(6,22,38,54):d.arc((X(cx-24*z),X(cy+(yy-8)*z),X(cx+27*z),X(cy+(yy+11)*z)),5,175,fill=(95,130,168,int(alpha*.75)),width=max(1,X(1.2*z)))
 poly(d,[Q(-55,-17),Q(-25,-31),Q(-17,-3),Q(-49,8)],(31,47,75,alpha));poly(d,[Q(51,-16),Q(23,-31),Q(17,-3),Q(48,9)],(31,47,75,alpha))
 poly(d,[Q(-27,-75),Q(-14,-94),Q(20,-94),Q(31,-74),Q(25,-27),Q(-23,-27)],(11,19,38,alpha));poly(d,[Q(-20,-84),Q(17,-84),Q(24,-69),Q(-24,-69)],(37,55,84,alpha));poly(d,[Q(-24,-67),Q(25,-67),Q(20,-47),Q(-20,-47)],(3,9,22,alpha))
 d.line([P(*Q(-14,-92)),P(*Q(-6,-107)),P(*Q(12,-93))],fill=(130,165,198,int(alpha*.9)),width=max(1,X(2*z)));d.line([P(*Q(-26,-74)),P(*Q(-22,-27)),P(*Q(25,-27)),P(*Q(30,-73))],fill=(87,120,160,int(alpha*.9)),width=max(1,X(1.7*z)))
 d.polygon([P(*Q(-16,-60)),P(*Q(-3,-58)),P(*Q(-6,-53)),P(*Q(-18,-55))],fill=(45,240,255,alpha));d.polygon([P(*Q(4,-58)),P(*Q(17,-60)),P(*Q(19,-55)),P(*Q(7,-53))],fill=(45,240,255,alpha))
 eyes=l.copy().filter(ImageFilter.GaussianBlur(X(6)));im=Image.alpha_composite(im,eyes)
 shoulder=Q(32,-8);armang=-1.15*pose+.12;aps=[Q(24,-8),Q(44,-4),Q(64,28),Q(50,39),Q(29,16)];aps=[rot(p,shoulder,armang) for p in aps];poly(d,aps,(32,49,82,alpha));d.line([P(*p) for p in aps+[aps[0]]],fill=(110,145,183,int(alpha*.8)),width=max(1,X(1.2*z)))
 hand=rot(Q(55,18),shoulder,armang);ang=-.88-1.05*pose;ux,uy=math.cos(ang),math.sin(ang);nx,ny=-uy,ux;tip=(hand[0]+ux*145*z,hand[1]+uy*145*z)
 p1=(hand[0]+nx*4*z,hand[1]+ny*4*z);p2=(tip[0]+nx*1.2*z,tip[1]+ny*1.2*z);p3=(tip[0]-nx*1.2*z,tip[1]-ny*1.2*z);p4=(hand[0]-nx*4*z,hand[1]-ny*4*z);poly(d,[p1,p2,p3,p4],(218,248,255,alpha));d.line([P(*p1),P(*p2)],fill=(95,220,255,alpha),width=max(1,X(1.3*z)));d.line([P(*p4),P(*p3)],fill=(25,110,220,alpha),width=max(1,X(1.3*z)))
 g1=(hand[0]+nx*11*z,hand[1]+ny*11*z);g2=(hand[0]-nx*11*z,hand[1]-ny*11*z);d.line([P(*g1),P(*g2)],fill=(145,180,210,alpha),width=max(1,X(4*z)));ge=(hand[0]-ux*18*z,hand[1]-uy*18*z);d.line([P(*hand),P(*ge)],fill=(24,33,53,alpha),width=max(1,X(6*z)))
 bg=layer();ImageDraw.Draw(bg).line([P(*hand),P(*tip)],fill=(30,210,255,int(alpha*.75)),width=max(1,X(12*z)));im=Image.alpha_composite(im,bg.filter(ImageFilter.GaussianBlur(X(12))));im=Image.alpha_composite(im,l);return im
BASE_BG=Image.new('RGBA',(SW,SH),(3,6,15,255));bp=BASE_BG.load()
for y in range(SH):
 yy=y/SH
 for x in range(SW):
  xx=x/SW;r=math.sqrt(((xx-.48)/.78)**2+((yy-.48)/.82)**2);v=cl(1-r);n=(math.sin(xx*15)+math.sin(yy*11)+2)/4;bp[x,y]=(int(2+8*v+2*n),int(5+14*v+4*n),int(15+29*v+8*n),255)
c=Image.new('L',(SW,SH),0);ImageDraw.Draw(c).ellipse((-X(70),-X(45),SW+X(70),SH+X(45)),fill=255);c=c.filter(ImageFilter.GaussianBlur(X(70)));VIGNETTE=layer();VIGNETTE.putalpha(ImageChops.invert(c).point(lambda v:int(v*.8)))
def background(t):
 im=BASE_BG.copy();f=layer();d=ImageDraw.Draw(f)
 for i in range(8):
  y=35+i*42+math.sin(t*3+i)*14;x=-180+((t*80+i*105)%950);d.ellipse((X(x),X(y-18),X(x+340),X(y+25)),fill=(18,45,80,18+i%3*5))
 im=Image.alpha_composite(im,f.filter(ImageFilter.GaussianBlur(X(22))));d=ImageDraw.Draw(im)
 for i,(ax,ay,sp,r) in enumerate(ambient):
  x=((ax*W+t*22*sp+i*3)%(W+60))-30;y=ay*H+math.sin(t*2+i)*7;a=int(18+32*(1-ay));d.ellipse((X(x-r),X(y-r),X(x+r),X(y+r)),fill=(45,145,220,a))
 return Image.alpha_composite(im,VIGNETTE)
def render(i):
 t=i/(N-1);im=background(t);pan=layer();d=ImageDraw.Draw(pan);d.rounded_rectangle((X(55),X(52),X(585),X(308)),radius=X(26),fill=(1,5,14,125),outline=(35,80,125,70),width=X(1));im=Image.alpha_composite(im,pan)
 show=sm((t-.02)/.12)*(1-sm((t-.92)/.08));cx,cy=225,181+math.sin(t*7)*1.5;z=108*(1+.035*math.sin(t*math.pi*10)*(1-cl((t-.48)/.2)))
 if show>0:
  im=glow(im,cx,cy,74,(0,175,255),90*show,18);im=heart(im,cx,cy,z,int(255*show),sm((t-.56)/.08),sm((t-.63)/.10),sm((t-.70)/.20))
 enter=out((t-.12)/.22);leave=sm((t-.84)/.13);kx=710-235*enter+45*leave;ky=203+5*math.sin(enter*math.pi);pose=-sm((t-.37)/.10)+2.15*sm5((t-.49)/.12);a=int(255*(1-leave))
 if .12<t<.38 or .48<t<.64:
  for n in(3,2,1):
   g=layer();g=knight(g,kx+(-12*n if t<.38 else 5*n),ky,.92,pose-(.13*n if t>.48 else 0),int(a*(.08*n)));im=Image.alpha_composite(im,g.filter(ImageFilter.GaussianBlur(X(2+n))))
 im=knight(im,kx,ky,.92,pose,a)
 sl=sm((t-.49)/.10)*(1-sm((t-.64)/.09))
 if sl>0:
  for n in range(6,0,-1):
   off=(n-1)*8;im=lineglow(im,(95-off,300+off*.35),(565-off,55+off*.35),1.3+n*.7,(80,215,255),int(150*sl*(1-(n-1)/7)),5+n)
  im=lineglow(im,(95,300),(565,55),4.2,(240,255,255),int(255*sl),10)
 impact=sm((t-.565)/.025)*(1-sm((t-.62)/.09))
 if impact>0:
  im=glow(im,cx,cy,150,(90,225,255),210*impact,26);b=layer();bd=ImageDraw.Draw(b)
  for n in range(28):
   ang=n*math.tau/28+.13;inn=14+30*(1-impact);outt=80+115*impact*(.65+.35*((n*17)%9)/9);bd.line([P(cx+math.cos(ang)*inn,cy+math.sin(ang)*inn),P(cx+math.cos(ang)*outt,cy+math.sin(ang)*outt)],fill=(155,242,255,int(170*impact)),width=X(1+n%3))
  im=Image.alpha_composite(im,b.filter(ImageFilter.GaussianBlur(X(1.2))));f=layer();r=16+38*impact;ImageDraw.Draw(f).ellipse((X(cx-r),X(cy-r),X(cx+r),X(cy+r)),fill=(255,255,255,int(245*impact)));im=Image.alpha_composite(im,f.filter(ImageFilter.GaussianBlur(X(8))))
 deb=sm((t-.62)/.12)*(1-sm((t-.94)/.06))
 if deb>0:
  l=layer();d=ImageDraw.Draw(l)
  for idx,(vx,vy,sp) in enumerate(shards):
   ang=math.atan2(vy,vx);dist=18+130*deb*sp;x=cx+math.cos(ang)*dist;y=cy+math.sin(ang)*dist+35*deb*deb;sz=(2+5.5*(1-deb))*sp;c=(90,225,255,int(220*(1-deb*.65))) if idx%3 else (235,255,255,int(235*(1-deb*.7)));poly(d,[(x-sz,y-sz*.4),(x+sz*.8,y-sz),(x+sz,y+sz*.5),(x-sz*.6,y+sz)],c)
  for ang,sp in sparks:
   dist=20+165*deb*sp;x=cx+math.cos(ang+deb*.8)*dist;y=cy+math.sin(ang+deb*.8)*dist*.72;r=1.1+2.2*(1-deb);d.ellipse((X(x-r),X(y-r),X(x+r),X(y+r)),fill=(35,195,255,int(180*(1-deb))))
  g=l.filter(ImageFilter.GaussianBlur(X(5)));g.putalpha(g.getchannel('A').point(lambda v:int(v*.55)));im=Image.alpha_composite(im,g);im=Image.alpha_composite(im,l)
 sh=sm((t-.585)/.18)*(1-sm((t-.84)/.12))
 if sh>0:
  l=layer();r=18+180*sh;ImageDraw.Draw(l).ellipse((X(cx-r),X(cy-r),X(cx+r),X(cy+r)),outline=(100,225,255,int(150*(1-sh))),width=X(2.5));im=Image.alpha_composite(im,l.filter(ImageFilter.GaussianBlur(X(2))))
 shake=impact+.35*sm((t-.62)/.04)*(1-sm((t-.75)/.11))
 if shake>0:
  dx=int(math.sin(i*2.9)*4.5*shake*S);dy=int(math.cos(i*2.1)*3*shake*S);q=Image.new('RGBA',(SW,SH),(0,0,0,255));q.alpha_composite(im,(dx,dy));im=q
 if impact>0:
  r,g,b,a=im.split();off=X(2.5*impact*.65);im=Image.merge('RGBA',(ImageChops.offset(r,off,0),g,ImageChops.offset(b,-off,0),a))
 fi=sm(t/.06);fo=sm((t-.91)/.09);im=Image.alpha_composite(im,Image.new('RGBA',(SW,SH),(0,0,0,int(255*(1-fi+fo-(1-fi)*fo)))))
 im=im.convert('RGB').resize((W,H),Image.Resampling.LANCZOS);im=ImageEnhance.Sharpness(im).enhance(1.14);im=im.quantize(colors=256,method=Image.Quantize.MEDIANCUT,dither=Image.Dither.FLOYDSTEINBERG);im.save(OUT/f'frame_{i:03d}.png',optimize=True)
for i in range(N):
 if (OUT/f'frame_{i:03d}.png').exists():
  continue
 render(i)
 if i%20==0:print(i)
print('Generated',N,'frames in',OUT)
