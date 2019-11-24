import numpy as np
import matplotlib.pyplot as plt
import argparse
import ast

parser = argparse.ArgumentParser()
parser.add_argument('fname')
args = parser.parse_args()


costs = []
bids = []
names = []

plt.rcParams.update({'font.size':16})

with open(args.fname, 'r') as f:
    lines = f.readlines()
    
    i = 0
    
    while i < len(lines):
        line = lines[i]
        if 'BID HISTORIC' in line:
            # Bid of first team
            names.append(line.split('CONFIG | ')[1].split(' BID HISTORIC')[0])
            bids.append(list(ast.literal_eval(line.split(' BID HISTORIC: ')[1].split(' ;')[0])))
            i = i + 1
            line = lines[i]
            costs.append(list(ast.literal_eval(line.split(' COST HISTORIC: ')[1].split(' ;')[0])))
            
        i = i + 1

n = len(costs[0])
n_exp = len(costs) // 2

x = np.arange(n)

for exp in range(n_exp):
    cost_1 = np.asarray(costs[2 * exp])
    cost_2 = np.asarray(costs[2 * exp + 1])

    bid_1 = np.asarray(bids[2 * exp])
    bid_2 = np.asarray(bids[2 * exp + 1])

    cum_gain_1 = np.zeros(n)
    cum_gain_2 = np.zeros(n)
    for i in range(n):
        if bid_1[i] < bid_2[i]:
            if i == 0:
                cum_gain_1[i] = bid_1[i] - cost_1[i]
                cum_gain_2[i] = 0
            else:
                cum_gain_1[i] = cum_gain_1[i - 1] + bid_1[i] - cost_1[i]
                cum_gain_2[i] = cum_gain_2[i-1]
        else:
            if i == 0:
                cum_gain_2[i] = bid_2[i] - cost_2[i]
                cum_gain_1[i] = 0
            else:
                cum_gain_2[i] = cum_gain_2[i - 1] + bid_2[i] - cost_2[i]
                cum_gain_1[i] = cum_gain_1[i-1]

    p1 = plt.bar(x, bid_1 - cost_1)
    p2 = plt.bar(x, bid_2 - cost_2, alpha=0.5)

    plt.plot(x, cum_gain_1)
    plt.plot(x, cum_gain_2)

    plt.ylabel('Gain = Bid - Cost')
    plt.title('Tournament with ' + str(n) + ' tasks')
    plt.legend((p1[0], p2[0]), (names[2 * exp], names[2 * exp + 1]))

    plt.show()

