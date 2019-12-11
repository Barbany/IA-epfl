import numpy as np
import matplotlib.pyplot as plt
import argparse
import ast

parser = argparse.ArgumentParser()
parser.add_argument('fname')
args = parser.parse_args()


costs = []
cost_Opp = []
bids = []
names = []

plt.rcParams.update({'font.size':16})

with open(args.fname, 'r') as f:
    lines = f.readlines()
    i = 0
    
    while i < len(lines):
        line = lines[i]
        if 'Minimum cost is' in line:
            costs.append(int(line.split('CONFIG | Ozuna :')[1].split(':')[1].rstrip()))
            #i = i + 1
            #line = lines[i]
        
        elif 'Min cost opponent' in line:
            # Bid of first team
            cost_Opp.append(int(line.split('CONFIG | Ozuna :')[1].split(':')[1].split('margin')[0]))
                
        line = lines[i]
        i = i + 1

plt.plot(costs)
plt.plot(cost_Opp)
plt.show()

