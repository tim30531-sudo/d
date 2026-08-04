from PIL import Image, ImageDraw, ImageFilter
import math
import os
import random
import sys

OUT_DIR = sys.argv[1]
W, H = 768, 432
FRAMES = 80
random.seed(30531)
os.makedirs(OUT_DIR, exist_ok=True)

particles = [(random.uniform(0, math.tau), random.uniform(45, 220), random.uniform(2, 7)) for _ in range(110)]

def clamp(v, a=0.0, b=1.0):
    return max(a, min(b, v))

def smooth(v):
    v = clamp(v)
    return v * v * (3.0 - 2.0 * v)

def out_cubic(v):
    v = clamp(v)
    return 1.0 - (1.0 - v) ** 3

def glow(im, cx, cy, radius, color, alpha, blur=20):
    layer = Image.new("RGBA", im.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    for r in range(int(radius), 0, -7):
        a = int(alpha * (1.0 - r / radius) ** 2)
        draw.ellipse((cx-r, cy-r, cx+r, cy+r), fill=(*color, a))
    im.alpha_composite(layer.filter(ImageFilter.GaussianBlur(blur)))

def heart_points(cx, cy, scale):
    points = []
    for i in range(180):
        angle = math.tau * i / 180.0
        x = 16 * math.sin(angle) ** 3
        y = 13 * math.cos(angle) - 5 * math.cos(2*angle) - 2 * math.cos(3*angle) - math.cos(4*angle)
        points.append((cx + x * 3.05 * scale, cy - y * 3.05 * scale))
    return points

def draw_heart(im, cx, cy, scale, crack, split, alpha):
    pts = heart_points(cx, cy, scale)
    glow(im, cx, cy, 105 * scale, (0, 135, 255), int(alpha * 0.78), 25)
    layer = Image.new("RGBA", im.size, (0,0,0,0))
    d = ImageDraw.Draw(layer)
    if split <= 0:
        d.polygon(pts, fill=(0, 175, 245, alpha), outline=(205, 252, 255, alpha))
        d.line(pts + [pts[0]], fill=(185, 248, 255, alpha), width=max(3, int(5*scale)), joint="curve")
        d.line([(cx-55*scale,cy-55*scale),(cx-25*scale,cy-76*scale),(cx+7*scale,cy-62*scale)],
               fill=(255,255,255,int(alpha*.95)), width=max(4,int(8*scale)))
        if crack > 0:
            c = [(cx+2,cy-68*scale),(cx-9*scale,cy-35*scale),(cx+8*scale,cy-12*scale),
                 (cx-10*scale,cy+18*scale),(cx+8*scale,cy+52*scale),(cx,cy+78*scale)]
            d.line(c, fill=(245,255,255,int(alpha*crack)), width=max(3,int(5*scale)))
            d.line([c[1],(cx-42*scale,cy-13*scale)], fill=(205,245,255,int(alpha*crack)), width=3)
            d.line([c[2],(cx+43*scale,cy+4*scale)], fill=(205,245,255,int(alpha*crack)), width=3)
    else:
        left = [p for p in pts if p[0] <= cx]
        right = [p for p in pts if p[0] >= cx]
        left = sorted(left, key=lambda p:p[1])
        right = sorted(right, key=lambda p:p[1], reverse=True)
        left = [(x-split, y+split*.18) for x,y in left] + [(cx-split,cy+80*scale),(cx-split,cy-68*scale)]
        right = [(x+split, y+split*.26) for x,y in right] + [(cx+split,cy-68*scale),(cx+split,cy+80*scale)]
        d.polygon(left, fill=(0,110,215,alpha), outline=(185,245,255,alpha))
        d.polygon(right, fill=(0,185,255,alpha), outline=(220,255,255,alpha))
    im.alpha_composite(layer.filter(ImageFilter.GaussianBlur(.55)))

def draw_knight(im, x, y, scale, alpha, swing, lean):
    layer = Image.new("RGBA", im.size, (0,0,0,0))
    d = ImageDraw.Draw(layer)
    A = int(alpha)
    d.polygon([(x-84*scale,y+20*scale),(x-124*scale,y+190*scale),(x+100*scale,y+194*scale),(x+82*scale,y+12*scale)],
              fill=(2,4,9,A))
    d.polygon([(x-62*scale,y+15*scale),(x-76*scale,y+105*scale),(x-52*scale,y+170*scale),
               (x+54*scale,y+170*scale),(x+77*scale,y+102*scale),(x+61*scale,y+14*scale)],
              fill=(17,25,38,A), outline=(110,132,160,A))
    for yy in (48,80,112,144):
        d.arc((x-56*scale,y+yy*scale,x+56*scale,y+(yy+45)*scale),190,350,
              fill=(82,105,137,int(A*.72)),width=max(2,int(4*scale)))
    for side in (-1,1):
        sx=x+side*69*scale
        d.polygon([(sx-side*7*scale,y+12*scale),(sx+side*48*scale,y+24*scale),
                   (sx+side*55*scale,y+68*scale),(sx+side*10*scale,y+78*scale),
                   (sx-side*22*scale,y+45*scale)], fill=(28,40,58,A), outline=(125,148,180,A))
    helm=[(x-48*scale,y-103*scale),(x-66*scale,y-58*scale),(x-58*scale,y+9*scale),
          (x-35*scale,y+31*scale),(x+36*scale,y+31*scale),(x+60*scale,y+8*scale),
          (x+66*scale,y-60*scale),(x+47*scale,y-103*scale)]
    d.polygon(helm, fill=(19,29,44,A), outline=(135,158,188,A))
    d.polygon([(x-42*scale,y-101*scale),(x-13*scale,y-128*scale),(x-1*scale,y-102*scale)], fill=(13,20,31,A))
    d.polygon([(x+42*scale,y-101*scale),(x+13*scale,y-128*scale),(x+1*scale,y-102*scale)], fill=(13,20,31,A))
    d.polygon([(x-57*scale,y-58*scale),(x+57*scale,y-58*scale),(x+48*scale,y-17*scale),(x-48*scale,y-17*scale)], fill=(0,1,3,A))
    d.line([(x-52*scale,y-54*scale),(x+52*scale,y-54*scale)], fill=(115,140,173,int(A*.7)), width=max(3,int(5*scale)))
    eyes=Image.new("RGBA",im.size,(0,0,0,0)); ed=ImageDraw.Draw(eyes)
    ed.polygon([(x-40*scale,y-47*scale),(x-7*scale,y-42*scale),(x-15*scale,y-31*scale),(x-42*scale,y-35*scale)], fill=(40,205,255,A))
    ed.polygon([(x+40*scale,y-47*scale),(x+7*scale,y-42*scale),(x+15*scale,y-31*scale),(x+42*scale,y-35*scale)], fill=(40,205,255,A))
    layer.alpha_composite(eyes.filter(ImageFilter.GaussianBlur(11)))
    layer.alpha_composite(eyes)
    arm=Image.new("RGBA",(310,350),(0,0,0,0)); ad=ImageDraw.Draw(arm)
    ad.rounded_rectangle((125,140,185,275),radius=18,fill=(26,39,57,A),outline=(130,153,184,A),width=5)
    ad.rectangle((110,252,202,285),fill=(18,27,41,A),outline=(120,143,174,A),width=5)
    ad.rectangle((127,75,183,91),fill=(145,165,190,A))
    ad.polygon([(139,-70),(170,-70),(181,76),(128,76)],fill=(210,232,248,A))
    ad.line([(143,-66),(139,73)],fill=(255,255,255,A),width=7)
    ad.line([(168,-66),(174,73)],fill=(35,185,255,int(A*.95)),width=6)
    angle = -12 - 122*swing + 5*lean
    arm=arm.rotate(angle,resample=Image.Resampling.BICUBIC,center=(155,270))
    layer.alpha_composite(arm,(int(x-155),int(y-265)))
    rim=Image.new("RGBA",im.size,(0,0,0,0)); rd=ImageDraw.Draw(rim)
    rd.line([(x-52*scale,y-96*scale),(x-75*scale,y+125*scale)],fill=(25,130,255,int(A*.38)),width=max(5,int(13*scale)))
    layer.alpha_composite(rim.filter(ImageFilter.GaussianBlur(12)))
    im.alpha_composite(layer)

def draw_slash(im, progress):
    layer=Image.new("RGBA",im.size,(0,0,0,0)); d=ImageDraw.Draw(layer)
    x1,y1=640,35; x2,y2=135,368
    q=out_cubic(progress)
    ex=x1+(x2-x1)*q; ey=y1+(y2-y1)*q
    for width,color,alpha in [(62,(0,70,255),50),(38,(0,170,255),110),(16,(190,248,255),235),(5,(255,255,255),255)]:
        d.line([(x1,y1),(ex,ey)],fill=(*color,alpha),width=width)
    im.alpha_composite(layer.filter(ImageFilter.GaussianBlur(12)))
    im.alpha_composite(layer)

def debris(im,cx,cy,p):
    layer=Image.new("RGBA",im.size,(0,0,0,0)); d=ImageDraw.Draw(layer)
    for i,(angle,dist,size) in enumerate(particles):
        q=clamp((p-(i%11)*.012)/(1-(i%11)*.012))
        if q <= 0: continue
        x=cx+math.cos(angle)*dist*q
        y=cy+math.sin(angle)*dist*q+70*q*q
        a=int(255*(1-q)); s=size*(1-.35*q)
        d.ellipse((x-s,y-s,x+s,y+s),fill=(55,205,255,a))
    im.alpha_composite(layer.filter(ImageFilter.GaussianBlur(3)))
    im.alpha_composite(layer)

for f in range(FRAMES):
    t=f/(FRAMES-1)
    im=Image.new("RGBA",(W,H),(2,4,10,255))
    d=ImageDraw.Draw(im)
    for yy in range(H):
        ratio=yy/H
        c=int(5+18*(1-ratio))
        d.line((0,yy,W,yy),fill=(c,c+3,c+14,255))
    fog=Image.new("RGBA",im.size,(0,0,0,0)); fd=ImageDraw.Draw(fog)
    for j in range(8):
        yy=275+j*17+math.sin(f*.13+j)*11
        fd.ellipse((-140+j*55,yy-35,900+j*35,yy+50),fill=(30,55,92,16+j*2))
    im.alpha_composite(fog.filter(ImageFilter.GaussianBlur(36)))

    heart_in=out_cubic((t-.02)/.13)
    pulse=1+.022*math.sin(f*.44)
    knight_in=out_cubic((t-.08)/.23)
    knight_x=850-(850-585)*knight_in
    anticipation=smooth((t-.27)/.12)
    swing=smooth((t-.39)/.18)
    crack=smooth((t-.52)/.07)
    split=out_cubic((t-.59)/.25)*88 if t>=.59 else 0

    draw_heart(im,302,216,1.2*heart_in*pulse,crack,split,int(255*clamp((t+.01)/.08)))
    draw_knight(im,knight_x,220,1.0,int(255*knight_in),swing,anticipation)

    if .43 <= t <= .61:
        draw_slash(im,(t-.43)/.18)
    if .50 <= t <= .59:
        strength=math.sin((t-.50)/.09*math.pi)
        glow(im,310,218,220,(50,175,255),int(245*strength),32)
        im.alpha_composite(Image.new("RGBA",im.size,(255,255,255,int(205*strength))))
    if t>=.57:
        debris(im,310,218,(t-.57)/.35)

    vg=Image.new("RGBA",im.size,(0,0,0,0)); vd=ImageDraw.Draw(vg)
    vd.rectangle((0,0,W,H),outline=(0,0,0,175),width=34)
    im.alpha_composite(vg.filter(ImageFilter.GaussianBlur(28)))
    if t>=.88:
        im.alpha_composite(Image.new("RGBA",im.size,(0,0,0,int(255*smooth((t-.88)/.12)))))

    im.convert("P",palette=Image.Palette.ADAPTIVE,colors=256).save(
        os.path.join(OUT_DIR,f"frame_{f:03d}.png"), optimize=True)

print(f"Generated {FRAMES} frames in {OUT_DIR}")
