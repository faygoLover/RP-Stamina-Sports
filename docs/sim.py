# Модель тренировок: зоны (100% свежая), стадии, восстановление 1 ч, отдых сидя между подходами.
import sys
EX = {
 # rep_s, stamina/rep, str/rep, fit/rep, {zone: %/rep}
 'bench':    (5, 35, 6, 0,   {'chest':0.55,'arms':0.45}),
 'pullup':   (4, 40, 3, 2,   {'arms':0.45,'chest':0.45}),
 'squat':    (4, 18, 0.7,1.7,{'legs':0.45,'abs':0.1}),
 'abs':      (6, 15, 1, 2,   {'abs':0.7}),
 'tread':    (20,300, 0, 20, {'legs':1.2}),
 'bag':      (1.5,12,0.25,1, {'arms':0.1,'abs':0.06}),
 'pushup':   (3, 20, 1.25,0.5,{'arms':0.35,'chest':0.35}),
}
def fm(F): return 1.5-0.1*F if F<=5 else 1.0-0.08*(F-5)
def run(F, order, hours=20):
    k=1+0.06*(F-4); M=1000*k; regen=6*k  # сидя
    z={n:100.0 for n in ['chest','arms','legs','abs']}; flag={n:0 for n in z}
    t=0; S=0; Fi=0; T=hours*3600; rec=100/3600
    def tick(dt, working=()):
        for n in z:
            if n not in working:
                z[n]=min(100,z[n]+rec*dt)
                if z[n]>=100: flag[n]=0
    while t<T:
        ex=None
        for e in order:
            if all(flag[n]==0 for n in EX[e][4]): ex=e;break
        if ex is None:
            tick(60); t+=60; continue
        rs,sc,xs,xf,zs=EX[ex]; st=M
        while st>0.25*M and all(flag[n]==0 for n in zs):
            st-=sc; t+=rs
            for n,c in zs.items():
                z[n]-=c*fm(F)
                if z[n]<30: flag[n]=1
            tick(rs,zs)
            S+=xs; Fi+=xf
        rest=(M-st)/regen+6; tick(rest); t+=rest
    return S/hours, Fi/hours
strf=['bench','pullup','pushup','squat','abs','bag','tread']
fitf=['tread','bag','squat','abs','pullup','bench','pushup']
for F in (4,7,10):
    print(F,'str-focus',[round(x) for x in run(F,strf)],'fit-focus',[round(x) for x in run(F,fitf)])

def cost(L): return 150*1.6**L*10
rate={}
for F in range(0,11):
    rate[F]=(run(F,strf,10),run(F,fitf,10))
def prog(mode, bonus=1.0):
    S,Fl=4,4; xs=xf=0; h=0; out=[]
    while (S<10 or Fl<10) and h<2000:
        if mode=='str': foc='s' if S<10 else 'f'
        elif mode=='fit': foc='f' if Fl<10 else 's'
        else: foc='s' if (S<=Fl and S<10) or Fl>=10 else 'f'
        (rs,rf)=rate[Fl][0] if foc=='s' else rate[Fl][1]
        dt=0.1; h+=dt
        if S<10: xs+=rs*bonus*dt
        if Fl<10: xf+=rf*bonus*dt
        while S<10 and xs>=cost(S): xs-=cost(S); S+=1; out.append((round(h,1),'S',S,Fl))
        while Fl<10 and xf>=cost(Fl): xf-=cost(Fl); Fl+=1; out.append((round(h,1),'F',S,Fl))
    return out
for mode in ('str','fit','bal'):
    print(mode, prog(mode))
print('bal+20%', prog('bal',1.2))
