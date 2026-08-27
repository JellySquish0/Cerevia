import json, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
with open('model_ppg_data_baru.ipynb', 'r', encoding='utf-8') as f:
    nb = json.load(f)
cells = nb['cells']
print(f'Total cells: {len(cells)}')
for i, c in enumerate(cells):
    src = ''.join(c['source'])
    ctype = c['cell_type']
    print(f'\n--- Cell {i} [{ctype}] ---')
    print(src.encode('ascii', errors='replace').decode('ascii'))
