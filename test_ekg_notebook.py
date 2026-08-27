import os, gc, glob, json
import numpy as np
import h5py
import tensorflow as tf
from tensorflow.keras import layers, Model
from sklearn.model_selection import train_test_split

DATA_DIR  = os.path.join(os.getcwd(), 'Data_new')
MODEL_DIR = os.path.join(os.getcwd(), 'Model')
os.makedirs(MODEL_DIR, exist_ok=True)

WINDOW_SIZE = 30
FS_ECG = 500
N_KELAS = 2

def decode_h5_string(f, key):
    raw = f[key][()].flatten()
    return ''.join(chr(int(c)) for c in raw)

# ---- CEK 1: Baca file ECG pertama ----
ecg_files = sorted(glob.glob(os.path.join(DATA_DIR, '*_ECG_01.mat')))
print(f'File ECG ditemukan: {len(ecg_files)}')
assert len(ecg_files) > 0, "Tidak ada file ECG!"

fp = ecg_files[0]
subj = os.path.basename(fp)[:2]
print(f'\nMembaca subjek {subj}: {fp}')

with h5py.File(fp, 'r') as f:
    af = f['AF_annotation'][()].flatten().astype(np.float32)
    rr = f['rr'][()].flatten().astype(np.float32)

print(f'  rr shape: {rr.shape}')
print(f'  af shape: {af.shape}')
print(f'  rr stats: min={rr.min():.3f} max={rr.max():.1f} mean={rr.mean():.3f}')
print(f'  af unique: {np.unique(af)}')

# ---- CEK 2: Preprocessing ----
rr_clean = rr.copy()
rr_clean[(rr < 0.25) | (rr > 2.5)] = np.nan
n_gap = np.sum(np.isnan(rr_clean))
print(f'\n  RR gap (outlier): {n_gap:,} dari {len(rr):,} ({100*n_gap/len(rr):.1f}%)')

af_int = np.full(len(af), -1, dtype=np.int32)
af_int[af == 0.0] = 0
af_int[af == 1.0] = 1
print(f'  NSR={np.sum(af==0.0):,} AFib={np.sum(af==1.0):,} Trans={np.sum(af==0.5):,}')

rr_diff = np.diff(rr_clean)
af_cut  = af_int[1:]

X_list, y_list = [], []
for i in range(WINDOW_SIZE, len(rr_diff)):
    window = rr_diff[i - WINDOW_SIZE: i]
    label  = af_cut[i - 1]
    if np.any(np.isnan(window)): continue
    if label == -1:              continue
    if np.any(np.abs(window) > 1.0): continue
    X_list.append(window.astype(np.float32))
    y_list.append(label)

print(f'\n  Windows valid: {len(X_list):,}')
print(f'  NSR windows : {sum(1 for l in y_list if l == 0):,}')
print(f'  AFib windows: {sum(1 for l in y_list if l == 1):,}')

# ---- CEK 3: Build model ----
class ClassToken(layers.Layer):
    def build(self, input_shape):
        self.cls = self.add_weight('cls_token',
            shape=(1, 1, input_shape[-1]),
            initializer='random_normal', trainable=True)
    def call(self, x):
        b = tf.shape(x)[0]
        return tf.concat([tf.broadcast_to(self.cls, [b, 1, x.shape[-1]]), x], axis=1)

def transformer_block(x, head_size=64, num_heads=4, ff_dim=128, drop=0.2):
    a = layers.LayerNormalization(1e-6)(x)
    a = layers.MultiHeadAttention(num_heads, head_size, dropout=drop)(a, a)
    a = layers.Dropout(drop)(a); r = a + x
    b = layers.LayerNormalization(1e-6)(r)
    b = layers.Dense(ff_dim, 'relu')(b)
    b = layers.Dropout(drop)(b)
    b = layers.Dense(x.shape[-1])(b)
    return b + r

def bangun_model(input_shape=(WINDOW_SIZE, 1), n_kelas=N_KELAS,
                 filters=50, kernel=4, head_size=64,
                 num_heads=4, ff_dim=128, n_blocks=2, drop=0.2):
    inp = layers.Input(shape=input_shape, name='input_rr_diff')
    x   = layers.Conv1D(filters, kernel, padding='causal',
                        activation='relu', name='cnn_lokal_1')(inp)
    x   = layers.BatchNormalization()(x)
    seq = x.shape[1]
    pos = layers.Embedding(seq, filters, name='pos_embedding')(tf.range(seq))
    x   = x + pos
    x   = ClassToken(name='cls_token_layer')(x)
    for _ in range(n_blocks):
        x = transformer_block(x, head_size, num_heads, ff_dim, drop)
    x   = x[:, 0, :]
    x   = layers.Dense(64, 'relu')(x)
    x   = layers.Dropout(drop)(x)
    out = layers.Dense(n_kelas, 'softmax', name='output')(x)
    return Model(inp, out, name='EKG_Test')

print('\nMembangun model...')
try:
    m = bangun_model()
    dummy = np.zeros((4, WINDOW_SIZE, 1), dtype=np.float32)
    out = m.predict(dummy, verbose=0)
    print(f'Model OK! Output shape: {out.shape}')
    m.summary(line_length=80)
except Exception as e:
    print(f'ERROR build model: {e}')

print('\n=== SEMUA CEK SELESAI ===')
