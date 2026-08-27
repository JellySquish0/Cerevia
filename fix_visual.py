import json

def fix_visual_cell(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        nb = json.load(f)

    for cell in nb['cells']:
        if cell['cell_type'] == 'code':
            # Identify the plotting cell
            if "ax3.plot(" in "".join(cell['source']) or "sample_nsr" in "".join(cell['source']):
                new_source = []
                for line in cell['source']:
                    # Fix Plot 3 (Perbandingan)
                    if "sample_nsr  = df_ltaf[df_ltaf['Label_Teks'] == 'NSR']['Fitur_BPM'].values[:200]" in line:
                        new_source.append("    # Ambil 1 jendela (30 detak) pertama lalu ubah string ke array float\n")
                        new_source.append("    nsr_str = df_ltaf[df_ltaf['Label_Teks'] == 'NSR']['Fitur_BPM'].values[0]\n")
                        new_source.append("    sample_nsr = [float(x) for x in nsr_str.split(',')]\n")
                        continue
                    if "sample_afib = df_ltaf[df_ltaf['Label_Teks'] == 'AFib']['Fitur_BPM'].values[:200]" in line:
                        new_source.append("    afib_str = df_ltaf[df_ltaf['Label_Teks'] == 'AFib']['Fitur_BPM'].values[0]\n")
                        new_source.append("    sample_afib = [float(x) for x in afib_str.split(',')]\n")
                        continue
                    
                    if "ax3.set_title('?? Selisih BPM: NSR vs AFib (200 detak)', fontweight='bold')" in line:
                        new_source.append("    ax3.set_title('?? Selisih BPM: NSR vs AFib (1 Jendela / 30 Detak)', fontweight='bold')\n")
                        continue
                        
                    # Fix Plot 4 (Tabel Preview) - Truncate the long string
                    if "contoh_baris.append(subset.iloc[0])" in line:
                        new_source.append("            row1 = subset.iloc[0].copy()\n")
                        new_source.append("            row1['Fitur_BPM'] = str(row1['Fitur_BPM'])[:15] + '...'\n")
                        new_source.append("            contoh_baris.append(row1)\n")
                        continue
                    if "contoh_baris.append(subset.iloc[1])" in line:
                        new_source.append("            row2 = subset.iloc[1].copy()\n")
                        new_source.append("            row2['Fitur_BPM'] = str(row2['Fitur_BPM'])[:15] + '...'\n")
                        new_source.append("            contoh_baris.append(row2)\n")
                        continue
                        
                    new_source.append(line)
                cell['source'] = new_source

    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(nb, f, indent=1, ensure_ascii=False)

fix_visual_cell('akuisisi_ekg_baru.ipynb')
print("Visual code fixed successfully!")
