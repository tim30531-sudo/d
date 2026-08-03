from PIL import Image, ImageDraw, ImageFilter
import math, random, os

FW,FH=512,288
COLS,ROWS=8,4
FRAMES=32
sheet=Image.new('RGBA',(FW*COLS,FH*ROWS),(0,0,0,255))
random.seed(30531)
particles=[(random.uniform(0,math.tau),random.uniform(35,150),random.uniform(1,4)) for _ in range(70)]

def clamp(v,a=0,b=1): return max(a,min(b,v))
def ease(v): v=clamp(v); return v*v*(3-2*v)
def out(v): v=clamp(v); return 1-(1-v)**3

def glow(im,cx,cy,r,color,alpha):
    l=Image.new('RGBA',im.size,(0,0,0,0)); d=ImageDraw.Draw(l)
    for rr in range(int(r),0,-5):
        a=int(alpha*(1-rr/r)**2)
        d.ellipse((cx-rr,cy-rr,cx+rr,cy+rr),fill=(*color,a))
    im.alpha_composite(l.filter(ImageFilter.GaussianBlur(12)))

def heart(im,cx,cy,s,crack,split,alpha):
    pts=[]
    for i in range(120):
        a=2*math.pi*i/120
        x=16*math.sin(a)**3
        y=13*math.cos(a)-5*math.cos(2*a)-2*math.cos(3*a)-math.cos(4*a)
        pts.append((cx+x*2.05*s,cy-y*2.05*s))
    glow(im,cx,cy,62*s,(0,120,255),int(alpha*.6))
    base=Image.new('RGBA',im.size,(0,0,0,0)); bd=ImageDraw.Draw(base)
    if split<=0:
        bd.polygon(pts,fill=(12,112,230,alpha),outline=(130,235,255,alpha))
        bd.line(pts+[pts[0]],fill=(145,240,255,alpha),width=max(2,int(3*s)),joint='curve')
        bd.line([(cx-24*s,cy-22*s),(cx-10*s,cy-35*s),(cx+2*s,cy-29*s)],fill=(235,255,255,int(alpha*.9)),width=max(2,int(4*s)))
        if crack>0:
            c=[(cx+1,cy-40*s),(cx-5*s,cy-20*s),(cx+3*s,cy-7*s),(cx-6*s,cy+10*s),(cx+4*s,cy+28*s),(cx,cy+46*s)]
            bd.line(c,fill=(235,255,255,int(alpha*crack)),width=max(2,int(3*s)))
            bd.line([c[1],(cx-22*s,cy-10*s)],fill=(180,235,255,int(alpha*crack)),width=2)
            bd.line([c[2],(cx+22*s,cy+1*s)],fill=(180,235,255,int(alpha*crack)),width=2)
    else:
        left=[p for p in pts if p[0]<=cx]; right=[p for p in pts if p[0]>=cx]
        left=sorted(left,key=lambda p:p[1]); right=sorted(right,key=lambda p:p[1],reverse=True)
        if len(left)>2:
            left=[(x-split,y+split*.2) for x,y in left]+[(cx-split,cy+47*s),(cx-split,cy-40*s)]
            bd.polygon(left,fill=(8,89,195,alpha),outline=(130,235,255,alpha))
        if len(right)>2:
            right=[(x+split,y+split*.28) for x,y in right]+[(cx+split,cy-40*s),(cx+split,cy+47*s)]
            bd.polygon(right,fill=(18,125,245,alpha),outline=(150,245,255,alpha))
    im.alpha_composite(base.filter(ImageFilter.GaussianBlur(.4)))

def knight(im,x,y,s,alpha,swing):
    k=Image.new('RGBA',im.size,(0,0,0,0)); d=ImageDraw.Draw(k); A=int(alpha)
    d.polygon([(x-62*s,y+12*s),(x-86*s,y+124*s),(x+76*s,y+130*s),(x+61*s,y+8*s)],fill=(3,5,9,A))
    for i in range(5):
        xx=x-48*s+i*24*s
        d.line([(xx,y+30*s),(xx-10*s,y+120*s)],fill=(20,28,40,int(A*.5)),width=max(2,int(4*s)))
    d.polygon([(x-47*s,y+12*s),(x-59*s,y+70*s),(x-40*s,y+112*s),(x+42*s,y+112*s),(x+60*s,y+67*s),(x+46*s,y+12*s)],fill=(20,27,39,A),outline=(95,112,136,A))
    d.line([(x,y+18*s),(x,y+105*s)],fill=(115,132,154,int(A*.65)),width=max(2,int(3*s)))
    d.arc((x-40*s,y+30*s,x+40*s,y+76*s),190,350,fill=(78,95,118,int(A*.7)),width=max(2,int(3*s)))
    for side in (-1,1):
        sx=x+side*50*s
        p=[(sx-side*5*s,y+10*s),(sx+side*33*s,y+20*s),(sx+side*40*s,y+48*s),(sx+side*9*s,y+55*s),(sx-side*15*s,y+35*s)]
        d.polygon(p,fill=(31,40,54,A),outline=(103,120,144,A))
    hp=[(x-34*s,y-67*s),(x-46*s,y-36*s),(x-40*s,y+5*s),(x-24*s,y+20*s),(x+25*s,y+20*s),(x+41*s,y+4*s),(x+46*s,y-37*s),(x+31*s,y-68*s)]
    d.polygon(hp,fill=(24,31,43,A),outline=(111,128,151,A))
    d.polygon([(x-30*s,y-66*s),(x-10*s,y-84*s),(x-1*s,y-67*s)],fill=(18,24,34,A))
    d.polygon([(x+30*s,y-66*s),(x+10*s,y-84*s),(x+1*s,y-67*s)],fill=(18,24,34,A))
    d.polygon([(x-40*s,y-38*s),(x+40*s,y-38*s),(x+33*s,y-11*s),(x-34*s,y-11*s)],fill=(1,2,4,A))
    d.line([(x-36*s,y-35*s),(x+36*s,y-35*s)],fill=(100,118,143,int(A*.7)),width=max(2,int(3*s)))
    e=Image.new('RGBA',im.size,(0,0,0,0)); ed=ImageDraw.Draw(e)
    ed.polygon([(x-28*s,y-30*s),(x-6*s,y-27*s),(x-11*s,y-21*s),(x-29*s,y-23*s)],fill=(40,175,255,A))
    ed.polygon([(x+28*s,y-30*s),(x+6*s,y-27*s),(x+11*s,y-21*s),(x+29*s,y-23*s)],fill=(40,175,255,A))
    k.alpha_composite(e.filter(ImageFilter.GaussianBlur(7))); k.alpha_composite(e)
    arm=Image.new('RGBA',(180,210),(0,0,0,0)); ad=ImageDraw.Draw(arm)
    ad.rounded_rectangle((72,78,110,160),radius=11,fill=(27,35,48,A),outline=(107,124,148,A),width=3)
    ad.rectangle((65,145,116,169),fill=(20,27,38,A),outline=(94,111,136,A),width=3)
    ad.rectangle((75,27,107,37),fill=(112,126,145,A))
    ad.polygon([(83,-40),(98,-40),(103,28),(78,28)],fill=(186,209,229,A))
    ad.line([(85,-38),(86,26)],fill=(250,255,255,A),width=4)
    ad.line([(98,-38),(100,27)],fill=(34,150,255,int(A*.9)),width=3)
    arm=arm.rotate(-5-100*swing,resample=Image.Resampling.BICUBIC,center=(91,155))
    k.alpha_composite(arm,(int(x-44*s-90),int(y+3*s-155)))
    rim=Image.new('RGBA',im.size,(0,0,0,0)); rd=ImageDraw.Draw(rim)
    rd.line([(x-39*s,y-64*s),(x-52*s,y+76*s)],fill=(25,105,210,int(A*.35)),width=max(3,int(9*s)))
    k.alpha_composite(rim.filter(ImageFilter.GaussianBlur(8))); im.alpha_composite(k)

def slash(im,p):
    l=Image.new('RGBA',im.size,(0,0,0,0)); d=ImageDraw.Draw(l)
    x1,y1=400,32; x2,y2=128,244; q=out(p); ex=x1+(x2-x1)*q; ey=y1+(y2-y1)*q
    for w,c,a in [(34,(0,80,255),55),(20,(0,170,255),115),(9,(185,245,255),235),(3,(255,255,255),255)]: d.line([(x1,y1),(ex,ey)],fill=(*c,a),width=w)
    im.alpha_composite(l.filter(ImageFilter.GaussianBlur(7))); im.alpha_composite(l)

def debris(im,cx,cy,p):
    l=Image.new('RGBA',im.size,(0,0,0,0)); d=ImageDraw.Draw(l)
    for i,(a,dist,size) in enumerate(particles):
        q=clamp((p-(i%9)*.015)/(1-(i%9)*.015))
        if q<=0: continue
        x=cx+math.cos(a)*dist*q; y=cy+math.sin(a)*dist*q+40*q*q; al=int(255*(1-q)); sz=size*(1-.4*q)
        d.ellipse((x-sz,y-sz,x+sz,y+sz),fill=(50,185,255,al))
    im.alpha_composite(l.filter(ImageFilter.GaussianBlur(2))); im.alpha_composite(l)

for f in range(FRAMES):
    im=Image.new('RGBA',(FW,FH),(2,4,9,255)); d=ImageDraw.Draw(im)
    for yy in range(FH):
        c=int(4+12*(1-yy/FH)); d.line((0,yy,FW,yy),fill=(c,c+2,c+10,255))
    fog=Image.new('RGBA',im.size,(0,0,0,0)); fd=ImageDraw.Draw(fog)
    for j in range(6):
        yy=180+j*13+math.sin(f*.22+j)*8; fd.ellipse((-80+j*40,yy-22,600+j*30,yy+35),fill=(30,48,78,14+j*2))
    im.alpha_composite(fog.filter(ImageFilter.GaussianBlur(24)))
    hin=out((f-1)/5); pulse=1+.025*math.sin(f*.8); kp=out((f-4)/9); kx=565-(565-390)*kp
    swing=ease((f-12)/7); crack=ease((f-18)/3); split=out((f-21)/8)*56 if f>=21 else 0
    heart(im,202,143,1.18*hin*pulse,crack,split,int(255*clamp((f+1)/4)))
    knight(im,kx,145,1.0,int(255*kp),swing)
    if 15<=f<=22: slash(im,(f-15)/7)
    if 19<=f<=22:
        strength=math.sin((f-19)/3*math.pi); glow(im,210,145,150,(50,160,255),int(230*strength)); im.alpha_composite(Image.new('RGBA',im.size,(255,255,255,int(210*strength))))
    if f>=21: debris(im,210,145,(f-21)/10)
    vg=Image.new('RGBA',im.size,(0,0,0,0)); vd=ImageDraw.Draw(vg); vd.rectangle((0,0,FW,FH),outline=(0,0,0,150),width=18); im.alpha_composite(vg.filter(ImageFilter.GaussianBlur(18)))
    if f>=28: im.alpha_composite(Image.new('RGBA',im.size,(0,0,0,int(255*ease((f-28)/3)))))
    sheet.alpha_composite(im,((f%COLS)*FW,(f//COLS)*FH))

out='src/main/resources/assets/deathknightscreen/textures/gui/death_knight_realistic.png'
os.makedirs(os.path.dirname(out),exist_ok=True)
sheet.convert('RGB').save(out,optimize=True)
print(out,os.path.getsize(out))
