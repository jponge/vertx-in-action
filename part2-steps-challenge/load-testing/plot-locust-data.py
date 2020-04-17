#!/usr/bin/env python
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

import argparse

parser = argparse.ArgumentParser(description="Plot some data from Hey csv output")
parser.add_argument("title", help="Plot title")
parser.add_argument("input", help="Data file in CSV format")
parser.add_argument("output", help="Output file name")
parser.add_argument("dpi", help="DPI resolution", type=int)
args = parser.parse_args()

data = pd.read_csv(args.input)
data = data.set_index("Name")

ax = data.plot.barh(y=["95%", "98%", "99%", "100%"], stacked=True, color=["tab:green", "tab:blue", "tab:orange", "tab:red"])
ax.set_xlabel("Latency (ms)")
ax.set_ylabel("Name")

plt.title = args.title

plt.tight_layout()
plt.savefig(f"{args.output}", dpi=args.dpi)
